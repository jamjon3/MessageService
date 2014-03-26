package edu.usf.cims.MessageService

import static org.junit.Assert.*
import org.junit.*

class TopicServiceTests {
    def topicService

    @Before
    void setup() {
        //This runs before each test
        def topic = new Topic([name: "TestTopic1"])
        topic.save(flush:true)

        def topic2 = new Topic([name: "TestTopic2"])
        topic2.save(flush:true)

        new Topic([name: "TestTopic3"]).save(flush:true)
        new Topic([name: "netidChange"]).save(flush:true)

        new Message([creator: "it-msgsvcadm", apiVersion: 1,createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-05-01T08:51:00"),createProg: "TopicServiceTests",messageData:[data: "message1"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])]).save(flush:true)
        new Message([creator: "it-msgsvcadm", apiVersion: 1,createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-06-01T09:51:00"),createProg: "TopicServiceTests",messageData:[data: "message2"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])]).save(flush:true)
        new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "TopicServiceTests",messageData:[data: "message3"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])]).save(flush:true)
        new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "TopicServiceTests",messageData:[data: "message4"],messageContainer:new MessageContainer([type:"topic",id:topic2.id,name:topic2.name])]).save(flush:true)
    }

    @After
    void tearDown() {
        //This runs after each test
        //Remove data from test database
        Topic.collection.drop()
        Message.collection.drop()
    }

/**
    listTopic Tests
**/
  @Test
    void testListTopics() {

        def topicList = topicService.listTopics()
        assert topicList.size == 4
        assert topicList[0].name == "TestTopic1"
        assert topicList[1].name == "TestTopic2"
        assert topicList[2].name == "TestTopic3"

    }

  @Test
    void testListTopicsPattern() {
        def topicList = topicService.listTopics("^(netid).*")
        assert topicList.size == 1
        assert topicList[0].name == "netidChange"
    }

/**
    addTopic Tests
**/
  @Test
    void testaddTopic() {
        def topic = topicService.addTopic('test-user', [messageData: [name: "newTopic"]])
        assert topic.name == "newTopic"
        assert Topic.collection.count() == 5
    }

  @Test
    void testaddTopicBadName() {
        def topic = topicService.addTopic('test-user', [messageData: [name: "new Topic"]])
        assert topic == "validator.invalid"
        assert Topic.collection.count() == 4
    }

  @Test
    void testaddTopicDuplicateName() {
        def topic = topicService.addTopic('test-user', [messageData: [name: "TestTopic1"]])
        assert topic == "unique"
        assert Topic.collection.count() == 4
    }

  @Test
    void testaddTopicNoData() {
        def topic = topicService.addTopic('test-user', [:])
        assert topic == null
        assert Topic.collection.count() == 4
    }

/**
    modifyTopic Tests
**/
  @Test
    void testModifyTopicSuccess() {
        def topic = topicService.modifyTopic('it-msgsvcadm', 'TestTopic1', [messageData: [name: "NewTopicName"]])
        assert topic.name == "NewTopicName"
    }

  @Test
    void testModifyTopicFailTopicMissing() {
        def topic = topicService.modifyTopic('it-msgsvcadm', 'BadTopicName', [messageData: [name: "NewTopicName"]])
        assert topic == "TopicNotFound"
    }

  @Test
    void testModifyTopicFailNotAuthorized() {
        def topic = topicService.modifyTopic('test-user', 'TestTopic1', [messageData: [name: "NewTopicName2"]])
        assert topic == "NotAuthorized"
    }

  @Test
    void testModifyTopicFailDuplicateName() {
        def topic = topicService.modifyTopic('it-msgsvcadm', 'TestTopic1', [messageData: [name: "TestTopic2"]])
        assert topic == "unique"
    }

  @Test
    void testModifyTopicBadName() {
        def topic = topicService.modifyTopic('it-msgsvcadm', 'TestTopic1', [messageData: [name: "Test Topic2"]])
        assert topic == "validator.invalid"
    }
