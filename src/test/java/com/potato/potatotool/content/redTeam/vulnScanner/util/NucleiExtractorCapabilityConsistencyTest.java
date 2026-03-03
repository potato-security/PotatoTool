package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Nuclei Extractor 能力一致性测试")
public class NucleiExtractorCapabilityConsistencyTest {

    @Test
    void testConverterExtractorCoverage_ShouldCoverRegexJsonXpathDslKval() {
        NucleiPocConverter converter = new NucleiPocConverter();

        NucleiYamlObj.Poc nucleiPoc = new NucleiYamlObj.Poc();
        nucleiPoc.setId("extractor-capability-gap");
        NucleiYamlObj.Info info = new NucleiYamlObj.Info();
        info.setName("Extractor Capability Gap");
        nucleiPoc.setInfo(info);

        NucleiYamlObj.Http http = new NucleiYamlObj.Http();
        http.setMethod("GET");
        http.setPath(Collections.singletonList("/{{BaseURL}}"));
        http.setMatchers(Collections.singletonList(statusMatcher(200)));

        List<NucleiYamlObj.TemplateMatcher> extractors = new ArrayList<>();
        extractors.add(regexExtractor("token", "token=(\\w+)", "body"));
        extractors.add(jsonExtractor("json_key", "$.data.key", "body"));
        extractors.add(xpathExtractor("xpath_key", "//token", "body"));
        extractors.add(dslExtractor("dsl_key", "tolower(body)", "body"));
        extractors.add(kvalExtractor("header_key", "Set-Cookie", "header"));
        http.setExtractors(extractors);

        nucleiPoc.setHttp(Collections.singletonList(http));

        PocObj.Poc converted = converter.convert(nucleiPoc);

        assertNotNull(converted);
        assertNotNull(converted.getVerifySteps());
        assertFalse(converted.getVerifySteps().isEmpty());

        PocObj.PocStep step = converted.getVerifySteps().get(0);
        assertNotNull(step.getExtractors());

        Set<PocObj.MatcherType> convertedTypes = new HashSet<>();
        for (PocObj.Matcher extractor : step.getExtractors()) {
            convertedTypes.add(extractor.getType());
        }

        assertTrue(convertedTypes.contains(PocObj.MatcherType.REGEX), "regex extractor 应被转换");
        assertTrue(convertedTypes.contains(PocObj.MatcherType.JSON), "json extractor 应被转换");
        assertTrue(convertedTypes.contains(PocObj.MatcherType.XPATH), "xpath extractor 应被转换");
        assertTrue(convertedTypes.contains(PocObj.MatcherType.DSL), "dsl extractor 应被转换");
        assertTrue(convertedTypes.contains(PocObj.MatcherType.KVAL), "kval extractor 应被转换");

        assertNotNull(converted.getUnsupportedCapabilities());
        assertFalse(hasUnsupportedCode(converted.getUnsupportedCapabilities(), "UNSUPPORTED_EXTRACTOR_TYPE"),
                "转换层不应再记录上述 extractor 的 UNSUPPORTED_EXTRACTOR_TYPE");
    }

    @Test
    void testRuntimeExtractorSuperset_ShouldContainConvertedExtractorTypes() {
        Set<PocObj.MatcherType> runtimeExtractorTypes = EnumSet.of(
                PocObj.MatcherType.REGEX,
                PocObj.MatcherType.JSON,
                PocObj.MatcherType.XPATH,
                PocObj.MatcherType.DSL,
                PocObj.MatcherType.KVAL
        );

        Set<PocObj.MatcherType> nucleiConvertedTypes = readNucleiConvertedExtractorTypes();

        for (PocObj.MatcherType runtimeType : runtimeExtractorTypes) {
            assertTrue(nucleiConvertedTypes.contains(runtimeType),
                    "Nuclei 转换层 extractor 应覆盖: " + runtimeType);
        }
    }

    private NucleiYamlObj.Status statusMatcher(int code) {
        NucleiYamlObj.Status m = new NucleiYamlObj.Status();
        m.setStatus(Collections.singletonList(code));
        m.setCondition(NucleiYamlObj.Condition.and);
        m.setName("status-ok");
        return m;
    }

    private NucleiYamlObj.TemplateMatcher regexExtractor(String name, String regex, String part) {
        NucleiYamlObj.Regex m = new NucleiYamlObj.Regex();
        m.setRegex(Collections.singletonList(regex));
        m.setPart(part);
        m.setGroup(1);
        m.setName(name);
        return m;
    }

    private NucleiYamlObj.TemplateMatcher jsonExtractor(String name, String jsonExpr, String part) {
        NucleiYamlObj.Json m = new NucleiYamlObj.Json();
        m.setJson(Collections.singletonList(jsonExpr));
        m.setPart(part);
        m.setName(name);
        return m;
    }

    private NucleiYamlObj.TemplateMatcher xpathExtractor(String name, String xpathExpr, String part) {
        NucleiYamlObj.Xpath m = new NucleiYamlObj.Xpath();
        m.setXpath(Collections.singletonList(xpathExpr));
        m.setPart(part);
        m.setName(name);
        return m;
    }

    private NucleiYamlObj.TemplateMatcher dslExtractor(String name, String expr, String part) {
        NucleiYamlObj.Dsl m = new NucleiYamlObj.Dsl();
        m.setDsl(Collections.singletonList(expr));
        m.setCondition(NucleiYamlObj.Condition.and);
        m.setName(name);
        return m;
    }

    private NucleiYamlObj.TemplateMatcher kvalExtractor(String name, String key, String part) {
        NucleiYamlObj.Kval m = new NucleiYamlObj.Kval();
        m.setKval(Collections.singletonList(key));
        m.setPart(part);
        m.setName(name);
        return m;
    }

    private Set<PocObj.MatcherType> readNucleiConvertedExtractorTypes() {
        NucleiPocConverter converter = new NucleiPocConverter();

        NucleiYamlObj.Poc nucleiPoc = new NucleiYamlObj.Poc();
        nucleiPoc.setId("extractor-types-read");
        NucleiYamlObj.Info info = new NucleiYamlObj.Info();
        info.setName("Extractor Types Read");
        nucleiPoc.setInfo(info);

        NucleiYamlObj.Http http = new NucleiYamlObj.Http();
        http.setMethod("GET");
        http.setPath(Collections.singletonList("/{{BaseURL}}"));
        http.setMatchers(Collections.singletonList(statusMatcher(200)));

        List<NucleiYamlObj.TemplateMatcher> extractors = new ArrayList<>();
        extractors.add(regexExtractor("regex_key", "token=(\\w+)", "body"));
        extractors.add(jsonExtractor("json_key", "$.data.key", "body"));
        extractors.add(xpathExtractor("xpath_key", "//token", "body"));
        extractors.add(dslExtractor("dsl_key", "tolower(body)", "body"));
        extractors.add(kvalExtractor("kval_key", "Set-Cookie", "header"));
        http.setExtractors(extractors);

        nucleiPoc.setHttp(Collections.singletonList(http));

        PocObj.Poc converted = converter.convert(nucleiPoc);
        assertNotNull(converted);
        assertNotNull(converted.getVerifySteps());
        assertFalse(converted.getVerifySteps().isEmpty());

        PocObj.PocStep step = converted.getVerifySteps().get(0);
        assertNotNull(step.getExtractors());

        Set<PocObj.MatcherType> types = EnumSet.noneOf(PocObj.MatcherType.class);
        for (PocObj.Matcher extractor : step.getExtractors()) {
            if (extractor != null && extractor.getType() != null) {
                types.add(extractor.getType());
            }
        }
        return types;
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
