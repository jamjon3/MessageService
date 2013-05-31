package edu.usf.cims.MessageService

import com.mongodb.DBCursor
import grails.converters.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class TopicService {

    static profiled = {
        createTopicMessage(tag: "createTopicMessage",message:"Created a new Topic Message")
        viewMessage(tag:"viewMessage",message:"Retrieved a Topic Message")
        listTopicMessages(tag:"listTopicMessages",message:"Retrieved a list of Topic Messages")
        filterTopicMessages(tag:"filterTopicMessages",message:"Retrieved a filtered list of Topic Messages")
    }

    static transactional = true


    /**
    * Methods dealing with Topics
    **/

    def listTopics(String pattern = "") {
        if(pattern) {
            def result = Topic.list().findAll { Pattern.compile(pattern.trim()).matcher(it.name).matches() }
            log.debug("Listing filtered list of ${result.size} topics for pattern ${pattern}")
            return result*.render()
        } else {
            log.debug("Listing all topics")
            return Topic.list()*.render()
        }
    }
    
    def addTopic(String username, Map message) {
        if((message.messageData)?(message.messageData.name):false) {
            def topic = new Topic([name: message.messageData.name])
            //Grant all rights to the topic creator
            topic.addReader(username)
            topic.addWriter(username)
            topic.addAdmin(username)

            //Add other permissions
            if(message.messageData?.permissions?.canRead){
                message.messageData?.permissions.canRead.each{ user ->
                    topic.addReader(user)
                }
            }
            if(message.messageData?.permissions?.canWrite){
                message.messageData.permissions.canWrite.each{ user ->
                    topic.addWriter(user)
                }
            }
            if(message.messageData?.permissions?.canAdmin){
                message.messageData.permissions.canAdmin.each{ user ->
                    topic.addAdmin(user)
                }
            }
            if(!topic.save(flush: true)) {
                log.warn("Failed saving topic ${message.messageData.name} because of error: ${topic.errors.getFieldError("name").code}")
                return topic.errors.getFieldError("name").code
            } else {
                log.info("Created new topic ${topic.name} with id ${topic.id as String} for user ${username}")
                return topic.render()
            }
        } else {
            log.error("No message data was given")
            return null
        }        
    }

/** TODO: Support more properties than just name **/
    def modifyTopic(String username, String topicName, Map message) {
        if(message.messageData.name) {
            def topic = Topic.findByName(topicName)
                       
            if(topic) {

                //Check authN
                if (! topic.canAdmin(username)) return "NotAuthorized"

                topic.name = message.messageData.name
                if(!topic.save(flush:true)) {
                    log.warn("Failed updating topic ${topic.id as String} because of error: ${topic.errors.getFieldError("name").code}")
                    return topic.errors.getFieldError("name").code
                } else {
                    //Use the GMongo API to update all the messages in this topic
                    def results = Message.collection.updateMulti([ "messageContainer.id" : topic.id],[$set: [ "messageContainer.name" : topic.name] ])
                    log.info("Updated topic ${topic.name} and ${results.n} messages")
                    return topic.render()
                }                                
            } else {
                log.warn("Invalid topic ( ${topicName} ) was given")
                return "TopicNotFound"
            }

        } else {
            log.error("No message data was given")
            return null
        }        
    }

    def deleteTopic(String username, String topicName) {
        def topic = Topic.findByName(topicName)
        if(topic) {
            //Check authN
            if (! topic.canAdmin(username)) return "NotAuthorized"

            def topicId = topic.id
            def deletedTopic = topic.render()
            def results = Message.collection.remove("messageContainer.id": topic.id)
            log.warn("${username} deleted ${results.n} messages from topic ${topicName}")
            Topic.collection.remove(id: topicId)
            log.warn("${username} deleted topic ${topic.name}")
            return deletedTopic
        } else {
            log.warn("Invalid topic ( ${topicName} ) was given")
            return "TopicNotFound"
        }       
    }

    def modifyPermissions(String username, String topicName, Map message){
        def topic = Topic.findByName(topicName)
        if(topic) {
            //Check authN
            if (! topic.canAdmin(username)) return "NotAuthorized"

            if (! message?.messageData?.permissions) return "NoPermissionData"

            def result = [canRead:[add:0,remove:0], canWrite:[add:0,remove:0], canAdmin:[add:0,remove:0]]

            //Add/Remove permissions
            if(message.messageData?.permissions?.canRead.add){
                message.messageData.permissions.canRead.add.each{ user ->
                    log.info("${username} added Read permission to topic ${topicName} for ${user}")
                    topic.addReader(user)
                    result.canRead.add++
                }
            }
            if(message.messageData?.permissions?.canRead.remove){
                message.messageData.permissions.canRead.remove.each{ user ->
                    log.info("${username} removed read permission to topic ${topicName} for ${user}")
                    topic.removeReader(user)
                    result.canRead.remove++
                }
            }
            if(message.messageData?.permissions?.canWrite.add){
                message.messageData.permissions.canWrite.add.each{ user ->
                    log.info("${username} added write permission to topic ${topicName} for ${user}")
                    topic.addWriter(user)
                    result.canWrite.add++
                }
            }
            if(message.messageData?.permissions?.canWrite.remove){
                message.messageData.permissions.canWrite.remove.each{ user ->
                    log.info("${username} removed write permission to topic ${topicName} for ${user}")
                    topic.removeWriter(user)
                    result.canWrite.remove++
                }
            }
            if(message.messageData?.permissions?.canAdmin.add){
                message.messageData.permissions.canAdmin.add.each{ user ->
                    log.warn("${username} added admin permission to topic ${topicName} for ${user}")
                    topic.addAdmin(user)
                    result.canAdmin.add++
                }
            }
            if(message.messageData?.permissions?.canAdmin.remove){
                message.messageData.permissions.canAdmin.remove.each{ user ->
                    log.warn("${username} removed admin permission to topic ${topicName} for ${user}")
                    topic.removeAdmin(user)
                    result.canAdmin.remove++
                }
            }

            if(!topic.save(flush:true)) {
                log.warn("Failed updating permissions for topic ${topicName}")
                return "UpdateFailed"
            } else {
                return result
            }       
        } else {
            log.warn("Invalid topic ( ${topicName} ) was given")
            return "TopicNotFound"
        }       
    }




    /**
    
    * Methods dealing with Messages

    **/

    def listTopicMessages(String username, String topicName) {
        def topic = Topic.findByName(topicName)
        if(topic) {

            //Check authN
            if (! topic.canRead(username)) return "NotAuthorized"

            def result = Message.findAllByMessageContainer(new MessageContainer([name:topic.name,type:"topic",id:topic.id]))
            log.debug("User ${username} listing ${result.size} messages for topic ${topicName}")
            return result*.render()
        } else {
            log.warn("Invalid topic ( ${topicName} ) was given")
            return "TopicNotFound"
        }
    }

    def filterTopicMessages(String username, String topicName, Date startTime, Date endTime) {
        def topic = Topic.findByName(topicName)
        if(topic) {

            //Check authN
            if (! topic.canRead(username)) return "NotAuthorized"

            def messageContainer = new MessageContainer([name:topicName,type:"topic",id:topic.id])
            return (endTime) ? Message.findAllByMessageContainerAndCreateTimeBetween(messageContainer,startTime,endTime)*.render() : Message.findAllByMessageContainerAndCreateTimeGreaterThanEquals(messageContainer,startTime)*.render()
        } else {
            log.warn("Invalid topic ( ${topicName} ) was given")
            return "TopicNotFound"
        }               
    }

    def createTopicMessage(String username, String topicName, Map message) {
        def topic = Topic.findByName(topicName)
        if (topic)  {

            //Check authN
            if (! topic.canWrite(username)) return "NotAuthorized"

            //Check for required fields
            if (! message.messageData) return "NoMessageData"
            if (! message.apiVersion) return "NoApiVersion"
            if (! message.createProg) return "NoCreateProgram"

            //Create the new message
            def topicMessage = new Message(creator: username, apiVersion: message.apiVersion, createProg: message.createProg, messageData: message.messageData as LinkedHashMap)
            topicMessage.messageContainer = [type: "topic", id: topic.id, name: topic.name]
       
            if(topicMessage.save()){
                log.info("Added new message ${topicMessage.id as String} to topic ${topicName} for ${username}")
                return topicMessage.render()
            }  else {
                log.error("Creating new message for topic ${topicName} failed! Message: ${message}")
                return "CreateFailed"
            }
        } else {
            log.warn("Invalid topic ( ${topicName} ) was given")
            return "TopicNotFound"           
        }
    }
    
    def viewMessage(String username, String topicName, String messageId) {
        def topic = Topic.findByName(topicName)
        if (topic) {

            //Check authN
            if (! topic.canRead(username)) return "NotAuthorized"                    

            def topicMessage = Message.findById(messageId)
            if (!topicMessage) {
                log.error("MessageID ( ${messageId} ) not found")
                return "MessageNotFound"
            }

            if (topicMessage.messageContainer.id != topic.id) {
                log.error("MessageID ( ${messageId} ) does not belong to topic ${topicName}")
                return "WrongTopicName"
            }

            return topicMessage.render()
        } else {
            log.warn("Invalid topic ( ${topicName} ) was given")
            return "TopicNotFound"
        }
    }

/*
    def modifyMessage(String username, String topicName, String messageId, Map messageUpdate) {
        def topic = Topic.findByName(topicName)
        if (topic) {
            
            //Check authN
            if (! topic.canWrite(username)) return "NotAuthorized"     

            def topicMessage = Message.findById(messageId)
            if (!topicMessage) {
                log.error("MessageID ( ${messageId} ) not found")
                return "MessageNotFound"
            }

            if (topicMessage.messageContainer.id != topic.id) {
                log.error("MessageID ( ${messageId} ) does not belong to topic ${topicName}")
                return "WrongTopicName"
            }

            messageUpdate.each { key,value ->
                if(key != 'id') {
                    if(key == 'messageData') {         
                        topicMessage.messageData = messageUpdate.messageData
                    } else if(!key.startsWith('messageData.')){
                        topicMessage[key] = value                                
                    }
                }
            }
            
            if(!topicMessage.save()) {
                log.error("Message ${messageId} update failed")
                return "SaveFailed"
            } else {
                log.info("Message ${messageId} updated")
                return topicMessage.render()
            }                                       
        } else { 
            log.warn("Invalid topic ( ${topicName} ) was given")
            return "TopicNotFound"
        } 
    }
*/

    def deleteMessage(String username, String topicName, String messageId) {
        def topic = Topic.findByName(topicName)
        if (topic) {            

            //Check authN
            if (! topic.canWrite(username)) return "NotAuthorized"   

            def topicMessage = Message.findById(messageId)
            if (!topicMessage) {
                log.error("MessageID ( ${messageId} ) not found")
                return "MessageNotFound"
            }

            if (topicMessage.messageContainer.id != topic.id) {
                log.error("MessageID ( ${messageId} ) does not belong to topic ${topicName}")
                return "WrongTopicName"
            }

            def topicMessageHash = topicMessage.render()

            topicMessage.delete()
            log.warn("Message ${messageId} deleted")
            return topicMessageHash    
        } else {
            log.warn("Invalid topic ( ${topicName} ) was given")
            return "TopicNotFound"
        }
    }
}
