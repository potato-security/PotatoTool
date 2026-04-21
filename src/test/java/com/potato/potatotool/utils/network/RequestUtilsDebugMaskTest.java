package com.potato.potatotool.utils.network;

import com.potato.potatotool.ToStart;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RequestUtils debug 脱敏测试")
class RequestUtilsDebugMaskTest {

    @Test
    @DisplayName("internalAiRequest=true 时 404 debug 与报错信息脱敏")
    void debug404AndExceptionMessageSanitizedForInternalAiRequest() {
        RequestObj requestObj = new RequestObj()
                .setUrl("http://127.0.0.1:50003/stream")
                .setInternalAiRequest(true);

        CaptureResult result = executeAndCaptureDebugAndError(requestObj);

        assertTrue(result.errLog.contains("资源未找到 (404): "));
        assertTrue(result.errLog.contains("内置AI服务地址"));
        assertTrue(!result.errLog.contains("127.0.0.1:50003"));

        assertTrue(result.exceptionMessage.contains("[×] 资源未找到: 内置AI服务地址"));
        assertTrue(!result.exceptionMessage.contains("127.0.0.1:50003"));
    }

    @Test
    @DisplayName("internalAiRequest=false 时 404 debug 与报错信息保留原地址")
    void debug404AndExceptionMessageNotSanitizedForNormalRequest() {
        RequestObj requestObj = new RequestObj()
                .setUrl("https://api.example.com/v1/chat/completions")
                .setInternalAiRequest(false);

        CaptureResult result = executeAndCaptureDebugAndError(requestObj);

        assertTrue(result.errLog.contains("资源未找到 (404): https://api.example.com/v1/chat/completions"));
        assertTrue(result.exceptionMessage.contains("[×] 资源未找到: https://api.example.com/v1/chat/completions"));
    }

    private CaptureResult executeAndCaptureDebugAndError(RequestObj requestObj) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws FileNotFoundException {
                        throw new FileNotFoundException("mock 404");
                    }
                })
                .build();

        PrintStream originalErr = System.err;
        boolean originalDebugMode = ToStart.debugMode;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream captureErr;
        try {
            captureErr = new PrintStream(outputStream, true, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }

        try {
            System.setErr(captureErr);
            ToStart.debugMode = true;
            Exception exception = assertThrows(Exception.class, () -> RequestUtils.requests(requestObj, client));
            String errLog = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            return new CaptureResult(errLog, exception.getMessage());
        } finally {
            ToStart.debugMode = originalDebugMode;
            System.setErr(originalErr);
        }
    }

    private static class CaptureResult {
        private final String errLog;
        private final String exceptionMessage;

        private CaptureResult(String errLog, String exceptionMessage) {
            this.errLog = errLog;
            this.exceptionMessage = exceptionMessage;
        }
    }
}
