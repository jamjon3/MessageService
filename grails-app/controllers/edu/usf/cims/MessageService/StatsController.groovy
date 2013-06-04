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
    def generalStats = {
      def results = [:]
      results.count = statsService.countAllMessages()
      results.oldestMessageData = statsService.getOldestMessage()
      results.newestMessageData = statsService.getNewestMessage()
      results.averageMessageAge = statsService.getAverageMessageAge()

      renderResponse results
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def queueStats = {
      def results = [:]
      results.count = statsService.getAllQueueCounts()
      results.oldestMessageData = statsService.getOldestQueueMessage()
      results.newestMessageData = statsService.getNewestQueueMessage()
      results.averageMessageAge = statsService.getAverageMessageAge('queue')

      renderResponse results
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def topicStats = {
      def results = [:]
      results.count = statsService.getAllTopicCounts()
      results.oldestMessageData = statsService.getOldestTopicMessage()
      results.newestMessageData = statsService.getNewestTopicMessage()
      results.averageMessageAge = statsService.getAverageMessageAge('topic')

      renderResponse results
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def detailedQueueStats = {
      def results = [:]

      def counts = statsService.getQueueCounts(params.name)
      if (params.status){
        results.count = 0
      } else {
        results.count = counts
      }
      results.oldestMessageData = statsService.getOldestQueueMessage(params.name, params.status)
      results.newestMessageData = statsService.getNewestQueueMessage(params.name, params.status)

      results.averageMessageAge = statsService.getAverageMessageAge('queue', params.name, params.status)
      renderResponse results
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def detailsTopicStats = {
      def results = [:]
      
      results.oldestMessageData = statsService.getOldestTopicMessage(params.name)
      results.newestMessageData = statsService.getNewestTopicMessage(params.name)

      results.averageMessageAge = statsService.getAverageMessageAge('topic', params.name)
      renderResponse results
    } 

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def topicCount = {
      renderResponse  statsService.getMessgesPerMinute()
      //renderResponse statsService.getTopicCounts()
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def queueCount = {
      renderResponse statsService.getQueueCounts()
    }

    /**

    **/
    @Secured(['ROLE_ITMESSAGESERVICEADMIN'])
    def requestError = {
        renderError(405, "UnsupportedHttpVerb: ${request.method} not allowed")
    }
}
