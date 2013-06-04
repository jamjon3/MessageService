package edu.usf.cims.MessageService

import org.codehaus.groovy.grails.commons.GrailsApplication
import java.text.SimpleDateFormat
import groovy.time.*
import grails.converters.*
import org.joda.time.*
import java.util.concurrent.TimeUnit

class StatsService {
    def mongo
    def grailsApplication
    static transactional = true

    //These are the statuses we want to track
    def statusList = ['pending','in-progress','error']

    def getAllQueueCounts(){
      def messageTotals = [:]
      def statusTotals = [:]
  
      Queue.findAll().each { queue ->      
        messageTotals[queue.name] = queue.render().stats.messages
        statusList.each { status ->
          if(! statusTotals[status]) statusTotals[status] = 0
          statusTotals[status] += queue.stats[status]
        }
      }
      return [messages: messageTotals, status: statusTotals]
    }

    
    def getAllTopicCounts(name = null){
      def messageTotals = [:]
      Topic.findAll().each { topic -> 
        messageTotals[topic.name] = topic.render().stats.messages
      }
      return [messages:messageTotals]
    }
    
    def getQueueCounts(name){
      def queue = Queue.collection.find([name: name]).limit(1) as Queue
      return queue.render().stats
    }
    
    def getTopicCounts(name = null){
      def topic = Topic.collection.find([name: name]).limit(1) as Topic
      return topic.render().stats
    }

    def countAllMessages(){
      def results = [topic:0, queue:0]

      getAllQueueCounts().messages.each { key, value ->
        results.queue += value
      }
      getAllTopicCounts().messages.each { key, value ->
        results.topic += value
      }

      return results
    }

    def getNewestQueueMessage(container = null, status = null){
      return getMessageByAge('queue', container, status, -1)
    }

    def getNewestTopicMessage(container = null){
      return getMessageByAge('topic', container, null, -1)
    }

    def getOldestQueueMessage(container = null, status = null){
      return getMessageByAge('queue', container, status, 1)
    }

    def getOldestTopicMessage(container = null){
      return getMessageByAge('topic', container, null, 1)
    }

    def getOldestMessage(status = null){
      return getMessageByAge(null, null, status, 1)
    }

    def getNewestMessage(status = null){
      return getMessageByAge(null, null, status, -1)
    }

    private def getMessageByAge(String containerType, String containerName, String status, Integer sortType){
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
      def collectStats = Message.collection.mapReduce(
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
        //Collection to store the results in
        "averageMessageAge",  
        //Query that defines the input to the Map Reduce Function
        searchParams
        )
      
        /*
        * Connect to MongoDB and get the result set.  Since this isn't a domain
        * object, we have to create a new connection */ 
        def db = mongo.getDB(grailsApplication.config.grails.mongo.databaseName)
        def result = db.averageMessageAge.find()

        if (result){
          return convertToTimeDuration(result[0].value.avgAge as Long) as String
        } else {
          return
        }
    }


    /**

**/
    def listStats(tagFilter = null) {
        def logFile = grailsApplication.config.statsLogFile
        return new File(logFile).withReader { inr ->
            def messageCalls = []
            def newMessage = [:]
            def tags = (tagFilter)?[ tagFilter ]:[
                "createQueueMessage",
                "createTopicMessage",
                "getNextMessage",
                "listInProgressMessages",
                "listTopicMessages",
                "peek",
                "viewMessage"
            ]
            inr.eachLine{line-> 
                if(line.startsWith("Performance")) {
                    def perfLine = line.tokenize()
                    newMessage.serviceCallDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(perfLine[2] + ' ' + perfLine[3])
                } else if(line.startsWith("Tag")) {
                    // Skip
                } else if(tags.find { tag -> line.startsWith(tag) }) {
                    def stats = line.tokenize()
                    newMessage.tag = stats[0]                    
                    newMessage.average = stats[1]
                    newMessage.minimum = stats[2]
                    newMessage.maximum = stats[3]
                    newMessage.standardDeviation = stats[4]                    
                } else if( !(line.trim()) ) {
                    if(newMessage.tag) {
                        messageCalls.add(newMessage.clone())
                    }
                    newMessage.clear()
                }
            }
            return messageCalls
        }.sort { it.serviceCallDate }
    }
    
