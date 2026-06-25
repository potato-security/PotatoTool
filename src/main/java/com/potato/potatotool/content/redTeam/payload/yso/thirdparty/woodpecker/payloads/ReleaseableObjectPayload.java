package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;


/**
 * @author mbechler
 *
 */
public interface ReleaseableObjectPayload<T> extends ObjectPayload<T> {

    void release( T obj ) throws Exception;
}
