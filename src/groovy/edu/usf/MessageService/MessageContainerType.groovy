package edu.usf.MessageService

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import org.grails.datastore.mapping.engine.types.AbstractMappingAwareCustomTypeMarshaller
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.mongo.query.MongoQuery
import org.grails.datastore.mapping.query.Query

/**
* Marshalling Object for saving the MessageContainer Object to MongoDB
* See http://springsource.github.com/grails-data-mapping/mongo/manual/guide/3.%20Mapping%20Domain%20Classes%20to%20Mongo%20Collections.html#3.6%20Custom%20User%20Types for more details
*
* @author Eric Pierce <epierce@usf.edu>
* @date 12/13/2012
**/
class MessageContainerType extends AbstractMappingAwareCustomTypeMarshaller<MessageContainer, DBObject, DBObject> {
    MessageContainerType() {
        super(MessageContainer)
    }

    @Override
    protected Object writeInternal(PersistentProperty property, String key, MessageContainer value, DBObject nativeTarget) {
        final converted = [name: value.name, type: value.type, id: value.id]
        nativeTarget.put(key, converted)
        return converted
    }

    @Override
    protected void queryInternal(PersistentProperty property, String key, Query.PropertyCriterion criterion, DBObject nativeQuery) {
        nativeQuery.put("${key}.name", criterion.value.name)
        nativeQuery.put("${key}.type", criterion.value.type)
    }

    @Override
    protected MessageContainer readInternal(PersistentProperty property, String key, DBObject nativeSource) {
        final map = nativeSource.get(key)
        if(map instanceof Map) {
            return new MessageContainer(map)
        }
        return null
    }
}