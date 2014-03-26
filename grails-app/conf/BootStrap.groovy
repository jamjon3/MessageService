import edu.usf.cims.MessageService.Topic
import edu.usf.cims.MessageService.Queue
import grails.util.GrailsNameUtils
import grails.util.GrailsUtil

class BootStrap {
    def grailsApplication
    def mongo

    def init = { servletContext ->
        /* GORM doesn't allow indexes on embedded fields, so we have to create them here */
        def db = mongo.getDB(grailsApplication.config.grails.mongo.databaseName)
        db.message.ensureIndex(['messageContainer.id':1 , status:1])
        db.message.ensureIndex(['messageContainer.id':1 , status:1, createTime:1])

        switch(GrailsUtil.environment){
            case "development":
                println "#### Development Mode (Start Up)"

                def username = 'it-msgsvcadm'
                def queueList = ['TestQueue','TestQueue2','TestQueue3','TestQueue4','TestQueue5']
                def topicList = ['TestTopic','TestTopic2','TestTopic3','TestTopic4','TestTopic5']

                queueList.each { queueName ->
                  if (! Queue.countByName(queueName)) {
                    println "#### Building a test queue - ${queueName}"
                    def queue = new Queue([ name: queueName,
                                            permissions: [  canRead:[username],
                                                            canWrite:[username],
                                                            canAdmin:[username]
                                                          ]
                                          ]).save(failOnError: true)
                  }
                }

                topicList.each { topicName ->
                  if (! Topic.countByName(topicName)) {
                    println "#### Building a test topic - ${topicName}"
                    def topic = new Topic([name: topicName])
                    topic.addReader(username)
                    topic.addWriter(username)
                    topic.addAdmin(username)
                    topic.save(flush:true)
                  }
                }

                break
            case "test":
                println "#### Test Mode (Start Up)"
                break
            case "production":
                println "#### Production Mode (Start Up)"
                break
        }
    }
    def destroy = {
        switch(GrailsUtil.environment){
            case "development":
                println "#### Development Mode (Shut Down)"
                break
            case "test":
                println "#### Test Mode (Shut Down)"
                break
            case "production":
                println "#### Production Mode (Shut Down)"
                break
        }
    }
}
