import edu.usf.cims.MessageService.Topic
import edu.usf.cims.MessageService.Queue
import grails.util.GrailsNameUtils
import grails.util.GrailsUtil

class BootStrap {

    def init = { servletContext ->
        switch(GrailsUtil.environment){
            case "development":
                println "#### Development Mode (Start Up)"
                println "#### Building some test topics"

                def username = 'it-msgsvcadm'
                def queue = new Queue([name: "TestQueue"])
                queue.addReader(username)
                queue.addWriter(username)
                queue.addAdmin(username)
                queue.save(flush:true) 
                
                println "#### Building some test queues"
                def topic = new Topic([name: "TestTopic"])
                topic.addReader(username)
                topic.addWriter(username)
                topic.addAdmin(username)
                topic.save(flush:true) 
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
