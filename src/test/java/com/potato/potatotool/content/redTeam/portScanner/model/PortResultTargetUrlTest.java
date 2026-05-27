package com.potato.potatotool.content.redTeam.portScanner.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 验证 toTargetUrl 的智能化：HTTP/HTTPS 服务才拼 scheme，其他给 host:port。 */
class PortResultTargetUrlTest {

    @Test
    void httpsServiceUsesHttpsScheme() {
        PortResult r = new PortResult("10.0.0.1", 8443, PortState.OPEN);
        r.setService("https-alt");
        assertEquals("https://10.0.0.1:8443", r.toTargetUrl());
    }

    @Test
    void tlsFlagWithHttpServiceUsesHttps() {
        PortResult r = new PortResult("10.0.0.1", 9999, PortState.OPEN);
        r.setService("http-alt");
        r.setTls(true);
        assertEquals("https://10.0.0.1:9999", r.toTargetUrl());
    }

    @Test
    void httpServiceUsesHttpScheme() {
        PortResult r = new PortResult("10.0.0.1", 8081, PortState.OPEN);
        r.setService("http-alt");
        assertEquals("http://10.0.0.1:8081", r.toTargetUrl());
    }

    @Test
    void wellKnownHttpPortFallsBackToHttp() {
        PortResult r = new PortResult("10.0.0.1", 80, PortState.OPEN);
        assertEquals("http://10.0.0.1:80", r.toTargetUrl());
    }

    @Test
    void wellKnownHttpsPortFallsBackToHttps() {
        PortResult r = new PortResult("10.0.0.1", 443, PortState.OPEN);
        assertEquals("https://10.0.0.1:443", r.toTargetUrl());
    }

    @Test
    void nonHttpServiceKeepsHostPort() {
        PortResult ssh = new PortResult("10.0.0.1", 22, PortState.OPEN);
        ssh.setService("ssh");
        assertEquals("10.0.0.1:22", ssh.toTargetUrl());

        PortResult mysql = new PortResult("10.0.0.1", 3306, PortState.OPEN);
        mysql.setService("mysql");
        assertEquals("10.0.0.1:3306", mysql.toTargetUrl());

        PortResult redis = new PortResult("10.0.0.1", 6379, PortState.OPEN);
        redis.setService("redis");
        assertEquals("10.0.0.1:6379", redis.toTargetUrl());

        PortResult smtps = new PortResult("10.0.0.1", 465, PortState.OPEN);
        smtps.setService("smtps");
        smtps.setTls(true);
        assertEquals("10.0.0.1:465", smtps.toTargetUrl());
    }

    @Test
    void unknownServiceOnArbitraryPortKeepsHostPort() {
        PortResult r = new PortResult("10.0.0.1", 12345, PortState.OPEN);
        assertEquals("10.0.0.1:12345", r.toTargetUrl());
    }
}
