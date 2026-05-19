package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.google.gson.JsonObject;
import com.potato.potatotool.utils.network.RequestObj;
import com.potato.potatotool.utils.network.RequestUtils;
import okhttp3.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RequestObj/RequestUtils 请求体模式决策测试")
class HttpHandlerPostMethodMappingTest {

    @Test
    @DisplayName("RequestObj 默认构造 postMethodExplicit=false")
    void requestObjDefaultPostMethodExplicitShouldBeFalse() {
        RequestObj requestObj = new RequestObj();
        assertFalse(requestObj.isPostMethodExplicit());
    }

    @Test
    @DisplayName("setPostMethod 后 postMethodExplicit=true")
    void setPostMethodShouldMarkExplicit() {
        RequestObj requestObj = new RequestObj().setPostMethod("RAW");
        assertTrue(requestObj.isPostMethodExplicit());
        assertEquals("RAW", requestObj.getPostMethod());
    }

    @Test
    @DisplayName("setPostData(JsonObject) 后 postMethodExplicit=true")
    void setJsonPostDataShouldMarkExplicit() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("a", 1);

        RequestObj requestObj = new RequestObj().setPostData(jsonObject);
        assertTrue(requestObj.isPostMethodExplicit());
        assertEquals("JSON", requestObj.getPostMethod());
    }

    @Test
    @DisplayName("setFormParameters 后 postMethodExplicit=true")
    void setFormParametersShouldMarkExplicit() {
        RequestObj requestObj = new RequestObj()
                .setFormParameters(Collections.<String, Object>singletonMap("a", "1"));

        assertTrue(requestObj.isPostMethodExplicit());
        assertEquals("FORM", requestObj.getPostMethod());
    }

    @Test
    @DisplayName("未显式+JSON Header+postData => JSON")
    void implicitJsonHeaderWithBodyShouldResolveToJson() throws Exception {
        RequestObj requestObj = new RequestObj();
        requestObj.setHeaders(Collections.singletonMap("Content-Type", "application/json; charset=utf-8"));
        requestObj.setPostData("{\"name\":\"potato\"}");

        assertEquals("JSON", resolveEffectivePostMethod(requestObj));
    }

    @Test
    @DisplayName("未显式+formParameters => FORM")
    void implicitFormParametersShouldResolveToForm() throws Exception {
        RequestObj requestObj = new RequestObj();

        Field formParametersField = RequestObj.class.getDeclaredField("formParameters");
        formParametersField.setAccessible(true);
        Map<String, Object> formParameters = new HashMap<String, Object>();
        formParameters.put("a", "1");
        formParametersField.set(requestObj, formParameters);

        assertFalse(requestObj.isPostMethodExplicit());
        assertEquals("FORM", resolveEffectivePostMethod(requestObj));
    }

    @Test
    @DisplayName("未显式+普通文本body => RAW")
    void implicitPlainTextBodyShouldResolveToRaw() throws Exception {
        RequestObj requestObj = new RequestObj();
        requestObj.setHeaders(Collections.singletonMap("Content-Type", "text/plain"));
        requestObj.setPostData("a=1&b=2");

        assertEquals("RAW", resolveEffectivePostMethod(requestObj));
    }

    @Test
    @DisplayName("显式RAW+JSON Header => 仍 RAW")
    void explicitRawShouldNotBeOverriddenByJsonHeader() throws Exception {
        RequestObj requestObj = new RequestObj();
        requestObj.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
        requestObj.setPostData("{\"name\":\"potato\"}");
        requestObj.setPostMethod("RAW");

        assertEquals("RAW", resolveEffectivePostMethod(requestObj));
    }

    @Test
    @DisplayName("RAW body 应保留调用方指定的 Content-Type")
    void rawBodyShouldPreserveCallerContentType() throws Exception {
        RequestObj requestObj = new RequestObj();
        requestObj.setUrl("http://127.0.0.1/uploads/user");
        requestObj.setMethod("POST");
        requestObj.setHeaders(Collections.singletonMap(
                "Content-Type",
                "multipart/form-data; boundary=---------------------------99652559321225150602861519786"));
        requestObj.setPostData("-----------------------------99652559321225150602861519786\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"demo.jpg\"\r\n\r\n"
                + "data\r\n"
                + "-----------------------------99652559321225150602861519786--\r\n");

        Request request = RequestUtils.buildRequest(requestObj);

        assertEquals(
                "multipart/form-data; boundary=---------------------------99652559321225150602861519786",
                request.body().contentType().toString());
    }

    @Test
    @DisplayName("Goby三花括号变量与普通变量都应替换")
    void gobyTripleBracePlaceholderShouldBeReplaced() {
        Map<String, String> variables = new HashMap<String, String>();
        variables.put("X-CSRF-Token", "token_value");

        String result = HttpHandler.replaceVariables(
                "{{{X-CSRF-Token}}}|{{X-CSRF-Token}}",
                variables);

        assertEquals("token_value|token_value", result);
    }

    private String resolveEffectivePostMethod(RequestObj requestObj) throws Exception {
        Method method = RequestUtils.class.getDeclaredMethod("resolveEffectivePostMethod", RequestObj.class);
        method.setAccessible(true);
        return (String) method.invoke(null, requestObj);
    }
}
