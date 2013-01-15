MessageService
==============

Simple RESTful message service using Grails and MongoDB.

## Yet another message service?
I know. Why re-invent the wheel, right?  I think this adds some features that other message services have missed.  I spent several months looking at the various message/queue systems that are available and I couldn't find one that met all of our needs.  Here's what I was looking for:

* RESTful API
* Support for queues *and* topics.  Several of our use-cases require pub/sub topics, so that eliminated most of the simple queueing systems (Resque/Jesque, beanstalkd, etc)
* Message persistence and durability
* Graceful handling of network issues.  Our secondary datacenter is about 50 miles away and our DR site is around 600 miles away, so network interruption is a distinct possibility.  
* This is just one piece of a larger IdM effort, so I want to keep it as simple as possible.

## Features
* Simple REST API
* Supports both Topics and Queues
* Supports CAS and HTTP-BASIC authentication
* MongoDB-based message persistence

 
