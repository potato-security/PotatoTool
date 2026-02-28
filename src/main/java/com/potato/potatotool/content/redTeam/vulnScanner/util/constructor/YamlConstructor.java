package com.potato.potatotool.content.redTeam.vulnScanner.util.constructor;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

/**
 * 通用的YAML构造器基类，处理带连字符的字段名映射
 * @author Potato
 * @date 2025/2/18 15:24
 */
public abstract class YamlConstructor extends Constructor {

    public YamlConstructor(Class<?> theRoot, LoaderOptions loaderOptions) {
        super(theRoot, loaderOptions);
        
        // 设置PropertyUtils处理带连字符的字段名映射
        PropertyUtils propertyUtils = new PropertyUtils() {
            @Override
            public Property getProperty(Class<?> type, String name) {
                // 处理带连字符的字段名映射 (max-request -> max_request)
                if (name.contains("-")) {
                    String underscoreName = name.replace("-", "_");
                    try {
                        return super.getProperty(type, underscoreName);
                    } catch (Exception e) {
                        // 如果下划线版本也找不到，继续尝试原名
                    }
                }
                try {
                    return super.getProperty(type, name);
                } catch (Exception e) {
                    // 忽略未映射的字段，返回 null 让 SnakeYAML 跳过
                    return null;
                }
            }
        };
        propertyUtils.setSkipMissingProperties(true);
        setPropertyUtils(propertyUtils);
    }

    /**
     * 注册类型描述
     * 子类应该在构造函数中调用此方法注册特定的类型描述
     * @param typeDescription 类型描述
     */
    protected void registerTypeDescription(TypeDescription typeDescription) {
        addTypeDescription(typeDescription);
    }
}