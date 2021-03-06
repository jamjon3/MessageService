package edu.usf.cims.MessageService

import grails.converters.*
import grails.plugin.springsecurity.annotation.Secured

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
                render errorText as JSON
            }
        }
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def index() {}

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def combinedStats() {
            // Yesterday
      def startTime = new Date() - 1
      // Now
      def endTime = new Date()

      try {
        if (params.startTime) startTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssz", "${params.startTime}-0000")
        if (params.endTime) endTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssz", "${params.endTime}-0000")
      } catch (java.text.ParseException e) {
        def reason = [code: 400, message: "Unparseable date. Dates must be in the format yyyy-MM-dd\'T\'HH:mm:ss (GMT)"]
        renderError(reason.code, reason.message)
        return
      }

      def timeScale = params.timeScale ?: "hour"
      def statType = params.statType ?: 'count'

      def results = [:]
      results.timeScale = timeScale

      switch(statType) {
        case 'dashboard':
          results.count = statsService.countAllMessages()
          results.oldestMessageData = statsService.getMessageByAge(null, null, null, 1)
          results.newestMessageData = statsService.getMessageByAge(null, null, null, -1)
          results.averageMessageAge = statsService.getAverageMessageAge()
          results.messagesPer = statsService.getAggregateMessageCount(null, null, timeScale, startTime, endTime)
          results.dataTransferred = statsService.getAggregateDataTransfer(null, null, timeScale, startTime, endTime)
          results.messageSize = statsService.getAverageMessageSize()
        break
        case 'count':
          results.count = statsService.countAllMessages()
        break
        case 'ageMinMax':
          results.oldestMessageData = statsService.getMessageByAge(null, null, null, 1)
          results.newestMessageData = statsService.getMessageByAge(null, null, null, -1)
        break
        case 'averageAge':
          results.averageMessageAge = statsService.getAverageMessageAge()
        break
        case 'aggregateCount':
          results.messagesPer = statsService.getAggregateMessageCount(null, null, timeScale, startTime, endTime)
        break
        case 'aggregateTransfer':
          results.dataTransferred = statsService.getAggregateDataTransfer(null, null, timeScale, startTime, endTime)
        break
        case 'averageSize':
          results.messageSize = statsService.getAverageMessageSize()
        break
      }

      renderResponse results
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def queueStats() {
            // Yesterday
      def startTime = new Date() - 1
      // Now
      def endTime = new Date()

      try {
        if (params.startTime) startTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssz", "${params.startTime}-0000")
        if (params.endTime) endTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssz", "${params.endTime}-0000")
      } catch (java.text.ParseException e) {
        def reason = [code: 400, message: "Unparseable date. Dates must be in the format yyyy-MM-dd\'T\'HH:mm:ss (GMT)"]
        renderError(reason.code, reason.message)
        return
      }

      def timeScale = params.timeScale ?: "hour"
      def queueName = params.name ?: null
      def statType = params.statType ?: 'count'
      def status = params.status ?: null

      def results = [:]

      switch(statType) {
        case 'count':
          results.count = statsService.getQueueCounts(queueName, status)
        break
        case 'ageMaxMin':
          results.oldestMessageData = statsService.getMessageByAge('queue', queueName, status, 1)
          results.newestMessageData = statsService.getMessageByAge('queue', queueName, status, -1)
        break
        case 'averageAge':
          results.averageMessageAge = statsService.getAverageMessageAge('queue', queueName, status)
        break
        case 'aggregateSum':
          results.messagesPerMinute = statsService.getAggregateMessageCount('QUEUE', queueName, timeScale, startTime, endTime)
        break
        case 'aggregateTransfer':
          results.dataTransferred = statsService.getAggregateDataTransfer('QUEUE', queueName, timeScale, startTime, endTime)
        break
        case 'averageSize':
          results.messageSize = statsService.getAverageMessageSize('queue', queueName, status)
        break
      }

      renderResponse results
    }

    @Secured(['ROLE_ITMESSAGESERVICEUSER'])
    def topicStats() {
      // Yesterday
      def startTime = new Date() - 1
      // Now
      def endTime = new Date()

      try {
        if (params.startTime) startTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssz", "${params.startTime}-0000")
        if (params.endTime) endTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssz", "${params.endTime}-0000")
      } catch (java.text.ParseException e) {
        def reason = [code: 400, message: "Unparseable date. Dates must be in the format yyyy-MM-dd\'T\'HH:mm:ss (GMT)"]
        renderError(reason.code, reason.message)
        return
      }

      def timeScale = params.timeScale ?: 'hour'
      def topicName = params.name ?: null
      def statType = params.statType ?: 'count'

      def results = [:]

      switch(statType) {
        case 'count':
          results.count = statsService.getTopicCounts(topicName)
        break
        case 'ageMaxMin':
          results.oldestMessageData = statsService.getMessageByAge('topic', topicName, null, 1)
          results.newestMessageData = statsService.getMessageByAge('topic', topicName, null, -1)
        break
        case 'averageAge':
          results.averageMessageAge = statsService.getAverageMessageAge('topic', topicName)
        break
        case 'aggregateSum':
          results.messagesPerMinute = statsService.getMessagesPerMinute('TOPIC', topicName, timeScale, startTime, endTime)
        break
        case 'aggregateTransfer':
          results.dataTransferred = statsService.getAggregateDataTransfer('TOPIC', topicName, timeScale, startTime, endTime)
        break
        case 'averageSize':
          results.messageSize = statsService.getAverageMessageSize('topic', topicName)
        break
      }

      renderResponse results
    }

    /**

    **/
    @Secured(['ROLE_ITMESSAGESERVICEADMIN'])
    def requestError() {
        renderError(405, "UnsupportedHttpVerb: ${request.method} not allowed")
    }
}
