package edu.usf.cims.MessageService

import com.mongodb.DBCursor
import org.bson.types.ObjectId

import grails.converters.*

class QueueService {
/*
    static profiled = {
        createQueueMessage(tag: "createQueueMessage",message:"Created a new Queue Message")
        peek(tag:"peek",message:"Peeked at a Queue Message")
        getNextMessage(tag:"getNextMessage",message:"Retrieved a Queue Messages")
        listInProgressMessages(tag:"listInProgressMessages",message:"Retrieved a list of Queue Messages that are in progress")
    }
*/
    static transactional = true


    /**
    * Methods dealing with Queues
    **/

    def listQueues(String pattern = "") {
        if(pattern) {
            def result = Queue.list().findAll { Pattern.compile(pattern.trim()).matcher(it.name).matches() }
            log.debug("Listing filtered list of ${result.size} queues for pattern ${pattern}")
            return result*.render()
        } else {
            log.debug("Listing all queues")
            return Queue.list()*.render()
        }
    }
    
    def addQueue(String username, Map message) {
        if((message.messageData)?(message.messageData.name):false) {
            def queue = new Queue([name: message.messageData.name])
            //Grant all rights to the queue creator
            queue.addReader(username)
            queue.addWriter(username)
            queue.addAdmin(username)

            //Add other permissions
            if(message.messageData?.permissions?.canRead){
                message.messageData?.permissions.canRead.each{ user ->
                    queue.addReader(user)
                }
            }
            if(message.messageData?.permissions?.canWrite){
                message.messageData.permissions.canWrite.each{ user ->
                    queue.addWriter(user)
                }
            }
            if(message.messageData?.permissions?.canAdmin){
                message.messageData.permissions.canAdmin.each{ user ->
                    queue.addAdmin(user)
                }
            }
            if(!queue.save(flush: true)) {
                log.warn("Failed saving queue ${message.messageData.name} because of error: ${queue.errors.getFieldError("name").code}")
                return queue.errors.getFieldError("name").code
            } else {
                log.info("Created new queue ${queue.name} with id ${queue.id as String} for user ${username}")
                return queue.render()
            }
        } else {
            log.error("No message data was given")
            return null
        }        
    }

    def modifyQueue(String username, String queueName, Map message) {
        if(message.messageData.name) {
            def queue = Queue.findByName(queueName)
                       
            if(queue) {

                //Check authN
                if (! queue.canAdmin(username)) return "NotAuthorized"

                queue.name = message.messageData.name
                if(!queue.save(flush:true)) {
                    log.warn("Failed updating queue ${queue.id as String} because of error: ${queue.errors.getFieldError("name").code}")
                    return queue.errors.getFieldError("name").code
                } else {
                    //Use the GMongo API to update all the messages in this queue
                    def results = Message.collection.updateMulti([ "messageContainer.id" : queue.id],[$set: [ "messageContainer.name" : queue.name] ])
                    log.info("Updated queue ${queue.name} and ${results.n} messages")
                    return queue.render()
                }                                
            } else {
                log.warn("Invalid queue ( ${queueName} ) was given")
                return "QueueNotFound"
            }

        } else {
            log.error("No message data was given")
            return null
        }        
    }

    def deleteQueue(String username, String queueName) {
        def queue = Queue.collection.find([name:queueName]) as Queue
        if(queue) {
            //Check authN
            if (! queue.canAdmin(username)) return "NotAuthorized"

            def queueId = queue.id
            def deletedQueue = queue.render()
            def results = Message.collection.remove("messageContainer.id": queueId)
            if (results){
              log.warn("${username} deleted ${results.n} messages from queue ${queueName}")
              Queue.collection.remove([ _id : queueId])
              log.warn("${username} deleted queue ${queue.name}")
              return deletedQueue
            }
        } else {
            log.warn("Invalid queue ( ${queueName} ) was given")
            return "QueueNotFound"
        }       
    }

