package edu.usf.MessageService

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.TestFor
import grails.converters.JSON

class TopicControllerTests extends GroovyTestCase {
    def topicService
    def stubSpringSecurityService = new StubSpringSecurityService()

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

    @Test
    void testListTopics() {    
        def controller = new TopicController()
        controller.topicService = topicService

        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.listTopics()
        assert controller.response.status == 200
        assert controller.response.contentAsString == '{"count":4,"topics":[{"name":"TestTopic1"},{"name":"TestTopic2"},{"name":"TestTopic3"},{"name":"netidChange"}]}'
    }

  @Test
    void testListTopicsPattern() {
        def controller = new TopicController()
        controller.topicService = topicService

        controller.request.contentType = "application/xml"
        controller.request.addHeader("Accept", "application/xml")

        controller.params.return = "XML"
        controller.params.pattern = "^(netid).*"
        controller.request.method = "GET"
        
        controller.listTopics()
        assert controller.response.status == 200
        
        assert controller.response.contentAsString =~ /<\?xml version="1.0" encoding="UTF-8"\?><map><entry key="count">1<\/entry><entry key="topics"><map><entry key="name">netidChange<\/entry><\/map><\/entry><\/map>/
    }

  @Test
    void testAddTopic() {

        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.request.content = '{"messageData": {"name": "newTopic"} }'

        controller.createTopic()
        
        assert controller.response.status == 200
        assert controller.response.contentAsString == '{"count":1,"topics":{"name":"newTopic"}}'
    }

  @Test
    void testAddTopicDuplicateName() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.request.content = '{"messageData": {"name": "TestTopic1"} }'

