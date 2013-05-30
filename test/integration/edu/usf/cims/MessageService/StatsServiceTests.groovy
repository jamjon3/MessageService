package edu.usf.cims.MessageService

import static org.junit.Assert.*
import org.junit.*

class StatsServiceTests {
    def statsService

    @Before
    void setup() {
        //This runs before each test
        def queue = new Queue([name: "TestQueue1"])
        queue.save(flush:true) 

        def queue2 = new Queue([name: "TestQueue2"])
        queue2.save(flush:true)
        
        new Queue([name: "TestQueue3"]).save(flush:true)
        new Queue([name: "netidChange"]).save(flush:true)

        new Message([creator: "it-msgsvcadm", apiVersion: 1, status: "pending", createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-05-01T08:51:00"),createProg: "StatsServiceTests",messageData:[data: "message1"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])]).save(flush:true)
        new Message([creator: "it-msgsvcadm", apiVersion: 1, status: "in-progress", createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-06-01T09:51:00"),createProg: "StatsServiceTests",messageData:[data: "message2"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])]).save(flush:true)
        new Message([creator: "it-msgsvcadm", apiVersion: 1, status: "pending", createProg: "StatsServiceTests",messageData:[data: "message3"],messageContainer:new MessageContainer([type:"queue",id:queue.id,name:queue.name])]).save(flush:true)
        new Message([creator: "it-msgsvcadm", apiVersion: 1, status: "pending", createProg: "StatsServiceTests",messageData:[data: "message4"],messageContainer:new MessageContainer([type:"queue",id:queue2.id,name:queue2.name])]).save(flush:true)


        //Create the Topics
        def topic = new Topic([name: "TestTopic1"])
        topic.save(flush:true) 

        def topic2 = new Topic([name: "TestTopic2"])
        topic2.save(flush:true)
        
        new Topic([name: "TestTopic3"]).save(flush:true)
        new Topic([name: "netidChange"]).save(flush:true)

        new Message([creator: "it-msgsvcadm", apiVersion: 1,createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-05-01T08:51:00"),createProg: "StatsServiceTests",messageData:[data: "message1"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])]).save(flush:true)
        new Message([creator: "it-msgsvcadm", apiVersion: 1,createTime: new Date().parse("yyyy-MM-dd'T'HH:mm:ss", "2012-06-01T09:51:00"),createProg: "StatsServiceTests",messageData:[data: "message2"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])]).save(flush:true)
        new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "StatsServiceTests",messageData:[data: "message3"],messageContainer:new MessageContainer([type:"topic",id:topic.id,name:topic.name])]).save(flush:true)
        new Message([creator: "it-msgsvcadm", apiVersion: 1,createProg: "StatsServiceTests",messageData:[data: "message4"],messageContainer:new MessageContainer([type:"topic",id:topic2.id,name:topic2.name])]).save(flush:true)
    
    }

    @After
    void tearDown() {       
        //This runs after each test
        //Remove data from test database
        Queue.collection.drop()
        Topic.collection.drop()
        Message.collection.drop()
    }

    @Test
    void testCountQueueMessages() {    
        def messageCount = statsService.countQueueMessages()
        
        assert messageCount == 4       
    }

    @Test
    void testCountQueueMessagesQueue() {    
        def messageCount = statsService.countQueueMessages("TestQueue1")
        
        assert messageCount == 3       
    }

    @Test
    void testCountQueueMessagesStatus() {    
        def messageCount = statsService.countQueueMessages(null,"in-progress")
        
        assert messageCount == 1       
    }

    @Test
    void testCountQueueMessagesStatusAndQueue() {    
        def messageCount = statsService.countQueueMessages("TestQueue1","pending")
        
        assert messageCount ==  2      
    }

    @Test
    void testCountTopicMessages() {    
        def messageCount = statsService.countTopicMessages()
        
        assert messageCount == 4       
    }

    @Test
    void testCountQueueMessagesTopic() {    
        def messageCount = statsService.countTopicMessages("TestTopic1")
        
        assert messageCount ==  3      
    }

    @Test
    void testListStats() {    
        statsService.grailsApplication.config.statsLogFile = "resources/perfStats-test.log"
        def statsListing = statsService.listStats()
        
        assert statsListing[0].tag == 'peek'
        assert statsListing[0].average == "1.0"      
    }

    @Test
    void testListStatsFilter() {    
        statsService.grailsApplication.config.statsLogFile = "resources/perfStats-test.log"
        def statsListing = statsService.listStats("viewMessage")
        
        assert statsListing[0].tag == 'viewMessage'
        assert statsListing[0].average == "0.6"      
    }
}