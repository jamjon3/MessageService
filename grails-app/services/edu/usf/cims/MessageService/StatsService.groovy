package edu.usf.cims.MessageService

import org.codehaus.groovy.grails.commons.GrailsApplication
import java.text.SimpleDateFormat
import groovy.time.*
import grails.converters.*
import org.joda.time.*



class StatsService {
    def grailsApplication
    static transactional = true

    //These are the statuses we want to track
    def statusList = ['pending','in-progress']

    def getQueueCounts(){
      def queueCounts = [:]
      Queue.findAll().each { queue ->
        queueCounts[queue.name] = [:]
        statusList.each { status ->
          queueCounts[queue.name][status] = countQueueMessages(queue.name, status)
        }
        
      }
      return queueCounts
    }

    def getTopicCounts(){
      def topicCounts = [:]
      Topic.findAll().each { topic ->
        topicCounts[topic.name] = countTopicMessages(topic.name)
      }
      return topicCounts
    }

    def countQueueMessages(queueName = null,status = null) {
        def queue = (queueName)?Queue.findByName(queueName):null
        if(queue && status) {
            Message.collection.count(["messageContainer.name": queue.name, "messageContainer.type": "queue", status: status])
        } else if(queue) {
            Message.collection.count(["messageContainer.name": queue.name, "messageContainer.type": "queue"])
        } else if(status) {
            Message.collection.count(["messageContainer.type": "queue", status: status])
        } else {
            Message.collection.count(["messageContainer.type": "queue"])
        }        
    }
    
    def countTopicMessages(topicName = null) {
        def topic = (topicName)?Topic.findByName(topicName):null
        if(topic) {
            Message.collection.count(["messageContainer.name": topic.name, "messageContainer.type": "topic"])           
        } else {
            Message.collection.count(["messageContainer.type": "topic"])           
        }        
    }

    def getOldestMessage(container = null, status = null){
      def result
      if (status && container){
        result = Message.collection.find([ "messageContainer.name" : container, status : status]).sort(createTime: 1).limit(1) as Message  
      } else if (container){
        result = Message.collection.find([ "messageContainer.name" : container]).sort(createTime: 1).limit(1) as Message
      } else if (status){
        result = Message.collection.find([ status : status]).sort(createTime: 1).limit(1) as Message
      } else {
        result = Message.collection.find().sort(createTime: 1).limit(1) as Message        
      }
      if (result){
        def resultMap = [:]
        resultMap.messageId = result.id
        resultMap.createTime = result.createTime
        resultMap.age = TimeCategory.minus( new Date(), result.createTime )

        return resultMap
      }
    }

    def getAverageMessageAge(containerName = null, status = null){
      /* Define a Map-Reduce function to get the average age */
      Message.collection.mapReduce(
        """
          function mapFunction() {
            var key = this.messageContainer.name
            var value = {
              name: this.messageContainer.name,
              total_age: new Date() - new Date(this.createTime),
              count: 1,
              avg_age: 0
            };
            emit(key, value);
          };
        """,
        """
          function reduce(key, values) {
            var reducedObject = {
              name: key,
              total_age: 0,
              count: 0,
              avg_age: 0
            };

            values.forEach( function(value) {
              reducedObject.total_age += value.total_age;
              reducedObject.count += value.count;
              if(reducedObject.total_age > 0) reducedObject.avg_age = reducedObject.total_age / reducedObject.count
              }
            );
            return reducedObject;
          };
        """,
        /* Collection to store the results in */
        "stats-avg_age",
        /* Query that defines the input to the Map Reduce Function*/
        [:]
        )
    }

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

}