        controller.createTopic()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Create failed: TestTopic1 already exists!"}'
    }

  @Test
    void testAddTopicBadName() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "oldTopic"
        controller.request.content = '{"messageData": {"name": "Test Topic1"} }'

        controller.createTopic()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Create failed: Test Topic1 invalid name!"}'
    }

  @Test
    void testModifyTopic() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic1"
        controller.request.content = '{"messageData": {"name": "oldTopic"} }'

        controller.modifyTopic()
        assert controller.response.status == 200
        assert controller.response.contentAsString == '{"count":1,"topics":{"name":"oldTopic"}}'
    }

  @Test
    void testModifyTopicDuplicateName() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic1"
        controller.request.content = '{"messageData": {"name": "TestTopic2"} }'

        controller.modifyTopic()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Update failed: TestTopic2 already exists!"}'
    }

  @Test
    void testModifyTopicBadName() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic1"
        controller.request.content = '{"messageData": {"name": "Test Topic1"} }'

        controller.modifyTopic()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Update failed: Test Topic1 invalid name!"}'
    }

  @Test
    void testModifyTopicNotFound() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
        
        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic10"
        controller.request.content = '{"messageData": {"name": "TestTopic11"} }'

        controller.modifyTopic()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Update failed: TestTopic10 does not exist"}'
    }

  @Test
    void testModifyTopicNotAuthorized() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")
        
        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic1"
        controller.request.content = '{"messageData": {"name": "TestTopic11"} }'

        controller.modifyTopic()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

  @Test
    void testModifyPermissions() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
        
        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic1"
        controller.request.content = '{"messageData":{"permissions":{"canRead":{"add":["epierce","chance"],"remove":[]},"canWrite":{"add":["chance"],"remove":[]},"canAdmin":{"add":["epierce"],"remove":[]}}}}'
        
        controller.modifyTopic()
        assert controller.response.status == 200
        assert controller.response.contentAsString == '{"canRead":{"add":2,"remove":0},"canWrite":{"add":1,"remove":0},"canAdmin":{"add":1,"remove":0}}'
    }

  @Test
    void testModifyPermissionsNotFound() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
        
        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic11"
        controller.request.content = '{"messageData":{"permissions":{"canRead":{"add":["epierce","chance"],"remove":[]},"canWrite":{"add":["chance"],"remove":[]},"canAdmin":{"add":["epierce"],"remove":[]}}}}'
        
        controller.modifyTopic()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Update failed: TestTopic11 does not exist"}'
    }

  @Test
    void testModifyPermissionsNoData() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
        
        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic1"
        controller.request.content = '{"messageData":{}}'
        
        controller.modifyTopic()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Message data required"}'
    }

  @Test
    void testModifyPermissionsNotAuthorized() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")
        
        controller.request.method = "PUT"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic1"
        controller.request.content = '{"messageData":{"permissions":{"canRead":{"add":["epierce","chance"],"remove":[]},"canWrite":{"add":["chance"],"remove":[]},"canAdmin":{"add":["epierce"],"remove":[]}}}}'
        
        controller.modifyTopic()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

  @Test
    void testDeleteTopic() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "DELETE"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic1"

        controller.deleteTopic()
        assert controller.response.status == 200
        assert controller.response.contentAsString == '{"count":1,"topics":{"name":"TestTopic1"}}'
    }

  @Test
    void testDeleteTopicNotFound() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "DELETE"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "oldTopic2"

        controller.deleteTopic()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"ResourceNotFound: oldTopic2 does not exist"}'
    }

  @Test
    void testDeleteTopicNotAuthorized() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")
        
        controller.request.method = "DELETE"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic1"

        controller.deleteTopic()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

 @Test
    void testListTopicMessages() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
 
        controller.params.name = "TestTopic1"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.listTopicMessages()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*message1.*message2.*message3.*/
    }

 @Test
    void testListTopicMessagesNotFound() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
 
        controller.params.name = "TestTopic11"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.listTopicMessages()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"ResourceNotFound: TestTopic11 does not exist"}'
    }

 @Test
    void testListTopicMessagesNotAuthorized() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")
 
        controller.params.name = "TestTopic1"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.listTopicMessages()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

 @Test
    void testListTopicMessagesFilteredStart() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.name = "TestTopic1"
        controller.params.startTime = "2012-01-01T00:00:00"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.filterTopicMessages()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*message2.*message3.*/
    }

    @Test
    void testListTopicMessagesFilteredStartAndEnd() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.name = "TestTopic1"
        controller.params.startTime = "2012-01-01T00:00:00"
        controller.params.endTime = "2013-01-01T00:00:00"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.filterTopicMessages()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*message2.*/
    }

  @Test
    void testListTopicMessagesFilteredNoResults() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.name = "TestTopic1"
        controller.params.startTime = "2020-01-01T00:00:00"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.filterTopicMessages()
        assert controller.response.status == 200
        assert controller.response.contentAsString == '{"count":0,"messages":[]}'
    }

  @Test
    void testListTopicMessagesFilteredBadInputStart() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.name = "TestTopic1"
        controller.params.startTime = "2012-06-01"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.filterTopicMessages()
        assert controller.response.status == 400       
    }

  @Test
    void testListTopicMessagesFilteredBadInputEnd() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.name = "TestTopic1"
        controller.params.startTime = "2012-06-01T08:52:00"
        controller.params.endTime = "2012-07-01"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.filterTopicMessages()
        //the JSON data contains escaped double quotes, so we have to escape the escape characters
        def dateErrorMessage = '{"error":"Unparseable date. Dates must be in the format yyyy-MM-dd\'T\'HH:mm:ss (GMT)"}'
        assert controller.response.contentAsString == dateErrorMessage
    }
    
  @Test
    void testListTopicMessagesFilteredNotFound() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.name = "TestTopic11"
        controller.params.startTime = "2020-01-01T00:00:00"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.filterTopicMessages()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"ResourceNotFound: TestTopic11 does not exist"}'       
    }

  @Test
    void testListTopicMessagesFilteredNotAuthorized() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")

        controller.params.name = "TestTopic1"
        controller.params.startTime = "2020-01-01T00:00:00"
        controller.request.method = "GET"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
 
        controller.filterTopicMessages()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'       
    }