    def getRunningStats() {
        return { mc ->
            return { rs ->
                rs.messagesPerMin = rs.queueMessagesPerMin + rs.topicMessagesPerMin
                rs.retrievedMessagesPerMin = rs.retrievedQueueMessagesPerMin + rs.retrievedTopicMessagesPerMin
                rs.oldestMessage = (rs.oldestMessageInQueue)?
                    (
                        (rs.oldestMessageInTopic)?
                            ( 
                                (rs.oldestMessageInQueue < rs.oldestMessageInTopic)?
                                    rs.oldestMessageInQueue
                                    :rs.oldestMessageInTopic
                            )
                            :rs.oldestMessageInQueue
                    ):rs.oldestMessageInTopic
                rs.newestMessage = (rs.newestMessageInQueue)?
                    (
                        (rs.newestMessageInTopic)?
                            ( 
                                (rs.newestMessageInQueue > rs.newestMessageInTopic)?
                                    rs.newestMessageInQueue
                                    :rs.newestMessageInTopic
                            )
                            :rs.newestMessageInQueue
                    ):rs.newestMessageInTopic
                long secondInMillis = 1000
                long minuteInMillis = secondInMillis * 60
                long hourInMillis = minuteInMillis * 60
                long dayInMillis = hourInMillis * 24
                long yearInMillis = dayInMillis * 365      
                def qmAgesInMS = QueueMessage.withCriteria {
                    projections {
                        groupProperty("createTime")
                    }
                }.collect { new Date().time - it.time }
                
                rs.averageQueueMessageAge = { long diff ->
                    def age = [:]
                    def darr = []
                    age.elapsedYears = (int) (diff / yearInMillis)
                    if(age.elapsedYears) {
                        darr.add("${age.elapsedYears} years")
                    }                    
                    diff = diff % yearInMillis
                    age.elapsedDays = (int) (diff / dayInMillis)
                    if(age.elapsedDays) {
                        darr.add("${age.elapsedDays} days")
                    }                    
                    diff = diff % dayInMillis
                    age.elapsedHours = (int) (diff / hourInMillis)
                    if(age.elapsedHours) {
                        darr.add("${age.elapsedHours} hours")
                    }                    
                    diff = diff % hourInMillis;
                    age.elapsedMinutes = (int) (diff / minuteInMillis)
                    if(age.elapsedMinutes) {
                        darr.add("${age.elapsedMinutes} min")
                    }                    
                    diff = diff % minuteInMillis;
                    age.elapsedSeconds = (int) (diff / secondInMillis)
                    if(age.elapsedSeconds) {
                        darr.add("${age.elapsedSeconds} sec")
                    }
                    return (darr.size())?darr.join(", "):'0 sec'
                }.call((long) (qmAgesInMS.size())?(qmAgesInMS.sum() / qmAgesInMS.size()):0)
                
                def tmAgesInMS = TopicMessage.withCriteria {
                    projections {
                        groupProperty("createTime")
                    }
                }.collect { new Date().time - it.time }
                
                rs.averageTopicMessageAge = { long diff ->
                    def age = [:]
                    def darr = []
                    age.elapsedYears = (int) (diff / yearInMillis)
                    if(age.elapsedYears) {
                        darr.add("${age.elapsedYears} years")
                    }                    
                    diff = diff % yearInMillis
                    age.elapsedDays = (int) (diff / dayInMillis)
                    if(age.elapsedDays) {
                        darr.add("${age.elapsedDays} days")
                    }                    
                    diff = diff % dayInMillis
                    age.elapsedHours = (int) (diff / hourInMillis)
                    if(age.elapsedHours) {
                        darr.add("${age.elapsedHours} hours")
                    }                    
                    diff = diff % hourInMillis;
                    age.elapsedMinutes = (int) (diff / minuteInMillis)
                    if(age.elapsedMinutes) {
                        darr.add("${age.elapsedMinutes} min")
                    }                    
                    diff = diff % minuteInMillis;
                    age.elapsedSeconds = (int) (diff / secondInMillis)
                    if(age.elapsedSeconds) {
                        darr.add("${age.elapsedSeconds} sec")
                    }
                    return (darr.size())?darr.join(", "):'0 sec'
                }.call((long) (tmAgesInMS.size())?(tmAgesInMS.sum() / tmAgesInMS.size()):0)
                                
                rs.averageMessageAge = { long diff ->
                    def age = [:]
                    def darr = []
                    age.elapsedYears = (int) (diff / yearInMillis)
                    if(age.elapsedYears) {
                        darr.add("${age.elapsedYears} years")
                    }                    
                    diff = diff % yearInMillis
                    age.elapsedDays = (int) (diff / dayInMillis)
                    if(age.elapsedDays) {
                        darr.add("${age.elapsedDays} days")
                    }                    
                    diff = diff % dayInMillis
                    age.elapsedHours = (int) (diff / hourInMillis)
                    if(age.elapsedHours) {
                        darr.add("${age.elapsedHours} hours")
                    }                    
                    diff = diff % hourInMillis;
                    age.elapsedMinutes = (int) (diff / minuteInMillis)
                    if(age.elapsedMinutes) {
                        darr.add("${age.elapsedMinutes} min")
                    }                    
                    diff = diff % minuteInMillis;
                    age.elapsedSeconds = (int) (diff / secondInMillis)
                    if(age.elapsedSeconds) {
                        darr.add("${age.elapsedSeconds} sec")
                    }
                    return (darr.size())?darr.join(", "):'0 sec'
                }.call((long) { 
                        if(qmAgesInMS.size() && tmAgesInMS.size()) {
                            return ((tmAgesInMS.sum()+qmAgesInMS.sum()) / (tmAgesInMS.size()+qmAgesInMS.size()))
                        } else if(qmAgesInMS.size()) {
                            return (qmAgesInMS.sum() / qmAgesInMS.size())
                        } else if(tmAgesInMS.size()) {
                            return (tmAgesInMS.sum() / tmAgesInMS.size())
                        } else {
                            return 0
                        }                                    
                    }.call() 
                )
                rs.lastRetrievedMessage = (rs.lastRetrievedQueueMessage)?
                    (
                        (rs.newestMessageInTopic)?
                            ( 
                                (rs.lastRetrievedQueueMessage > rs.newestMessageInTopic)?
                                    rs.lastRetrievedQueueMessage
                                    :rs.newestMessageInTopic
                            )
                            :rs.lastRetrievedQueueMessage
                    ):rs.newestMessageInTopic
                
                return rs
            }.call([
                queueMessagesPerMin: { qm ->
                    def min = qm.min { it.serviceCallDate }
                    if(min) {
                        return qm.size()/(((new Date().time - min.serviceCallDate.time)/1000)/60)   
                    }
                    return 0
                }.call(mc.findAll { it.tag == "createQueueMessage"}),
                topicMessagesPerMin: { tm ->
                    def min = tm.min { it.serviceCallDate }
                    if(min) {
                        return tm.size()/(((new Date().time - min.serviceCallDate.time)/1000)/60)   
                    }
                    return 0
                }.call(mc.findAll { it.tag == "createTopicMessage"}),
                retrievedQueueMessagesPerMin: { qm ->
                    def min = qm.min { it.serviceCallDate }
                    if(min) {
                        return qm.size()/(((new Date().time - min.serviceCallDate.time)/1000)/60)   
                    }
                    return 0
                }.call(mc.findAll { it.tag == "getNextMessage"}),
                retrievedTopicMessagesPerMin: { tm ->
                    def min = tm.min { it.serviceCallDate }
                    if(min) {
                        return tm.size()/(((new Date().time - min.serviceCallDate.time)/1000)/60)   
                    }
                    return 0
                }.call(mc.findAll { it.tag == "viewMessage"}),
                oldestMessageInQueue: QueueMessage.createCriteria().get {
                    projections {
                        min("createTime")
                    }
                },
                newestMessageInQueue: QueueMessage.createCriteria().get {
                    projections {
                        max("createTime")
                    }
                },
                oldestMessageInTopic: TopicMessage.createCriteria().get {
                    projections {
                        min("createTime")
                    }
                },
                newestMessageInTopic: TopicMessage.createCriteria().get {
                    projections {
                        max("createTime")
                    }
                },
                lastRetrievedQueueMessage: { qm ->
                    if(qm.size()) {
                        return qm.max { it.serviceCallDate }
                    }
                    return null
                }.call(mc.findAll { it.tag == "getNextMessage"}),
                lastRetrievedTopicMessage: { tm ->
                    if(tm.size()) {
                        return tm.max { it.serviceCallDate }
                    }
                    return null
                }.call(mc.findAll { it.tag == "viewMessage"})
                
            ])            
        }.call(listStats())
    }

