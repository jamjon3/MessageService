class UrlMappings {

	static mappings = {

        /**
        Queues
        **/
        "/$auth/queue"(controller:"queue",parseRequest: true){ 
            action = [GET:"listQueues", PUT:"requestError", DELETE:"requestError", POST:"createQueue"] 
        } 
        "/$auth/queue/$name"(controller:"queue",parseRequest: true){ 
            action = [GET:"getNextMessage", PUT:"modifyQueue", DELETE:"deleteQueue", POST:"createQueueMessage"] 
        } 
        "/$auth/queue/$name/$id"(controller:"queue",parseRequest: true){ 
            action = [GET:"viewMessage", PUT:"modifyMessage", DELETE:"deleteMessage", POST:"requestError"] 
        } 
        "/$auth/queue/$name/in-progress"(controller:"queue",parseRequest: true){ 
            action = [GET:"listInProgressMessages", PUT:"requestError", DELETE:"requestError", POST:"requestError"] 
        } 
        "/$auth/queue/$name/peek"(controller:"queue",parseRequest: true){ 
            action = [GET:"peek", PUT:"requestError", DELETE:"requestError", POST:"requestError"] 
        }

        /**
        Topics
        **/

        "/$auth/topic"(controller:"topic", parseRequest:true){ 
            action = [GET:"listTopics", PUT:"requestError", DELETE:"requestError", POST:"createTopic"] 
        } 
        "/$auth/topic/$name"(controller:"topic",parseRequest: true){ 
            action = [GET:"listTopicMessages", PUT:"modifyTopic", DELETE:"deleteTopic", POST:"createTopicMessage"] 
        } 
        "/$auth/topic/$name/filter"(controller:"topic",parseRequest: true){ 
            action = [GET:"filterTopicMessages", PUT:"requestError", DELETE:"requestError", POST:"requestError"] 
        }
        "/$auth/topic/$name/$id"(controller:"topic",parseRequest: true){ 
            action = [GET:"viewMessage", PUT:"modifyMessage", DELETE:"deleteMessage", POST:"requestError"] 
        } 

        /**
        Stats
        **/

        "/$auth/stats"(controller:"stats", parseRequest:true) {
            action = [GET:"index", PUT:"requestError", DELETE:"requestError", POST:"requestError"]
        }
        "/$auth/stats/$statType"(controller:"stats", parseRequest:true) {
            action = [GET:"combinedStats", PUT:"requestError", DELETE:"requestError", POST:"requestError"]
        }
        "/$auth/stats/$statType/topic"(controller:"stats", parseRequest:true) {
            action = [GET:"topicStats", PUT:"requestError", DELETE:"requestError", POST:"requestError"]
        }
        "/$auth/stats/$statType/topic/$name"(controller:"stats", parseRequest:true) {
            action = [GET:"topicStats", PUT:"requestError", DELETE:"requestError", POST:"requestError"]
        }
        "/$auth/stats/$statType/queue"(controller:"stats", parseRequest:true) {
            action = [GET:"queueStats", PUT:"requestError", DELETE:"requestError", POST:"requestError"]
        }
        "/$auth/stats/$statType/queue/$name"(controller:"stats", parseRequest:true) {
            action = [GET:"queueStats", PUT:"requestError", DELETE:"requestError", POST:"requestError"]
        }
        "/$auth/stats/$statType/queue/$name/$status"(controller:"stats", parseRequest:true) {
            action = [GET:"queueStats", PUT:"requestError", DELETE:"requestError", POST:"requestError"]
        }           
        
		"/"(view:"/index",parseRequest: true)
		
		"500"(view:'/error')
	}
}