/**
    deleteTopic Tests
**/
  @Test
    void testDeleteTopic() {
        def topic = topicService.deleteTopic('it-msgsvcadm', 'TestTopic1')
        assert Topic.collection.count() == 4
        assert Message.collection.count() == 1
    }

  @Test
    void testDeleteTopicNotFound() {
        def topic = topicService.deleteTopic('it-msgsvcadm', 'BadTopicName')
        assert topic == "TopicNotFound"
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

        def topic = topicService.modifyPermissions('it-msgsvcadm', 'TestTopic1', messageData)
        assert topic.canRead.add == 2
        assert topic.canWrite.add == 1
        assert topic.canAdmin.add == 1
    }

  @Test
    void testModifyPermissionsFailTopicMissing() {
        def messageData = [ messageData:
                                [ permissions :
                                    [
                                        canRead : [add : ["epierce","chance"], remove : [] ],
                                        canWrite : [add : ["chance"], remove : [] ],
                                        canAdmin : [add : ["epierce"], remove : [] ]
                                    ]
                                ]
                            ]
        def topic = topicService.modifyPermissions('it-msgsvcadm', 'BadTopicName', [messageData: [name: "NewTopicName"]])
        assert topic == "TopicNotFound"
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
        def topic = topicService.modifyPermissions('test-user', 'TestTopic1', [messageData: [name: "NewTopicName2"]])
        assert topic == "NotAuthorized"
    }

  @Test
    void testModifyPermissionsFailNoMessageData() {
        def messageData = [ messageData: []]
        def topic = topicService.modifyPermissions('it-msgsvcadm', 'TestTopic1', [messageData: [name: "NewTopicName2"]])
        assert topic == "NoPermissionData"
    }
/**
    listTopicMessages Tests
**/
  @Test
    void testListTopicMessages() {
        def topicMessages = topicService.listTopicMessages('it-msgsvcadm', "TestTopic1")
        assert topicMessages.size == 3
        assert topicMessages[0].createProg == "TopicServiceTests"
        assert topicMessages[1].messageData.data == "message2"
        assert topicMessages[2].messageData.data == "message3"
    }

  @Test
    void testListTopicMessagesNotAuthorized() {
        def topicMessages = topicService.listTopicMessages('test-user', "TestTopic1")
        assert topicMessages == 'NotAuthorized'
    }

  @Test
    void testListTopicMessagesNotFound() {
        def topicMessages = topicService.listTopicMessages('it-msgsvcadm', "BadTopicName")
        assert topicMessages == 'TopicNotFound'
    }

/**
    filterTopicMessages Tests
**/
  @Test
    void testFilterTopicMessagesStartOnly() {
        def startTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss","2012-06-01T08:52:00")
        def topicMessages = topicService.filterTopicMessages('it-msgsvcadm', "TestTopic1", startTime, null)
        assert topicMessages.size == 2
        assert topicMessages[0].messageData.data == "message2"

    }

  @Test
    void testFilterTopicMessagesStartAndEnd() {
        def startTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss","2012-04-01T08:52:00")
        def endTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss","2012-07-07T08:52:00")

        def topicMessages = topicService.filterTopicMessages('it-msgsvcadm', "TestTopic1", startTime, endTime)
        assert topicMessages.size == 2
        assert topicMessages[0].messageData.data == "message1"
        assert topicMessages[1].messageData.data == "message2"
    }

  @Test
    void testFilterTopicMessagesNotAuthorized() {
        def startTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss","2012-04-01T08:52:00")
        def endTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss","2012-07-07T08:52:00")

        def topicMessages = topicService.filterTopicMessages('test-user', "TestTopic1", startTime, endTime)
        assert topicMessages == 'NotAuthorized'
    }

  @Test
    void testFilterTopicMessagesNotFound() {
        def startTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss","2012-04-01T08:52:00")
        def endTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss","2012-07-07T08:52:00")

        def topicMessages = topicService.filterTopicMessages('it-msgsvcadm', "BadTopicName", startTime, endTime)
        assert topicMessages == 'TopicNotFound'
    }

