package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetObj;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.DomainInfo;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.NetAssets;
import com.potato.potatotool.utils.data.JsonUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * @author Potato
 * @date 2024/11/25 17:27
 */
public class AssetExcelExporter {
    private XSSFWorkbook workbook;
    private Map<String, CellStyle> styles;

    public static boolean generateRepIng = false;

    public AssetExcelExporter() {
        generateRepIng = true;
        try {
            if(workbook == null) this.workbook = new XSSFWorkbook();
            this.styles = createStyles(workbook);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public String exportToExcel(AssetObj assetObj, String filePath) {
        List<NetAssets> companyDomainInfoList = assetObj.getCompanyDomainInfoList();
        try {
            // 创建主要信息概览sheet
            createOverviewSheet(companyDomainInfoList);

           // 创建公司详情信息sheet
           createCompanyDetailSheet(assetObj);

           // 创建公司资产详细信息sheet
           for (NetAssets company : companyDomainInfoList) {
               createCompanyAssetSheet(company);
           }

           // 创建敏感信息汇总sheet
           createSensitiveInfoSheet(companyDomainInfoList);

           // 创建信息泄露汇总sheet
           createLeakageInfoSheet(companyDomainInfoList);

           // 保存文件
           File file = new File(filePath);
           File parentDir = file.getParentFile();
           if (parentDir!= null &&!parentDir.exists()) {
               parentDir.mkdirs();
           }
           try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
               workbook.write(fileOut);
           }
            workbook.close();
        } catch (Exception e) {
            if(debugMode) e.printStackTrace();
            return e.toString();
        }finally {
            generateRepIng = false;
        }

        return null;
    }

    private void createOverviewSheet(List<NetAssets> companyDomainInfoList) {
        XSSFSheet sheet = workbook.createSheet("资产概览");
        sheet.setDefaultColumnWidth(40);

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"公司/个人名称", "域名数量", "IP数量", "端口数量", "域名列表"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            safeSetCellValue(cell, headers[i]);
            cell.setCellStyle(styles.get("header"));
        }

        // 填充数据
        int rowNum = 1;
        for (NetAssets company : companyDomainInfoList) {
            Row row = sheet.createRow(rowNum++);
            safeSetCellValue(row.createCell(0), company.getCompanyName());

            Set<String> uniqueIps = new HashSet<>();
            Set<String> uniquePorts = new HashSet<>();
            Set<String> domains = new HashSet<>();

            for (DomainInfo domain : company.getDomainInfoList()) {
                String ip = domain.getIp();
                String port = domain.getPort();
                String domainStr = domain.getDomain();
                if (ip != null && !ip.isEmpty()) uniqueIps.add(ip);
                if (port != null && !port.isEmpty()) uniquePorts.add(port);
                if (domainStr != null && !domainStr.isEmpty()) domains.add(domainStr);
            }

            safeSetCellValue(row.createCell(1), domains.size());
            safeSetCellValue(row.createCell(2), uniqueIps.size());
            safeSetCellValue(row.createCell(3), uniquePorts.size());
            // 如果文本超出最大限制（32767字符），则分割成多个单元格
            String combinedText = String.join("\n", domains);
            if (combinedText.length() > 32767) {
                // 你可以根据需要将文本拆分为多个单元格
                int cellCount = (combinedText.length() / 32767) + 1;
                for (int i = 0; i < cellCount; i++) {
                    String part = combinedText.substring(i * 32767, Math.min((i + 1) * 32767, combinedText.length()));
                    safeSetCellValue(row.createCell(4 + i), part);
                    Cell cell = row.getCell(4 + i);
                    if (cell == null) {
                        cell = row.createCell(i);
                    }
                    cell.setCellStyle(styles.get("cell"));
                }
            } else {
                safeSetCellValue(row.createCell(4), combinedText);
            }

            // 应用样式
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.getCell(i);
                if (cell == null) {
                    cell = row.createCell(i);
                }
                cell.setCellStyle(styles.get("cell"));
            }
        }

