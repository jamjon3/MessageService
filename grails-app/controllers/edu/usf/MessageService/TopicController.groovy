package edu.usf.MessageService

import grails.converters.*
import edu.usf.MessageService.TopicService
import grails.plugins.springsecurity.Secured

class TopicController {
    def topicService  
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
    def listTopics = {
        def topicResult = topicService.listTopics(params.pattern) 
        if (topicResult instanceof List){
            def resultMap = [count:topicResult.size,topics:topicResult]
            renderResponse resultMap  
            return 
        } else {
            switch(topic) {
                default:
                    renderError(400, "Request failed.")
                    return
                break
            }
        }

    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def createTopic = { 
        def username = springSecurityService.authentication.name
        def message = getMessageBody()

        if (message){      
            def topic = topicService.addTopic(username, message)
            if(topic instanceof Map) {
                renderResponse([count:1,topics:topic])
                return
            } else {
                switch(topic) {
                    case "unique":
                        renderError(400, "Create failed: ${message.messageData.name} already exists!") 
                        return   
                    break
                    case "validator.invalid":
                        renderError(400, "Create failed: ${message.messageData.name} invalid name!")
                        return    
                    break
                    default:
                        renderError(400, 'Topic could not be created')
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
    def modifyTopic = {
        def username = springSecurityService.authentication.name
        def message = getMessageBody()
        if(message?.messageData){
            def result = ""

            //Are we updating the permissions or the topic settings?
            if (message?.messageData?.permissions) { 
                result = topicService.modifyPermissions(username, params.name, message)
            } else {
                result = topicService.modifyTopic(username, params.name, message)
            }
                
            if(result instanceof Map) {
                if (result.canRead){
                    //This was a permissions modification
                    renderResponse result
                } else {
                    renderResponse([count:1,topics:result])
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
                    case "TopicNotFound":
                        renderError(404, "Update failed: ${params.name} does not exist")
                        return    
                    break
                    case "NoMessageData":
                        renderError(400, "Message data required to modify topic!")
                        return    
                    break
                    case "NotAuthorized":
                        renderError(403, "You are not authorized to perform this operation")
                        return    
                    break
                    default:
                        renderError(400, 'Update failed: Topic could not be updated')
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
    def deleteTopic = {
        def username = springSecurityService.authentication.name
        def topic = topicService.deleteTopic(username, params.name)
        if(topic instanceof Map) {
            renderResponse([count:1,topics:topic])
            return   
        } else {
            switch(topic) {
                case "TopicNotFound":
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
    def listTopicMessages = {
        def username = springSecurityService.authentication.name
        def topicMessages = topicService.listTopicMessages(username, params.name)
        if(topicMessages instanceof List) {
            def resultMap = [count:topicMessages.size,messages:topicMessages]
            renderResponse resultMap
            return
        } else {
            switch(topicMessages) {
                case "TopicNotFound":
                    renderError(404, "ResourceNotFound: ${params.name} does not exist")
                    return
                break
                case "NotAuthorized":
                    renderError(403, "You are not authorized to perform this operation")
                    return    
                break
                default:
                    renderResponse resultMap
                    return
                break
            }
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def filterTopicMessages = {
        def username = springSecurityService.authentication.name
        def startTime = null
        def endTime = null
        try {
            startTime = (params.startTime)?(new Date().parse("yyyy-MM-dd'T'HH:mm:ssz", "${params.startTime}GMT")):null
            endTime = (params.endTime)?(new Date().parse("yyyy-MM-dd'T'HH:mm:ssZ", "${params.endTime}GMT")):null
        } catch (java.text.ParseException e) {
            renderError(400, "${e.message} Dates must be in the format yyyy-MM-dd\'T\'HH:mm:ss (GMT)")
            return
        }

        //Valid start time is required
        if(startTime){ 
            def topicMessages = topicService.filterTopicMessages(username, params.name,startTime,endTime)
            if(topicMessages instanceof List) {
                def resultMap = [count:topicMessages.size,messages:topicMessages]
                log.debug("Listing ${topicMessages.size} messages from topic ${params.name}")
                renderResponse resultMap    
                return                      
            } else {
                switch(topicMessages) {
                    case "TopicNotFound":
                        renderError(404, "ResourceNotFound: ${params.name} does not exist")
                        return       
                    break
                    case "NotAuthorized":
                        renderError(403, "You are not authorized to perform this operation")
                        return    
                    break
                    default:
                        renderError(400, 'Messages could not be found') 
                        return       
                    break
                }
            }
        } else {
            renderError(400, 'MissingRequiredQueryParameter: startTime parameter not found')
            return
        }
    }  

    
    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def createTopicMessage = {
        def username = springSecurityService.authentication.name
        def message = getMessageBody()

        if (message) { 
            if (! message.apiVersion) message.apiVersion = 1
            def topicMessages = topicService.createTopicMessage(username, params.name,message)
        
            if(topicMessages instanceof Map) {
                renderResponse([count:1,messages:topicMessages])
                return                           
            } else {
                switch(topicMessages) {
                    case "TopicNotFound":
                        renderError(404, 'Topic does not exist') 
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
            def topicMessage = topicService.viewMessage(username, params.name, params.id)
            if(topicMessage instanceof Map) {
                renderResponse([count:1,messages:topicMessage])
                return                          
            } else {
                switch(topicMessage) {
                    case "TopicNotFound":
                        renderError(404, 'Topic does not exist') 
                        return       
                    break
                    case "MessageNotFound":
                        renderError(404, 'Message does not exist') 
                        return       
                    break
                    case "WrongTopicName":
                        renderError(400, 'Requested message does not belong to requested topic') 
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
            def topicMessage = topicService.modifyMessage(username, params.name, params.id, message)
            if(topicMessage instanceof Map) {
                renderResponse([count:1,messages:topicMessage])
                return                          
            } else {
                switch(topicMessage) {
                    case "TopicNotFound":
                        renderError(404, 'Topic does not exist') 
                        return       
                    break
                    case "MessageNotFound":
                        renderError(404, 'Message does not exist') 
                        return       
                    break
                    case "WrongTopicName":
                        renderError(400, 'Requested message does not belong to requested topic') 
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
            def topicMessage = topicService.deleteMessage(username,params.name,params.id)
            if(topicMessage instanceof Map) {
                renderResponse([count:1,messages:topicMessage])
                return                          
            } else {
                switch(topicMessage) {
                    case "TopicNotFound":
                        renderError(404, 'Topic does not exist') 
                        return       
                    break
                    case "MessageNotFound":
                        renderError(404, 'Message does not exist') 
                        return       
                    break
                    case "WrongTopicName":
                        renderError(400, 'Requested message does not belong to requested topic') 
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
    
    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def requestError = {
        renderError(405, "UnsupportedHttpVerb: ${request.method} not allowed")
        return
    }
    
}
