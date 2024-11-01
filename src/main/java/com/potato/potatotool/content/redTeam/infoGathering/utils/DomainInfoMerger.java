package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.DomainInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Potato
 * @date 2024/11/1 10:31
 */
public class DomainInfoMerger {
    private static final Gson gson = new Gson();

    // 封装has\!=null\.isJsonNull
    // 抛出异常
    // 过滤筛选
    // 更新AssetObj

    public List<DomainInfo> mergeDomainInfos(JsonArray... jsonArrays) {
        List<DomainInfo> mergedList = new ArrayList<>();

        for (JsonArray jsonArray : jsonArrays) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();

                DomainInfo domainInfo = new DomainInfo();

                if (jsonObject.has("ip") && jsonObject.get("ip") != null && !jsonObject.get("ip").isJsonNull() && !jsonObject.get("ip").getAsString().contains("*")) {
                    domainInfo.setIp(jsonObject.get("ip").getAsString());
                    if(jsonObject.has("ip_str") && jsonObject.get("ip_str") != null && !jsonObject.get("ip_str").isJsonNull()){
                        domainInfo.setIp(jsonObject.get("ip_str").getAsString());
                    }
                }
                if (jsonObject.has("port") && jsonObject.get("ip") != null && !jsonObject.get("port").isJsonNull()) {
                    domainInfo.setPort(String.valueOf(jsonObject.get("port")));
                }
                if (jsonObject.has("portinfo") && jsonObject.get("portinfo") != null && !jsonObject.get("portinfo").isJsonNull()){
                    JsonObject portinfoJsonObject = jsonObject.getAsJsonObject("portinfo");

                    domainInfo.setPort(String.valueOf(portinfoJsonObject.get("port")));

                    if (portinfoJsonObject.has("title") && portinfoJsonObject.get("title") != null && !portinfoJsonObject.get("title").isJsonNull()) {
                        domainInfo.setTitle(portinfoJsonObject.get("title").getAsString());
                    }

                    if (portinfoJsonObject.has("os") && portinfoJsonObject.get("os") != null && !portinfoJsonObject.get("os").isJsonNull()) {
                        domainInfo.setOs(portinfoJsonObject.get("os").getAsString());
                    }
                }
                if (jsonObject.has("protocol") && jsonObject.get("protocol") != null && !jsonObject.get("protocol").isJsonNull()) {
                    if(jsonObject.get("protocol").isJsonObject()){
                        domainInfo.setProtocol(jsonObject.getAsJsonObject("protocol").get("application").getAsString());
                    }else {
                        domainInfo.setProtocol(jsonObject.get("protocol").getAsString());
                    }
                }
                if (jsonObject.has("service") && jsonObject.get("service") != null && !jsonObject.get("service").isJsonNull()){
                    JsonObject serviceJsonObject = jsonObject.getAsJsonObject("service");

                    if(serviceJsonObject.has("name") && serviceJsonObject.get("name") != null && !serviceJsonObject.get("name").isJsonNull()){
                        domainInfo.setProtocol(serviceJsonObject.get("name").getAsString());
                    }

                    if(serviceJsonObject.has("html") && serviceJsonObject.get("html") != null && !serviceJsonObject.get("html").isJsonNull()){
                        JsonObject httpJsonObject = serviceJsonObject.getAsJsonObject("http");

                        if(httpJsonObject.has("host") && httpJsonObject.get("host") != null && !httpJsonObject.get("host").isJsonNull()){
                            domainInfo.setHost(httpJsonObject.get("host").getAsString());
                        }

                        if(httpJsonObject.has("status_code") && httpJsonObject.get("status_code") != null && !httpJsonObject.get("status_code").isJsonNull()){
                            domainInfo.setStatusCode(String.valueOf(httpJsonObject.get("status_code")));
                        }

                        if (httpJsonObject.has("title") && httpJsonObject.get("title") != null && !httpJsonObject.get("title").isJsonNull()) {
                            domainInfo.setTitle(httpJsonObject.get("title").getAsString());
                        }

                        if (httpJsonObject.has("icp") && httpJsonObject.get("icp") != null && !httpJsonObject.get("icp").isJsonNull()) {
                            domainInfo.setIcp(httpJsonObject.get("icp").getAsString());
                        }
                    }

                    if (serviceJsonObject.has("response") && serviceJsonObject.get("response") != null && !serviceJsonObject.get("response").isJsonNull()) {
                        domainInfo.setResponse(serviceJsonObject.get("response").getAsString());
                    }
                }
                if (jsonObject.has("http") && jsonObject.get("http") != null && !jsonObject.get("http").isJsonNull()){
                    JsonObject httpJsonObject = jsonObject.getAsJsonObject("http");
                    if(httpJsonObject.has("html") && httpJsonObject.get("html") != null && !httpJsonObject.get("html").isJsonNull()){
                        domainInfo.setProtocol("http");
                        domainInfo.setResponse(httpJsonObject.get("html").getAsString());
                    }
                    if(httpJsonObject.has("host") && httpJsonObject.get("host") != null && !httpJsonObject.get("host").isJsonNull()){
                        domainInfo.setHost(httpJsonObject.get("host").getAsString());
                    }
                    if (httpJsonObject.has("title") && httpJsonObject.get("title") != null && !httpJsonObject.get("title").isJsonNull()) {
                        domainInfo.setTitle(httpJsonObject.get("title").getAsString());
                    }
                }
                if (jsonObject.has("domain") && jsonObject.get("domain") != null && !jsonObject.get("domain").isJsonNull()) {
                    if(jsonObject.get("domain").isJsonArray()){
                        StringBuilder result = new StringBuilder();
                        for (JsonElement element : jsonArray) {
                            result.append(element.getAsString()).append("\n");
                        }
                        domainInfo.setDomain(result.toString().trim());
                    }else {
                        domainInfo.setDomain(jsonObject.get("domain").getAsString());
                    }
                }
                if (jsonObject.has("host") && jsonObject.get("host") != null && !jsonObject.get("host").isJsonNull()) {
                    domainInfo.setHost(jsonObject.get("host").getAsString());
                }
                if (jsonObject.has("url") && jsonObject.get("url") != null && !jsonObject.get("url").isJsonNull()) {
                    domainInfo.setUrl(jsonObject.get("url").getAsString());
                }
                if (jsonObject.has("status_code")) {
                    domainInfo.setStatusCode(String.valueOf(jsonObject.get("status_code")));
                }
                if (jsonObject.has("title") && jsonObject.get("title") != null && !jsonObject.get("title").isJsonNull()) {
                    domainInfo.setTitle(jsonObject.get("title").getAsString());
                }
                if (jsonObject.has("web_title") && jsonObject.get("web_title") != null && !jsonObject.get("web_title").isJsonNull()) {
                    domainInfo.setTitle(jsonObject.get("web_title").getAsString());
                }
                if (jsonObject.has("icp") && jsonObject.get("icp") != null && !jsonObject.get("icp").isJsonNull()) {
                    domainInfo.setIcp(jsonObject.get("icp").getAsString());
                }
                if (jsonObject.has("number") && jsonObject.get("number") != null && !jsonObject.get("number").isJsonNull()) {
                    domainInfo.setIcp(jsonObject.get("number").getAsString());
                }
                if (jsonObject.has("certs_subject_org") && jsonObject.get("certs_subject_org") != null && !jsonObject.get("certs_subject_org").isJsonNull()) {
                    domainInfo.setCertsSubjectOrg(jsonObject.get("certs_subject_org").getAsString());
                }

                if (jsonObject.has("component") && jsonObject.get("component") != null && !jsonObject.get("component").isJsonNull()) {
                    List<String> components = new ArrayList<>();
                    jsonObject.getAsJsonArray("component").forEach(comp -> {
                        JsonObject componentObj = comp.getAsJsonObject();
                        components.add(componentObj.get("name").getAsString() + " " + componentObj.get("version").getAsString());
                    });
                    domainInfo.setComponents(components);
                }

                if (jsonObject.has("components") && jsonObject.get("components") != null && !jsonObject.get("components").isJsonNull()) {
                    List<String> components = new ArrayList<>();
                    jsonObject.getAsJsonArray("components").forEach(comp -> {
                        JsonObject componentObj = comp.getAsJsonObject();
                        JsonArray productTypeJsonArray = componentObj.getAsJsonArray("product_type");
                        StringBuilder result = new StringBuilder();
                        for (JsonElement element : productTypeJsonArray) {
                            result.append(element.getAsString()).append(",");
                        }
                        components.add(componentObj.get("product_name_cn").getAsString() + " " + result.toString().trim());
                    });
                    domainInfo.setComponents(components);
                }

                if (jsonObject.has("os")) {
                    domainInfo.setOs(jsonObject.get("os").getAsString());
                }
                if (jsonObject.has("company") && jsonObject.get("company") != null && !jsonObject.get("company").isJsonNull()) {
                    domainInfo.setCompany(jsonObject.get("company").getAsString());
                }
                if (jsonObject.has("country") && jsonObject.get("country") != null && !jsonObject.get("country").isJsonNull()) {
                    domainInfo.setCountry(jsonObject.get("country").getAsString());
                }
                if (jsonObject.has("city") && jsonObject.get("city") != null && !jsonObject.get("city").isJsonNull()) {
                    domainInfo.setCity(jsonObject.get("city").getAsString());
                }
                if (jsonObject.has("location") && jsonObject.get("location") != null && !jsonObject.get("location").isJsonNull()) {
                    JsonObject locationJsonObject = jsonObject.getAsJsonObject("location");

                    if (locationJsonObject.has("country_cn") && locationJsonObject.get("country_cn") != null && !locationJsonObject.get("country_cn").isJsonNull()) {
                        domainInfo.setCountry(locationJsonObject.get("country_cn").getAsString());
                    }else if(locationJsonObject.has("country_en") && locationJsonObject.get("country_en") != null && !locationJsonObject.get("country_en").isJsonNull()){
                        domainInfo.setCountry(locationJsonObject.get("country_en").getAsString());
                    }

                    if (locationJsonObject.has("city_cn") && locationJsonObject.get("city_cn") != null && !locationJsonObject.get("city_cn").isJsonNull()) {
                        domainInfo.setCity(locationJsonObject.get("city_cn").getAsString());
                    }else if(locationJsonObject.has("city_en") && locationJsonObject.get("city_en") != null && !locationJsonObject.get("city_en").isJsonNull()){
                        domainInfo.setCity(locationJsonObject.get("city_en").getAsString());
                    }

                    if (locationJsonObject.has("country_name") && locationJsonObject.get("country_name") != null && !locationJsonObject.get("country_name").isJsonNull()) {
                        domainInfo.setCountry(locationJsonObject.get("country_name").getAsString());
                    }
                    if (locationJsonObject.has("city") && locationJsonObject.get("city") != null && !locationJsonObject.get("city").isJsonNull()) {
                        domainInfo.setCity(locationJsonObject.get("city").getAsString());
                    }
                }
                if (jsonObject.has("geoinfo") && jsonObject.get("geoinfo") != null && !jsonObject.get("geoinfo").isJsonNull()) {
                    JsonObject geoinfoJsonObject = jsonObject.getAsJsonObject("geoinfo");

                    if (geoinfoJsonObject.has("country") && geoinfoJsonObject.get("country") != null && !geoinfoJsonObject.get("country").isJsonNull()) {
                        if(geoinfoJsonObject.getAsJsonObject("country").getAsJsonObject("name").get("zh-CN")!=null && !geoinfoJsonObject.getAsJsonObject("country").getAsJsonObject("name").get("zh-CN").isJsonNull()) {
                            domainInfo.setCountry(geoinfoJsonObject.getAsJsonObject("country").getAsJsonObject("name").get("zh-CN").getAsString());
                        }else {
                            domainInfo.setCountry(geoinfoJsonObject.getAsJsonObject("country").getAsJsonObject("name").get("en").getAsString());
                        }
                    }

                    if (geoinfoJsonObject.has("city") && geoinfoJsonObject.get("city") != null && !geoinfoJsonObject.get("city").isJsonNull()) {
                        if(geoinfoJsonObject.getAsJsonObject("city").getAsJsonObject("name").get("zh-CN")!=null && !geoinfoJsonObject.getAsJsonObject("city").getAsJsonObject("name").get("zh-CN").isJsonNull()) {
                            domainInfo.setCity(geoinfoJsonObject.getAsJsonObject("city").getAsJsonObject("name").get("zh-CN").getAsString());
                        }else {
                            domainInfo.setCity(geoinfoJsonObject.getAsJsonObject("city").getAsJsonObject("name").get("en").getAsString());
                        }
                    }

                }
                if (jsonObject.has("response") && jsonObject.get("response") != null && !jsonObject.get("response").isJsonNull()) {
                    domainInfo.setResponse(jsonObject.get("response").getAsString());
                }
                if (jsonObject.has("banner") && jsonObject.get("banner") != null && !jsonObject.get("banner").isJsonNull()) {
                    domainInfo.setResponse(jsonObject.get("banner").getAsString());
                }



                mergedList.add(domainInfo);
            }

        }

        return mergedList;
    }
}
