package com.potato.potatotool.content.redTeam.vulnScanner.util.constructor;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.XrayYamlObj;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;

/**
 * @author Potato
 * @date 2025/3/18 15:24
 */
public class XrayConstructor extends YamlConstructor {

    public XrayConstructor(Class<?> theRoot, LoaderOptions loaderOptions) {
        super(theRoot, loaderOptions);
        // 注册XrayYamlObj.Poc类
        registerTypeDescription(new TypeDescription(XrayYamlObj.Poc.class));
    }

}
