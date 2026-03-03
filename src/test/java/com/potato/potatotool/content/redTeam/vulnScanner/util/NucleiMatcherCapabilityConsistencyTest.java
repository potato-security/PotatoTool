package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Nuclei Matcher 能力一致性测试")
public class NucleiMatcherCapabilityConsistencyTest {

    @Test
    void testConverterMatcherCoverage_ShouldCoverRuntimeSupportedSizeAndTimeType() {
        NucleiPocConverter converter = new NucleiPocConverter();

        NucleiYamlObj.Poc nucleiPoc = new NucleiYamlObj.Poc();
        nucleiPoc.setId("matcher-capability-gap");
        NucleiYamlObj.Info info = new NucleiYamlObj.Info();
        info.setName("Matcher Capability Gap");
        nucleiPoc.setInfo(info);

        NucleiYamlObj.Http http = new NucleiYamlObj.Http();
        http.setMethod("GET");
        http.setPath(Collections.singletonList("/{{BaseURL}}"));

        List<NucleiYamlObj.TemplateMatcher> matchers = new ArrayList<>();
        matchers.add(statusMatcher(200));
        matchers.add(sizeMatcher(100));
        matchers.add(timeMatcher("1"));
        http.setMatchers(matchers);

        nucleiPoc.setHttp(Collections.singletonList(http));

        PocObj.Poc converted = converter.convert(nucleiPoc);

        assertNotNull(converted);
        assertNotNull(converted.getVerifySteps());
        assertFalse(converted.getVerifySteps().isEmpty());

        PocObj.PocStep step = converted.getVerifySteps().get(0);
        assertNotNull(step.getMatchers());

        Set<PocObj.MatcherType> convertedTypes = new HashSet<>();
        for (PocObj.Matcher matcher : step.getMatchers()) {
            convertedTypes.add(matcher.getType());
        }

        assertTrue(convertedTypes.contains(PocObj.MatcherType.STATUS), "status matcher 应被转换");
        assertTrue(convertedTypes.contains(PocObj.MatcherType.SIZE), "size matcher 应转换为 SIZE");
        assertTrue(convertedTypes.contains(PocObj.MatcherType.TIME), "time matcher 应转换为 TIME");
        assertFalse(convertedTypes.contains(PocObj.MatcherType.UNKNOWN), "size/time matcher 不应降级为 UNKNOWN");

        assertNotNull(converted.getUnsupportedCapabilities());
        assertFalse(hasUnsupportedCode(converted.getUnsupportedCapabilities(), "UNSUPPORTED_MATCHER_TYPE"),
                "转换层不应再记录 size/time 的 UNSUPPORTED_MATCHER_TYPE");
    }

    @Test
    void testRuntimeHttpMatcherSuperset_ShouldContainSizeAndTime() {
        Set<PocObj.MatcherType> runtimeHttpTypes = EnumSet.of(
                PocObj.MatcherType.STATUS,
                PocObj.MatcherType.TIME,
                PocObj.MatcherType.DSL,
                PocObj.MatcherType.CEL,
                PocObj.MatcherType.WORD,
                PocObj.MatcherType.REGEX,
                PocObj.MatcherType.SIZE,
                PocObj.MatcherType.JSON,
                PocObj.MatcherType.BINARY,
                PocObj.MatcherType.HASH,
                PocObj.MatcherType.GROUP
        );

        Set<PocObj.MatcherType> nucleiConvertedTypes = readNucleiConvertedTypes();

        assertTrue(runtimeHttpTypes.contains(PocObj.MatcherType.SIZE), "运行时 HTTP matcher 应支持 SIZE");
        assertTrue(runtimeHttpTypes.contains(PocObj.MatcherType.TIME), "运行时 HTTP matcher 应支持 TIME");

        assertTrue(nucleiConvertedTypes.contains(PocObj.MatcherType.SIZE), "Nuclei 转换层应覆盖 SIZE");
        assertTrue(nucleiConvertedTypes.contains(PocObj.MatcherType.TIME), "Nuclei 转换层应覆盖 TIME");
    }

    private NucleiYamlObj.Status statusMatcher(int code) {
        NucleiYamlObj.Status m = new NucleiYamlObj.Status();
        m.setStatus(Collections.singletonList(code));
        m.setCondition(NucleiYamlObj.Condition.and);
        m.setName("status-ok");
        return m;
    }

    private NucleiYamlObj.TemplateMatcher sizeMatcher(int value) {
        NucleiYamlObj.Size m = new NucleiYamlObj.Size();
        m.setSize(Collections.singletonList(value));
        m.setCondition(NucleiYamlObj.Condition.and);
        m.setName("size-ok");
        return m;
    }

    private NucleiYamlObj.TemplateMatcher timeMatcher(String seconds) {
        NucleiYamlObj.Time m = new NucleiYamlObj.Time();
        m.setTime(Collections.singletonList(seconds));
        m.setCondition(NucleiYamlObj.Condition.and);
        m.setName("time-ok");
        return m;
    }

    private Set<PocObj.MatcherType> readNucleiConvertedTypes() {
        try {
            Field field = NucleiPocConverter.class.getDeclaredField("SUPPORTED_MATCHER_TYPES");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<String> rawTypes = (Set<String>) field.get(null);

            Set<PocObj.MatcherType> convertedTypes = EnumSet.noneOf(PocObj.MatcherType.class);
            for (String rawType : rawTypes) {
                if ("status".equals(rawType)) {
                    convertedTypes.add(PocObj.MatcherType.STATUS);
                } else if ("size".equals(rawType)) {
                    convertedTypes.add(PocObj.MatcherType.SIZE);
                } else if ("time".equals(rawType)) {
                    convertedTypes.add(PocObj.MatcherType.TIME);
                } else if ("word".equals(rawType)) {
                    convertedTypes.add(PocObj.MatcherType.WORD);
                } else if ("regex".equals(rawType)) {
                    convertedTypes.add(PocObj.MatcherType.REGEX);
                } else if ("binary".equals(rawType)) {
                    convertedTypes.add(PocObj.MatcherType.BINARY);
                } else if ("dsl".equals(rawType)) {
                    convertedTypes.add(PocObj.MatcherType.DSL);
                } else if ("xpath".equals(rawType)) {
                    convertedTypes.add(PocObj.MatcherType.XPATH);
                } else if ("json".equals(rawType)) {
                    convertedTypes.add(PocObj.MatcherType.JSON);
                } else if ("kval".equals(rawType)) {
                    convertedTypes.add(PocObj.MatcherType.KVAL);
                }
            }
            return convertedTypes;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("读取 Nuclei 转换层支持集失败: " + e.getMessage());
            return EnumSet.noneOf(PocObj.MatcherType.class);
        }
    }

    private boolean hasUnsupportedCode(List<Map<String, Object>> list, String code) {
        if (list == null) {
            return false;
        }
        for (Map<String, Object> item : list) {
            if (item != null && code.equals(String.valueOf(item.get("code")))) {
                return true;
            }
        }
        return false;
    }
}
