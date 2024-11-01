package com.potato.potatotool.content.redTeam.infoGathering.classObj;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * @author Potato
 * @date 2024/10/31 09:09
 */
public class DomainInfo {
    private static String ip;
    private static String port;
    private static String protocol;// http / https / http/ssl 等
    private static String domain;
    private static String host;
    private static String url;
    private static String statusCode;
    private static String title;
    private static String icp;
    private static String certsSubjectOrg;
    private static List<String> components;
    private static String os;
    private static String company;
    private static String country;
    private static String city;
    private static String response;

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("ip", this.getIp());
        jsonObject.addProperty("port", this.getPort());
        jsonObject.addProperty("protocol", this.getProtocol());
        jsonObject.addProperty("domain", this.getDomain());
        jsonObject.addProperty("host", this.getHost());
        jsonObject.addProperty("url", this.getUrl());
        jsonObject.addProperty("statusCode", this.getStatusCode());
        jsonObject.addProperty("title", this.getTitle());
        jsonObject.addProperty("icp", this.getIcp());
        jsonObject.addProperty("certsSubjectOrg", this.getCertsSubjectOrg());

        JsonArray componentsArray = new JsonArray();
        if (this.getComponents() != null) {
            this.getComponents().forEach(componentsArray::add);
        }
        jsonObject.add("components", componentsArray);

        jsonObject.addProperty("os", this.getOs());
        jsonObject.addProperty("company", this.getCompany());
        jsonObject.addProperty("country", this.getCountry());
        jsonObject.addProperty("city", this.getCity());
        jsonObject.addProperty("banner", this.getResponse());
        return jsonObject;
    }

    public static String getIp() {
        return ip;
    }

    public static void setIp(String ip) {
        DomainInfo.ip = ip;
    }

    public static String getPort() {
        return port;
    }

    public static void setPort(String port) {
        DomainInfo.port = port;
    }

    public static String getProtocol() {
        return protocol;
    }

    public static void setProtocol(String protocol) {
        DomainInfo.protocol = protocol;
    }

    public static String getDomain() {
        return domain;
    }

    public static void setDomain(String domain) {
        DomainInfo.domain = domain;
    }

    public static String getHost() {
        return host;
    }

    public static void setHost(String host) {
        DomainInfo.host = host;
    }

    public static String getUrl() {
        return url;
    }

    public static void setUrl(String url) {
        if(url.equals("暂无权限")) return;
        DomainInfo.url = url;
    }

    public static String getStatusCode() {
        return statusCode;
    }

    public static void setStatusCode(String statusCode) {
        if(statusCode.equals("暂无权限")) return;
        DomainInfo.statusCode = statusCode;
    }

    public static String getTitle() {
        return title;
    }

    public static void setTitle(String title) {
        DomainInfo.title = title;
    }

    public static String getIcp() {
        return icp;
    }

    public static void setIcp(String icp) {
        if(icp.equals("暂无权限")) return;
        DomainInfo.icp = icp;
    }

    public static String getCertsSubjectOrg() {
        return certsSubjectOrg;
    }

    public static void setCertsSubjectOrg(String certsSubjectOrg) {
        DomainInfo.certsSubjectOrg = certsSubjectOrg;
    }

    public static List<String> getComponents() {
        return components;
    }

    public static void setComponents(List<String> components) {
        DomainInfo.components = components;
    }

    public static String getOs() {
        return os;
    }

    public static void setOs(String os) {
        DomainInfo.os = os;
    }

    public static String getCompany() {
        return company;
    }

    public static void setCompany(String company) {
        if(company.equals("暂无权限")) return;
        DomainInfo.company = company;
    }

    public static String getCountry() {
        return country;
    }

    public static void setCountry(String country) {
        DomainInfo.country = country;
    }

    public static String getCity() {
        return city;
    }

    public static void setCity(String city) {
        DomainInfo.city = city;
    }

    public static String getResponse() {
        return response;
    }

    public static void setResponse(String response) {
        DomainInfo.response = response.length() > 500 ? response.substring(0, 500) : response;;
    }
}