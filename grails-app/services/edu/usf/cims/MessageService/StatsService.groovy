package edu.usf.cims.MessageService

import org.codehaus.groovy.grails.commons.GrailsApplication
import java.text.SimpleDateFormat
import groovy.time.*
import grails.converters.*
import org.joda.time.*
import java.util.concurrent.TimeUnit

import com.mongodb.MapReduceCommand.OutputType

class StatsService {
    def mongo
    def grailsApplication
    static transactional = true

    //These are the statuses we want to track
    def statusList = ['pending','in-progress','error']

    def getAllQueueCounts(){
      def queueCounts = [:]
      Queue.findAll().each { queue ->
        queueCounts[queue.name] = [:]
          queueCounts[queue.name] = getQueueCounts(queue.name)        
      }
     
      return queueCounts
    }

    def getAllTopicCounts(){
      def messageTotals = [:]
      Topic.findAll().each { topic ->
        messageTotals[topic.name] = countMessages('topic', topic.name)
      }
      return [messages:messageTotals]
    }
    
    def countMessages(containerType = null, containerName = null, status = null){
       def searchParams = getSearchParams(containerType, containerName, status)
       return Message.collection.count(searchParams)
    }

    def getQueueCounts(name, status = null){
      def queueCounts = [:]
      queueCounts.messages = countMessages('queue', name, status)
      
      if (! status) {
        queueCounts.status = [:]
        statusList.each { myStatus ->
          queueCounts.status[myStatus] = countMessages('queue', name, myStatus)
        }
      }

      return queueCounts
    }
    
    def getTopicCounts(name){
      def topicCounts = [:]
      topicCounts.messages = countMessages('topic', name)
      return topicCounts
    }

    def countAllMessages(){
      def results = [topic:0, queue:0]

      getAllQueueCounts().each { name, queueStats ->
        results.queue += queueStats.messages
      }
      getAllTopicCounts().messages.each { name, topicStats ->
        results.topic += topicStats.value
      }

      return results
    }

    def getMessageByAge(String containerType, String containerName, String status, Integer sortType){
      def searchParams = getSearchParams(containerType, containerName, status)

      def result = Message.collection.find(searchParams).sort(createTime: sortType).limit(1) as Message

      def resultMap = [messageId:'none', messageDetails:'No messages found!', createTime: 0, age: null]
      if (result){
        resultMap.messageId = result.render().id
        resultMap.messageDetails = result.render().messageDetails
        resultMap.createTime = result.render().createTime
        resultMap.age = TimeCategory.minus( new Date(), result.createTime ) as String
      }
      return resultMap
    }

    def getAverageMessageAge(containerType = null, containerName = null, status = null){
       def searchParams = getSearchParams(containerType, containerName, status)

      // Define a Map-Reduce function to get the average age
      def result = Message.collection.mapReduce(
        """
          function mapFunction() {
            var key = "AvgMesgAge"
            var value = {
              totalAge: new Date() - new Date(this.createTime),
              count: 1,
              avgAge: 0
            };
            emit(key, value);
          };
        """,
        """
          function reduce(key, values) {
            var reducedObject = {
              totalAge: 0,
              count: 0,
              avgAge: 0
            };

            values.forEach( function(value) {
              reducedObject.totalAge += value.totalAge;
              reducedObject.count += value.count;
              if(reducedObject.totalAge > 0) reducedObject.avgAge = reducedObject.totalAge / reducedObject.count
              }
            );
            return reducedObject;
          };
        """,
        //Name of collection to write results to 
        "",
        //Return result instead of writing it to a collection
        OutputType.INLINE,  
        //Query that defines the input to the Map Reduce Function
        searchParams
        )

        /*
        * Since we're using OutputType.INLINE, this isn't needed.  Leaving it for reference.
        *
        * Connect to MongoDB and get the result set.  Since this isn't a domain
        * object, we have to create a new connection
        * def db = mongo.getDB(grailsApplication.config.grails.mongo.databaseName)
        * def result = db.averageMessageAge.find()
        */

        if (result?.commandResult?.results[0]?.value?.avgAge){
          return convertToTimeDuration(result.commandResult.results[0].value.avgAge as Long) as String
        } else {
          return
        }
    }

