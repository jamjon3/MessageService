package edu.usf.cims.MessageService

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.TestFor
import grails.converters.JSON

class QueueControllerTests extends GroovyTestCase {
    def queueService
    def stubSpringSecurityService = new StubSpringSecurityService()

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

    @Test
    void testListQueues() {    
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.listQueues()
        assert controller.response.status == 200
        assert controller.response.contentAsString == '{"count":4,"queues":[{"name":"TestQueue1"},{"name":"TestQueue2"},{"name":"TestQueue3"},{"name":"netidChange"}]}'
    }

  @Test
    void testListQueuesPattern() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.contentType = "application/xml"
        controller.request.addHeader("Accept", "application/xml")

        controller.params.return = "XML"
        controller.params.pattern = "^(netid).*"
        controller.request.method = "GET"
        
        controller.listQueues()
        assert controller.response.status == 200
        
        assert controller.response.contentAsString =~ /<\?xml version="1.0" encoding="UTF-8"\?><map><entry key="count">1<\/entry><entry key="queues"><map><entry key="name">netidChange<\/entry><\/map><\/entry><\/map>/
    }

  @Test
    void testAddQueue() {

        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.request.content = '{"messageData": {"name": "newQueue"} }'

        controller.createQueue()
        
        assert controller.response.status == 200
        assert controller.response.contentAsString == '{"count":1,"queues":{"name":"newQueue"}}'
    }

  @Test
    void testAddQueueDuplicateName() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.request.content = '{"messageData": {"name": "TestQueue1"} }'

        controller.createQueue()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Create failed: TestQueue1 already exists!"}'
    }

  @Test
    void testAddQueueBadName() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "oldQueue"
        controller.request.content = '{"messageData": {"name": "Test Queue1"} }'

        controller.createQueue()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Create failed: Test Queue1 invalid name!"}'
    }

  @Test
    void testModifyQueue() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue1"
        controller.request.content = '{"messageData": {"name": "oldQueue"} }'

        controller.modifyQueue()
        assert controller.response.status == 200
        assert controller.response.contentAsString == '{"count":1,"queues":{"name":"oldQueue"}}'
    }

  @Test
    void testModifyQueueDuplicateName() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue1"
        controller.request.content = '{"messageData": {"name": "TestQueue2"} }'

        controller.modifyQueue()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Update failed: TestQueue2 already exists!"}'
    }

  @Test
    void testModifyQueueBadName() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue1"
        controller.request.content = '{"messageData": {"name": "Test Queue1"} }'

        controller.modifyQueue()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Update failed: Test Queue1 invalid name!"}'
    }

  @Test
    void testModifyQueueNotFound() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
        
        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue10"
        controller.request.content = '{"messageData": {"name": "TestQueue11"} }'

        controller.modifyQueue()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Update failed: TestQueue10 does not exist"}'
    }

  @Test
    void testModifyQueueNotAuthorized() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")
        
        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue1"
        controller.request.content = '{"messageData": {"name": "TestQueue11"} }'

        controller.modifyQueue()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

  @Test
    void testModifyPermissions() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
        
        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue1"
        controller.request.content = '{"messageData":{"permissions":{"canRead":{"add":["epierce","chance"],"remove":[]},"canWrite":{"add":["chance"],"remove":[]},"canAdmin":{"add":["epierce"],"remove":[]}}}}'
        
        controller.modifyQueue()
        assert controller.response.status == 200
        assert controller.response.contentAsString == '{"canRead":{"add":2,"remove":0},"canWrite":{"add":1,"remove":0},"canAdmin":{"add":1,"remove":0}}'
    }

  @Test
    void testModifyPermissionsNotFound() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
        
        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue11"
        controller.request.content = '{"messageData":{"permissions":{"canRead":{"add":["epierce","chance"],"remove":[]},"canWrite":{"add":["chance"],"remove":[]},"canAdmin":{"add":["epierce"],"remove":[]}}}}'
        
        controller.modifyQueue()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Update failed: TestQueue11 does not exist"}'
    }

  @Test
    void testModifyPermissionsNoData() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
        
        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue1"
        controller.request.content = ''
        
        controller.modifyQueue()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Message data required"}'
    }

  @Test
    void testModifyPermissionsNotAuthorized() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")
        
        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue1"
        controller.request.content = '{"messageData":{"permissions":{"canRead":{"add":["epierce","chance"],"remove":[]},"canWrite":{"add":["chance"],"remove":[]},"canAdmin":{"add":["epierce"],"remove":[]}}}}'
        
        controller.modifyQueue()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

  @Test
    void testDeleteQueue() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "DELETE"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue1"

        controller.deleteQueue()
        assert controller.response.status == 200
        assert controller.response.contentAsString == '{"count":1,"queues":{"name":"TestQueue1"}}'
    }

  @Test
    void testDeleteQueueNotFound() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "DELETE"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "oldQueue2"

        controller.deleteQueue()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"ResourceNotFound: oldQueue2 does not exist"}'
    }

  @Test
    void testDeleteQueueNotAuthorized() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")
        
        controller.request.method = "DELETE"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue1"

        controller.deleteQueue()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

 @Test
    void testGetNextMessage() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
 
        controller.params.name = "TestQueue1"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.request.remoteAddr = "127.0.0.1"
 
        controller.getNextMessage()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*taken.*ipAddr.*message1.*/
    }

 @Test
    void testGetNextMessageNotFound() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
 
        controller.params.name = "TestQueue11"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.getNextMessage()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"ResourceNotFound: TestQueue11 does not exist"}'
    }

 @Test
    void testGetNextMessageNotAuthorized() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")
 
        controller.params.name = "TestQueue1"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.getNextMessage()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

 @Test
    void testPeek() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
 
        controller.params.name = "TestQueue1"
        controller.params.count = 10
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.request.remoteAddr = "127.0.0.1"
 
        controller.peek()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*message1.*message3.*/
    }

 @Test
    void testPeekNotFound() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
 
        controller.params.name = "TestQueue11"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.peek()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"ResourceNotFound: TestQueue11 does not exist"}'
    }

 @Test
    void testPeekNotAuthorized() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")
 
        controller.params.name = "TestQueue1"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.peek()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

 @Test
    void testListInProgressMessages() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
 
        controller.params.name = "TestQueue1"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.request.remoteAddr = "127.0.0.1"
 
        controller.listInProgressMessages()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*message2.*/
    }

 @Test
    void testListInProgressMessagesNotFound() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
 
        controller.params.name = "TestQueue11"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.listInProgressMessages()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"ResourceNotFound: TestQueue11 does not exist"}'
    }

 @Test
    void testListInProgressMessagesNotAuthorized() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")
 
        controller.params.name = "TestQueue1"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.listInProgressMessages()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

