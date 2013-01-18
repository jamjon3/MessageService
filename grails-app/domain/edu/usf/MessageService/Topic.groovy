package edu.usf.MessageService

import org.bson.types.ObjectId

class Topic {
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

    def render() { 
        return [
            name: name
        ]
    }

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

    public String toString() {
        return "${name} (Topic)"
    }
}
