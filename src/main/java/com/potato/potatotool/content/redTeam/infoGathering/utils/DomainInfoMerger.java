package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.potato.potatotool.content.redTeam.infoGathering.cdn.CdnChecker;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.DomainInfo;

import java.net.URL;
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
                domainInfo.setCND(isLikelyCdn(ip, domain));
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
                        for (JsonElement element : jsonObject.getAsJsonArray("domain")) {
                            result.append(element.getAsString()).append("\n");
                        }
                        domainInfo.setDomain(result.toString().trim());
                    }else {
                        domainInfo.setDomain(jsonObject.get("domain").getAsString());
                    }
                }

                domainInfo.setCND(isLikelyCdn(domainInfo.getIp(), domainInfo.getDomain()));

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
        Map<String, List<DomainInfo>> groupedDomainInfoMap = new LinkedHashMap<>();

        for (DomainInfo domainInfo : domainInfoList) {
            String baseKey = buildBaseKey(domainInfo);
            List<DomainInfo> bucket = groupedDomainInfoMap.get(baseKey);
            if (bucket == null) {
                bucket = new ArrayList<>();
                groupedDomainInfoMap.put(baseKey, bucket);
            }

            DomainInfo existingInfo = findMergeCandidate(bucket, domainInfo);
            if (existingInfo == null) {
                bucket.add(domainInfo);
            } else {
                mergeDomainInfo(existingInfo, domainInfo);
            }
        }
        // 外侧Obj直接使用了入参domainInfoList地址，故多此一步
        domainInfoList.clear();
        for (List<DomainInfo> bucket : groupedDomainInfoMap.values()) {
            domainInfoList.addAll(bucket);
        }

        return domainInfoList;
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
        mergeTextField(target.getDataSource(), source.getDataSource(), target::setDataSource);

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

    private static void mergeTextField(String targetValue, String sourceValue, Consumer<String> setter) {
        if (sourceValue == null || sourceValue.trim().isEmpty()) {
            return;
        }

        if (targetValue == null || targetValue.trim().isEmpty()) {
            setter.accept(sourceValue);
            return;
        }

        LinkedHashSet<String> mergedValues = new LinkedHashSet<>();
        mergedValues.addAll(splitTextValues(targetValue));
        mergedValues.addAll(splitTextValues(sourceValue));
        setter.accept(String.join("\n", mergedValues));
    }

    // 获取List<DomainInfo> B 中 ip 在 A 中不存在的所有元素。
    public static List<DomainInfo> getUniqueDomainInfoInB(List<DomainInfo> listA, List<DomainInfo> listB) {
        Map<String, List<DomainInfo>> groupedA = new HashMap<>();
        for (DomainInfo domainInfo : listA) {
            String baseKey = buildBaseKey(domainInfo);
            List<DomainInfo> bucket = groupedA.get(baseKey);
            if (bucket == null) {
                bucket = new ArrayList<>();
                groupedA.put(baseKey, bucket);
            }
            bucket.add(domainInfo);
        }

        return listB.stream()
                .filter(domainInfo -> {
                    List<DomainInfo> bucket = groupedA.get(buildBaseKey(domainInfo));
                    if (bucket == null || bucket.isEmpty()) {
                        return true;
                    }
                    for (DomainInfo existing : bucket) {
                        if (isSameAsset(existing, domainInfo)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private static DomainInfo findMergeCandidate(List<DomainInfo> bucket, DomainInfo candidate) {
        for (DomainInfo existing : bucket) {
            if (isSameAsset(existing, candidate)) {
                return existing;
            }
        }
        return null;
    }

    private static boolean isSameAsset(DomainInfo left, DomainInfo right) {
        if (!buildBaseKey(left).equals(buildBaseKey(right))) {
            return false;
        }

        Set<String> leftIdentifiers = collectIdentifiers(left);
        Set<String> rightIdentifiers = collectIdentifiers(right);
        if (leftIdentifiers.isEmpty() || rightIdentifiers.isEmpty()) {
            return true;
        }

        for (String identifier : leftIdentifiers) {
            if (rightIdentifiers.contains(identifier)) {
                return true;
            }
        }
        return false;
    }

    private static String buildBaseKey(DomainInfo domainInfo) {
        return safeText(domainInfo.getIp()) + ":" + safeText(domainInfo.getPort());
    }

    private static Set<String> collectIdentifiers(DomainInfo domainInfo) {
        LinkedHashSet<String> identifiers = new LinkedHashSet<>();
        addIdentifiers(identifiers, domainInfo.getDomain());
        addIdentifiers(identifiers, domainInfo.getHost());
        addIdentifier(identifiers, extractHost(domainInfo.getUrl()));
        return identifiers;
    }

    private static void addIdentifiers(Set<String> identifiers, String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return;
        }
        for (String item : splitTextValues(rawValue)) {
            addIdentifier(identifiers, item);
        }
    }

    private static void addIdentifier(Set<String> identifiers, String rawValue) {
        if (rawValue == null) {
            return;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return;
        }
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.startsWith("*.")) {
            normalized = normalized.substring(2);
        }
        identifiers.add(normalized);
    }

    private static List<String> splitTextValues(String rawValue) {
        return Arrays.stream(rawValue.split("[\\n,;]+"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }

    private static String extractHost(String urlValue) {
        if (urlValue == null || urlValue.trim().isEmpty()) {
            return null;
        }
        try {
            return new URL(urlValue).getHost();
        } catch (Exception e) {
            return urlValue;
        }
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isLikelyCdn(String ip, String domain) {
        return CdnChecker.isCdnIp(ip) || CdnChecker.isCdnDomain(domain);
    }
}
