package edu.usf.MessageService

import org.bson.types.ObjectId

class Queue {
    ObjectId id
    String name
    Map permissions = [ canRead:['it-msgsvcadm'], canWrite:['it-msgsvcadm'], canAdmin:['it-msgsvcadm'] ]

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
        permissions.canRead.add(username)
    }

    def addWriter(username){
        permissions.canWrite.add(username)
    }

    def addAdmin(username){
        permissions.canAdmin.add(username)
    }

    def removeReader(username){
        if(username != 'it-msgsrvadm'){
            permissions.canRead.remove(username)
        } 
    }

    def removeWriter(username){
        if(username != 'it-msgsrvadm'){
            permissions.canWrite.remove(username)
        }
    }

    def removeAdmin(username){
        if(username != 'it-msgsrvadm'){
            permissions.canAdmin.remove(username)
        }
    }
    
    def render() { 
        return [
            name: name
        ]
    }
    
    public String toString() {
        render()
    }
}