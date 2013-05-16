grails {
    mongo {
	replicaPair = [ "127.0.0.1:27117", "127.0.0.1:27118", "127.0.0.1:27119" ]
        databaseName = "MessageService"
        options {
            autoConnectRetry = true
	    connectTimeout = 3000
        }
    }
}        
