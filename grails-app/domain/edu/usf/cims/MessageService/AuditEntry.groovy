package edu.usf.cims.MessageService

import org.bson.types.ObjectId

class AuditEntry {
    ObjectId id
    Date auditTime = new Date()
    String actor = 'UNKNOWN'
    String action = 'UNKNOWN'
    String ipAddress = '0.0.0.0'
    String containerName = 'NONE'
    String containerType = 'SYSTEM'
    String reason = 'NONE'
    HashMap details = [:]

    static constraints = {
        actor( blank: false, nullable: false)
        ipAddress (blank: false, nullable: false)
        containerName(blank: false, nullable: false)
        containerType(blank: false, nullable: false, inList:['QUEUE', 'TOPIC', 'SYSTEM'])
        action( blank: false, nullable: false,
          inList:[
            'UNKNOWN',
            'ERROR',
            'CREATE_QUEUE_ERROR',
            'CREATE_TOPIC_ERROR',
            'CREATE_MESSAGE_ERROR',
            'MODIFY_QUEUE_ERROR',
            'MODIFY_TOPIC_ERROR',
            'MODIFY_MESSAGE_ERROR',
            'VIEW_ERROR',
            'LIST_ERROR',
            'DELETE_MESSAGE_ERROR',
            'DELETE_QUEUE_ERROR',
            'DELETE_TOPIC_ERROR',
            'PEEK_ERROR',
            'INPROGRESS_ERROR',
            'CREATE_MESSAGE',
            'CREATE_TOPIC',
            'CREATE_QUEUE',
            'MODIFY_MESSAGE_STATUS',
            'MODIFY_QUEUE_NAME',
            'MODIFY_TOPIC_NAME',
            'MODIFY_TOPIC_PERM',
            'MODIFY_QUEUE_PERM',
            'VIEW_MESSAGE',
            'VIEW_TOPIC',
            'LIST_TOPICS',
            'LIST_QUEUES',
            'DELETE_MESSAGE',
            'DELETE_QUEUE',
            'DELETE_TOPIC',
            'PEEK',
            'INPROGRESS'
            ]
        )
    }

    static mapping = {
        collection "audit"
        auditTime index:true
        actor index:true
        action index:true
        containerName index:true
        containerType index:true
    }
}