    def modifyPermissions(String username, String queueName, Map message){
        def queue = Queue.findByName(queueName)
        if(queue) {
            //Check authN
            if (! queue.canAdmin(username)) return "NotAuthorized"

            if (! message?.messageData?.permissions) return "NoPermissionData"

            def result = [canRead:[add:0,remove:0], canWrite:[add:0,remove:0], canAdmin:[add:0,remove:0]]

            //Add/Remove permissions
            if(message.messageData?.permissions?.canRead.add){
                message.messageData.permissions.canRead.add.each{ user ->
                    log.info("${username} added Read permission to queue ${queueName} for ${user}")
                    queue.addReader(user)
                    result.canRead.add++
                }
            }
            if(message.messageData?.permissions?.canRead.remove){
                message.messageData.permissions.canRead.remove.each{ user ->
                    log.info("${username} removed read permission to queue ${queueName} for ${user}")
                    queue.removeReader(user)
                    result.canRead.remove++
                }
            }
            if(message.messageData?.permissions?.canWrite.add){
                message.messageData.permissions.canWrite.add.each{ user ->
                    log.info("${username} added write permission to queue ${queueName} for ${user}")
                    queue.addWriter(user)
                    result.canWrite.add++
                }
            }
            if(message.messageData?.permissions?.canWrite.remove){
                message.messageData.permissions.canWrite.remove.each{ user ->
                    log.info("${username} removed write permission to queue ${queueName} for ${user}")
                    queue.removeWriter(user)
                    result.canWrite.remove++
                }
            }
            if(message.messageData?.permissions?.canAdmin.add){
                message.messageData.permissions.canAdmin.add.each{ user ->
                    log.warn("${username} added admin permission to queue ${queueName} for ${user}")
                    queue.addAdmin(user)
                    result.canAdmin.add++
                }
            }
            if(message.messageData?.permissions?.canAdmin.remove){
                message.messageData.permissions.canAdmin.remove.each{ user ->
                    log.warn("${username} removed admin permission to queue ${queueName} for ${user}")
                    queue.removeAdmin(user)
                    result.canAdmin.remove++
                }
            }

            if(!queue.save(flush:true)) {
                log.warn("Failed updating permissions for queue ${queueName}")
                return "UpdateFailed"
            } else {
                return result
            }       
        } else {
            log.warn("Invalid queue ( ${queueName} ) was given")
            return "QueueNotFound"
        }       
    }




    /**
    
    * Methods dealing with Messages

    **/

    def getNextMessage(String username, String queueName, String ipAddress = "0.0.0.0") {
        def queue = Queue.findByName(queueName)
        if(queue) {

            //Check authN
            if (! queue.canRead(username)) return "NotAuthorized"

            def taken = [date: new Date(), user: username, ipAddress: ipAddress]

            //Search for the oldest message in the 'pending' status and change it to 'in-progress'
            def result = Message.collection.findAndModify(  ["messageContainer.id" : queue.id, status : "pending"], 
                                                            [$sort: [createTime: 1]], 
                                                            [$set: [status: "in-progress", taken: taken]]) as Message
            if (result?.id) {
                //Update the stats on the Queue
                Queue.collection.findAndModify(['_id': queue.id], [$inc:['stats.in-progress': 1]])
                                Queue.collection.findAndModify(['_id': queue.id], [$inc:['stats.pending': -1]])
                result.status = "in-progress"
                result.taken = taken
                log.debug("User ${username} picked up message ${result.id} from queue ${queueName}")
                return result.render()
            } else {
                return "NoResults"
            } 

        } else {
            log.warn("Invalid queue ( ${queueName} ) was given")
            return "QueueNotFound"
        }
    }

    def peek(String username, String queueName, Integer numMessages = 1) {
        def queue = Queue.findByName(queueName)
        if(queue) {

            //Check authN
            if (! queue.canRead(username)) return "NotAuthorized"

            def results = Message.findAllByMessageContainerAndStatus(new MessageContainer([name:queue.name,type:"queue",id:queue.id]),"pending",[max:numMessages, sort:"createTime", order:"asc"])
            log.debug("User ${username} peeked at ${results.size} from queue ${queueName}")
            return results*.render()

        } else {
            log.warn("Invalid queue ( ${queueName} ) was given")
            return "QueueNotFound"
        }
    }

    def listInProgressMessages(String username, String queueName) {
        def queue = Queue.findByName(queueName)
        if(queue) {

            //Check authN
            if (! queue.canRead(username)) return "NotAuthorized"

            def results = Message.findAllByMessageContainerAndStatus(new MessageContainer([name:queue.name,type:"queue",id:queue.id]),"in-progress",[sort:"createTime", order:"asc"])
            log.debug("User ${username} listed ${results.size} in-progress messages from queue ${queueName}")
            return results*.render()
            
        } else {
            log.warn("Invalid queue ( ${queueName} ) was given")
            return "QueueNotFound"
        }
    }