        for (int i = 0; i < 5; i++) {
            manualSetColumnWidth(sheet, i);
        }
    }

    private void createCompanyDetailSheet(AssetObj assetObj) {
        XSSFSheet sheet = workbook.createSheet("公司或个人详情");
        sheet.setDefaultColumnWidth(40);

        int currentRow = 0;

        // 域名基础信息部分
        JsonObject seoMap = assetObj.getSeoMap();
        if (seoMap != null) {
            currentRow = addSectionTitle(sheet, currentRow, "SEO基础信息", 3);

            // 域名信息
            if (seoMap.has("域名信息")) {
                currentRow = addSubSectionTitle(sheet, currentRow, "域名信息", 3);
                currentRow = addKeyValueSection(sheet, currentRow, seoMap.getAsJsonObject("域名信息"));
            }

            // 备案信息
            if (seoMap.has("备案信息")) {
                currentRow = addSubSectionTitle(sheet, currentRow, "备案信息", 3);
                currentRow = addKeyValueSection(sheet, currentRow, seoMap.getAsJsonObject("备案信息"));
            }

            // 网站信息
            if (seoMap.has("网站信息")) {
                currentRow = addSubSectionTitle(sheet, currentRow, "网站信息", 3);
                currentRow = addKeyValueSection(sheet, currentRow, seoMap.getAsJsonObject("网站信息"));
            }

            // 权重信息
            if (seoMap.has("权重信息")) {
                currentRow = addSubSectionTitle(sheet, currentRow, "权重信息", 6);
                currentRow = addKeyValueSection(sheet, currentRow, seoMap.getAsJsonObject("权重信息"));
            }
            // 添加一个空白行作为分隔
            currentRow++;
        }

        // 工商和公司信息部分
        JsonObject companyInfoMap = assetObj.getCompanyInfoMap();
        if (companyInfoMap != null) {
            currentRow = addSectionTitle(sheet, currentRow, companyInfoMap.get("企业名称").toString() + "概要",7);

            // 工商信息
            if (companyInfoMap.has("工商信息")) {
                currentRow = addSubSectionTitle(sheet, currentRow, "工商信息", 7);
                currentRow = addKeyValueSection(sheet, currentRow, companyInfoMap.getAsJsonObject("工商信息"));
            }

            // 微信公众号信息
            if (companyInfoMap.has("微信公众号")) {
                currentRow = addSubSectionTitle(sheet, currentRow, "微信公众号信息", 3);
                currentRow = addKeyValueSection(sheet, currentRow, companyInfoMap.getAsJsonArray("微信公众号"));
            }

            // 软件著作
            if (companyInfoMap.has("软件著作")) {
                currentRow = addSubSectionTitle(sheet, currentRow, "软件著作信息", 2);
                currentRow = addKeyValueSection(sheet, currentRow, companyInfoMap.getAsJsonArray("软件著作"));
            }

            // 网站备案
            if (companyInfoMap.has("网站备案")) {
                currentRow = addSubSectionTitle(sheet, currentRow, "网站备案信息", 3);
                currentRow = addKeyValueSection(sheet, currentRow, companyInfoMap.getAsJsonArray("网站备案"));
            }
            currentRow++;
        }

        // 公司补充信息部分
        JsonArray companyDetailsInfoMap = assetObj.getCompanyDetailsInfoMap();
        if (companyDetailsInfoMap != null) {

            for (int i = 0; i < companyDetailsInfoMap.size(); i++) {
                JsonObject companyInfo = companyDetailsInfoMap.get(i).getAsJsonObject();
                for (String companyName : companyInfo.keySet()) {
                    currentRow = addSectionTitle(sheet, currentRow, companyName+"_补充信息", 5);
                    JsonObject details = companyInfo.get(companyName).getAsJsonObject();

                    // App信息
                    if (details.has("app") && !details.getAsJsonArray("app").isEmpty()) {
                        currentRow = addSubSectionTitle(sheet, currentRow, "App信息", 5);
                        currentRow = addKeyValueSection(sheet, currentRow, details.getAsJsonArray("app"));
                    }

                    // 微信信息
                    if (details.has("wx") && !details.getAsJsonArray("wx").isEmpty()) {
                        currentRow = addSectionTitle(sheet, currentRow, "微信信息", 6);
                        currentRow = addKeyValueSection(sheet, currentRow, details.getAsJsonArray("wx"));
                    }

                    // 域名和备案信息
                    if (details.has("domainAndIcp") && !details.getAsJsonArray("domainAndIcp").isEmpty()) {
                        currentRow = addSectionTitle(sheet, currentRow, "域名备案信息", 4);
                        currentRow = addKeyValueSection(sheet, currentRow, details.getAsJsonArray("domainAndIcp"));
                    }

                    // 公司权重信息
                    if (details.has("weightCompany") && !details.getAsJsonArray("weightCompany").isEmpty()) {
                        currentRow = addSectionTitle(sheet, currentRow, "公司权重信息",8);
                        currentRow = addKeyValueSection(sheet, currentRow, details.getAsJsonArray("weightCompany"));
                    }
                    currentRow++;
                }
            }
        }
    }
    private int addSectionTitle(XSSFSheet sheet, int currentRow, String title, int colNum) {
        Row titleRow = sheet.createRow(currentRow++);
        Cell titleCell = titleRow.createCell(0);
        safeSetCellValue(titleCell, title);
        sheet.addMergedRegion(new CellRangeAddress(currentRow - 1, currentRow - 1, 0, colNum - 1));
        for (int i = 0; i <= colNum - 1; i++) {
            Cell cell = titleRow.getCell(i);
            if (cell == null) {
                cell = titleRow.createCell(i);
            }
            cell.setCellStyle(styles.get("title"));
        }

        return currentRow;
    }
    private int addSubSectionTitle(XSSFSheet sheet, int currentRow, String title, int colNum) {
        Row titleRow = sheet.createRow(currentRow++);
        Cell titleCell = titleRow.createCell(0);
        safeSetCellValue(titleCell, title);
        sheet.addMergedRegion(new CellRangeAddress(currentRow - 1, currentRow - 1, 0, colNum - 1));
        for (int i = 0; i <= colNum - 1; i++) {
            Cell cell = titleRow.getCell(i);
            if (cell == null) {
                cell = titleRow.createCell(i);
            }
            cell.setCellStyle(styles.get("subTitle"));
        }

        return currentRow;
    }

    private int addKeyValueSection(XSSFSheet sheet, int startRow, JsonElement jsonElement) {
        // 创建表头行
        if(jsonElement.isJsonObject()) {
            JsonObject dataObject = jsonElement.getAsJsonObject();

            Row headerRow = sheet.createRow(startRow++);
            Set<String> keys = dataObject.keySet();
            int colCount = 0;
            for (String key : keys) {
                Cell cell = headerRow.createCell(colCount++);
                safeSetCellValue(cell, key);
                cell.setCellStyle(styles.get("header"));
            }

            // 创建数据行
            Row dataRow = sheet.createRow(startRow++);
            colCount = 0;
            for (String key : keys) {
                JsonElement value = dataObject.get(key);
                Cell cell = dataRow.createCell(colCount++);

                // 处理不同类型的值
                if (value == null || value.isJsonNull()) {
                    safeSetCellValue(cell, "/");
                } else if (value.isJsonPrimitive()) {
                    safeSetCellValue(cell, value.getAsString());
                } else if (value.isJsonArray()) {
                    safeSetCellValue(cell, jsonArrayToString(value.getAsJsonArray()));
                } else if (value.isJsonObject()) {
                    safeSetCellValue(cell, value.toString());
                }
            }
            for (int i = 0; i < keys.size(); i++) {
                Cell cell = dataRow.getCell(i);
                if (cell == null) {
                    cell = dataRow.createCell(i);
                }
                cell.setCellStyle(styles.get("cell"));
            }
        }else if(jsonElement.isJsonArray() && jsonElement.getAsJsonArray().size()>0 && jsonElement.getAsJsonArray().get(0).isJsonObject()){
            JsonArray dataArray = jsonElement.getAsJsonArray();
            Set<String> keys = new HashSet<>();
            for (JsonElement element : dataArray) {
                JsonObject jsonObject = element.getAsJsonObject();
                for (String key : jsonObject.keySet()) {
                    keys.add(key);
                }
            }

            Row headerRow = sheet.createRow(startRow++);
            int colCount = 0;
            for (String key : keys) {
                Cell cell = headerRow.createCell(colCount++);
                safeSetCellValue(cell, key);
                cell.setCellStyle(styles.get("header"));
            }

            // 创建数据行
            for (JsonElement element : dataArray) {
                Row dataRow = sheet.createRow(startRow++);
                colCount = 0;
                JsonObject dataObject = element.getAsJsonObject();
                for (String key : keys) {
                    Cell cell = dataRow.createCell(colCount++);

                    if(!dataObject.has(key)) safeSetCellValue(cell, "/");
                    JsonElement value = dataObject.get(key);
                    // 处理不同类型的值
                    if (value == null || value.isJsonNull()) {
                        safeSetCellValue(cell, "/");
                    } else if (value.isJsonPrimitive()) {
                        safeSetCellValue(cell, value.getAsString());
                    } else if (value.isJsonArray()) {
                        safeSetCellValue(cell, jsonArrayToString(value.getAsJsonArray()));
                    } else if (value.isJsonObject()) {
                        safeSetCellValue(cell, value.toString());
                    }
                }
                for (int i = 0; i < keys.size(); i++) {
                    Cell cell = dataRow.getCell(i);
                    if (cell == null) {
                        cell = dataRow.createCell(i);
                    }
                    cell.setCellStyle(styles.get("cell"));
                }
            }
        }

        return startRow;
    }
    private String jsonArrayToString(JsonArray jsonArray) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < jsonArray.size(); i++) {
            sb.append(jsonArray.get(i).toString());
            if (i < jsonArray.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private boolean sheetExists(String sheetName) {
        for (Sheet sheet : workbook) {
            if (sheet.getSheetName().equals(sheetName)) {
                return true;
            }
        }
        return false;
    }

    private void createCompanyAssetSheet(NetAssets company) {
        String name = company.getCompanyName();
        if((name==null || name.isEmpty()|| name.equals("-")) && company.getDomainInfoList().size()>0) name = company.getDomainInfoList().get(0).getDomain();
        if((name==null || name.isEmpty()|| name.equals("-")) && company.getDomainInfoList().size()>0) name = company.getDomainInfoList().get(0).getIp();
        String sheetName = name + "_资产";
        int suffix = 1;
        while (sheetExists(sheetName)) {
            sheetName = name + "_资产" + suffix;
            suffix++;
        }
        XSSFSheet sheet = workbook.createSheet(sheetName);
        sheet.setDefaultColumnWidth(40);

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"域名", "数据来源", "IP", "端口", "协议", "状态码_by平台", "isCND", "标题", "ICP备案", "证书组织",
                "组件", "操作系统", "所属公司/个人", "国家", "城市", "URL_By平台", "响应_By平台", "状态码_By本地", "URL_By本地", "标题_By本地", "响应_By本地", "图标Url", "图标Md5", "图标Mmh3", "图标Base64"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            safeSetCellValue(cell, headers[i]);
            cell.setCellStyle(styles.get("header"));
        }

        // 填充数据
        int rowNum = 1;
        for (DomainInfo domain : company.getDomainInfoList()) {
            Row row = sheet.createRow(rowNum++);

            safeSetCellValue(row.createCell(0), valueOrDefault(domain.getDomain()));
            safeSetCellValue(row.createCell(1), valueOrDefault(domain.getDataSource()));
            String ip = valueOrDefault(domain.getIp());
            safeSetCellValue(row.createCell(2), ip);
            String port = valueOrDefault(domain.getPort());
            safeSetCellValue(row.createCell(3), port);
            String protocol= valueOrDefault(domain.getProtocol());
            safeSetCellValue(row.createCell(4), protocol);
            safeSetCellValue(row.createCell(5), valueOrDefault(domain.getStatusCode()));
            safeSetCellValue(row.createCell(6), valueOrDefault(domain.isCND()));
            safeSetCellValue(row.createCell(7), valueOrDefault(domain.getTitle()));
            safeSetCellValue(row.createCell(8), valueOrDefault(domain.getIcp()));
            safeSetCellValue(row.createCell(9), valueOrDefault(domain.getCertsSubjectOrg()));
            safeSetCellValue(row.createCell(10), valueOrDefault(domain.getComponents()));
            safeSetCellValue(row.createCell(11), valueOrDefault(domain.getOs()));
            safeSetCellValue(row.createCell(12), valueOrDefault(domain.getCompany()));
            safeSetCellValue(row.createCell(13), valueOrDefault(domain.getCountry()));
            safeSetCellValue(row.createCell(14), valueOrDefault(domain.getCity()));
            String url = valueOrDefault(domain.getUrl());
            if(url.equals("\\") && protocol.toLowerCase().contains("http")){
                url = protocol + "://" + ip + ":" + port;
            }
            safeSetCellValue(row.createCell(15), url);
            safeSetCellValue(row.createCell(16), valueOrDefault(domain.getResponse()));

            if (domain.isDoWebInfoMap() && domain.getWebInfoMap() != null) {
                Map<String, Object> webInfo = domain.getWebInfoMap();
                safeSetCellValue(row.createCell(17), getStringOrDefault(webInfo, "statusCode"));
                safeSetCellValue(row.createCell(18), getStringOrDefault(webInfo, "url"));
                safeSetCellValue(row.createCell(19), getStringOrDefault(webInfo, "title"));
                safeSetCellValue(row.createCell(20), getStringOrDefault(webInfo, "body"));
                safeSetCellValue(row.createCell(21), getStringOrDefault(webInfo, "iconUrl"));
                safeSetCellValue(row.createCell(22), getStringOrDefault(webInfo, "iconMd5"));
                safeSetCellValue(row.createCell(23), getStringOrDefault(webInfo, "iconMmh3"));
                try {
                    safeSetCellValue(row.createCell(24), getStringOrDefault(webInfo, "iconBase64"));
                }catch (Exception e){
                    safeSetCellValue(row.createCell(24), "内容过长省略");
                }
            }

            // 应用样式
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.getCell(i);
                if (cell == null) {
                    cell = row.createCell(i);
                }
                cell.setCellStyle(styles.get("cell"));
            }
        }

        // 自动调整列宽 - 使用手动计算避免 AWT 依赖
        for (int i = 0; i < headers.length; i++) {
            manualSetColumnWidth(sheet, i);
        }
    }

    private void createSensitiveInfoSheet(List<NetAssets> companyDomainInfoList) {
        XSSFSheet sheet = workbook.createSheet("敏感信息汇总");
        sheet.setDefaultColumnWidth(40);

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"公司/个人名称", "URL", "电话号码", "身份证号", "密码", "IP", "内网IP", "邮箱", "Key泄露"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            safeSetCellValue(cell, headers[i]);
            cell.setCellStyle(styles.get("header"));
        }

        int rowNum = 1;
        for (NetAssets company : companyDomainInfoList) {
            for (DomainInfo domain : company.getDomainInfoList()) {
                if (domain.isDoWebInfoMap() && domain.getWebInfoMap() != null) {
                    Map<String, Object> webInfo = domain.getWebInfoMap();
                    if (webInfo.get("sensitive") != null) {
                        List<Map<String, Object>> sensitiveList = (List<Map<String, Object>>) webInfo.get("sensitive");
                        for (Map<String, Object> sensitive : sensitiveList) {
                            Row row = sheet.createRow(rowNum++);
                            safeSetCellValue(row.createCell(0), company.getCompanyName());
                            safeSetCellValue(row.createCell(1), String.valueOf(sensitive.get("Url")));
                            safeSetCellValue(row.createCell(2), getListOrDefault(sensitive, "PhoneNumber"));
                            safeSetCellValue(row.createCell(3), getListOrDefault(sensitive, "IdCard"));
                            safeSetCellValue(row.createCell(4), getListOrDefault(sensitive, "Password"));
                            safeSetCellValue(row.createCell(5), getListOrDefault(sensitive, "IP"));
                            safeSetCellValue(row.createCell(6), getListOrDefault(sensitive, "InternalIP"));
                            safeSetCellValue(row.createCell(7), getListOrDefault(sensitive, "Email"));
                            safeSetCellValue(row.createCell(8), getListOrDefault(sensitive, "Key"));

                            // 应用样式
                            for (int i = 0; i < headers.length; i++) {
                                Cell cell = row.getCell(i);
                                if (cell == null) {
                                    cell = row.createCell(i);
                                }
                                cell.setCellStyle(styles.get("cell"));
                            }
                        }
                    }else if(webInfo.get("internalLinks") != null){
                        Set<String> internalLinks = (Set<String>) webInfo.get("internalLinks");
                        for (String internalLink : internalLinks){
                            Row row = sheet.createRow(rowNum++);
                            safeSetCellValue(row.createCell(0), internalLink);
                            safeSetCellValue(row.createCell(1), "\\");
                            safeSetCellValue(row.createCell(2), "\\");
                            safeSetCellValue(row.createCell(3), "\\");
                            safeSetCellValue(row.createCell(4), "\\");
                            safeSetCellValue(row.createCell(5), "\\");
                            safeSetCellValue(row.createCell(6), "\\");
                            safeSetCellValue(row.createCell(7), "\\");
                            safeSetCellValue(row.createCell(8), "\\");

                            // 应用样式
                            for (int i = 0; i < headers.length; i++) {
                                Cell cell = row.getCell(i);
                                if (cell == null) {
                                    cell = row.createCell(i);
                                }
                                cell.setCellStyle(styles.get("cell"));
                            }
                        }
                    }
                }
            }
        }

        // 自动调整列宽 - 使用手动计算避免 AWT 依赖
        for (int i = 0; i < headers.length; i++) {
            manualSetColumnWidth(sheet, i);
        }
    }

    private String getListOrDefault(Map<String, Object> sensitive, String key) {
        try {
            List<String> list = (List<String>) sensitive.get(key);
            return (list != null && list.size()>0)
                    ? String.join("\n", list)
                    : "\\";
        }catch (Exception e){
            e.printStackTrace();
            return "\\";
        }
    }

    private String getStringOrDefault(Map<String, Object> objectMap, String key) {
        try {
            Object value = objectMap.get(key);
            if (value!= null) {
                String res = value.toString();
                return (res!= null &&!res.isEmpty())
                        ? res
                        : "\\";
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return "\\";
    }

    private String valueOrDefault(Object value) {
        if (value == null) return "\\";

        if (value instanceof String) {
            return ((String) value).isEmpty() ? "\\" : (String) value;
        }

        if (value instanceof Integer) {
            return value.toString();
        }

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.isEmpty() ? "\\" : String.join(", ", (List<String>) list);
        }

        return value.toString();
    }

    /**
     * 安全地设置单元格值，防止 null 导致的 NullPointerException
     * @param cell 单元格对象
     * @param value 要设置的值
     */
    private void safeSetCellValue(Cell cell, String value) {
        cell.setCellValue(value == null ? "" : value);
    }

    /**
     * 安全地设置单元格数值，防止 null 导致的 NullPointerException
     * @param cell 单元格对象
     * @param value 要设置的数值
     */
    private void safeSetCellValue(Cell cell, Integer value) {
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
    }


    // 【功能】：自动调整列宽
    // 【BUG】：单元格未创建或值为 null，autoSizeColumn 在遍历时调用 SheetUtil.getCellWidth 会抛出 NPE
    // POI 在计算过程中会触发 AWT 字体加载（例如执行 sun.awt.FontConfiguration.getVersion），这也可能导致 NPE
    // 由于 JavaFX 的 FXMLLoader 使用线程上下文类加载器，如果 autoSizeColumn 触发了 AWT 相关逻辑，可能间接影响了类加载器状态，导致后续加载 FXML 时找不到类
    // 【解决方案】：使用 manualSetColumnWidth 手动计算列宽，完全避免 AWT 依赖，不会影响 JavaFX
    /**
     * 手动计算并设置列宽（更安全的替代方案，不依赖 AWT）
     * 正确处理换行符、制表符等特殊字符
     * 
     * @param sheet 工作表
     * @param columnIndex 列索引
     */
    private void manualSetColumnWidth(Sheet sheet, int columnIndex) {
        int maxWidth = 0;
        
        // 遍历该列的所有行，找出最大宽度
        for (Row row : sheet) {
            Cell cell = row.getCell(columnIndex);
            if (cell != null) {
                String cellValue = getCellValueAsString(cell);
                if (cellValue != null && !cellValue.isEmpty()) {
                    int cellWidth = calculateTextWidth(cellValue);
                    maxWidth = Math.max(maxWidth, cellWidth);
                }
            }
        }
        
        // 设置列宽，限制最大宽度为 255 个字符
        maxWidth = Math.min(maxWidth + 512, 255 * 256); // 额外留一些边距
        maxWidth = Math.max(maxWidth, 10 * 256); // 最小宽度
        sheet.setColumnWidth(columnIndex, maxWidth);
    }
    
    /**
     * 计算文本宽度，正确处理换行符、制表符等特殊字符
     * 
     * @param text 文本内容
     * @return 宽度（POI 单位）
     */
    private int calculateTextWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // 如果包含换行符，按行分割，取最长行的宽度
        String[] lines = text.split("\n");
        int maxLineWidth = 0;
        
        for (String line : lines) {
            int lineWidth = 0;
            
            for (char c : line.toCharArray()) {
                if (c == '\t') {
                    // 制表符按 4 个空格计算
                    lineWidth += 256 * 4;
                } else if (c == '\r') {
                    // 回车符忽略
                    continue;
                } else if (Character.isISOControl(c)) {
                    // 其他控制字符忽略
                    continue;
                } else if (c > 127) {
                    // 中文字符、日文、韩文等宽字符
                    lineWidth += 512;
                } else if (c >= '0' && c <= '9') {
                    // 数字稍微宽一点
                    lineWidth += 280;
                } else if (c >= 'A' && c <= 'Z') {
                    // 大写字母稍微宽一点
                    lineWidth += 300;
                } else if (c >= 'a' && c <= 'z') {
                    // 小写字母
                    lineWidth += 256;
                } else if (c == ' ') {
                    // 空格
                    lineWidth += 200;
                } else {
                    // 其他 ASCII 字符（标点符号等）
                    lineWidth += 256;
                }
            }
            
            maxLineWidth = Math.max(maxLineWidth, lineWidth);
        }
        
        return maxLineWidth;
    }
    
    /**
     * 获取单元格的字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private void createLeakageInfoSheet(List<NetAssets> companyDomainInfoList) {
        XSSFSheet sheet = workbook.createSheet("信息泄露汇总");
        sheet.setDefaultColumnWidth(40);

        // 创建Google泄露信息部分
        int currentRow = 0;
        Row titleRow = sheet.createRow(currentRow++);
        Cell titleCell = titleRow.createCell(0);
        safeSetCellValue(titleCell, "Google泄露信息");
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        for (int i = 0; i <= 5; i++) {
            Cell cell = titleRow.getCell(i);
            if (cell == null) {
                cell = titleRow.createCell(i);
            }
            cell.setCellStyle(styles.get("title"));
        }

        // Google泄露表头
        Row googleHeader = sheet.createRow(currentRow++);
        String[] googleHeaders = {"公司/个人名称", "域名", "标题", "URL", "内容描述", "泄露详情"};
        createHeaderRow(googleHeader, googleHeaders);

        // 填充Google泄露数据
        for (NetAssets company : companyDomainInfoList) {
            for (DomainInfo domain : company.getDomainInfoList()) {
                if (domain.getGoogldLeakage() != null) {
                    JsonArray leakages = domain.getGoogldLeakage();
                    for (int i = 0; i < leakages.size(); i++) {
                        Row row = sheet.createRow(currentRow++);
                        safeSetCellValue(row.createCell(0), company.getCompanyName());
                        safeSetCellValue(row.createCell(1), leakages.get(i).getAsJsonObject().get("domain").getAsString());
                        safeSetCellValue(row.createCell(2), leakages.get(i).getAsJsonObject().get("title").getAsString());
                        safeSetCellValue(row.createCell(3), leakages.get(i).getAsJsonObject().get("url").getAsString());
                        safeSetCellValue(row.createCell(4), leakages.get(i).getAsJsonObject().get("des").getAsString());
                        safeSetCellValue(row.createCell(5), JsonUtils.jsonArrayToString(leakages.get(i).getAsJsonObject().getAsJsonArray("leakageArray"),"\n"));

                        for (int j = 0; j < googleHeaders.length; j++) {
                            Cell cell = row.getCell(j);
                            if (cell == null) {
                                cell = row.createCell(i);
                            }
                            cell.setCellStyle(styles.get("cell"));
                        }
                    }
                }
            }
        }

        // 添加间隔行
        currentRow++;

        // 创建Git仓库泄露信息部分
        Row gitTitle = sheet.createRow(currentRow++);
        Cell gitTitleCell = gitTitle.createCell(0);
        safeSetCellValue(gitTitleCell, "Git仓库泄露信息");
        sheet.addMergedRegion(new CellRangeAddress(currentRow-1, currentRow-1, 0, 3));
        for (int i = 0; i <= 3; i++) {
            Cell cell = gitTitle.getCell(i);
            if (cell == null) {
                cell = gitTitle.createCell(i);
            }
            cell.setCellStyle(styles.get("title"));
        }

        // Git泄露表头
        Row gitHeader = sheet.createRow(currentRow++);
        String[] gitHeaders = {"公司/个人名称", "仓库名称", "仓库URL", "仓库描述"};
        createHeaderRow(gitHeader, gitHeaders);

        // 填充Git泄露数据
        for (NetAssets company : companyDomainInfoList) {
            for (DomainInfo domain : company.getDomainInfoList()) {
                if (domain.getGitRepoLeakage() != null) {
                    JsonArray repos = domain.getGitRepoLeakage();
                    for (int i = 0; i < repos.size(); i++) {
                        Row row = sheet.createRow(currentRow++);
                        safeSetCellValue(row.createCell(0), company.getCompanyName());
                        safeSetCellValue(row.createCell(1), repos.get(i).getAsJsonObject().get("repoName").getAsString());
                        safeSetCellValue(row.createCell(2), repos.get(i).getAsJsonObject().get("repoUrl").getAsString());
                        safeSetCellValue(row.createCell(3), repos.get(i).getAsJsonObject().get("repoDes").getAsString());

                        for (int j = 0; j < gitHeaders.length; j++) {
                            Cell cell = row.getCell(j);
                            if (cell == null) {
                                cell = row.createCell(i);
                            }
                            cell.setCellStyle(styles.get("cell"));
                        }
                    }
                }
            }
        }

        // 自动调整列宽 - 使用手动计算避免 AWT 依赖
        for (int i = 0; i < 6; i++) {
            manualSetColumnWidth(sheet, i);
        }
    }

    private void createHeaderRow(Row headerRow, String[] headers) {
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            safeSetCellValue(cell, headers[i]);
            cell.setCellStyle(styles.get("header"));
        }
    }

    private Map<String, CellStyle> createStyles(XSSFWorkbook workbook) {
        Map<String, CellStyle> styles = new HashMap<>();

        // 标题样式
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setBorderTop(BorderStyle.THIN);
        titleStyle.setBorderBottom(BorderStyle.THICK);
        titleStyle.setBorderLeft(BorderStyle.THIN);
        titleStyle.setBorderRight(BorderStyle.THIN);
        titleStyle.setBottomBorderColor(IndexedColors.DARK_BLUE.getIndex());
        styles.put("title", titleStyle);

        CellStyle subTitleStyle = workbook.createCellStyle();
        subTitleStyle.setFont(titleFont);
        subTitleStyle.setAlignment(HorizontalAlignment.CENTER);
        subTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        subTitleStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        subTitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        subTitleStyle.setBorderTop(BorderStyle.THIN);
        subTitleStyle.setBorderBottom(BorderStyle.THIN);
        subTitleStyle.setBorderLeft(BorderStyle.THIN);
        subTitleStyle.setBorderRight(BorderStyle.THIN);
        styles.put("subTitle", subTitleStyle);

        // 表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setWrapText(true);
        styles.put("header", headerStyle);

        // 单元格样式
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.LEFT);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setWrapText(true);
        styles.put("cell", cellStyle);

        return styles;
    }

}