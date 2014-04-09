package edu.usf.cims.MessageService

import grails.converters.*
import grails.plugin.springsecurity.annotation.Secured
import groovy.time.TimeCategory

class QueueController {
    def queueService
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
                render errorText as JSON
              }
            }
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def listQueues() {
      def username = springSecurityService.authentication.name
      def ipAddress = request.getRemoteAddr()
      def queueResult = queueService.listQueues(params.pattern)

      if (queueResult instanceof List){
          def resultMap = [count:queueResult.size,queues:queueResult]
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'LIST_QUEUES'])
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
    def createQueue() {
        def username = springSecurityService.authentication.name
        def ipAddress = request.getRemoteAddr()
        def message = getMessageBody()

        if (message){
            def queue = queueService.addQueue(username, message)
            if(queue instanceof Map) {
                auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'CREATE_QUEUE', containerName: message.messageData.name, containerType: 'QUEUE'])
                renderResponse([count:1,queues:queue])
                return
            } else {
              def reason = [:]
              switch(queue) {
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
                    reason.message = 'Queue could not be created'
                  break
              }

              auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'CREATE_QUEUE_ERROR', containerName: message.messageData.name, containerType: 'QUEUE', reason: reason.message])
              renderError(reason.code, reason.message)
              return
            }
        } else {
          def reason = 'Message data required'
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'CREATE_QUEUE_ERROR', containerType: 'QUEUE', reason: reason])
          renderError(400, reason)
          return
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def modifyQueue() {
        def username = springSecurityService.authentication.name
        def ipAddress = request.getRemoteAddr()

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
                    auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'MODIFY_QUEUE_PERM', containerName: message.messageData.name, containerType: 'QUEUE'])
                    renderResponse result
                } else {
                    auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'MODIFY_QUEUE_NAME', containerName: message.messageData.name, containerType: 'QUEUE'])
                    renderResponse([count:1,queues:result])
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
                    case "QueueNotFound":
                      reason.code = 404
                      reason.message = "Update failed: ${params.name} does not exist"
                    break
                    case "NoMessageData":
                      reason.code = 400
                      reason.message = "Message data required to modify queue!"
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
                auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'MODIFY_QUEUE_ERROR', containerName: message.messageData.name, containerType: 'QUEUE', reason: reason.message])
                renderError(reason.code, reason.message)
                return
            }
        } else {
          def reason = 'Message data required'
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'MODIFY_QUEUE_ERROR', containerType: 'QUEUE', reason: reason])
          renderError(400, reason)
          return
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def deleteQueue() {
      def username = springSecurityService.authentication.name
      def ipAddress = request.getRemoteAddr()
      def queue = queueService.deleteQueue(username, params.name)

      if(queue instanceof Map) {
        auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'DELETE_QUEUE', containerName: params.name, containerType: 'QUEUE'])
        renderResponse([count:1,queues:queue])
        return
      } else {
        def reason = [:]
        switch(queue) {
            case "QueueNotFound":
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
        auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'DELETE_QUEUE_ERROR', containerName: params.name, containerType: 'QUEUE', reason: reason.message])
        renderError(reason.code, reason.message)
        return

      }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def getNextMessage() {
        def username = springSecurityService.authentication.name
        def ipAddress = request.getRemoteAddr()

        def queueMessage = queueService.getNextMessage(username, params.name, ipAddress)
        if(queueMessage instanceof Map) {
            def resultMap = [count:1,messages:[queueMessage]]

            auditService.writeAuditEntry([  actor: username,
                                            ipAddress: ipAddress,
                                            action: 'VIEW_MESSAGE',
                                            containerName: params.name,
                                            containerType: 'QUEUE',
                                            details:[ messageId: queueMessage.id,
                                                      messageSize: resultMap.toString().length(),
                                                      messageAge: new Date().getTime() - queueMessage.createTime.getTime() ]
                                                    ])

            renderResponse resultMap
            return
        } else {
          def reason = [:]
          switch(queueMessage) {
            case "NoResults":
                auditService.writeAuditEntry([  actor: username,
                                                ipAddress: ipAddress,
                                                action: 'VIEW_MESSAGE',
                                                containerName: params.name,
                                                containerType: 'QUEUE' ])
                renderResponse([count:0,messages:[]])
                return
            break
            case "QueueNotFound":
              reason.code = 404
              reason.message = "ResourceNotFound: ${params.name} does not exist"
            break
            case "NotAuthorized":
              reason.code = 403
              reason.message = "You are not authorized to perform this operation"
            break
          }
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'VIEW_ERROR', containerName: params.name, containerType: 'QUEUE', reason: reason.message])
          renderError(reason.code, reason.message)
          return
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def peek() {
      def username = springSecurityService.authentication.name
      def ipAddress = request.getRemoteAddr()

      def queueMessages = queueService.peek(username, params.name, params.count as Integer)
      if(queueMessages instanceof List) {
          def resultMap = [count:queueMessages.size,messages:queueMessages]
          auditService.writeAuditEntry([  actor: username,
                                          ipAddress: ipAddress,
                                          action: 'PEEK',
                                          containerName: params.name,
                                          containerType: 'QUEUE',
                                          details:[ messageCount: resultMap.count,
                                                    messageSize: resultMap.toString().length() ]
                                        ])
          renderResponse resultMap
          return
      } else {
        def reason = [:]
        switch(queueMessages) {
          case "QueueNotFound":
            reason.code = 404
            reason.message = "ResourceNotFound: ${params.name} does not exist"
          break
          case "NotAuthorized":
            reason.code = 403
            reason.message = "You are not authorized to perform this operation"
          break
        }
        auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'PEEK_ERROR', containerName: params.name, containerType: 'QUEUE', reason: reason.message])
        renderError(reason.code, reason.message)
        return
      }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def listInProgressMessages() {
      def username = springSecurityService.authentication.name
      def ipAddress = request.getRemoteAddr()

      def queueMessages = queueService.listInProgressMessages(username, params.name)
      if(queueMessages instanceof List) {
          def resultMap = [count:queueMessages.size,messages:queueMessages]
          auditService.writeAuditEntry([  actor: username,
                                          ipAddress: ipAddress,
                                          action: 'INPROGRESS',
                                          containerName: params.name,
                                          containerType: 'QUEUE',
                                          details:[ messageCount: resultMap.count,
                                                    messageSize: resultMap.toString().length() ]
                                        ])

          renderResponse resultMap
          return
      } else {
        def reason = [:]
        switch(queueMessages) {
          case "QueueNotFound":
            reason.code = 404
            reason.message = "ResourceNotFound: ${params.name} does not exist"
          break
          case "NotAuthorized":
            reason.code = 403
            reason.message = "You are not authorized to perform this operation"
          break
        }
        auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'INPROGRESS_ERROR', containerName: params.name, containerType: 'QUEUE', reason: reason.message])
        renderError(reason.code, reason.message)
        return
      }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def createQueueMessage() {
      def username = springSecurityService.authentication.name
      def ipAddress = request.getRemoteAddr()
      def message = getMessageBody()

      if (message) {
        if (! message.apiVersion) message.apiVersion = 1
        def queueMessages = queueService.createQueueMessage(username, params.name,message)

        if(queueMessages instanceof Map) {
          auditService.writeAuditEntry([  actor: username,
                                      ipAddress: ipAddress,
                                      action: 'CREATE_MESSAGE',
                                      containerName: params.name,
                                      containerType: 'QUEUE',
                                      details:[ messageId: queueMessages.id,
                                                messageSize: queueMessages.toString().length() ]
                                      ])

            renderResponse([count:1,messages:queueMessages])
            return
        } else {
          def reason = [:]
          switch(queueMessages) {
            case "QueueNotFound":
              reason.code = 404
              reason.message = 'Queue does not exist'
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
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'CREATE_MESSAGE_ERROR', containerName: params.name, containerType: 'QUEUE', reason: reason.message])
          renderError(reason.code, reason.message)
          return
        }
      }else {
        def reason = [code: 400, message: 'Message data required']
        auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'CREATE_MESSAGE_ERROR', containerName: params.name, containerType: 'QUEUE', reason: reason.message])
        renderError(reason.code, reason.message)
        return
      }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def viewMessage() {
      def username = springSecurityService.authentication.name
      def ipAddress = request.getRemoteAddr()
      if (params.id){
        def queueMessage = queueService.viewMessage(username, params.name, params.id)
        if(queueMessage instanceof Map) {
          auditService.writeAuditEntry([  actor: username,
                                          ipAddress: ipAddress,
                                          action: 'VIEW_MESSAGE',
                                          containerName: params.name,
                                          containerType: 'QUEUE',
                                          details:[ messageId: queueMessage.id,
                                                    messageSize: queueMessage.toString().length(),
                                                    messageAge: new Date().getTime() - queueMessage.createTime.getTime() ]
                                      ])
            renderResponse([count:1,messages:queueMessage])
            return
        } else {
          def reason = [:]
          switch(queueMessage) {
            case "QueueNotFound":
              reason.code = 404
              reason.message = 'Queue does not exist'
            break
            case "MessageNotFound":
              reason.code = 404
              reason.message = 'Message does not exist'
            break
            case "WrongQueueName":
              reason.code = 400
              reason.message = 'Requested message does not belong to requested queue'
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
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'VIEW_ERROR', containerName: params.name, containerType: 'QUEUE', reason: reason.message, details:[ messageId: params.id]])
          renderError(reason.code, reason.message)
          return
        }
      } else {
        def reason = [code: 400, message: 'MissingRequiredQueryParameter: Message id required']
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'VIEW_ERROR', containerName: params.name, containerType: 'QUEUE', reason: reason.message])
          renderError(reason.code, reason.message)
          return
      }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def modifyMessage() {
      def username = springSecurityService.authentication.name
      def ipAddress = request.getRemoteAddr()
      if (params.id){
        def message = getMessageBody()
        if(! message) {
          def reason = [code: 400, message: 'Message data required']
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'MODIFY_MESSAGE_ERROR', containerName: params.name, containerType: 'QUEUE', reason: reason.message])
          renderError(reason.code, reason.message)
          return
        }
        def queueMessage = queueService.modifyMessage(username, params.name, params.id, message, ipAddress)
        if(queueMessage instanceof Map) {
          auditService.writeAuditEntry([  actor: username,
                                          ipAddress: ipAddress,
                                          action: 'MODIFY_MESSAGE_STATUS',
                                          containerName: params.name,
                                          containerType: 'QUEUE',
                                          details:[ messageId: queueMessage.id ]
                                      ])
            renderResponse([count:1,messages:queueMessage])
            return
        } else {
          def reason = [:]
          switch(queueMessage) {
            case "QueueNotFound":
              reason.code = 404
              reason.message = 'Queue does not exist'
            break
            case "MessageNotFound":
              reason.code = 404
              reason.message = 'Message does not exist'
            break
            case "WrongQueueName":
              reason.code = 400
              reason.message = 'Requested message does not belong to requested queue'
            break
            case "NotAuthorized":
              reason.code = 403
              reason.message = 'You are not authorized to perform this operation'
            break
            case "BadMessageStatus":
              reason.code = 400
              reason.message = "${message.status} is not a valid message status"
            break
            case "MessageStatusOnly":
              reason.code = 400
              reason.message = 'Message status is the only modifiable data in a message'
            break
            case "MessageStatusRequired":
              reason.code = 400
              reason.message = 'Message status is a required parameter'
            break
            default:
              reason.code = 400
              reason.message = 'Message could not be updated'
            break
          }
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'MODIFY_MESSAGE_ERROR', containerName: params.name, containerType: 'QUEUE', reason: reason.message, details:[ messageId: params.id]])
          renderError(reason.code, reason.message)
          return
        }
      } else {
        def reason = [code: 400, message: 'MissingRequiredQueryParameter: Message id required']
        auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'MODIFY_MESSAGE_ERROR', containerName: params.name, containerType: 'QUEUE', reason: reason.message])
        renderError(reason.code, reason.message)
        return
      }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def deleteMessage() {
      def username = springSecurityService.authentication.name
      def ipAddress = request.getRemoteAddr()
      if (params.id){
        def queueMessage = queueService.deleteMessage(username,params.name,params.id)
        if(queueMessage instanceof Map) {
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'DELETE_MESSAGE', containerName: params.name, containerType: 'QUEUE', details:[ messageId: params.id]])
          renderResponse([count:1,messages:queueMessage])
          return
        } else {
          def reason = [:]
          switch(queueMessage) {
            case "QueueNotFound":
              reason.code = 404
              reason.message = 'Queue does not exist'
            break
            case "MessageNotFound":
              reason.code = 404
              reason.message = 'Message does not exist'
            break
            case "WrongQueueName":
              reason.code = 400
              reason.message = 'Requested message does not belong to requested queue'
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
          auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'DELETE_MESSAGE_ERROR', containerName: params.name, containerType: 'QUEUE', reason: reason.message, details:[ messageId: params.id]])
          renderError(reason.code, reason.message)
          return
        }
    } else {
        def reason = [code: 400, message: 'MissingRequiredQueryParameter: Message id required']
        auditService.writeAuditEntry([actor: username, ipAddress: ipAddress, action: 'DELETE_MESSAGE_ERROR', containerName: params.name, containerType: 'QUEUE', reason: reason.message])
        renderError(reason.code, reason.message)
        return
      }

    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def requestError() {
        renderError(405, "UnsupportedHttpVerb: ${request.method} not allowed")
        return
    }

}
