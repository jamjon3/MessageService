package edu.usf.cims.MessageService
 
class StubSpringSecurityService {

    def currentUser = 'test-user'

    Object getCurrentUser() {
        return currentUser
    }

    def getAuthentication() {
        return [name: currentUser]
    }

    def setUser(String username) {
        this.currentUser = username
    }

}