package edu.usf.cims.MessageService

import org.bson.types.ObjectId

class Queue {
    ObjectId id
    String name
    Map permissions = [ canRead:['it-msgsvcadm'], canWrite:['it-msgsvcadm'], canAdmin:['it-msgsvcadm'] ]
    Map stats = ['in-progress' : 0, 'pending' : 0 , 'error' : 0]

    static constraints = {
        name(   
                unique: true,
                blank: false,
                nullable: false,
                size: 3..255,
                //Custom constraint - only allow upper, lower, digits, dash and underscore
                validator: { val, obj -> val ==~ /[A-Za-z0-9_-]+/ }
            )       
    }
    
    static mapping = {}

    def canRead(username){
        permissions.canRead.contains(username)
    }

    def canWrite(username){
        permissions.canWrite.contains(username)
    }

    def canAdmin(username){
        permissions.canAdmin.contains(username)
    }

    def getReaders() {
        return permissions.canRead
    }

    def getWriters() {
        return permissions.canWrite
    }

    def getAdmins() {
        return permissions.canAdmin
    }

    def addReader(username){
        if (! canRead(username)) permissions.canRead.add(username)
    }

    def addWriter(username){
        if (! canWrite(username)) permissions.canWrite.add(username)
    }

    def addAdmin(username){
        if (! canAdmin(username)) permissions.canAdmin.add(username)
    }

    def removeReader(username){
        if((username != 'it-msgsrvadm')&&(canRead(username))){
            permissions.canRead.remove(username)
        } 
    }

    def removeWriter(username){
        if((username != 'it-msgsrvadm')&&(canWrite(username))){
            permissions.canWrite.remove(username)
        }
    }

    def removeAdmin(username){
        if((username != 'it-msgsrvadm')&&(canAdmin(username))){
            permissions.canAdmin.remove(username)
        }
    }
    
    def render() { 
        def count = 0
        stats.each { key, value ->
          count = count + value
        }
        def result = [
            name: name,
            stats: [
              messages: count,
              status: stats
            ]
        ]
        return result
    }
    
    public String toString() {
        render()
    }
}