package com.potato.potatotool.content.redTeam.vulnScanner.util.constructor;

import com.potato.potatotool.content.redTeam.vulnScanner.util.constructor.YamlConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.nodes.*;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Potato
 * @date 2025/3/18 15:24
 */
public class NucleiConstructor extends YamlConstructor {
    private static final Map<String, Class<? extends NucleiYamlObj.TemplateMatcher>> TYPE_MAP = new HashMap<>();

    static {
        TYPE_MAP.put("status", NucleiYamlObj.Status.class);
        TYPE_MAP.put("word", NucleiYamlObj.Word.class);
        TYPE_MAP.put("binary", NucleiYamlObj.Binary.class);
        TYPE_MAP.put("dsl", NucleiYamlObj.Dsl.class);
        TYPE_MAP.put("regex", NucleiYamlObj.Regex.class);
        TYPE_MAP.put("json", NucleiYamlObj.Json.class);
        TYPE_MAP.put("kval", NucleiYamlObj.Kval.class);
        TYPE_MAP.put("xpath", NucleiYamlObj.Xpath.class);
    }

    public NucleiConstructor(Class<?> theRoot, LoaderOptions loaderOptions) {
        super(theRoot, loaderOptions);
        // 注册NucleiYamlObj.Poc类
        registerTypeDescription(new TypeDescription(NucleiYamlObj.Poc.class));
    }

    @Override
    protected Object constructObject(Node node) {
        if (node instanceof MappingNode) {
            MappingNode mappingNode = (MappingNode) node;
            Class<? extends NucleiYamlObj.TemplateMatcher> clazz = findClass(mappingNode);
            if (clazz != null) {
                mappingNode.setType(clazz);
                return super.constructObject(node);
            }
        }
        return super.constructObject(node);
    }

    private Class<? extends NucleiYamlObj.TemplateMatcher> findClass(MappingNode node) {
        for (NodeTuple tuple : node.getValue()) {
            Node keyNode = tuple.getKeyNode();
            if (keyNode instanceof ScalarNode && "type".equals(((ScalarNode) keyNode).getValue())) {
                Node valueNode = tuple.getValueNode();
                if (valueNode instanceof ScalarNode) {
                    String type = ((ScalarNode) valueNode).getValue();
                    return TYPE_MAP.get(type);
                }
            }
        }
        return null;
    }
}