@Test
    void testAddNewQueueMessageBody() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue1"
        controller.request.content = '{"createProg":"testProg","messageData":{"netid":"epierce"}}'

        controller.createQueueMessage()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*"count":1.*"name":"TestQueue1".*"netid":"epierce".*/
    }

  @Test
    void testAddNewQueueMessageParameter() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue1"
        controller.params.message = '{"createProg":"testProg","messageData":{"netid":"epierce"}}'

        controller.createQueueMessage()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*"count":1.*"name":"TestQueue1".*"netid":"epierce".*/
    }

  @Test
    void testAddNewQueueMessageNoData() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue1"

        controller.createQueueMessage()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Message data required"}'
    }

  @Test
    void testAddNewQueueMessageBadData() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
        
        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue1"
        controller.request.content = '{"createProg":"testProg","messageData":{"netid":}}'

        controller.createQueueMessage()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Message data is invalid"}'
    }

@Test
    void testAddNewQueueMessageNotFound() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue10"
        controller.request.content = '{"createProg":"testProg","messageData":{"netid":"epierce"}}'

        controller.createQueueMessage()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Queue does not exist"}'
    }

@Test
    void testAddNewQueueMessageNotAuthorized() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestQueue1"
        controller.request.content = '{"createProg":"testProg","messageData":{"netid":"epierce"}}'

        controller.createQueueMessage()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

