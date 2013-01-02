package edu.usf.MessageService

import static org.junit.Assert.*
import org.junit.*

class QueueServiceTests {
    def queueService

    @Before
    void setup() {
        //This runs before each test
        def queue = new Queue([name: "TestQueue1"])
        queue.save(flush:true) 

        def queue2 = new Queue([name: "TestQueue2"])
        queue2.save(flush:true)
        
        new Queue([name: "TestQueue3"]).save(flush:true)
        new Queue([name: "netidChange"]).save(flush:true)

        new Message([creator: "it-msgsvcadm", apiVersion: 1, status: "pending", createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-05-01T08:51:00"),createProg: "QueueServiceTests",messageData:[data: "message1"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])]).save(flush:true)
        new Message([creator: "it-msgsvcadm", apiVersion: 1, status: "in-progress", createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-06-01T09:51:00"),createProg: "QueueServiceTests",messageData:[data: "message2"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])]).save(flush:true)
        new Message([creator: "it-msgsvcadm", apiVersion: 1, status: "pending", createProg: "QueueServiceTests",messageData:[data: "message3"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])]).save(flush:true)
        new Message([creator: "it-msgsvcadm", apiVersion: 1, status: "pending", createProg: "QueueServiceTests",messageData:[data: "message4"],messageContainer:new MessageContainer([type:"queue",id:queue2.id,name:queue2.name])]).save(flush:true)
    }

    @After
    void tearDown() {       
        //This runs after each test
        //Remove data from test database
        Queue.collection.drop()
        Message.collection.drop()
    }

/**
    listQueue Tests
**/
  @Test
    void testListQueues() {
        
        def queueList = queueService.listQueues()
        assert queueList.size == 4
        assert queueList[0].name == "TestQueue1"
        assert queueList[1].name == "TestQueue2"
        assert queueList[2].name == "TestQueue3"
        
    }

  @Test
    void testListQueuesPattern() {
        def queueList = queueService.listQueues("^(netid).*")
        assert queueList.size == 1
        assert queueList[0].name == "netidChange"
    }   

/**
    addQueue Tests
**/
  @Test
    void testaddQueue() {
        def queue = queueService.addQueue('test-user', [messageData: [name: "newQueue"]])
        assert queue.name == "newQueue"       
        assert Queue.collection.count() == 5
    }

  @Test
    void testaddQueueBadName() {
        def queue = queueService.addQueue('test-user', [messageData: [name: "new Queue"]])
        assert queue == "validator.invalid"       
        assert Queue.collection.count() == 4
    }

  @Test
    void testaddQueueDuplicateName() {
        def queue = queueService.addQueue('test-user', [messageData: [name: "TestQueue1"]])
        assert queue == "unique"       
        assert Queue.collection.count() == 4
    }

  @Test
    void testaddQueueNoData() {
        def queue = queueService.addQueue('test-user', [:])
        assert queue == null      
        assert Queue.collection.count() == 4
    }

/**
    modifyQueue Tests
**/
  @Test
    void testModifyQueueSuccess() {
        def queue = queueService.modifyQueue('it-msgsvcadm', 'TestQueue1', [messageData: [name: "NewQueueName"]])
        assert queue.name == "NewQueueName"        
    }

  @Test
    void testModifyQueueFailQueueMissing() {
        def queue = queueService.modifyQueue('it-msgsvcadm', 'BadQueueName', [messageData: [name: "NewQueueName"]])
        assert queue == "QueueNotFound"        
    }

  @Test
    void testModifyQueueFailNotAuthorized() {
        def queue = queueService.modifyQueue('test-user', 'TestQueue1', [messageData: [name: "NewQueueName2"]])
        assert queue == "NotAuthorized"        
    }

  @Test
    void testModifyQueueFailDuplicateName() {
        def queue = queueService.modifyQueue('it-msgsvcadm', 'TestQueue1', [messageData: [name: "TestQueue2"]])
        assert queue == "unique"        
    }

  @Test
    void testModifyQueueBadName() {
        def queue = queueService.modifyQueue('it-msgsvcadm', 'TestQueue1', [messageData: [name: "Test Queue2"]])
        assert queue == "validator.invalid"        
    }
/**
    deleteQueue Tests
**/
  @Test
    void testDeleteQueue() {
        def queue = queueService.deleteQueue('it-msgsvcadm', 'TestQueue1')
        assert Queue.collection.count() == 4
        assert Message.collection.count() == 1               
    }

  @Test
    void testDeleteQueueNotFound() {
        def queue = queueService.deleteQueue('it-msgsvcadm', 'BadQueueName')
        assert queue == "QueueNotFound"
    }

/**
    modifyPermissions Tests
**/
  @Test
    void testModifyPermissionsSuccess() {
        def messageData = [ messageData: 
                                [ permissions :                                     
                                    [ 
                                        canRead : [add : ["epierce","chance"], remove : [] ],
                                        canWrite : [add : ["chance"], remove : [] ],
                                        canAdmin : [add : ["epierce"], remove : [] ]
                                    ]
                                ]
                            ]
                            
        def queue = queueService.modifyPermissions('it-msgsvcadm', 'TestQueue1', messageData)
        assert queue.canRead.add == 2
        assert queue.canWrite.add == 1
        assert queue.canAdmin.add == 1        
    }

  @Test
    void testModifyPermissionsFailQueueMissing() {
        def messageData = [ messageData: 
                                [ permissions :                                     
                                    [ 
                                        canRead : [add : ["epierce","chance"], remove : [] ],
                                        canWrite : [add : ["chance"], remove : [] ],
                                        canAdmin : [add : ["epierce"], remove : [] ]
                                    ]
                                ]
                            ]
        def queue = queueService.modifyPermissions('it-msgsvcadm', 'BadQueueName', [messageData: [name: "NewQueueName"]])
        assert queue == "QueueNotFound"        
    }

  @Test
    void testModifyPermissionsFailNotAuthorized() {
        def messageData = [ messageData: 
                                [ permissions :                                     
                                    [ 
                                        canRead : [add : ["epierce","chance"], remove : [] ],
                                        canWrite : [add : ["chance"], remove : [] ],
                                        canAdmin : [add : ["epierce"], remove : [] ]
                                    ]
                                ]
                            ]
        def queue = queueService.modifyPermissions('test-user', 'TestQueue1', [messageData: [name: "NewQueueName2"]])
        assert queue == "NotAuthorized"        
    }

  @Test
    void testModifyPermissionsFailNoMessageData() {
        def messageData = [ messageData: []]
        def queue = queueService.modifyPermissions('it-msgsvcadm', 'TestQueue1', [messageData: [name: "NewQueueName2"]])
        assert queue == "NoPermissionData"        
    }
/**
    getNextMessage Tests
**/
  @Test
    void testGetNextMessage() {
        def queueMessage = queueService.getNextMessage('it-msgsvcadm', "TestQueue1", "127.0.0.1")
        assert queueMessage.createProg == "QueueServiceTests"
        assert queueMessage.status == "in-progress"
    }

  @Test
    void testGetNextMessageNotAuthorized() {
        def queueMessage = queueService.getNextMessage('test-user', "TestQueue1", "127.0.0.1")
        assert queueMessage == 'NotAuthorized'
    }

  @Test
    void testListQueueMessagesNotFound() {
        def queueMessage = queueService.getNextMessage('it-msgsvcadm', "BadQueueName", "127.0.0.1")
        assert queueMessage == 'QueueNotFound'
    }

/**
    peek Tests
**/
  @Test
    void testPeek() {
        def queueMessages = queueService.peek('it-msgsvcadm', "TestQueue1", 10)
        assert queueMessages.size == 2
        assert queueMessages[0].messageData.data == "message1"
    }

  @Test
    void testPeekNotAuthorized() {
        def queueMessage = queueService.peek('test-user', "TestQueue1")
        assert queueMessage == 'NotAuthorized'
    }

  @Test
    void testPeekNotFound() {
        def queueMessage = queueService.peek('it-msgsvcadm', "BadQueueName", 5)
        assert queueMessage == 'QueueNotFound'
    }

/**
    listInProgressMessages Tests
**/
  @Test
    void testListInProgressMessages() {
        def queueMessages = queueService.listInProgressMessages('it-msgsvcadm', "TestQueue1")
        assert queueMessages.size == 1
        assert queueMessages[0].createProg == "QueueServiceTests"
        assert queueMessages[0].status == "in-progress"
    }

  @Test
    void testlistInProgressMessagesNotAuthorized() {
        def queueMessage = queueService.listInProgressMessages('test-user', "TestQueue1")
        assert queueMessage == 'NotAuthorized'
    }

  @Test
    void testlistInProgressMessagesNotFound() {
        def queueMessage = queueService.listInProgressMessages('it-msgsvcadm', "BadQueueName")
        assert queueMessage == 'QueueNotFound'
    }

/**
    CreateQueueMessages Tests
**/
  @Test
    void testCreateQueueMessage() {
        def queueMessage = queueService.createQueueMessage('it-msgsvcadm', 'TestQueue1', [ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])     
       
        assert queueMessage.createProg == "serviceTest2"
        assert queueMessage.messageData.mydata == "new message"
        assert queueMessage.messageDetails.messageContainer.type == "queue"

    }

  @Test
    void testCreateQueueMessageNotAuthorized() {
        def queueMessage = queueService.createQueueMessage('test-user', 'TestQueue1', [ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])     
        assert queueMessage == "NotAuthorized"
        assert Queue.collection.count() == 4
    }

  @Test
    void testCreateQueueMessageNotFound() {
        def queueMessage = queueService.createQueueMessage('it-msgsvcadm', 'BadQueueName', [ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])     
        assert queueMessage == "QueueNotFound"
        assert Queue.collection.count() == 4
    }

  @Test
    void testCreateQueueMessageNoAPI() {
        def queueMessage = queueService.createQueueMessage('it-msgsvcadm', 'TestQueue1', [ createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])     
        assert queueMessage == "NoApiVersion"
        assert Queue.collection.count() == 4
    }

  @Test
    void testCreateQueueMessageNoCreateProgram() {
        def queueMessage = queueService.createQueueMessage('it-msgsvcadm', 'TestQueue1', [ apiVersion:1, "messageData" : [mydata: "new message", mydata2: "blah"] ])     
        assert queueMessage == "NoCreateProgram"
        assert Queue.collection.count() == 4
    }

  @Test
    void testCreateQueueMessageNoMessage() {
        def queueMessage = queueService.createQueueMessage('it-msgsvcadm', 'TestQueue1', [ apiVersion:1, createProg: "serviceTest2" ])     
        assert queueMessage == "NoMessageData"
        assert Queue.collection.count() == 4
    }

/**
    ViewMessage Tests
**/
  @Test
    void testViewMessage() {
        def message = queueService.createQueueMessage('it-msgsvcadm', "TestQueue2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def queueMessage = queueService.viewMessage('it-msgsvcadm', "TestQueue2", message.id as String)
        assert queueMessage.messageData.mydata == "new message"
    } 

  @Test
    void testViewMessageNotAuthorized() {
        def message = queueService.createQueueMessage('it-msgsvcadm', "TestQueue2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def queueMessage = queueService.viewMessage('test-user', "TestQueue2", message.id as String)
        assert queueMessage == "NotAuthorized"
    }

  @Test
    void testViewMessageQueueNotFound() {
        def message = queueService.createQueueMessage('it-msgsvcadm', "TestQueue2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def queueMessage = queueService.viewMessage('it-msgsvcadm', "BadQueueName", message.id as String)
        assert queueMessage == "QueueNotFound"
    }

  @Test
    void testViewMessageNotFound() {
        def message = queueService.createQueueMessage('it-msgsvcadm', "TestQueue2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def queueMessage = queueService.viewMessage('it-msgsvcadm', "TestQueue2", "50d09bf489a87426a9cdcdb4")
        assert queueMessage == "MessageNotFound"
    }

  @Test
    void testViewMessageWrongQueue() {
        def message = queueService.createQueueMessage('it-msgsvcadm', "TestQueue2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def queueMessage = queueService.viewMessage('it-msgsvcadm', "TestQueue1", message.id as String)
        assert queueMessage == "WrongQueueName"
    }    

/**
    deleteMessages Tests
**/
  @Test  
    void testDeleteMessage() {        
        def message = queueService.createQueueMessage('it-msgsvcadm', "TestQueue2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def queueMessage = queueService.deleteMessage('it-msgsvcadm', "TestQueue2", message.id as String)     
        assert queueMessage.messageData.mydata == "new message"
        assert Message.collection.count() == 4
    } 

  @Test
    void testDeleteMessageNotAuthorized() {
        def message = queueService.createQueueMessage('it-msgsvcadm', "TestQueue2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def queueMessage = queueService.deleteMessage('test-user', "TestQueue2", message.id as String)
        assert queueMessage == "NotAuthorized"
    }

  @Test
    void testDeleteMessageQueueNotFound() {
        def message = queueService.createQueueMessage('it-msgsvcadm', "TestQueue2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def queueMessage = queueService.deleteMessage('it-msgsvcadm', "BadQueueName", message.id as String)
        assert queueMessage == "QueueNotFound"
    }

  @Test
    void testDeleteMessageNotFound() {
        def message = queueService.createQueueMessage('it-msgsvcadm', "TestQueue2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def queueMessage = queueService.deleteMessage('it-msgsvcadm', "TestQueue2", "50d09bf489a87426a9cdcdb4")
        assert queueMessage == "MessageNotFound"
    }

  @Test
    void testDeleteMessageWrongQueue() {
        def message = queueService.createQueueMessage('it-msgsvcadm', "TestQueue2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def queueMessage = queueService.deleteMessage('it-msgsvcadm', "TestQueue1", message.id as String)
        assert queueMessage == "WrongQueueName"
    }        

}