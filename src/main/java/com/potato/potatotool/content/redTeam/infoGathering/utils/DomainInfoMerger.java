package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.potato.potatotool.content.redTeam.infoGathering.cdn.CdnChecker;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.DomainInfo;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Potato
 * @date 2024/11/1 10:31
 */
public class DomainInfoMerger {

    public static List<DomainInfo> mergeDomainInfos(String dataSource, JsonObject jsonObject) {
        List<DomainInfo> mergedList = new ArrayList<>();
        if(jsonObjectHasKey(jsonObject, "ports")){
            for (JsonElement jsonElement : jsonObject.getAsJsonArray("ports")) {
                DomainInfo domainInfo = new DomainInfo();
                domainInfo.setDataSource(dataSource);
                String ip = jsonObject.get("ip").getAsString();
                String domain = jsonObject.get("domain").getAsString();
                domainInfo.setIp(ip);
                domainInfo.setDomain(domain);
                domainInfo.setCND(CdnChecker.isCdnIp(ip) && CdnChecker.isCdnDomain(domain));
                JsonObject portsInfo = jsonElement.getAsJsonObject();
                domainInfo.setPort(portsInfo.get("port").getAsString());
                domainInfo.setProtocol(portsInfo.get("protocol").getAsString());
                mergedList.add(domainInfo);
            }
        }
        return mergedList;
    }

