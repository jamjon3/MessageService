grails {
    mongo {
        databaseName = "MessageService"
        options {
          autoConnectRetry = true
	         connectTimeout = 3000
        }
    }
}        
