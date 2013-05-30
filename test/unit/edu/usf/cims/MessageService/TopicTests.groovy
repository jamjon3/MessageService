package edu.usf.cims.MessageService

import grails.test.mixin.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(Topic)
class TopicTests {

    void testNewTopic() {
        mockDomain(Topic)
        assert ([name: 'testTopic'] as Topic).validate()
    }

    void testTopicName() {
        mockDomain(Topic)
        def newTopic = new Topic([name: 'Bad Topic'])
        assert newTopic.validate() == false
        assert newTopic.errors.hasFieldErrors("name")
        assert newTopic.errors.getFieldError("name").rejectedValue == 'Bad Topic'

    }

    void testPermissions(){
        mockDomain(Topic)

        def topic = new Topic([name: 'TestTopic'])
        assert topic.validate() == true
        assert topic.permissions.canRead[0] == 'it-msgsvcadm'
        assert topic.permissions.canWrite[0] == 'it-msgsvcadm'
        assert topic.permissions.canAdmin[0] == 'it-msgsvcadm'
    }

    void testAddReader(){
        mockDomain(Topic)

        def topic = new Topic([name: 'TestTopic'])
        assert topic.validate() == true

        topic.addReader('epierce')
        assert topic.permissions.canRead[1] == 'epierce'
        assert topic.canRead('epierce')
        assert topic.canRead('chance') == false
    }

    void testAddWriter(){
        mockDomain(Topic)

        def topic = new Topic([name: 'TestTopic'])
        assert topic.validate() == true

        topic.addWriter('epierce')
        assert topic.permissions.canWrite[1] == 'epierce'
        assert topic.canWrite('epierce')
        assert topic.canWrite('chance') == false

    }

    void testAddAdmin(){
        mockDomain(Topic)

        def topic = new Topic([name: 'TestTopic'])
        assert topic.validate() == true

        topic.addAdmin('epierce')
        assert topic.permissions.canAdmin[1] == 'epierce'
        assert topic.canAdmin('epierce')
        assert topic.canAdmin('chance') == false
    }

    void testRemoveReader(){
        mockDomain(Topic)

        def topic = new Topic([name: 'TestTopic'])
        assert topic.validate() == true

        topic.addReader('epierce')
        assert topic.permissions.canRead[1] == 'epierce'

        topic.removeReader('epierce')
        assert topic.canRead('epierce') == false
        assert topic.permissions.canRead.size == 1

    }

    void testRemoveWriter(){
        mockDomain(Topic)

        def topic = new Topic([name: 'TestTopic'])
        assert topic.validate() == true

        topic.addWriter('epierce')
        assert topic.permissions.canWrite[1] == 'epierce'

        topic.removeWriter('epierce')
        assert topic.canWrite('epierce') == false
        assert topic.permissions.canWrite.size == 1

    }

    void testRemoveAdmin(){
        mockDomain(Topic)

        def topic = new Topic([name: 'TestTopic'])
        assert topic.validate() == true

        topic.addAdmin('epierce')
        assert topic.permissions.canAdmin[1] == 'epierce'

        topic.removeAdmin('epierce')
        assert topic.canAdmin('epierce') == false
        assert topic.permissions.canAdmin.size == 1
    }
}
