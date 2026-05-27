package com.potato.potatotool.content.redTeam.vulnScanner.loader;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PocRepository 关键字搜索测试")
class PocRepositorySearchTest {

    @Test
    @DisplayName("应支持按ID、名称、描述关键字搜索")
    void shouldSearchByIdNameAndDescription() {
        PocRepository repository = new PocRepository();
        repository.addPoc(buildPoc("spring-rce-001", "Spring Boot RCE", "Actuator RCE verification"));
        repository.addPoc(buildPoc("struts-sqli-001", "Apache Struts SQLi", "classic OGNL injection"));

        List<PocObj.Poc> byId = repository.search("spring-rce");
        List<PocObj.Poc> byName = repository.search("struts");
        List<PocObj.Poc> byDescription = repository.search("ognl");

        assertEquals(1, byId.size());
        assertEquals("spring-rce-001", byId.get(0).getId());

        assertEquals(1, byName.size());
        assertEquals("struts-sqli-001", byName.get(0).getId());

        assertEquals(1, byDescription.size());
        assertEquals("struts-sqli-001", byDescription.get(0).getId());
    }

    @Test
    @DisplayName("空关键字不应返回全部POC")
    void shouldReturnEmptyForBlankKeyword() {
        PocRepository repository = new PocRepository();
        repository.addPoc(buildPoc("only-one", "Only One", "single poc"));

        assertTrue(repository.search("").isEmpty());
        assertTrue(repository.search("   ").isEmpty());
        assertTrue(repository.search(null).isEmpty());
    }

    private PocObj.Poc buildPoc(String id, String name, String description) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(name);
        poc.setDescription(description);
        poc.setOriginalFormat("nuclei");
        poc.setProtocol("http");
        poc.setSeverity(PocObj.Severity.HIGH);
        poc.setTags(Arrays.asList("search", "regression"));
        return poc;
    }
}