    public static List<DomainInfo> mergeDomainInfos(String dataSource, JsonArray... jsonArrays) {
        List<DomainInfo> mergedList = new ArrayList<>();

        for (JsonArray jsonArray : jsonArrays) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();

                DomainInfo domainInfo = new DomainInfo();
                domainInfo.setDataSource(dataSource);

                if (jsonObjectHasKey(jsonObject, "ip") && !jsonObject.get("ip").getAsString().contains("*")) {
                    domainInfo.setIp(jsonObject.get("ip").getAsString());
                    if(jsonObjectHasKey(jsonObject, "ip_str")){
                        domainInfo.setIp(jsonObject.get("ip_str").getAsString());
                    }
                }
                if (jsonObjectHasKey(jsonObject, "port")) {
                    domainInfo.setPort(stringValueOf(jsonObject.get("port")));
                }
                if (jsonObjectHasKey(jsonObject, "portinfo")){
                    JsonObject portinfoJsonObject = jsonObject.getAsJsonObject("portinfo");

                    domainInfo.setPort(stringValueOf(portinfoJsonObject.get("port")));

                    if (jsonObjectHasKey(portinfoJsonObject, "title")) {
                        JsonElement title = portinfoJsonObject.get("title");
                        if(title.isJsonArray()){
                            JsonArray titleList = title.getAsJsonArray();
                            domainInfo.setTitle(titleList.get(titleList.size() - 1).getAsString());
                        }else {
                            domainInfo.setTitle(title.getAsString());
                        }
                    }

                    if (jsonObjectHasKey(portinfoJsonObject, "os")) {
                        domainInfo.setOs(portinfoJsonObject.get("os").getAsString());
                    }
                }
                if (jsonObjectHasKey(jsonObject, "protocol")) {
                    if(jsonObject.get("protocol").isJsonObject()){
                        domainInfo.setProtocol(jsonObject.getAsJsonObject("protocol").get("application").getAsString());
                    }else {
                        domainInfo.setProtocol(jsonObject.get("protocol").getAsString());
                    }
                }
                if (jsonObjectHasKey(jsonObject, "service")){
                    JsonObject serviceJsonObject = jsonObject.getAsJsonObject("service");

                    if(jsonObjectHasKey(serviceJsonObject, "name")){
                        domainInfo.setProtocol(serviceJsonObject.get("name").getAsString());
                    }

                    if(jsonObjectHasKey(serviceJsonObject, "http")){
                        JsonObject httpJsonObject = serviceJsonObject.getAsJsonObject("http");

                        if(jsonObjectHasKey(httpJsonObject, "host")){
                            domainInfo.setHost(httpJsonObject.get("host").getAsString());
                        }

                        if(jsonObjectHasKey(httpJsonObject, "status_code")){
                            domainInfo.setStatusCode(stringValueOf(httpJsonObject.get("status_code")));
                        }

                        if (jsonObjectHasKey(httpJsonObject, "title")) {
                            domainInfo.setTitle(httpJsonObject.get("title").getAsString());
                        }

                        if (jsonObjectHasKey(httpJsonObject, "icp")) {
                            JsonElement icpJsonElement = httpJsonObject.get("icp");
                            if(icpJsonElement.isJsonPrimitive()){
                                domainInfo.setIcp(httpJsonObject.get("icp").getAsString());
                            }else{
                                if(jsonObjectHasKey(icpJsonElement.getAsJsonObject(), "main_licence")){
                                    domainInfo.setIcp(icpJsonElement.getAsJsonObject().getAsJsonObject("main_licence").get("licence").getAsString());
                                }
                            }
                        }
                    }

                    if (jsonObjectHasKey(serviceJsonObject, "response")) {
                        domainInfo.setResponse(serviceJsonObject.get("response").getAsString());
                    }
                }
                if (jsonObjectHasKey(jsonObject, "http")){
                    JsonObject httpJsonObject = jsonObject.getAsJsonObject("http");
                    if(jsonObjectHasKey(httpJsonObject, "html")){
                        domainInfo.setProtocol("http");
                        domainInfo.setResponse(httpJsonObject.get("html").getAsString());
                    }
                    if(jsonObjectHasKey(httpJsonObject, "host")){
                        domainInfo.setHost(httpJsonObject.get("host").getAsString());
                    }
                    if (jsonObjectHasKey(httpJsonObject, "title")) {
                        domainInfo.setTitle(httpJsonObject.get("title").getAsString());
                    }
                }
                if (jsonObjectHasKey(jsonObject, "domain")) {
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

                domainInfo.setCND(CdnChecker.isCdnIp(domainInfo.getIp()) && CdnChecker.isCdnDomain(domainInfo.getDomain()));

                if (jsonObjectHasKey(jsonObject, "host")) {
                    domainInfo.setHost(jsonObject.get("host").getAsString());
                }
                if (jsonObjectHasKey(jsonObject, "url")) {
                    domainInfo.setUrl(jsonObject.get("url").getAsString());
                }
                if (jsonObjectHasKey(jsonObject, "status_code")) {
                    domainInfo.setStatusCode(stringValueOf(jsonObject.get("status_code")));
                }
                if (jsonObjectHasKey(jsonObject, "title")) {
                    domainInfo.setTitle(jsonObject.get("title").getAsString());
                }
                if (jsonObjectHasKey(jsonObject, "web_title")) {
                    domainInfo.setTitle(jsonObject.get("web_title").getAsString());
                }
                if (jsonObjectHasKey(jsonObject, "icp")) {
                    domainInfo.setIcp(jsonObject.get("icp").getAsString());
                }
                if (jsonObjectHasKey(jsonObject, "number")) {
                    domainInfo.setIcp(jsonObject.get("number").getAsString());
                }
                if (jsonObjectHasKey(jsonObject, "certs_subject_org")) {
                    domainInfo.setCertsSubjectOrg(jsonObject.get("certs_subject_org").getAsString());
                }

                if (jsonObjectHasKey(jsonObject, "component")) {
                    List<String> components = new ArrayList<>();
                    jsonObject.getAsJsonArray("component").forEach(comp -> {
                        JsonObject componentObj = comp.getAsJsonObject();
                        components.add(componentObj.get("name").getAsString() + " " + componentObj.get("version").getAsString());
                    });
                    domainInfo.setComponents(components);
                }

                if (jsonObjectHasKey(jsonObject, "components")) {
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

                if (jsonObjectHasKey(jsonObject, "os")) {
                    domainInfo.setOs(jsonObject.get("os").getAsString());
                }
                if (jsonObjectHasKey(jsonObject, "company")) {
                    domainInfo.setCompany(jsonObject.get("company").getAsString());
                }
                if (jsonObjectHasKey(jsonObject, "country")) {
                    domainInfo.setCountry(jsonObject.get("country").getAsString());
                }
                if (jsonObjectHasKey(jsonObject, "city")) {
                    domainInfo.setCity(jsonObject.get("city").getAsString());
                }
                if (jsonObjectHasKey(jsonObject, "location")) {
                    JsonObject locationJsonObject = jsonObject.getAsJsonObject("location");

                    if (jsonObjectHasKey(locationJsonObject, "country_cn")) {
                        domainInfo.setCountry(locationJsonObject.get("country_cn").getAsString());
                    }else if(jsonObjectHasKey(locationJsonObject, "country_en")){
                        domainInfo.setCountry(locationJsonObject.get("country_en").getAsString());
                    }

                    if (jsonObjectHasKey(locationJsonObject, "city_cn")) {
                        domainInfo.setCity(locationJsonObject.get("city_cn").getAsString());
                    }else if(jsonObjectHasKey(locationJsonObject, "city_en")){
                        domainInfo.setCity(locationJsonObject.get("city_en").getAsString());
                    }

                    if (jsonObjectHasKey(locationJsonObject, "country_name")) {
                        domainInfo.setCountry(locationJsonObject.get("country_name").getAsString());
                    }
                    if (jsonObjectHasKey(locationJsonObject, "city")) {
                        domainInfo.setCity(locationJsonObject.get("city").getAsString());
                    }
                }
                if (jsonObjectHasKey(jsonObject, "geoinfo")) {
                    JsonObject geoinfoJsonObject = jsonObject.getAsJsonObject("geoinfo");

                    if (jsonObjectHasKey(geoinfoJsonObject, "country")) {
                        JsonObject nameJsonObject = geoinfoJsonObject.getAsJsonObject("country").getAsJsonObject("names");
                        if(jsonObjectHasKey(nameJsonObject, "zh-CN")) {
                            domainInfo.setCountry(nameJsonObject.get("zh-CN").getAsString());
                        }else {
                            domainInfo.setCountry(nameJsonObject.get("en").getAsString());
                        }
                    }

                    if (jsonObjectHasKey(geoinfoJsonObject, "city")) {
                        JsonObject nameJsonObject = geoinfoJsonObject.getAsJsonObject("city").getAsJsonObject("names");
                        if(jsonObjectHasKey(nameJsonObject, "zh-CN")) {
                            domainInfo.setCity(nameJsonObject.get("zh-CN").getAsString());
                        }else {
                            domainInfo.setCity(nameJsonObject.get("en").getAsString());
                        }
                    }

                }
                if (jsonObjectHasKey(jsonObject, "response")) {
                    domainInfo.setResponse(jsonObject.get("response").getAsString());
                }
                if (jsonObjectHasKey(jsonObject, "banner")) {
                    domainInfo.setResponse(jsonObject.get("banner").getAsString());
                }

                mergedList.add(domainInfo);
            }

        }

        return mergedList;
    }

