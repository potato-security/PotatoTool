package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.potato.potatotool.utils.network.RequestObj;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("HttpHandler Raw HTTP 组装覆盖测试")
class HttpHandlerRawRequestCoverageTest {

    @Test
    @DisplayName("同 host 的绝对 URL 当前应按目标基路径拼接")
    void shouldKeepCurrentBasePathMergeForSameHostAbsoluteUrl() {
        RequestObj requestObj = new RequestObj();
        HttpHandler.processRawRequest(
                requestObj,
                Collections.singletonList(
                        "GET http://example.com/admin/login?from=raw HTTP/1.1\n" +
                        "Host: example.com\n\n"
                ),
                "http://example.com/base",
                new LinkedHashMap<String, String>()
        );

        assertEquals("GET", requestObj.getMethod());
        assertEquals("http://example.com/base/admin/login?from=raw", requestObj.getUrl());
    }

    @Test
    @DisplayName("不同 host 的绝对 URL 应保留原始绝对地址")
    void shouldKeepAbsoluteUrlWhenHostDiffersFromTarget() {
        RequestObj requestObj = new RequestObj();
        HttpHandler.processRawRequest(
                requestObj,
                Collections.singletonList(
                        "GET http://169.254.169.254/latest/meta-data HTTP/1.1\n" +
                        "Host: 169.254.169.254\n\n"
                ),
                "http://example.com/base",
                new LinkedHashMap<String, String>()
        );

        assertEquals("http://169.254.169.254/latest/meta-data", requestObj.getUrl());
    }

    @Test
    @DisplayName("重复 Header 当前语义应保留最后一个值")
    void shouldKeepLastDuplicateHeaderValue() {
        RequestObj requestObj = new RequestObj();
        HttpHandler.processRawRequest(
                requestObj,
                Collections.singletonList(
                        "POST /submit HTTP/1.1\n" +
                        "Host: example.com\n" +
                        "X-Test: first\n" +
                        "X-Test: second\n" +
                        "\n" +
                        "body=1"
                ),
                "http://example.com",
                new LinkedHashMap<String, String>()
        );

        assertEquals("second", requestObj.getHeaders().get("X-Test"));
        assertEquals("body=1", new String(requestObj.getPostData()));
    }
}
