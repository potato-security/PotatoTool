package com.potato.potatotool.content.redTeam.infoGathering.classObj;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * @author Potato
 * @date 2024/10/31 09:09
 */
public class DomainInfo {
    private String ip;
    private String port;
    private String protocol;// http / https / http/ssl 等
    private String domain;
    private boolean isCND = false;
    private String host;
    private String url;
    private String statusCode;
    private String title;
    private String icp;
    private String certsSubjectOrg;
    private List<String> components;
    private String os;
    private String company;
    private String country;
    private String city;
    private String response;
    private boolean doWebInfoMap = false;
    private Map<String, Object> webInfoMap;
    private JsonArray googldLeakage;
    private JsonArray gitRepoLeakage;
    private String dataSource;

    public DomainInfo() {
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("ip", this.getIp());
        jsonObject.addProperty("port", this.getPort());
        jsonObject.addProperty("protocol", this.getProtocol());
        jsonObject.addProperty("domain", this.getDomain());
        jsonObject.addProperty("isCND", this.isCND());
        jsonObject.addProperty("host", this.getHost());
        jsonObject.addProperty("url", this.getUrl());
        jsonObject.addProperty("statusCode", this.getStatusCode());
        jsonObject.addProperty("title", this.getTitle());
        jsonObject.addProperty("icp", this.getIcp());
        jsonObject.addProperty("certsSubjectOrg", this.getCertsSubjectOrg());
        jsonObject.addProperty("dataSource", this.getDataSource());

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

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if(url.equals("暂无权限")) return;
        this.url = url;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        if(statusCode.equals("暂无权限")) return;
        this.statusCode = statusCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIcp() {
        return icp;
    }

    public void setIcp(String icp) {
        if(icp.equals("暂无权限")) return;
        this.icp = icp;
    }

    public String getCertsSubjectOrg() {
        return certsSubjectOrg;
    }

    public void setCertsSubjectOrg(String certsSubjectOrg) {
        this.certsSubjectOrg = certsSubjectOrg;
    }

    public List<String> getComponents() {
        return components;
    }

    public void setComponents(List<String> components) {
        this.components = components;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        if(company.equals("暂无权限")) return;
        this.company = company;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response.length() > 500 ? response.substring(0, 500) : response;;
    }

    public boolean isCND() {
        return isCND;
    }

    public void setCND(boolean CND) {
        this.isCND = CND;
    }

    public Map<String, Object> getWebInfoMap() {
        return webInfoMap;
    }

    public void setWebInfoMap(Map<String, Object> webInfoMap) {
        this.webInfoMap = webInfoMap;
    }

    public JsonArray getGoogldLeakage() {
        return googldLeakage;
    }

    public void setGoogldLeakage(JsonArray googldLeakage) {
        this.googldLeakage = googldLeakage;
    }

    public JsonArray getGitRepoLeakage() {
        return gitRepoLeakage;
    }

    public void setGitRepoLeakage(JsonArray gitRepoLeakage) {
        this.gitRepoLeakage = gitRepoLeakage;
    }

    public boolean isDoWebInfoMap() {
        return doWebInfoMap;
    }

    public void setDoWebInfoMap(boolean doWebInfoMap) {
        this.doWebInfoMap = doWebInfoMap;
    }


    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        if(dataSource==null) dataSource = "";
        this.dataSource = dataSource;
    }
}