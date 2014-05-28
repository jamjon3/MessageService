package edu.usf.cims.MessageService

import org.codehaus.groovy.grails.commons.GrailsApplication
import java.text.SimpleDateFormat
import groovy.time.*
import grails.converters.*

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
      def topicCounts = [:]
      Topic.findAll().each { topic ->
        topicCounts[topic.name] = countMessages('topic',topic.name, null)
      }
      return topicCounts
    }

    def countMessages(containerType = null, containerName = null, status = null){
       def searchParams = getSearchParams(containerType, containerName, status)
       return Message.collection.count(searchParams)
    }

    def getQueueCounts(name, statusType = null){
      if (! name) return getAllQueueCounts()
      def queueCounts = [:]
      queueCounts.messages = countMessages('queue', name, statusType)

      if (! statusType) {
        queueCounts.status = [:]
        statusList.each {
          queueCounts.status[it] = countMessages('queue', name, it)
        }
      }

      return queueCounts
    }

    def getTopicCounts(name){
      if (! name) return getAllTopicCounts()
      def topicCounts = [:]
      topicCounts.messages = countMessages('topic', name, null)
      return topicCounts
    }

    def countAllMessages(){
      def results = [
                      cols:[
                          [id:"type", label:"Container Type", type:"string"],
                          [id:"msg", label:"Messages", type:"number"]],
                      rows:[
                        [c:
                          [[v:"Queue"],[v:0]]],
                        [c:
                          [[v:"Topic"],[v:0]]]
                      ]
                    ]

      getAllQueueCounts().each { name, queueStats ->
        results.rows[0].c[1].v += queueStats.messages
      }
      getAllTopicCounts().each { name, value ->
        results.rows[1].c[1].v += value
      }

      return results
    }

    def getMessageByAge(String containerType, String containerName, String status, Integer sortType){
      def searchParams = getSearchParams(containerType, containerName, status)

      def result = Message.collection.find(searchParams).sort(createTime: sortType).limit(1) as Message

      def resultMap = [messageId:'none', messageType:'none', container:'none', createTime: 0, age: null]
      if (result){
        resultMap.messageId = result.render().id
        resultMap.messageType = result.render().messageDetails.messageContainer.type
        resultMap.container = result.render().messageDetails.messageContainer.name
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


    def getAggregateMessageCount(containerType, timeScale, startTime, endTime){
      return getAggregateMessageCount(containerType, null, timeScale, startTime, endTime)
    }

    def getAggregateMessageCount(containerType, containerName, timeScale, startTime, endTime){
       def searchParams = [  auditTime : [ $gte : startTime, $lt : endTime ] ]
        if(containerType) searchParams.containerType = containerType
        if(containerName) searchParams.containerName = containerName

       def groupParams = getGroupParams(['a': '$action'], timeScale)

       def rawResult = AuditEntry.collection.aggregate(
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
          ],
          [$sort: ['_id': 1]]
        )

       def results = [
                      cols:[
                          [id:"a", label:"Time", type:"datetime"],
                          [id:"b", label:"Viewed", type:"number"],
                          [id:"c", label:"Written", type:"number"],
                          [id:"d", label:"Deleted", type:"number"]],
                      rows:[]
                    ]

        def data = [:]

        //Loop through the results from mongo and consolidate them into a single line per time period
        rawResult?.commandResult?.result.each {
          def date = getDateFromMongoData(timeScale, it._id)

          if (! data."${date}") data."${date}" = [date:date, view:0, create:0, delete:0]
          if (it._id.a == 'CREATE_MESSAGE') data."${date}".create = it.count
          if (it._id.a == 'VIEW_MESSAGE') data."${date}".view = it.count
          if (it._id.a == 'DELETE_MESSAGE') data."${date}".view = it.count
        }

        data.each { key,value ->
          results.rows.add([c:[[v: value.date],[v: value.view],[v: value.create],[v: value.delete]]])
        }

       return results
     }

    def getAggregateDataTransfer(containerType, timeScale, startTime, endTime){
      return getAggregateDataTransfer(containerType, null, timeScale, startTime, endTime)
    }

    def getAggregateDataTransfer(containerType, containerName, timeScale, startTime, endTime){
       def searchParams = [ auditTime : [ $gte : startTime, $lt : endTime ] ]
        if(containerType) searchParams.containerType = containerType
        if(containerName) searchParams.containerName = containerName

       def groupParams = getGroupParams(['a': '$action'], timeScale)

       def rawResult = AuditEntry.collection.aggregate(
          [$match: searchParams ],
          [$project:  ['action' : 1,
                       'dataTransfer' : '$details.messageSize',
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
                      'bytesTransferred' : [ $sum : '$dataTransfer' ],
                      'count' : [ $sum : 1 ]
                    ]
          ]
        )

        def results = [
                      cols:[
                          [id:"a", label:"Time", type:"datetime"],
                          [id:"b", label:"Bytes Transferred", type:"number"]],
                      rows:[]
                    ]

        //Loop through the results from mongo and add them to the Google Charts object
        rawResult?.commandResult?.result.each {
          def date = getDateFromMongoData(timeScale, it._id)
          results.rows.add([c:[[v: date],[v: it.bytesTransferred]]])
        }

       return results
     }

      def getAverageMessageSize(containerType = null, containerName = null, status = null){
       def searchParams = getSearchParams(containerType, containerName, status)

      // Define a Map-Reduce function to get the average size
      def result = Message.collection.mapReduce(
        """
          function mapFunction() {
            var key = "AvgMesgSize"
            var value = {
              totalSize: Object.bsonsize(this),
              count: 1,
              avgBytes: 0
            };
            emit(key, value);
          };
        """,
        """
          function reduce(key, values) {
            var reducedObject = {
              totalSize: 0,
              count: 0,
              avgSize: 0
            };

            values.forEach( function(value) {
              reducedObject.totalSize += value.totalSize;
              reducedObject.count += value.count;
              if(reducedObject.totalSize > 0) reducedObject.avgBytes = reducedObject.totalSize / reducedObject.count
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

        if (result?.commandResult?.results[0]?.value?.avgBytes){
          return result.commandResult.results[0].value.avgBytes as Integer
        } else {
          return
        }
    }

/**
 Helper Methods
**/
    /**
    * Returns a map that can be used as search parameters for a MongoDB query
    **/
    private def getSearchParams(containerType, containerName, status){
      def searchParams = [:]
      def messageContainer

      if(status) searchParams.putAt('status', status)
      if(containerName) searchParams.putAt('messageContainer.name', containerName)
      if(containerType) searchParams.putAt('messageContainer.type', containerType)

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

    private def getDateFromMongoData(timeScale, data){
       def params = [:]

        switch(timeScale) {
          case 'year':
            params = [year: data.y, month: 0, day: 0, hour: 0, minute: 0, second: 0]
          break
          case 'month':
            params = [year: data.y, month: data.m - 1, day: 0, hour: 0, minute: 0, second: 0]
          break
          case 'day':
            params = [year: data.y, month: data.m - 1, day: data.d, hour: 0, minute: 0, second: 0]
          break
          case 'hour':
            params = [year: data.y, month: data.m - 1, day: data.d, hour: data.h, minute: 0, second: 0]
          break
          case 'minute':
            params = [year: data.y, month: data.m - 1, day: data.d, hour: data.h, minute: data.min, second: 0]
          break
          case 'second':
            params = [year: data.y, month: data.m - 1, day: data.d, hour: data.h, minute: data.min, second: data.sec]
          break

        }
        return  "Date(${params.year}, ${params.month}, ${params.day}, ${params.hour}, ${params.minute}, ${params.second})"
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