    def retrievedQueueMessagesPerMin() {
        return { mc ->
            def max = mc.max { it.serviceCallDate }
            def min = mc.min { it.serviceCallDate }
            if(max) {
                return mc.size()/(((new Date().time - min.serviceCallDate.time)/1000)/60)                
            }
            mc.clear()
            return 0
        }.call(listStats().findAll { it.tag == "getNextMessage"})
    }

    def retrievedTopicMessagesPerMin() {
        return { mc ->
            def max = mc.max { it.serviceCallDate }
            def min = mc.min { it.serviceCallDate }
            if(max) {
                // return mc.size()/(((max.serviceCallDate.time - min.serviceCallDate.time)/1000)/60)                
                return mc.size()/(((new Date().time - min.serviceCallDate.time)/1000)/60)                
            }
            mc.clear()
            return 0
        }.call(listStats().findAll { it.tag == "viewMessage" })
    }
    
    def queuedMessagesPerMin() {
        return { mc ->
            def max = mc.max { it.serviceCallDate }
            def min = mc.min { it.serviceCallDate }
            if(max) {
                // return mc.size()/(((max.serviceCallDate.time - min.serviceCallDate.time)/1000)/60)                
                return mc.size()/(((new Date().time - min.serviceCallDate.time)/1000)/60)                
            }
            mc.clear()
            return 0
        }.call(listStats().findAll { it.tag == "createQueueMessage"})
    }

    def topicMessagesPerMin() {
        return { mc ->
            def max = mc.max { it.serviceCallDate }
            def min = mc.min { it.serviceCallDate }
            if(max) {
                // return mc.size()/(((max.serviceCallDate.time - min.serviceCallDate.time)/1000)/60)                
                return mc.size()/(((new Date().time - min.serviceCallDate.time)/1000)/60)                
            }
            mc.clear()
            return 0
        }.call(listStats().findAll { it.tag == "createTopicMessage"})
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
