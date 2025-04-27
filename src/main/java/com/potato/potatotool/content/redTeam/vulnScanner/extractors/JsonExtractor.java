package com.potato.potatotool.content.redTeam.vulnScanner.extractors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JSON提取工具类
 * @author Potato
 * @date 2025/3/21 15:00
 */
public class JsonExtractor {
    private static final Scope SCOPE = Scope.newEmptyScope();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static {
        // 使用BuiltinFunctionLoader加载内置函数
        BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, SCOPE);
    }

    private JsonExtractor() {
        // 私有构造函数，防止实例化
    }

    /**
     * 从JSON内容中提取值
     * @param content JSON字符串内容
     * @param jqExprs jq表达式列表
     * @return 提取的内容（多个结果用逗号分隔）
     */
    public static String extractJson(String content, List<String> jqExprs) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(content);
            List<String> results = new ArrayList<>();

            for (String expr : jqExprs) {
                try {
                    JsonQuery query = JsonQuery.compile(expr, Versions.JQ_1_6);
                    List<JsonNode> nodes = new ArrayList<>();
                    query.apply(SCOPE, root, nodes::add);
                    nodes.stream()
                            .map(node -> node.isTextual() ? node.asText() : node.toString())
                            .forEach(results::add);
                } catch (JsonQueryException e) {
                    System.out.println("编译/应用jq-expr失败: "+ expr + e);
                }
            }
            return String.join(",", results);
        } catch (Exception e) {
            System.out.println("JSON解析错误" + e);
            return null;
        }
    }

    public static boolean matchJson(String content, List<String> jqExprs) {
        String data = extractJson(content,jqExprs);
        return (data!=null && data!="")? true: false;
    }

    public static void main(String[] args) {
        String json = "{\n" +
                "  \"server\": {\n" +
                "    \"name\": \"nginx\",\n" +
                "    \"version\": \"1.23.4\"\n" +
                "  },\n" +
                "  \"entities\": [\n" +
                "    {\n" +
                "      \"roles\": [\"registrant\", \"admin\"],\n" +
                "      \"vcardArray\": [\n" +
                "        [\"version\", {}, \"text\", \"4.0\"],\n" +
                "        [\n" +
                "          {\n" +
                "            \"type\": \"adr\",\n" +
                "            \"params\": {},\n" +
                "            \"value\": [\"\", \"\", \"123 Main St\", \"New York\", \"NY\", \"10001\"]\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"tel\",\n" +
                "            \"params\": {},\n" +
                "            \"value\": \"tel:+1-555-1234\"\n" +
                "          }\n" +
                "        ]\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"roles\": [\"guest\"],\n" +
                "      \"vcardArray\": [\n" +
                "        [\"version\", {}, \"text\", \"4.0\"],\n" +
                "        [\n" +
                "          {\n" +
                "            \"type\": \"email\",\n" +
                "            \"params\": {},\n" +
                "            \"value\": \"test@example.com\"\n" +
                "          }\n" +
                "        ]\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"m.server\": \"matrix.example.com:8448\",\n" +
                "  \"m.homeserver\": {\n" +
                "    \"base_url\": \"https://matrix.example.com\"\n" +
                "  },\n" +
                "  \"standaloneDatabase\": \"postgresql://user:pass@db:5432/app\"\n" +
                "}";
        List<String> rules = Arrays.asList(
                ".server.name",
                ".entities[] | select(.roles[] | contains(\"registrant\")) | .vcardArray[1].[] | select(.[0] == \"adr\") | .[-1][-1]",
                ".entities[] | select(.roles[] | contains(\"registrant\")) | .vcardArray[1][] | select(.[0] == \"adr\") | .[-1][-1]",
                ".entities[] | select(.roles[] | contains(\"registrant\")) | .vcardArray[1][] | select(.type == \"adr\") | .[-1][-1]",
                ".entities[] | select(.roles[] | contains(\"registrant\")) | .vcardArray[1][] | select(.type == \"adr\") | .value[-1]"
        );
        // github仓库已提Cannot index object with number问题，待更新

        System.out.println(extractJson(json, rules));
    }
} 