package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.google.gson.JsonArray;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetObj;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.DomainInfo;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.NetAssets;
import com.potato.potatotool.utils.jsonUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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

    public AssetExcelExporter() {
        this.workbook = new XSSFWorkbook();
        this.styles = createStyles(workbook);
    }

    public String exportToExcel(AssetObj assetObj, String filePath) {
        List<NetAssets> companyDomainInfoList = assetObj.getCompanyDomainInfoList();
        try {
            // 创建主要信息概览sheet
            createOverviewSheet(companyDomainInfoList);

            // 为每个公司创建详细信息sheet
            for (NetAssets company : companyDomainInfoList) {
                createCompanyDetailSheet(company);
            }

            // 创建敏感信息汇总sheet
            createSensitiveInfoSheet(companyDomainInfoList);

            // 创建信息泄露汇总sheet
            createLeakageInfoSheet(companyDomainInfoList);

            // 保存文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
            workbook.close();
        } catch (Exception e) {
            if(debugMode) e.printStackTrace();
            return e.toString();
        }
        return null;
    }

    private void createOverviewSheet(List<NetAssets> companyDomainInfoList) {
        XSSFSheet sheet = workbook.createSheet("资产概览");
//        sheet.setDefaultColumnWidth(15);

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"公司/个人名称", "域名数量", "IP数量", "端口数量", "域名列表"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.get("header"));
        }

        // 填充数据
        int rowNum = 1;
        for (NetAssets company : companyDomainInfoList) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(company.getCompanyName());

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

            row.createCell(1).setCellValue(domains.size());
            row.createCell(2).setCellValue(uniqueIps.size());
            row.createCell(3).setCellValue(uniquePorts.size());
            row.createCell(4).setCellValue(String.join("\n", domains));

            // 应用样式
            for (int i = 0; i < 5; i++) {
                row.getCell(i).setCellStyle(styles.get("cell"));
            }
        }

        // 自动调整列宽
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createCompanyDetailSheet(NetAssets company) {
        XSSFSheet sheet = workbook.createSheet(company.getCompanyName());
//        sheet.setDefaultColumnWidth(20);

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"域名", "IP", "端口", "协议", "状态码", "标题", "ICP备案", "证书组织",
                "组件", "操作系统", "所属公司/个人", "国家", "城市"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.get("header"));
        }

        // 填充数据
        int rowNum = 1;
        for (DomainInfo domain : company.getDomainInfoList()) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(domain.getDomain());
            row.createCell(1).setCellValue(domain.getIp());
            row.createCell(2).setCellValue(domain.getPort());
            row.createCell(3).setCellValue(domain.getProtocol());
            row.createCell(4).setCellValue(domain.getStatusCode());
            row.createCell(5).setCellValue(domain.getTitle());
            row.createCell(6).setCellValue(domain.getIcp());
            row.createCell(7).setCellValue(domain.getCertsSubjectOrg());
            row.createCell(8).setCellValue(domain.getComponents() != null ?
                    String.join(", ", domain.getComponents()) : "");
            row.createCell(9).setCellValue(domain.getOs());
            row.createCell(10).setCellValue(domain.getCompany());
            row.createCell(11).setCellValue(domain.getCountry());
            row.createCell(12).setCellValue(domain.getCity());

            // 应用样式
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.getCell(i);
                if (cell != null) {
                    cell.setCellStyle(styles.get("cell"));
                }
            }
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createSensitiveInfoSheet(List<NetAssets> companyDomainInfoList) {
        XSSFSheet sheet = workbook.createSheet("敏感信息汇总");
//        sheet.setDefaultColumnWidth(25);

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"公司/个人名称", "URL", "电话号码", "身份证号", "密码", "IP", "内网IP", "邮箱", "Key泄露"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
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
                            row.createCell(0).setCellValue(company.getCompanyName());
                            row.createCell(1).setCellValue(String.valueOf(sensitive.get("Url")));
                            row.createCell(2).setCellValue(String.valueOf(sensitive.get("PhoneNumber")));
                            row.createCell(3).setCellValue(String.valueOf(sensitive.get("IdCard")));
                            row.createCell(4).setCellValue(String.valueOf(sensitive.get("Password")));
                            row.createCell(5).setCellValue(String.valueOf(sensitive.get("Ip")));
                            row.createCell(6).setCellValue(String.valueOf(sensitive.get("InternalIp")));
                            row.createCell(7).setCellValue(String.valueOf(sensitive.get("Email")));
                            row.createCell(8).setCellValue(String.valueOf(sensitive.get("Key")));

                            // 应用样式
                            for (int i = 0; i < headers.length; i++) {
                                Cell cell = row.getCell(i);
                                if (cell != null) {
                                    cell.setCellStyle(styles.get("cell"));
                                }
                            }
                        }
                    }
                }
            }
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createLeakageInfoSheet(List<NetAssets> companyDomainInfoList) {
        XSSFSheet sheet = workbook.createSheet("信息泄露汇总");
//        sheet.setDefaultColumnWidth(30);

        // 创建Google泄露信息部分
        int currentRow = 0;
        Row titleRow = sheet.createRow(currentRow++);
        Cell titleCell = titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Google泄露信息");
        titleCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

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
                        row.createCell(0).setCellValue(company.getCompanyName());
                        row.createCell(1).setCellValue(leakages.get(i).getAsJsonObject().get("domain").getAsString());
                        row.createCell(2).setCellValue(leakages.get(i).getAsJsonObject().get("title").getAsString());
                        row.createCell(3).setCellValue(leakages.get(i).getAsJsonObject().get("url").getAsString());
                        row.createCell(4).setCellValue(leakages.get(i).getAsJsonObject().get("des").getAsString());
                        row.createCell(5).setCellValue(jsonUtils.jsonArrayToString(leakages.get(i).getAsJsonObject().getAsJsonArray("leakageArray"),"\n"));
                    }
                }
            }
        }

        // 添加间隔行
        currentRow++;

        // 创建Git仓库泄露信息部分
        Row gitTitle = sheet.createRow(currentRow++);
        Cell gitTitleCell = gitTitle.createCell(0);
        gitTitleCell.setCellValue("Git仓库泄露信息");
        gitTitleCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(new CellRangeAddress(currentRow-1, currentRow-1, 0, 3));

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
                        row.createCell(0).setCellValue(company.getCompanyName());
                        row.createCell(1).setCellValue(repos.get(i).getAsJsonObject().get("repoName").getAsString());
                        row.createCell(2).setCellValue(repos.get(i).getAsJsonObject().get("repoUrl").getAsString());
                        row.createCell(3).setCellValue(repos.get(i).getAsJsonObject().get("repoDes").getAsString());
                    }
                }
            }
        }

        // 自动调整列宽
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createHeaderRow(Row headerRow, String[] headers) {
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
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
        titleStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setBorderBottom(BorderStyle.THICK);
        titleStyle.setBottomBorderColor(IndexedColors.DARK_BLUE.getIndex());
        styles.put("title", titleStyle);

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
        cellStyle.setTopBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        cellStyle.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        cellStyle.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        cellStyle.setRightBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        cellStyle.setWrapText(true);
        styles.put("cell", cellStyle);

        return styles;
    }

}