@Test
    void testViewMessage() {
        def queue = new Queue([name: "TestQueue4"])
        queue.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "QueueServiceTests",messageData:[data: "message5"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])])
        message.save(flush:true)

        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "GET"
        controller.params.name = "TestQueue4"
        controller.params.id = message.id as String
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.viewMessage()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*messageDetails.*TestQueue4.*QueueServiceTests.*message5.*/          
    }

  @Test
    void testViewMessageWrongQueue() {
        def queue = new Queue([name: "TestQueue5"])
        queue.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "QueueServiceTests",messageData:[data: "message5"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])])
        message.save(flush:true)

        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "GET"
        controller.params.name = "TestQueue1"
        controller.params.id = message.id as String
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.viewMessage()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Requested message does not belong to requested queue"}'
    }
  @Test
    void testViewMessageQueueNotFound() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "GET"
        controller.params.name = "TestQueue11"
        controller.params.id = "50ce774003641900865a1d0e" //This ID does not exist
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.viewMessage()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Queue does not exist"}'
    }

  @Test
    void testViewMessageNotFound() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "GET"
        controller.params.name = "TestQueue1"
        controller.params.id = "50ce774003641900865a1d0e" //This ID does not exist
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.viewMessage()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Message does not exist"}'
    }

  @Test
    void testViewMessageNotAuthorized() {
        def queue = new Queue([name: "TestQueue4"])
        queue.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "QueueServiceTests",messageData:[data: "message5"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])])
        message.save(flush:true)
        
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")

        controller.request.method = "GET"
        controller.params.name = "TestQueue4"
        controller.params.id = message.id as String 
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.viewMessage()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

 @Test
    void testModifyMessage() {
        def queue = new Queue([name: "TestQueue6"])
        queue.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "QueueServiceTests",messageData:[data: "message"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])])
        message.save(flush:true)

        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.id = message.id as String
        controller.params.name = "TestQueue6"
        controller.params.message = '{"status":"in-progress"}'
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*messageDetails.*TestQueue6.*in-progress.*/    
    }

 @Test
    void testModifyMessageMissingStatus() {
        def queue = new Queue([name: "TestQueue6"])
        queue.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "QueueServiceTests",messageData:[data: "message"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])])
        message.save(flush:true)

        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.id = message.id as String
        controller.params.name = "TestQueue6"
        controller.params.message = '{"createProg":"controllerTestChanged","messageData":{"name":"messageChanged"}}'
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Message status is a required parameter"}'         
    }

 @Test
    void testModifyMessageStatusOnly() {
        def queue = new Queue([name: "TestQueue6"])
        queue.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "QueueServiceTests",messageData:[data: "message"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])])
        message.save(flush:true)

        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.id = message.id as String
        controller.params.name = "TestQueue6"
        controller.params.message = '{"createProg":"controllerTestChanged","status":"in-progress","messageData":{"name":"messageChanged"}}'
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Message status is the only modifiable data in a message"}'         
    }

  @Test
    void testModifyMessageNotFound() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.id = "50ce774003641900865a1d0e" //This ID does not exist
        controller.params.name = "TestQueue1"
        controller.request.content = '{"status":"in-progress"}'
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Message does not exist"}'
    }

  @Test
    void testModifyMessageMissingData() {
        def queue = new Queue([name: "TestQueue6"])
        queue.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "QueueServiceTests",messageData:[data: "message"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])])
        message.save(flush:true)

        def controller = new QueueController()
        controller.queueService = queueService
        controller.params.name = "TestQueue6"
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
        
        controller.params.id = message.id as String
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Message data required"}'         
    }

  @Test
    void testModifyMessageBadData() {
        def queue = new Queue([name: "TestQueue6"])
        queue.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "QueueServiceTests",messageData:[data: "message"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])])
        message.save(flush:true)

        def controller = new QueueController()
        controller.queueService = queueService
        controller.params.id = message.id as String
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.name = "TestQueue6"
        controller.request.content = '{"status""in-progress"}'
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Message data is invalid"}'         
    }

  @Test
    void testModifyMessageNotAuthorized() {
        def queue = new Queue([name: "TestQueue6"])
        queue.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-05-01T08:51:00"),createProg: "QueueServiceTests",messageData:[data: "message1"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])])
        message.save(flush:true)

        def controller = new QueueController()
        controller.queueService = queueService
        controller.params.id = message.id as String
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")

        controller.params.name = "TestQueue6"
        controller.request.content = '{"createProg":"testProg","messageData":{"netid":"epierce"}}'
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

  @Test
    void testModifyMessageWrongQueue() {
        def queue = new Queue([name: "TestQueue6"])
        queue.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-05-01T08:51:00"),createProg: "QueueServiceTests",messageData:[data: "message1"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])])
        message.save(flush:true)

        def controller = new QueueController()
        controller.queueService = queueService  
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.id = message.id as String
        controller.params.name = "TestQueue1"
        controller.request.content = '{"status":"in-progress"}'
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Requested message does not belong to requested queue"}'         
    }

  @Test
    void testModifyMessageQueueNotFound() {
        def queue = new Queue([name: "TestQueue6"])
        queue.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-05-01T08:51:00"),createProg: "QueueServiceTests",messageData:[data: "message1"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])])
        message.save(flush:true)

        def controller = new QueueController()
        controller.queueService = queueService
        controller.params.id = message.id as String
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.name = "TestQueue7"
        controller.request.content = '{"createProg":"testProg","messageData":{"netid":"epierce"}}'
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Queue does not exist"}'       
    }

 @Test
    void testDeleteMessage() {
        def queue = new Queue([name: "TestQueue9"])
        queue.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-05-01T08:51:00"),createProg: "QueueServiceTests",messageData:[data: "message1"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])])
        message.save(flush:true)

        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
        
        controller.request.method = "DELETE"
        controller.params.name = "TestQueue9"
        controller.params.id = message.id as String
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.deleteMessage()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*messageDetails.*TestQueue9.*QueueServiceTests.*message1.*/          
    }

 @Test
    void testDeleteMessageNotAuthorized() {
        def queue = new Queue([name: "TestQueue9"])
        queue.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-05-01T08:51:00"),createProg: "QueueServiceTests",messageData:[data: "message1"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])])
        message.save(flush:true)

        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")
        
        controller.request.method = "DELETE"
        controller.params.name = "TestQueue9"
        controller.params.id = message.id as String
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.deleteMessage()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'        
    }

  @Test
    void testDeleteMessageNotFound() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "DELETE"
        controller.params.name = "TestQueue1"
        controller.params.id = "50ce774003641900865a1d0e" //This ID does not exist
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.deleteMessage()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Message does not exist"}'
    }

  @Test
    void testDeleteMessageQueueNotFound() {
        def controller = new QueueController()
        controller.queueService = queueService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "DELETE"
        controller.params.name = "TestQueue11"
        controller.params.id = "50ce774003641900865a1d0e" //This ID does not exist
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.deleteMessage()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Queue does not exist"}'
    }    
}
