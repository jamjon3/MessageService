package edu.usf.cims.MessageService

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

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def index = {
      def results = [:]
      results.oldestMessageData = statsService.getOldestMessage()
      results.newestMessageData = statsService.getNewestMessage()
      renderResponse results
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def queueStats = {
      def results = [:]
      results.oldestMessageData = statsService.getOldestQueueMessage()
      results.newestMessageData = statsService.getNewestQueueMessage()
      renderResponse results
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def topicStats = {
      def results = [:]
      results.oldestMessageData = statsService.getOldestTopicMessage()
      if (! results.oldestMessageData instanceof Map){
        renderError 400 "Got error collecting data on the oldest topic message."
      }
      results.newestMessageData = statsService.getNewestTopicMessage()
      if (! results.newestMessageData instanceof Map){
        renderError 400 "Got error collecting data on the newest topic message."
      }

      renderResponse results
    }

    @Secured(['ROLE_ITMESSAGESERVICEADMIN'])
    def requestError = {
        renderError(405, "UnsupportedHttpVerb: ${request.method} not allowed")
    }
}
