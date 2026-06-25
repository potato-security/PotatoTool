package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

public interface ObjectPayload<T> {
    T getObject(String command) throws Exception;
}
