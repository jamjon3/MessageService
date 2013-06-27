package edu.usf.cims.MessageService

import org.bson.types.ObjectId

class MessageContainer{
    Map messageContainer
    MessageContainer(Map messageContainer) {
        this.messageContainer = messageContainer
    }

    public String toString(){
        return this.messageContainer.type as String
    }

    public String getType(){
        this.messageContainer.type
    }
    public String getName(){
        this.messageContainer.name
    }
    public ObjectId getId(){
        this.messageContainer.id
    }

    public boolean isQueue() {
        this.messageContainer.type == "queue" ? true : false
    }

    public boolean isTopic() {
        this.messageContainer.type == "topic" ? true : false
    }
}