@Test
    void testAddNewTopicMessageBody() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic1"
        controller.request.content = '{"createProg":"testProg","messageData":{"netid":"epierce"}}'

        controller.createTopicMessage()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*"count":1.*"name":"TestTopic1".*"netid":"epierce".*/
    }

  @Test
    void testAddNewTopicMessageParameter() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic1"
        controller.params.message = '{"createProg":"testProg","messageData":{"netid":"epierce"}}'

        controller.createTopicMessage()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*"count":1.*"name":"TestTopic1".*"netid":"epierce".*/
    }

  @Test
    void testAddNewTopicMessageNoData() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic1"

        controller.createTopicMessage()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Message data required"}'
    }

  @Test
    void testAddNewTopicMessageBadData() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
        
        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic1"
        controller.request.content = '{"createProg":"testProg","messageData":{"netid":}}'

        controller.createTopicMessage()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Message data is invalid"}'
    }

@Test
    void testAddNewTopicMessageNotFound() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic10"
        controller.request.content = '{"createProg":"testProg","messageData":{"netid":"epierce"}}'

        controller.createTopicMessage()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Topic does not exist"}'
    }

@Test
    void testAddNewTopicMessageNotAuthorized() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")

        controller.request.method = "POST"
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        controller.params.name = "TestTopic1"
        controller.request.content = '{"createProg":"testProg","messageData":{"netid":"epierce"}}'

        controller.createTopicMessage()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

