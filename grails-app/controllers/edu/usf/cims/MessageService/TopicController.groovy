package edu.usf.cims.MessageService

import grails.converters.*
import grails.plugins.springsecurity.Secured
import groovy.time.TimeCategory

class TopicController {
    def topicService  
    def auditService
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
        withFormat {
            html {
                if (params.return?.toUpperCase() == 'XML'){
                    render responseText as XML
                }else{
                  //Handle JSONP
                  if (params.callback) {
                    render(contentType: "text/javascript", encoding: "UTF-8", text: "${params.callback}(${responseText.encodeAsJSON()})")       
                  } else {
                    render responseText as JSON
                  }
                }
            }
            xml {
                render responseText as XML
            }
            json {
              //Handle JSONP
              if (params.callback) {
                render(contentType: "text/javascript", encoding: "UTF-8", text: "${params.callback}(${responseText.encodeAsJSON()})")       
              } else {
                render responseText as JSON
              }
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
              //Handle JSONP
              if (params.callback) {
                render(contentType: "text/javascript", encoding: "UTF-8", text: "${params.callback}(${responseText.encodeAsJSON()})")       
              } else {
                render responseText as JSON
              }
            }
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def listTopics = {
      def username = springSecurityService.authentication.name
      def ipAddress = request.getRemoteAddr()
      def topicResult = topicService.listTopics(params.pattern) 
      
      if (topicResult instanceof List){
          def resultMap = [count:topicResult.size,topics:topicResult]
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'LIST_TOPICS'])
          renderResponse resultMap  
          return 
      } else {
          switch(topicResult) {
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
        def ipAddress = request.getRemoteAddr()
        def message = getMessageBody()

        if (message){      
            def topic = topicService.addTopic(username, message)
            if(topic instanceof Map) {
                auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'CREATE_TOPIC', containerName: message.messageData.name, containerType: 'TOPIC'])
                renderResponse([count:1, topics:topic])
                return
            } else {
              def reason = [:]
              switch(topic) {
                  case "unique":
                    reason.code = 400
                    reason.message = "Create failed: ${message.messageData.name} already exists!"
                    break
                  case "validator.invalid":
                    reason.code = 400
                    reason.message = "Create failed: ${message.messageData.name} invalid name!"    
                  break
                  default:
                    reason.code = 400
                    reason.message = 'Topic could not be created'
                  break
              }
              
              auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'CREATE_TOPIC_ERROR', containerName: message.messageData.name, containerType: 'TOPIC', reason: reason.message])
              renderError(reason.code, reason.message) 
              return
            }
        } else {
          def reason = 'Message data required'
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'CREATE_TOPIC_ERROR', containerType: 'TOPIC', reason: reason])
          renderError(400, reason)
          return
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def modifyTopic = {
        def username = springSecurityService.authentication.name
        def ipAddress = request.getRemoteAddr()

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
                    auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'MODIFY_TOPIC_PERM', containerName: message.messageData.name, containerType: 'TOPIC'])
                    renderResponse result
                } else {
                    auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'MODIFY_TOPIC_NAME', containerName: message.messageData.name, containerType: 'TOPIC'])
                    renderResponse([count:1,topics:result])
                }
                return
            } else {
              def reason = [:]
                switch(result) {
                    case "unique":
                      reason.code = 400
                      reason.message = "Update failed: ${message.messageData.name} already exists!"
                    break
                    case "validator.invalid":
                      reason.code = 400
                      reason.message = "Update failed: ${message.messageData.name} invalid name!"
                    break
                    case "TopicNotFound":
                      reason.code = 404
                      reason.message = "Update failed: ${params.name} does not exist"
                    break
                    case "NoMessageData":
                      reason.code = 400
                      reason.message = "Message data required to modify topic!"
                    break
                    case "NotAuthorized":
                      reason.code = 403
                      reason.message = "You are not authorized to perform this operation"
                    break
                    default:
                      reason.code = 400
                      reason.message = "Update failed: Queue could not be updated"
                    break
                }
                auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'MODIFY_TOPIC_ERROR', containerName: message.messageData.name, containerType: 'TOPIC', reason: reason.message])
                renderError(reason.code, reason.message)
                return
            }
        } else {
          def reason = 'Message data required'
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'MODIFY_TOPIC_ERROR', containerType: 'TOPIC', reason: reason])
          renderError(400, reason)
          return
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])    
    def deleteTopic = {
      def username = springSecurityService.authentication.name
      def ipAddress = request.getRemoteAddr()
      def topic = topicService.deleteTopic(username, params.name)

      if(topic instanceof Map) {
        auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'DELETE_TOPIC', containerName: params.name, containerType: 'TOPIC'])
        renderResponse([count:1,topics:topic])
        return   
      } else {
        def reason = [:]
        switch(topic) {
            case "TopicNotFound":
              reason.code = 404
              reason.message = "ResourceNotFound: ${params.name} does not exist"
            break
            case "NotAuthorized":
              reason.code = 403
              reason.message = "You are not authorized to perform this operation"
            break
            default:
              reason.code = 400
              reason.message = "Error: Could not delete ${params.name}."
            break
        }
        auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'DELETE_TOPIC_ERROR', containerName: params.name, containerType: 'TOPIC', reason: reason.message])
        renderError(reason.code, reason.message)
        return
          
      }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def listTopicMessages = {
        def username = springSecurityService.authentication.name
        def ipAddress = request.getRemoteAddr()

        def topicMessages = topicService.listTopicMessages(username, params.name)
        if(topicMessages instanceof List) {
            def resultMap = [count:topicMessages.size, messages:topicMessages]
            
            auditService.writeAuditEntry([  actor: username, 
                                            ipAddress: ipAddress, 
                                            action: 'VIEW_MESSAGE', 
                                            containerName: params.name, 
                                            containerType: 'TOPIC', 
                                            details:[ messageCount: topicMessages.size,
                                                      messageSize: resultMap.toString().length() ]
                                                    ])

            renderResponse resultMap
            return
        } else {
          def reason = [:]
          switch(topicMessages) {
            case "NoResults":
                auditService.writeAuditEntry([  actor: username, 
                                                ipAddress: ipAddress, 
                                                action: 'VIEW_MESSAGE', 
                                                containerName: params.name, 
                                                containerType: 'TOPIC' ])
                renderResponse([count:0,messages:[]])
                return
            break
            case "TopicNotFound":
              reason.code = 404
              reason.message = "ResourceNotFound: ${params.name} does not exist"
            break
            case "NotAuthorized":
              reason.code = 403
              reason.message = "You are not authorized to perform this operation"
            break
          }
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'VIEW_ERROR', containerName: params.name, containerType: 'TOPIC', reason: reason.message])
          renderError(reason.code, reason.message)
          return
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def filterTopicMessages = {
        def username = springSecurityService.authentication.name
        def ipAddress = request.getRemoteAddr()

        def startTime = null
        def endTime = null
        try {
          if (params.startTime) startTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssz", "${params.startTime}-0000")
          if (params.endTime) endTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssz", "${params.endTime}-0000")
        } catch (java.text.ParseException e) {
          def reason = [code: 400, message: "Unparseable date. Dates must be in the format yyyy-MM-dd\'T\'HH:mm:ss (GMT)"]
          renderError(reason.code, reason.message)
          return
        }

        //Valid start time is required
        if(startTime){ 
            def topicMessages = topicService.filterTopicMessages(username, params.name,startTime,endTime)
            if(topicMessages instanceof List) {
                def resultMap = [count:topicMessages.size, messages:topicMessages]
                auditService.writeAuditEntry([  actor: username, 
                                                ipAddress: ipAddress, 
                                                action: 'VIEW_MESSAGE', 
                                                containerName: params.name, 
                                                containerType: 'TOPIC', 
                                                details:[ messageCount: topicMessages.size,
                                                          messageSize: resultMap.toString().length() ]
                                                      ])              
                
                log.debug("Listing ${topicMessages.size} messages from topic ${params.name}")
                renderResponse resultMap    
                return                      
            } else {
              def reason = [:]
              switch(topicMessages) {
                case "NoResults":
                    auditService.writeAuditEntry([  actor: username, 
                                                    ipAddress: ipAddress, 
                                                    action: 'VIEW_MESSAGE', 
                                                    containerName: params.name, 
                                                    containerType: 'TOPIC' ])
                    renderResponse([count:0,messages:[]])
                    return
                break
                case "TopicNotFound":
                  reason.code = 404
                  reason.message = "ResourceNotFound: ${params.name} does not exist"
                break
                case "NotAuthorized":
                  reason.code = 403
                  reason.message = "You are not authorized to perform this operation"
                break
              }
              renderError(reason.code, reason.message)
              return
            }
        } else {
          def reason = [code: 400, message: 'MissingRequiredQueryParameter: startTime parameter not found' ]
            renderError(reason.code, reason.message)
            return
        }
    } 

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def createTopicMessage = {
      def username = springSecurityService.authentication.name
      def ipAddress = request.getRemoteAddr()
      def message = getMessageBody()

      if (message) { 
        if (! message.apiVersion) message.apiVersion = 1
        def topicMessages = topicService.createTopicMessage(username, params.name,message)
    
        if(topicMessages instanceof Map) {
          auditService.writeAuditEntry([  actor: username, 
                                      ipAddress: ipAddress, 
                                      action: 'CREATE_MESSAGE', 
                                      containerName: params.name, 
                                      containerType: 'TOPIC', 
                                      details:[ messageId: topicMessages.messageId,
                                                messageSize: topicMessages.toString().length() ]
                                      ])

            renderResponse([count:1,messages:topicMessages])
            return                           
        } else {
          def reason = [:]
          switch(topicMessages) {
            case "TopicNotFound":
              reason.code = 404
              reason.message = 'Topic does not exist'
            break
            case "NotAuthorized":
              reason.code = 403
              reason.message = 'You are not authorized to perform this operation'              
            break
            case 'NoMessageData':
              reason.code = 400
              reason.message = 'Message data missing or bad format'
            break
            case 'NoApiVersion':
              reason.code = 400
              reason.message = 'Message data missing API Version'
            break
            case 'NoCreateProgram':
              reason.code = 400
              reason.message = 'Message data missing Create Program'
            break
            default:
              reason.code = 400
              reason.message = 'Message could not be created'
            break
          }
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'CREATE_MESSAGE_ERROR', containerName: params.name, containerType: 'TOPIC', reason: reason.message])
          renderError(reason.code, reason.message)
          return
        }
      }else {
        def reason = [code: 400, message: 'Message data required']
        auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'CREATE_MESSAGE_ERROR', containerName: params.name, containerType: 'TOPIC', reason: reason.message])
        renderError(reason.code, reason.message)
        return
      }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def viewMessage = {
      def username = springSecurityService.authentication.name
      def ipAddress = request.getRemoteAddr()
      if (params.id){
        def topicMessage = topicService.viewMessage(username, params.name, params.id)
        if(topicMessage instanceof Map) {
          auditService.writeAuditEntry([  actor: username, 
                                          ipAddress: ipAddress, 
                                          action: 'VIEW_MESSAGE', 
                                          containerName: params.name, 
                                          containerType: 'TOPIC', 
                                          details:[ messageId: topicMessage.messageId,
                                                    messageSize: topicMessage.toString().length(),
                                                    messageAge: TimeCategory.minus( new Date(), topicMessage.createTime) ]
                                      ])
            renderResponse([count:1,messages:topicMessage])
            return                          
        } else {
          def reason = [:]
          switch(topicMessage) {
            case "TopicNotFound":
              reason.code = 404
              reason.message = 'Topic does not exist'
            break
            case "MessageNotFound":
              reason.code = 404
              reason.message = 'Message does not exist'
            break
            case "WrongTopicName":
              reason.code = 400
              reason.message = 'Requested message does not belong to requested topic'
            break
            case "NotAuthorized":
              reason.code = 403
              reason.message = 'You are not authorized to perform this operation'
            break
            default:
              reason.code = 400
              reason.message = 'Message could not be retreived'
            break
          }
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'VIEW_ERROR', containerName: params.name, containerType: 'TOPIC', reason: reason.message, details:[ messageId: params.id]])
          renderError(reason.code, reason.message)
          return
        }
      } else {
        def reason = [code: 400, message: 'MissingRequiredQueryParameter: Message id required']
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'VIEW_ERROR', containerName: params.name, containerType: 'TOPIC', reason: reason.message])
          renderError(reason.code, reason.message)
          return
      }
    }


    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def deleteMessage = {
      def username = springSecurityService.authentication.name
      def ipAddress = request.getRemoteAddr()
      if (params.id){
        def topicMessage = topicService.deleteMessage(username,params.name,params.id)
        if(topicMessage instanceof Map) {
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'DELETE_MESSAGE', containerName: params.name, containerType: 'TOPIC', details:[ messageId: params.id]])
          renderResponse([count:1,messages:topicMessage])
          return                          
        } else {
          def reason = [:]
          switch(topicMessage) {
            case "TopicNotFound":
              reason.code = 404
              reason.message = 'Topic does not exist'
            break
            case "MessageNotFound":
              reason.code = 404
              reason.message = 'Message does not exist'
            break
            case "WrongTopicName":
              reason.code = 400
              reason.message = 'Requested message does not belong to requested topic'
            break
            case "NotAuthorized":
              reason.code = 403
              reason.message = "You are not authorized to perform this operation"
            break
            default:
              reason.code = 400
              reason.message = 'Message could not be deleted'
            break
          }
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'DELETE_MESSAGE_ERROR', containerName: params.name, containerType: 'TOPIC', reason: reason.message, details:[ messageId: params.id]])
          renderError(reason.code, reason.message)
          return
        }
    } else {
        def reason = [code: 400, message: 'MissingRequiredQueryParameter: Message id required']
        auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'DELETE_MESSAGE_ERROR', containerName: params.name, containerType: 'TOPIC', reason: reason.message])
        renderError(reason.code, reason.message)
        return
      }

    }
    
    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def requestError = {
        renderError(405, "UnsupportedHttpVerb: ${request.method} not allowed")
        return
    }
    
}