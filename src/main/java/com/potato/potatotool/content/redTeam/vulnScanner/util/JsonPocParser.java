package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.GobyJsonObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.XrayYamlObj;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Potato
 * @date 2025/3/12 15:36
 */
public class JsonPocParser {
    private static final Gson gson = new Gson();

    public JsonPocParser() {
    }

    public GobyJsonObj.PocJson loadGobyJsonPocFile(String fileName) throws IOException {
        byte[] fileContent = Files.readAllBytes(Paths.get(fileName));
        return gson.fromJson(new String(fileContent, StandardCharsets.UTF_8), GobyJsonObj.PocJson.class);
    }

    public void saveXrayPocFile(String filename, XrayYamlObj.Poc xrayPoc) throws IOException {
        // 设置 YAML 选项
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        // 创建 Representer 并设置自定义 PropertyUtils
        NullSkippingRepresenter representer = new NullSkippingRepresenter(options);
        representer.setPropertyUtils(new CustomPropertyUtils());
        representer.addClassTag(XrayYamlObj.Rule.class, Tag.MAP);
        // 创建 Yaml 对象
        Yaml yaml = new Yaml(representer, options);
        yaml.setBeanAccess(BeanAccess.FIELD); // 关键设置：按字段顺序序列化
        String yamlStr = yaml.dumpAs(xrayPoc, Tag.MAP, null);
        Files.write(Paths.get(filename), transformVars(yamlStr).getBytes(StandardCharsets.UTF_8));
    }

    public static class CustomPropertyUtils extends PropertyUtils {
        @Override
        protected Set<Property> createPropertySet(Class<?> type, BeanAccess bAccess) {
            Map<String, Property> props = getPropertiesMap(type, bAccess);
            return new LinkedHashSet<>(props.values()); // 使用 LinkedHashSet 保证顺序
        }
    }