@Test
    void testViewMessage() {
        def topic = new Topic([name: "TestTopic4"])
        topic.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "TopicServiceTests",messageData:[data: "message5"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])])
        message.save(flush:true)

        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "GET"
        controller.params.name = "TestTopic4"
        controller.params.id = message.id as String
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.viewMessage()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*messageDetails.*TestTopic4.*TopicServiceTests.*message5.*/          
    }

  @Test
    void testViewMessageWrongTopic() {
        def topic = new Topic([name: "TestTopic5"])
        topic.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "TopicServiceTests",messageData:[data: "message5"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])])
        message.save(flush:true)

        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "GET"
        controller.params.name = "TestTopic1"
        controller.params.id = message.id as String
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.viewMessage()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Requested message does not belong to requested topic"}'
    }
  @Test
    void testViewMessageTopicNotFound() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "GET"
        controller.params.name = "TestTopic11"
        controller.params.id = "50ce774003641900865a1d0e" //This ID does not exist
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.viewMessage()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Topic does not exist"}'
    }

  @Test
    void testViewMessageNotFound() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "GET"
        controller.params.name = "TestTopic1"
        controller.params.id = "50ce774003641900865a1d0e" //This ID does not exist
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.viewMessage()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Message does not exist"}'
    }

  @Test
    void testViewMessageNotAuthorized() {
        def topic = new Topic([name: "TestTopic4"])
        topic.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "TopicServiceTests",messageData:[data: "message5"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])])
        message.save(flush:true)
        
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")

        controller.request.method = "GET"
        controller.params.name = "TestTopic4"
        controller.params.id = message.id as String 
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.viewMessage()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

 @Test
    void testModifyMessage() {
        def topic = new Topic([name: "TestTopic6"])
        topic.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "TopicServiceTests",messageData:[data: "message"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])])
        message.save(flush:true)

        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.id = message.id as String
        controller.params.name = "TestTopic6"
        controller.params.message = '{"createProg":"controllerTestChanged","messageData":{"name":"messageChanged"}}'
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*${message.id as String}.*controllerTestChanged.*messageChanged.*/         
    }

  @Test
    void testModifyMessageNotFound() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.id = "50ce774003641900865a1d0e" //This ID does not exist
        controller.params.name = "TestTopic1"
        controller.request.content = '{"createProg":"controllerTestChanged","messageData":{"name":"messageChanged"}}'
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Message does not exist"}'
    }

  @Test
    void testModifyMessageMissingData() {
        def topic = new Topic([name: "TestTopic6"])
        topic.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "TopicServiceTests",messageData:[data: "message"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])])
        message.save(flush:true)

        def controller = new TopicController()
        controller.topicService = topicService
        controller.params.name = "TestTopic6"
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
        
        controller.params.id = message.id as String
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"MissingRequiredQueryParameter: Message data required"}'         
    }

  @Test
    void testModifyMessageBadData() {
        def topic = new Topic([name: "TestTopic6"])
        topic.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "TopicServiceTests",messageData:[data: "message"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])])
        message.save(flush:true)

        def controller = new TopicController()
        controller.topicService = topicService
        controller.params.id = message.id as String
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.name = "TestTopic6"
        controller.request.content = '{"createProg":"controllerTestChanged","messageData":{"name""messageChanged"}}'
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Message data is invalid"}'         
    }

  @Test
    void testModifyMessageNotAuthorized() {
        def topic = new Topic([name: "TestTopic6"])
        topic.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-05-01T08:51:00"),createProg: "TopicServiceTests",messageData:[data: "message1"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])])
        message.save(flush:true)

        def controller = new TopicController()
        controller.topicService = topicService
        controller.params.id = message.id as String
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")

        controller.params.name = "TestTopic6"
        controller.request.content = '{"createProg":"testProg","messageData":{"netid":"epierce"}}'
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'
    }

  @Test
    void testModifyMessageWrongTopic() {
        def topic = new Topic([name: "TestTopic6"])
        topic.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-05-01T08:51:00"),createProg: "TopicServiceTests",messageData:[data: "message1"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])])
        message.save(flush:true)

        def controller = new TopicController()
        controller.topicService = topicService  
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.id = message.id as String
        controller.params.name = "TestTopic1"
        controller.request.content = '{"createProg":"testProg","messageData":{"netid":"epierce"}}'
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 400
        assert controller.response.contentAsString == '{"error":"Requested message does not belong to requested topic"}'         
    }

  @Test
    void testModifyMessageTopicNotFound() {
        def topic = new Topic([name: "TestTopic6"])
        topic.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-05-01T08:51:00"),createProg: "TopicServiceTests",messageData:[data: "message1"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])])
        message.save(flush:true)

        def controller = new TopicController()
        controller.topicService = topicService
        controller.params.id = message.id as String
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.params.name = "TestTopic7"
        controller.request.content = '{"createProg":"testProg","messageData":{"netid":"epierce"}}'
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")
        
        controller.modifyMessage()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Topic does not exist"}'       
    }

 @Test
    void testDeleteMessage() {
        def topic = new Topic([name: "TestTopic9"])
        topic.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-05-01T08:51:00"),createProg: "TopicServiceTests",messageData:[data: "message1"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])])
        message.save(flush:true)

        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")
        
        controller.request.method = "DELETE"
        controller.params.name = "TestTopic9"
        controller.params.id = message.id as String
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.deleteMessage()
        assert controller.response.status == 200
        assert controller.response.contentAsString =~ /.*messageDetails.*TestTopic9.*TopicServiceTests.*message1.*/          
    }

 @Test
    void testDeleteMessageNotAuthorized() {
        def topic = new Topic([name: "TestTopic9"])
        topic.save(flush:true)

        def message = new Message([creator: "it-msgsvcadm", apiVersion: 1,createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-05-01T08:51:00"),createProg: "TopicServiceTests",messageData:[data: "message1"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])])
        message.save(flush:true)

        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("test-user")
        
        controller.request.method = "DELETE"
        controller.params.name = "TestTopic9"
        controller.params.id = message.id as String
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.deleteMessage()
        assert controller.response.status == 403
        assert controller.response.contentAsString == '{"error":"You are not authorized to perform this operation"}'        
    }

  @Test
    void testDeleteMessageNotFound() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "DELETE"
        controller.params.name = "TestTopic1"
        controller.params.id = "50ce774003641900865a1d0e" //This ID does not exist
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.deleteMessage()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Message does not exist"}'
    }

  @Test
    void testDeleteMessageTopicNotFound() {
        def controller = new TopicController()
        controller.topicService = topicService
        controller.springSecurityService = stubSpringSecurityService
        controller.springSecurityService.setUser("it-msgsvcadm")

        controller.request.method = "DELETE"
        controller.params.name = "TestTopic11"
        controller.params.id = "50ce774003641900865a1d0e" //This ID does not exist
        controller.request.contentType = "text/json"
        controller.request.addHeader("Accept", "text/json")

        controller.deleteMessage()
        assert controller.response.status == 404
        assert controller.response.contentAsString == '{"error":"Topic does not exist"}'
    }    
}