    def createQueueMessage(String username, String queueName, Map message) {
        def queue = Queue.findByName(queueName)
        if (queue)  {

            //Check authN
            if (! queue.canWrite(username)) return "NotAuthorized"

            //Check for required fields
            if (! message.messageData) return "NoMessageData"
            if (! message.apiVersion) return "NoApiVersion"
            if (! message.createProg) return "NoCreateProgram"

            //Create the new message
            def queueMessage = new Message(creator: username, status: "pending", apiVersion: message.apiVersion, createProg: message.createProg, messageData: message.messageData as LinkedHashMap)
            queueMessage.messageContainer = [type: "queue", id: queue.id, name: queue.name]
       
            if(queueMessage.save(flush:true)){
                Queue.collection.findAndModify(['_id': queue.id], [$inc:['stats.pending': 1]])
                log.info("Added new message ${queueMessage.id as String} to queue ${queueName} for ${username}")
                return queueMessage.render()
            }  else {
                log.error("Creating new message for queue ${queueName} failed! Message: ${message}")
                return "CreateFailed"
            }

            if (queueMessage.messageContainer.id != queue.id) {
                log.error("MessageID ( ${messageId} ) does not belong to queue ${queueName}")
                return "WrongQueueName"
            }

            def queueMessageHash = queueMessage.render()

            queueMessage.delete()
            log.warn("Message ${messageId} deleted")
            return queueMessageHash    
        } else {
            log.warn("Invalid queue ( ${queueName} ) was given")
            return "QueueNotFound"           
        }
    }
    
    def viewMessage(String username, String queueName, String messageId) {
        def queue = Queue.findByName(queueName)
        if (queue) {

            //Check authN
            if (! queue.canRead(username)) return "NotAuthorized"                    

            def queueMessage = Message.findById(messageId)
            if (!queueMessage) {
                log.error("MessageID ( ${messageId} ) not found")
                return "MessageNotFound"
            }

            if (queueMessage.messageContainer.id != queue.id) {
                log.error("MessageID ( ${messageId} ) does not belong to queue ${queueName}")
                return "WrongQueueName"
            }

            return queueMessage.render()
        } else {
            log.warn("Invalid queue ( ${queueName} ) was given")
            return "QueueNotFound"
        }
    }

    /**
    * Limit updates to status changes.  Any other change requires a new message to be created
    **/
    def modifyMessage(String username, String queueName, String messageId, Map messageUpdate, String ipAddress = "0.0.0.0") {
        def queue = Queue.findByName(queueName)
        if (queue) {
            
            //Check authN
            if (! queue.canWrite(username)) return "NotAuthorized"     

            if (! messageUpdate.status) {
              return "MessageStatusRequired"
            } else if (messageUpdate.messageData){
              return "MessageStatusOnly"
            }
            def queueMessage = Message.collection.findOne([ _id : new ObjectId(messageId)]) as Message
            if (!queueMessage) {
                log.error("MessageID ( ${messageId} ) not found")
                return "MessageNotFound"
            }

            if (queueMessage.messageContainer.id != queue.id) {
                log.error("MessageID ( ${messageId} ) does not belong to queue ${queueName}")
                return "WrongQueueName"
            }

            def origStatus = queueMessage.status

            //Make sure the status passed is valid
            queueMessage.status = messageUpdate.status 
            if (! queueMessage.validate()){
              if(queueMessage.errors.hasFieldErrors("status")) {
                log.error("Update failed: ${messageUpdate.status} is not valid")
                return "BadMessageStatus"
              }
              log.error("Update failed: validation errors")
              return "ValidationError"
            }

            def updated = [date: new Date(), user: username, ipAddress: ipAddress]

            //Update the message and add the 'updated' data
            def result = Message.collection.update([ _id : new ObjectId(messageId)],[$set: [status: messageUpdate.status, updated: updated] ])

            if (result) {

              //Update Queue stats
              Queue.collection.findAndModify(['_id': topic.id], [$inc:["status.${Origstatus}": -1]])
              Queue.collection.findAndModify(['_id': topic.id], [$inc:["status.${queueMessage.status}": 1]])

              log.info("Updated ${messageId} to status ${messageUpdate.status}")
              //redo the search and return the updated object
              queueMessage = Message.collection.findOne([ _id : new ObjectId(messageId)]) as Message
              return queueMessage.render()
            } else {
                log.error("Message ${messageId} update failed")
                return "SaveFailed"
            }                                       
        } else { 
            log.warn("Invalid queue ( ${queueName} ) was given")
            return "QueueNotFound"
        } 
    }

    def deleteMessage(String username, String queueName, String messageId) {
        def queue = Queue.findByName(queueName)
        if (queue) {            

            //Check authN
            if (! queue.canWrite(username)) return "NotAuthorized"   

            def queueMessage = Message.findById(messageId)
            if (!queueMessage) {
                log.error("MessageID ( ${messageId} ) not found")
                return "MessageNotFound"
            }

            if (queueMessage.messageContainer.id != queue.id) {
                log.error("MessageID ( ${messageId} ) does not belong to queue ${queueName}")
                return "WrongQueueName"
            }

            def queueMessageHash = queueMessage.render()

            queueMessage.delete(flush: true)
            Queue.collection.findAndModify(['_id': topic.id], [$inc:["status.${queue.status}": -1]])
            log.warn("Message ${messageId} deleted")
            return queueMessageHash    
        } else {
            log.warn("Invalid queue ( ${queueName} ) was given")
            return "QueueNotFound"
        }
    }
}
