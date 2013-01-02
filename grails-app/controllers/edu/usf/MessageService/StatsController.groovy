package edu.usf.MessageService

import grails.converters.*
import grails.plugins.springsecurity.Secured

class StatsController {
    def statsService

    private def getMessageBody() {
        def message = ''
        if (params.message){
            message = params.message
        } else {
            request.withFormat {
                html {
                    message = request.JSON
                }
                xml {
                    message = request.XML 
                }
                json {
                    message = request.JSON
                }
            }
        } 
    }

    def renderResponse (responseText) {
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

    @Secured(['ROLE_ITPRSUPERVISOR'])
    def listStats = {
        renderResponse statsService.listStats(params.tagFilter)        
    }

    @Secured(['ROLE_ITPRSUPERVISOR'])
    def listAllQueueMessages = {
        renderResponse statsService.listAllQueueMessages(params.queueName,params.status)
    }

    @Secured(['ROLE_ITPRSUPERVISOR'])
    def listAllTopicMessages = {
        renderResponse statsService.listAllTopicMessages(params.topicName)
    }

    def requestError = {
        renderError(405, "UnsupportedHttpVerb: ${request.method} not allowed")
    }
}
