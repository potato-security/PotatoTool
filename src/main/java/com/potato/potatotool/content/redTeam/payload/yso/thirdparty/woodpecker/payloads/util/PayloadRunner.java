package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.ObjectPayload;

public abstract class PayloadRunner {
    public static void run(Class<? extends ObjectPayload<?>> clazz, String[] args) {
        throw new UnsupportedOperationException("Command-line payload runner is not packaged in PotatoTool");
    }
}
