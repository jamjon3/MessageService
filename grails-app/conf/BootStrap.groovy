import edu.usf.MessageService.Topic
import edu.usf.MessageService.Queue
import grails.util.GrailsNameUtils
import grails.util.GrailsUtil

class BootStrap {

    def init = { servletContext ->
        switch(GrailsUtil.environment){
            case "development":
                println "#### Development Mode (Start Up)"
                println "#### Building some test topics"
                println "#### Building some test queues"
                [
                    [name: "TestTopic1"] as Topic,
                    [name: "TestTopic2"] as Topic,
                    [name: "TestTopic3"] as Topic,
                    [name: "TestQueue1"] as Queue,
                    [name: "TestQueue2"] as Queue,
                    [name: "TestQueue3"] as Queue
                ].each { domObj ->
                    if(!domObj.save(flush: true, insert: true, validate: true)) {
                        domObj.errors.allErrors.each {
                            println it
                        }                
                    } else {
                        println "Created new ${GrailsNameUtils.getShortName(domObj.class)} ${domObj.name}"                
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