/**
    CreateTopicMessages Tests
**/
  @Test
    void testCreateTopicMessage() {
        def topicMessage = topicService.createTopicMessage('it-msgsvcadm', 'TestTopic1', [ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])

        assert topicMessage.createProg == "serviceTest2"
        assert topicMessage.messageData.mydata == "new message"
        assert topicMessage.messageDetails.messageContainer.type == "topic"

    }

  @Test
    void testCreateTopicMessageNotAuthorized() {
        def topicMessage = topicService.createTopicMessage('test-user', 'TestTopic1', [ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        assert topicMessage == "NotAuthorized"
        assert Topic.collection.count() == 4
    }

  @Test
    void testCreateTopicMessageNotFound() {
        def topicMessage = topicService.createTopicMessage('it-msgsvcadm', 'BadTopicName', [ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        assert topicMessage == "TopicNotFound"
        assert Topic.collection.count() == 4
    }

  @Test
    void testCreateTopicMessageNoAPI() {
        def topicMessage = topicService.createTopicMessage('it-msgsvcadm', 'TestTopic1', [ createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        assert topicMessage == "NoApiVersion"
        assert Topic.collection.count() == 4
    }

  @Test
    void testCreateTopicMessageNoCreateProgram() {
        def topicMessage = topicService.createTopicMessage('it-msgsvcadm', 'TestTopic1', [ apiVersion:1, "messageData" : [mydata: "new message", mydata2: "blah"] ])
        assert topicMessage == "NoCreateProgram"
        assert Topic.collection.count() == 4
    }

  @Test
    void testCreateTopicMessageNoMessage() {
        def topicMessage = topicService.createTopicMessage('it-msgsvcadm', 'TestTopic1', [ apiVersion:1, createProg: "serviceTest2" ])
        assert topicMessage == "NoMessageData"
        assert Topic.collection.count() == 4
    }

/**
    ViewMessage Tests
**/
  @Test
    void testViewMessage() {
        def message = topicService.createTopicMessage('it-msgsvcadm', "TestTopic2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def topicMessage = topicService.viewMessage('it-msgsvcadm', "TestTopic2", message.id as String)
        assert topicMessage.messageData.mydata == "new message"
    }

  @Test
    void testViewMessageNotAuthorized() {
        def message = topicService.createTopicMessage('it-msgsvcadm', "TestTopic2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def topicMessage = topicService.viewMessage('test-user', "TestTopic2", message.id as String)
        assert topicMessage == "NotAuthorized"
    }

  @Test
    void testViewMessageTopicNotFound() {
        def message = topicService.createTopicMessage('it-msgsvcadm', "TestTopic2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def topicMessage = topicService.viewMessage('it-msgsvcadm', "BadTopicName", message.id as String)
        assert topicMessage == "TopicNotFound"
    }

  @Test
    void testViewMessageNotFound() {
        def message = topicService.createTopicMessage('it-msgsvcadm', "TestTopic2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def topicMessage = topicService.viewMessage('it-msgsvcadm', "TestTopic2", "50d09bf489a87426a9cdcdb4")
        assert topicMessage == "MessageNotFound"
    }

  @Test
    void testViewMessageWrongTopic() {
        def message = topicService.createTopicMessage('it-msgsvcadm', "TestTopic2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def topicMessage = topicService.viewMessage('it-msgsvcadm', "TestTopic1", message.id as String)
        assert topicMessage == "WrongTopicName"
    }

/**
    deleteMessages Tests
**/
  @Test
    void testDeleteMessage() {
        def message = topicService.createTopicMessage('it-msgsvcadm', "TestTopic2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def topicMessage = topicService.deleteMessage('it-msgsvcadm', "TestTopic2", message.id as String)
        assert topicMessage.messageData.mydata == "new message"
        assert Message.collection.count() == 4
    }

  @Test
    void testDeleteMessageNotAuthorized() {
        def message = topicService.createTopicMessage('it-msgsvcadm', "TestTopic2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def topicMessage = topicService.deleteMessage('test-user', "TestTopic2", message.id as String)
        assert topicMessage == "NotAuthorized"
    }

  @Test
    void testDeleteMessageTopicNotFound() {
        def message = topicService.createTopicMessage('it-msgsvcadm', "TestTopic2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def topicMessage = topicService.deleteMessage('it-msgsvcadm', "BadTopicName", message.id as String)
        assert topicMessage == "TopicNotFound"
    }

  @Test
    void testDeleteMessageNotFound() {
        def message = topicService.createTopicMessage('it-msgsvcadm', "TestTopic2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def topicMessage = topicService.deleteMessage('it-msgsvcadm', "TestTopic2", "50d09bf489a87426a9cdcdb4")
        assert topicMessage == "MessageNotFound"
    }

  @Test
    void testDeleteMessageWrongTopic() {
        def message = topicService.createTopicMessage('it-msgsvcadm', "TestTopic2",[ apiVersion:1, createProg: "serviceTest2","messageData" : [mydata: "new message", mydata2: "blah"] ])
        def topicMessage = topicService.deleteMessage('it-msgsvcadm', "TestTopic1", message.id as String)
        assert topicMessage == "WrongTopicName"
    }

}
