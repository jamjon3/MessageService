package edu.usf.MessageService

import grails.converters.*
import edu.usf.MessageService.QueueService
import grails.plugins.springsecurity.Secured

class QueueController {
    def queueService  
    def springSecurityService

    private def getMessageBody() {
        try {
            if (params.message){
                return JSON.parse(params.message)
            } else {
                request.withFormat {
                    html {
                        return request.JSON
                    }
                    xml {
                        return request.XML 
                    }
                    json {
                        return request.JSON
                    }
                }
            }
        } catch(Exception e) {
            renderError(400, 'Message data is invalid')
        } 
    }

    def renderResponse (responseText) {
        response.status = 200
        withFormat {
            html {
                if (params.return?.toUpperCase() == 'XML'){
                    render responseText as XML
                }else{
                    render responseText as JSON
                }
            }
            xml {
                render responseText as XML
            }
            json {
                render responseText as JSON
            }
        }
    }

    def renderError(statusCode, errorMessage){
        response.status = statusCode
        def errorText = [error: errorMessage ]

        withFormat {
            html {
                if (params.return?.toUpperCase() == 'XML'){
                    render errorText as XML
                }else{
                    render errorText as JSON
                }
            }
            xml {
                render errorText as XML
            }
            json {
                render errorText as JSON
            }
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def listQueues = {
        def queueResult = queueService.listQueues(params.pattern) 
        if (queueResult instanceof List){
            def resultMap = [count:queueResult.size,queues:queueResult]
            renderResponse resultMap  
            return 
        } else {
            switch(queue) {
                default:
                    renderError(400, "Request failed.")
                    return
                break
            }
        }

    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def createQueue = { 
        def username = springSecurityService.authentication.name
        def message = getMessageBody()

        if (message){      
            def queue = queueService.addQueue(username, message)
            if(queue instanceof Map) {
                renderResponse([count:1,queues:queue])
                return
            } else {
                switch(queue) {
                    case "unique":
                        renderError(400, "Create failed: ${message.messageData.name} already exists!") 
                        return   
                    break
                    case "validator.invalid":
                        renderError(400, "Create failed: ${message.messageData.name} invalid name!")
                        return    
                    break
                    default:
                        renderError(400, 'Queue could not be created')
                        return
                    break
                }                
            }
        } else {
            renderError(400, 'Message data required')
            return
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def modifyQueue = {
        def username = springSecurityService.authentication.name
        def message = getMessageBody()
        if(message?.messageData){
            def result = ""

            //Are we updating the permissions or the queue settings?
            if (message?.messageData?.permissions) { 
                result = queueService.modifyPermissions(username, params.name, message)
            } else {
                result = queueService.modifyQueue(username, params.name, message)
            }
                
            if(result instanceof Map) {
                if (result.canRead){
                    //This was a permissions modification
                    renderResponse result
                } else {
                    renderResponse([count:1,queues:result])
                }
                return
            } else {
                switch(result) {
                    case "unique":
                        renderError(400, "Update failed: ${message.messageData.name} already exists!")
                        return    
                    break
                    case "validator.invalid":
                        renderError(400, "Update failed: ${message.messageData.name} invalid name!")
                        return    
                    break
                    case "QueueNotFound":
                        renderError(404, "Update failed: ${params.name} does not exist")
                        return    
                    break
                    case "NoMessageData":
                        renderError(400, "Message data required to modify queue!")
                        return    
                    break
                    case "NotAuthorized":
                        renderError(403, "You are not authorized to perform this operation")
                        return    
                    break
                    default:
                        renderError(400, 'Update failed: Queue could not be updated')
                        return
                    break
                }
            }
        } else {
            renderError(400, 'Message data required')
            return
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def deleteQueue = {
        def username = springSecurityService.authentication.name
        def queue = queueService.deleteQueue(username, params.name)
        if(queue instanceof Map) {
            renderResponse([count:1,queues:queue])
            return   
        } else {
            switch(queue) {
                case "QueueNotFound":
                    renderError(404, "ResourceNotFound: ${params.name} does not exist")
                    return
                break
                case "NotAuthorized":
                    renderError(403, "You are not authorized to perform this operation")
                    return    
                break
                default:
                    renderError(400, "Error: Could not delete ${params.name}.")
                    return
                break
            }
            
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def getNextMessage = {
        def username = springSecurityService.authentication.name
        def ipAddress = request.getRemoteAddr()

        def queueMessage = queueService.getNextMessage(username, params.name, ipAddress)
        if(queueMessage instanceof Map) {
            def resultMap = [count:1,messages:[queueMessage]]
            renderResponse resultMap
            return
        } else {
            switch(queueMessage) {
                case "NoResults":
                    renderResponse([count:0,messages:[]])
                    return
                break
                case "QueueNotFound":
                    renderError(404, "ResourceNotFound: ${params.name} does not exist")
                    return
                break
                case "NotAuthorized":
                    renderError(403, "You are not authorized to perform this operation")
                    return    
                break
            }
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def peek = {
        def username = springSecurityService.authentication.name

        def queueMessages = queueService.peek(username, params.name, params.count)
        if(queueMessages instanceof List) {
            def resultMap = [count:1,messages:[queueMessages]]
            renderResponse resultMap
            return
        } else {
            switch(queueMessages) {
                case "QueueNotFound":
                    renderError(404, "ResourceNotFound: ${params.name} does not exist")
                    return
                break
                case "NotAuthorized":
                    renderError(403, "You are not authorized to perform this operation")
                    return    
                break
            }
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def listInProgressMessages = {
        def username = springSecurityService.authentication.name

        def queueMessages = queueService.listInProgressMessages(username, params.name)
        if(queueMessages instanceof List) {
            def resultMap = [count:1,messages:[queueMessages]]
            renderResponse resultMap
            return
        } else {
            switch(queueMessages) {
                case "QueueNotFound":
                    renderError(404, "ResourceNotFound: ${params.name} does not exist")
                    return
                break
                case "NotAuthorized":
                    renderError(403, "You are not authorized to perform this operation")
                    return    
                break
            }
        }
    }  

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def createQueueMessage = {
        def username = springSecurityService.authentication.name
        def message = getMessageBody()

        if (message) { 
            if (! message.apiVersion) message.apiVersion = 1
            def queueMessages = queueService.createQueueMessage(username, params.name,message)
        
            if(queueMessages instanceof Map) {
                renderResponse([count:1,messages:queueMessages])
                return                           
            } else {
                switch(queueMessages) {
                    case "QueueNotFound":
                        renderError(404, 'Queue does not exist') 
                        return       
                    break
                    case "NotAuthorized":
                        renderError(403, "You are not authorized to perform this operation")
                        return    
                    break
                    case 'NoMessageData':
                        renderError(400, 'Message data missing or bad format') 
                        return       
                    break
                    case 'NoApiVersion':
                        renderError(400, 'Message data missing API Version') 
                        return       
                    break
                    case 'NoCreateProgram':
                        renderError(400, 'Message data missing Create Program') 
                        return       
                    break
                    default:
                        renderError(400, 'Message could not be created') 
                        return       
                    break
                }
            }
        }else {
            renderError(400, 'Message data required')
            return
        }

    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def viewMessage = {
        def username = springSecurityService.authentication.name
        if (params.id){
            def queueMessage = queueService.viewMessage(username, params.name, params.id)
            if(queueMessage instanceof Map) {
                renderResponse([count:1,messages:queueMessage])
                return                          
            } else {
                switch(queueMessage) {
                    case "QueueNotFound":
                        renderError(404, 'Queue does not exist') 
                        return       
                    break
                    case "MessageNotFound":
                        renderError(404, 'Message does not exist') 
                        return       
                    break
                    case "WrongQueueName":
                        renderError(400, 'Requested message does not belong to requested queue') 
                        return       
                    break
                    case "NotAuthorized":
                        renderError(403, "You are not authorized to perform this operation")
                        return    
                    break
                    default:
                        renderError(400, 'Message could not be retreived') 
                        return       
                    break
                }                  
            }
        } else {
            renderError(400, 'MissingRequiredQueryParameter: Message id required')
            return
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def modifyMessage = {
        def username = springSecurityService.authentication.name
        if (params.id){
            def message = getMessageBody()
            if(! message) {
                renderError(400, 'MissingRequiredQueryParameter: Message data required')
                return
            }
            def queueMessage = queueService.modifyMessage(username, params.name, params.id, message)
            if(queueMessage instanceof Map) {
                renderResponse([count:1,messages:queueMessage])
                return                          
            } else {
                switch(queueMessage) {
                    case "QueueNotFound":
                        renderError(404, 'Queue does not exist') 
                        return       
                    break
                    case "MessageNotFound":
                        renderError(404, 'Message does not exist') 
                        return       
                    break
                    case "WrongQueueName":
                        renderError(400, 'Requested message does not belong to requested queue') 
                        return       
                    break
                    case "NotAuthorized":
                        renderError(403, "You are not authorized to perform this operation")
                        return    
                    break
                    default:
                        renderError(400, 'Message could not be updated') 
                        return       
                    break
                }                  
            }
        } else {
            renderError(400, 'MissingRequiredQueryParameter: Message id required')
            return
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def deleteMessage = {
        def username = springSecurityService.authentication.name
         if (params.id){
            def queueMessage = queueService.deleteMessage(username,params.name,params.id)
            if(queueMessage instanceof Map) {
                renderResponse([count:1,messages:queueMessage])
                return                          
            } else {
                switch(queueMessage) {
                    case "QueueNotFound":
                        renderError(404, 'Queue does not exist') 
                        return       
                    break
                    case "MessageNotFound":
                        renderError(404, 'Message does not exist') 
                        return       
                    break
                    case "WrongQueueName":
                        renderError(400, 'Requested message does not belong to requested queue') 
                        return       
                    break
                    case "NotAuthorized":
                        renderError(403, "You are not authorized to perform this operation")
                        return    
                    break
                    default:
                        renderError(400, 'Message could not be deleted') 
                        return       
                    break
                }                  
            }
        } else {
            renderError(400, 'MissingRequiredQueryParameter: Message id required')
            return
        }

    }
    
    def requestError = {
        renderError(405, "UnsupportedHttpVerb: ${request.method} not allowed")
        return
    }

    //force an error page
    @Secured(['ROLE_DOES_NOT_EXIST'])
    def showRoleError = {}
    
}