    private static String stringValueOf(JsonElement jsonElement) {
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return null;
        }

        if (jsonElement.isJsonPrimitive()) {
            JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();
            return primitive.getAsString();
        }

        return null;
    }

    private static boolean jsonObjectHasKey(JsonObject jsonObject, String key) {
        return jsonObject.has(key) && jsonObject.get(key) != null && !jsonObject.get(key).isJsonNull();
    }

    public static List<DomainInfo> mergeDomainInfoList(List<DomainInfo> domainInfoList) {
        Map<String, DomainInfo> mergedMap = new HashMap<>();

        for (DomainInfo domainInfo : domainInfoList) {
            String key = domainInfo.getIp() + ":" + domainInfo.getPort();

            if (mergedMap.containsKey(key)) {
                DomainInfo existingInfo = mergedMap.get(key);
                mergeDomainInfo(existingInfo, domainInfo);
            } else {
                mergedMap.put(key, domainInfo);
            }
        }

        return new ArrayList<>(mergedMap.values());
    }

    private static void mergeDomainInfo(DomainInfo target, DomainInfo source) {
        setIfNull(target::getProtocol, source.getProtocol(), target::setProtocol);
        setIfNull(target::getDomain, source.getDomain(), target::setDomain);
        setIfNull(target::getHost, source.getHost(), target::setHost);
        setIfNull(target::getUrl, source.getUrl(), target::setUrl);
        setIfNull(target::getStatusCode, source.getStatusCode(), target::setStatusCode);
        setIfNull(target::getTitle, source.getTitle(), target::setTitle);
        setIfNull(target::getIcp, source.getIcp(), target::setIcp);
        setIfNull(target::getCertsSubjectOrg, source.getCertsSubjectOrg(), target::setCertsSubjectOrg);
        setIfNull(target::getOs, source.getOs(), target::setOs);
        setIfNull(target::getCompany, source.getCompany(), target::setCompany);
        setIfNull(target::getCountry, source.getCountry(), target::setCountry);
        setIfNull(target::getCity, source.getCity(), target::setCity);
        setIfNull(target::getResponse, source.getResponse(), target::setResponse);

        // Merge components and remove duplicates
        if (source.getComponents() != null) {
            if (target.getComponents() == null) {
                target.setComponents(new ArrayList<>());
            }
            Set<String> mergedComponents = new LinkedHashSet<>(target.getComponents());
            mergedComponents.addAll(source.getComponents());
            target.setComponents(new ArrayList<>(mergedComponents));
        }
    }

    private static <T> void setIfNull(Supplier<T> getter, T sourceValue, Consumer<T> setter) {
        if (getter.get() == null && sourceValue != null) {
            setter.accept(sourceValue);
        }
    }

    // 获取List<DomainInfo> B 中 ip 在 A 中不存在的所有元素。
    public static List<DomainInfo> getUniqueDomainInfoInB(List<DomainInfo> listA, List<DomainInfo> listB) {
        // 获取List A中的所有IP，存入Set
        Set<String> ipSetA = listA.stream()
                .map(DomainInfo::getIp)
                .collect(Collectors.toSet());

        // 过滤出List B中IP不在Set A中的DomainInfo
        return listB.stream()
                .filter(domainInfo -> !ipSetA.contains(domainInfo.getIp()))
                .collect(Collectors.toList());
    }
}
