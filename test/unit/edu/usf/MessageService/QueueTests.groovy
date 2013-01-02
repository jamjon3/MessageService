package edu.usf.MessageService

import grails.test.mixin.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(Queue)
class QueueTests {

    void testNewQueue() {
        mockDomain(Queue)
        assert (([name: 'testQueue'] as Queue).validate())
    }

	void testQueueName() {
        mockDomain(Queue)
        def newQueue = new Queue([name: 'Bad Queue'])
        assert newQueue.validate() == false
        assert newQueue.errors.hasFieldErrors("name")
        assert newQueue.errors.getFieldError("name").rejectedValue == 'Bad Queue'

    }
}
