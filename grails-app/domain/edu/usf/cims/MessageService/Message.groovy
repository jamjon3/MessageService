package edu.usf.cims.MessageService

import org.bson.types.ObjectId

class Message {
    ObjectId id
    Date createTime = new Date()
    String creator
    String createProg
    String apiVersion
    String status = 'n/a'
    Map messageData
    MessageContainer messageContainer

    static constraints = {
        status(inList:['in-progress','pending','completed','error','n/a'],blank:false,nullable:false)
        createProg(blank:false,nullable:false)
    }

    static mapping = {
        createTime index:true
        messageContainer index:true
    }

    def render() { 
        def text = [    id: id as String,
                        creator: creator,
                        messageDetails: [ 
                          messageContainer: [
                            type: messageContainer.type, 
                            id: messageContainer.id as String, 
                            name: messageContainer.name] 
                        ],
                        apiVersion: apiVersion,
                        createTime: createTime,
                        createProg: createProg,
                        messageData: messageData
                    ]
                    if (messageContainer.isQueue()) {
                        text.status = status
                        if (this["taken"]) text.messageDetails.taken = this["taken"]
                        if (this["updated"]) text.messageDetails.updated = this["updated"]
                    }
                    
        return text
    }

    public String toString() {
            render() as String
    }
    
}