    public static class NullSkippingRepresenter extends Representer {
        public NullSkippingRepresenter(DumperOptions options) {
            super(options);
        }
        @Override
        protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
            if (propertyValue == null) {
                return null;
            }
            if (propertyValue instanceof String && ((String) propertyValue).trim().isEmpty()) {
                return null;
            }
            if (propertyValue instanceof Collection && ((Collection<?>) propertyValue).isEmpty()) {
                return null;
            }
            if (propertyValue instanceof Map && ((Map<?, ?>) propertyValue).isEmpty()) {
                return null;
            }
            if (propertyValue instanceof Boolean && !((Boolean) propertyValue)) {
                return null;
            }
            return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
        }
    }

    private String transformVars(String content) {
        return content.replace("{{{", "{{").replace("}}}", "}}");
    }

    public XrayYamlObj.Poc run(GobyJsonObj.PocJson gobyPoc) throws Exception {
        if (gobyPoc.getScanSteps().size() < 2) {
            throw new Exception("no ScanSteps");
        }

        XrayYamlObj.Poc xrayPoc = new XrayYamlObj.Poc();
        // 基础信息部分
        xrayPoc.setName(gobyPoc.getName());
        xrayPoc.setManual(true);
        xrayPoc.setQuery(gobyPoc.getGobyQuery());
        xrayPoc.setTransport("http");

        XrayYamlObj.Detail detail = new XrayYamlObj.Detail();
        detail.setAuthor(gobyPoc.getAuthor());
        detail.setLinks(gobyPoc.getReferences());
        detail.setTags(String.join(",", gobyPoc.getTags()));
        detail.setDescription(gobyPoc.getDescription());
        xrayPoc.setDetail(detail);

        // 规则处理
        List<Object> scanStepOpts = gobyPoc.getScanSteps().subList(1, gobyPoc.getScanSteps().size());

        for (int index = 0; index < scanStepOpts.size(); index++) {
            LinkedHashMap<String, Object> scanStepOpt = gson.fromJson(
                    gson.toJson(scanStepOpts.get(index)),
                    new TypeToken<LinkedHashMap<String, Object>>(){}.getType());

            XrayYamlObj.Rule rule = new XrayYamlObj.Rule();
            List<XrayYamlObj.VariableMapItem> sets = new ArrayList<>();

            // 解析 ScanStep
            analyseScanStep(scanStepOpt, rule, sets);

            // 构建规则条目
            xrayPoc.getRules().put("r" + index, rule);

            // 处理全局变量
            for (XrayYamlObj.VariableMapItem set : sets) {
                xrayPoc.getSet().put(set.getKey(), set.getValue());
            }
        }

        // 构建表达式
        String condition = gobyPoc.getScanSteps().get(0).equals("OR") ? " || " : " && ";
        StringBuilder expression = new StringBuilder();
        for (int i = 0; i < scanStepOpts.size(); i++) {
            expression.append("r").append(i).append("()");
            if (i != scanStepOpts.size() - 1) {
                expression.append(condition);
            }
        }
        xrayPoc.setExpression(expression.toString());

        return xrayPoc;
    }

    private void analyseScanStep(LinkedHashMap<String, Object> scanStepOpt, XrayYamlObj.Rule rule,
                                 List<XrayYamlObj.VariableMapItem> sets) throws Exception {
        // 处理 SetVariable
        if (scanStepOpt.containsKey("SetVariable")) {
            List<String> setVars = gson.fromJson(
                    gson.toJson(scanStepOpt.get("SetVariable")),
                    new TypeToken<List<String>>(){}.getType());
            rule.setOutput(analyseSetVariable(setVars));
        }

        // 处理 Request
        if (!scanStepOpt.containsKey("Request")) {
            throw new Exception("no Request");
        }

        GobyJsonObj.Request request = gson.fromJson(
                gson.toJson(scanStepOpt.get("Request")),
                GobyJsonObj.Request.class);

        XrayYamlObj.RuleRequest ruleRequest = new XrayYamlObj.RuleRequest();
        ruleRequest.setPath(request.getUri());
        ruleRequest.setFollow_redirects(request.isFollow_redirect());
        ruleRequest.setBody(request.getData());
        ruleRequest.setHeaders(request.getHeader());
        ruleRequest.setMethod(request.getMethod());
        rule.setRequest(ruleRequest);

        // 处理 Request 的 SetVariable
        if (request.getSet_variable() != null) {
            sets.addAll(analyseRequestSetVariable(request.getSet_variable()));
        }

        // 处理 ResponseTest
        if (!scanStepOpt.containsKey("ResponseTest")) {
            throw new Exception("no Response");
        }

        GobyJsonObj.ResponseTest responseTest = gson.fromJson(
                gson.toJson(scanStepOpt.get("ResponseTest")),
                GobyJsonObj.ResponseTest.class);

        if (responseTest.getChecks() != null && !responseTest.getChecks().isEmpty()) {
            if ("group".equals(responseTest.getType())) {
                List<String> checks = analyseCheck(responseTest.getChecks());
                if ("AND".equals(responseTest.getOperation())) {
                    rule.setExpression(String.join(" && ", checks));
                } else {
                    rule.setExpression(String.join(" || ", checks));
                }
            }
        } else {
            rule.setExpression("response.status >= 200");
        }
    }

    private LinkedHashMap<String, String> analyseSetVariable(List<String> varLine) throws Exception {
        LinkedHashMap<String, String> output = new LinkedHashMap<>();
        for (String v : varLine) {
            String[] vars = v.split("\\|");
            if (vars.length < 2 || "output".equals(vars[0])) continue;

            switch (vars[1]) {
                case "lastbody":
                    output.put(vars[0], "response.body");
                    break;
                case "lastheader":
                    output.put(vars[0], "response.headers");
                    break;
                default:
                    throw new Exception("not support SetVariable type");
            }
        }
        return output;
    }

    private List<XrayYamlObj.VariableMapItem> analyseRequestSetVariable(List<String> varLine) throws Exception {
        List<XrayYamlObj.VariableMapItem> items = new ArrayList<>();
        for (String v : varLine) {
            String[] vars = v.split("\\|");
            if (vars.length < 4 || !"rand".equals(vars[1])) continue;

            XrayYamlObj.VariableMapItem item = new XrayYamlObj.VariableMapItem();
            item.setKey(vars[0]);
            if ("int".equals(vars[2])) {
                item.setValue("randomInt(400000, 448000)");
            } else if ("str".equals(vars[2])) {
                item.setValue("randomLowercase(" + vars[3] + ")");
            } else {
                throw new Exception("not support SetVariable type");
            }
            items.add(item);
        }
        return items;
    }

    private List<String> analyseCheck(List<GobyJsonObj.Check> checks) {
        List<String> results = new ArrayList<>();
        for (GobyJsonObj.Check check : checks) {
            if ("item".equals(check.getType())) {
                switch (check.getVariable()) {
                    case "$code":
                        results.add("response.status " + check.getOperation() + " " + check.getValue());
                        break;
                    case "$body":
                        handleBodyCheck(check, results);
                        break;
                    case "$head":
                        handleHeaderCheck(check, results);
                        break;
                }
            }
        }
        return results;
    }

    private void handleBodyCheck(GobyJsonObj.Check check, List<String> results) {
        String value = convertStr(check.getValue());
        switch (check.getOperation()) {
            case "contains":
                results.add("response.body.bcontains(b\"" + value + "\")");
                break;
            case "regex":
                results.add("\"" + value + "\".bmatches(response.body)");
                break;
            case "not contains":
                results.add("!response.body.bcontains(b\"" + value + "\")");
                break;
        }
    }

    private void handleHeaderCheck(GobyJsonObj.Check check, List<String> results) {
        String value = convertStr(check.getValue());
        switch (check.getOperation()) {
            case "contains":
                results.add("response.raw_header.bcontains(b\"" + value + "\")");
                break;
            case "regex":
                results.add("\"" + value + "\".bmatches(response.raw_header)");
                break;
            case "not contains":
                results.add("!response.raw_header.bcontains(b\"" + value + "\")");
                break;
        }
    }

    private String convertStr(String input) {
        return input.replace("\"", "\\\"");
    }
}