    def getMessagesPerMinute(containerType, actionType, timeScale, startTime, endTime){
      return getMessagesPerMinute(containerType, null, actionType, timeScale, startTime, endTime)
    }
    
    def getMessagesPerMinute(containerType, containerName, actionType, timeScale, startTime, endTime){
       def searchParams = [ action: actionType,
                            auditTime : [ $gte : startTime, $lt : endTime ]
                          ]
        if(containerType) searchParams.containerType = containerType
        if(containerName) searchParams.containerName = containerName              

       def groupParams = getGroupParams(['a': '$action'], timeScale)

       def result = AuditEntry.collection.aggregate(
          [$match: searchParams ],
          [$project:  ['action' : 1,
                      'datetime':[
                                    'y': ['$year' : '$auditTime'],
                                    'm': ['$month' : '$auditTime'],
                                    'd': ['$dayOfMonth' : '$auditTime'],
                                    'h': ['$hour' : '$auditTime'],
                                    'min': ['$minute' : '$auditTime'],
                                    'sec': ['$second' : '$auditTime']
                                    ]
                      ]
          ],
          [$group:  ['_id': groupParams,
                      'count' : [ $sum : 1 ]
                    ]
          ]
        )

       return result?.commandResult?.result
     }


/**
 Helper Methods
**/
    /**
    * Returns a map that can be used as search parameters for a MongoDB query
    **/
    private def getSearchParams(containerType, containerName, status){
      def searchParams
      def messageContainer

      if (containerName) {
        if(containerType == 'topic') {
          messageContainer = Topic.findByName(containerName)
          if (!messageContainer) return 'ContainerNotFound'
        } else if (containerType == 'queue'){
          messageContainer = Queue.findByName(containerName)
          if (!messageContainer) return 'ContainerNotFound'        
        }
      }

      if (status && containerName && messageContainer){
        searchParams = ["messageContainer.name": messageContainer.name, "messageContainer.type": containerType, status: status]
      } else if (containerName && messageContainer){
        searchParams = ["messageContainer.name": messageContainer.name, "messageContainer.type": containerType]
      } else if (status && messageContainer){
        searchParams = ["messageContainer.type": containerType, status: status]
      } else if (containerType){
        searchParams = ["messageContainer.type": containerType]
      } else if (status){
        searchParams = [status: status]
      } 

      return searchParams
    }

    private def getGroupParams(groupParams, timeScale){
        
        def params = [:]

        switch(timeScale) {
          case 'year':
            params = ['y': '$datetime.y']
          break
          case 'month':
            params = ['y': '$datetime.y','m': '$datetime.m']
          break
          case 'day':
            params = ['y': '$datetime.y','m': '$datetime.m','d': '$datetime.d']
          break
          case 'hour':
            params = ['y': '$datetime.y','m': '$datetime.m','d': '$datetime.d','h': '$datetime.h']
          break
          case 'minute':
            params = ['y': '$datetime.y','m': '$datetime.m','d': '$datetime.d','h': '$datetime.h', 'min': '$datetime.min']
          break
          case 'second':
            params = ['y': '$datetime.y','m': '$datetime.m','d': '$datetime.d','h': '$datetime.h', 'min': '$datetime.min', 'sec': '$datetime.sec']
          break
          
        }
        return groupParams.plus(params)
    }

    private def convertToTimeDuration(input){

      def days = input / 86400000 as int
      def remainder = input - days * 86400000
      def hours = remainder / 3600000 as int
      remainder = remainder - hours * 3600000
      def mins = remainder / 60000 as int
      remainder = remainder - mins * 60000
      def secs = remainder / 1000 as int
      remainder = remainder - secs * 1000
      def mils = remainder as int

      new TimeDuration(days, hours, mins, secs, mils)
    }

}
