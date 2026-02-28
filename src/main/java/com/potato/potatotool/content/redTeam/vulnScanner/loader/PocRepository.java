package com.potato.potatotool.content.redTeam.vulnScanner.loader;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.exception.PocLoadException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * POC仓库
 * 负责POC的存储、索引、搜索和管理
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class PocRepository {
    
    // POC存储（使用ID作为键）
    private final Map<String, PocObj.Poc> pocMap = new ConcurrentHashMap<>();
    
    // POC索引
    private final Map<String, Set<String>> tagIndex = new ConcurrentHashMap<>();
    private final Map<PocObj.Severity, Set<String>> severityIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> protocolIndex = new ConcurrentHashMap<>();
    
    // 新增索引
    private final Map<PocObj.PocCategory, Set<String>> categoryIndex = new ConcurrentHashMap<>();
    private final Map<PocObj.InputType, Set<String>> inputTypeIndex = new ConcurrentHashMap<>();
    
    // POC加载器
    private final PocLoader pocLoader;
    
    public PocRepository() {
        this.pocLoader = new PocLoader();
    }
    
    /**
     * 加载POC目录
     */
    public int loadDirectory(String directory) throws PocLoadException {
        List<PocObj.Poc> pocs = pocLoader.loadFromDirectory(directory);
        int count = 0;
        
        for (PocObj.Poc poc : pocs) {
            if (addPoc(poc)) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 添加POC到仓库
     */
    public boolean addPoc(PocObj.Poc poc) {
        if (poc == null || poc.getId() == null) {
            return false;
        }
        
        // 存储POC
        pocMap.put(poc.getId(), poc);
        
        // 更新索引
        updateIndexes(poc);
        
        return true;
    }
    
    /**
     * 更新索引
     */
    private void updateIndexes(PocObj.Poc poc) {
        String pocId = poc.getId();
        
        // 标签索引
        if (poc.getTags() != null) {
            for (String tag : poc.getTags()) {
                tagIndex.computeIfAbsent(tag.toLowerCase(), k -> ConcurrentHashMap.newKeySet())
                        .add(pocId);
            }
        }
        
        // 严重程度索引
        if (poc.getSeverity() != null) {
            severityIndex.computeIfAbsent(poc.getSeverity(), k -> ConcurrentHashMap.newKeySet())
                         .add(pocId);
        }
        
        // 协议索引
        if (poc.getProtocol() != null) {
            protocolIndex.computeIfAbsent(poc.getProtocol().toLowerCase(), k -> ConcurrentHashMap.newKeySet())
                         .add(pocId);
        }
        
        // 分类索引
        if (poc.getCategory() != null) {
            categoryIndex.computeIfAbsent(poc.getCategory(), k -> ConcurrentHashMap.newKeySet())
                         .add(pocId);
        }
        
        // 输入类型索引
        if (poc.getInputType() != null) {
            inputTypeIndex.computeIfAbsent(poc.getInputType(), k -> ConcurrentHashMap.newKeySet())
                           .add(pocId);
        }
    }
    
    /**
     * 根据ID获取POC
     */
    public PocObj.Poc getPoc(String id) {
        return pocMap.get(id);
    }
    
    /**
     * 获取所有POC
     */
    public List<PocObj.Poc> getAllPocs() {
        return new ArrayList<>(pocMap.values());
    }
    
    /**
     * 获取POC数量
     */
    public int getCount() {
        return pocMap.size();
    }
    
    /**
     * 根据标签搜索POC
     */
    public List<PocObj.Poc> findByTag(String tag) {
        Set<String> pocIds = tagIndex.get(tag.toLowerCase());
        if (pocIds == null) {
            return Collections.emptyList();
        }
        
        return pocIds.stream()
                     .map(pocMap::get)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
    }
    
    /**
     * 根据多个标签搜索POC（AND关系）
     */
    public List<PocObj.Poc> findByTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        
        Set<String> result = null;
        
        for (String tag : tags) {
            Set<String> pocIds = tagIndex.get(tag.toLowerCase());
            if (pocIds == null) {
                return Collections.emptyList();
            }
            
            if (result == null) {
                result = new HashSet<>(pocIds);
            } else {
                result.retainAll(pocIds);
            }
        }
        
        if (result == null) {
            return Collections.emptyList();
        }
        
        return result.stream()
                     .map(pocMap::get)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
    }
    
    /**
     * 根据严重程度搜索POC
     */
    public List<PocObj.Poc> findBySeverity(PocObj.Severity severity) {
        Set<String> pocIds = severityIndex.get(severity);
        if (pocIds == null) {
            return Collections.emptyList();
        }
        
        return pocIds.stream()
                     .map(pocMap::get)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
    }
    
    /**
     * 根据严重程度范围搜索POC
     */
    public List<PocObj.Poc> findBySeverityRange(PocObj.Severity minSeverity) {
        List<PocObj.Poc> result = new ArrayList<>();
        
        for (PocObj.Severity severity : PocObj.Severity.values()) {
            if (severity.ordinal() >= minSeverity.ordinal()) {
                List<PocObj.Poc> pocs = findBySeverity(severity);
                result.addAll(pocs);
            }
        }
        
        return result;
    }
    
    /**
     * 根据协议搜索POC
     */
    public List<PocObj.Poc> findByProtocol(String protocol) {
        Set<String> pocIds = protocolIndex.get(protocol.toLowerCase());
        if (pocIds == null) {
            return Collections.emptyList();
        }
        
        return pocIds.stream()
                     .map(pocMap::get)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
    }
    
    /**
     * 关键词搜索POC（搜索名称和描述）
     */
    public List<PocObj.Poc> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        
        return pocMap.values().stream()
                     .filter(poc -> matchesKeyword(poc, lowerKeyword))
                     .collect(Collectors.toList());
    }
    
    /**
     * 判断POC是否匹配关键词
     */
    private boolean matchesKeyword(PocObj.Poc poc, String keyword) {
        if (poc.getName() != null && poc.getName().toLowerCase().contains(keyword)) {
            return true;
        }
        if (poc.getDescription() != null && poc.getDescription().toLowerCase().contains(keyword)) {
            return true;
        }
        if (poc.getId() != null && poc.getId().toLowerCase().contains(keyword)) {
            return true;
        }
        return false;
    }
    
    /**
     * 清空仓库
     */
    public void clear() {
        pocMap.clear();
        tagIndex.clear();
        severityIndex.clear();
        protocolIndex.clear();
        categoryIndex.clear();
        inputTypeIndex.clear();
    }
    
    /**
     * 移除POC
     */
    public boolean removePoc(String id) {
        PocObj.Poc removed = pocMap.remove(id);
        if (removed != null) {
            removeFromIndexes(removed);
            return true;
        }
        return false;
    }
    
    /**
     * 从索引中移除POC
     */
    private void removeFromIndexes(PocObj.Poc poc) {
        String pocId = poc.getId();
        
        // 从标签索引移除
        if (poc.getTags() != null) {
            for (String tag : poc.getTags()) {
                Set<String> pocIds = tagIndex.get(tag.toLowerCase());
                if (pocIds != null) {
                    pocIds.remove(pocId);
                }
            }
        }
        
        // 从严重程度索引移除
        if (poc.getSeverity() != null) {
            Set<String> pocIds = severityIndex.get(poc.getSeverity());
            if (pocIds != null) {
                pocIds.remove(pocId);
            }
        }
        
        // 从协议索引移除
        if (poc.getProtocol() != null) {
            Set<String> pocIds = protocolIndex.get(poc.getProtocol().toLowerCase());
            if (pocIds != null) {
                pocIds.remove(pocId);
            }
        }
        
        // 从分类索引移除
        if (poc.getCategory() != null) {
            Set<String> pocIds = categoryIndex.get(poc.getCategory());
            if (pocIds != null) {
                pocIds.remove(pocId);
            }
        }
        
        // 从输入类型索引移除
        if (poc.getInputType() != null) {
            Set<String> pocIds = inputTypeIndex.get(poc.getInputType());
            if (pocIds != null) {
                pocIds.remove(pocId);
            }
        }
    }
    
    /**
     * 根据分类搜索POC
     */
    public List<PocObj.Poc> findByCategory(PocObj.PocCategory category) {
        Set<String> pocIds = categoryIndex.get(category);
        if (pocIds == null) {
            return Collections.emptyList();
        }
        
        return pocIds.stream()
                     .map(pocMap::get)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
    }
    
    /**
     * 根据输入类型搜索POC
     */
    public List<PocObj.Poc> findByInputType(PocObj.InputType inputType) {
        Set<String> pocIds = inputTypeIndex.get(inputType);
        if (pocIds == null) {
            return Collections.emptyList();
        }
        
        return pocIds.stream()
                     .map(pocMap::get)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
    }
    
    /**
     * 根据多个分类搜索POC（OR关系）
     */
    public List<PocObj.Poc> findByCategories(Set<PocObj.PocCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }
        
        Set<String> allPocIds = new HashSet<>();
        for (PocObj.PocCategory category : categories) {
            Set<String> pocIds = categoryIndex.get(category);
            if (pocIds != null) {
                allPocIds.addAll(pocIds);
            }
        }
        
        return allPocIds.stream()
                        .map(pocMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
    }
    
    /**
     * 排除特定分类的POC
     */
    public List<PocObj.Poc> findExcludingCategories(Set<PocObj.PocCategory> excludeCategories) {
        if (excludeCategories == null || excludeCategories.isEmpty()) {
            return getAllPocs();
        }
        
        return pocMap.values().stream()
                     .filter(poc -> !excludeCategories.contains(poc.getCategory()))
                     .collect(Collectors.toList());
    }
    
    /**
     * 根据规范化标签匹配 POC
     */
    public List<PocObj.Poc> findByNormalizedTags(Set<String> normalizedTags) {
        if (normalizedTags == null || normalizedTags.isEmpty()) {
            return Collections.emptyList();
        }
        
        return pocMap.values().stream()
                     .filter(poc -> {
                         Set<String> pocTags = poc.getNormalizedTags();
                         if (pocTags == null || pocTags.isEmpty()) {
                             return false;
                         }
                         // 检查是否有交集
                         for (String tag : pocTags) {
                             if (normalizedTags.contains(tag)) {
                                 return true;
                             }
                         }
                         return false;
                     })
                     .collect(Collectors.toList());
    }
    
    /**
     * 获取统计信息
     */
    public RepositoryStats getStats() {
        return new RepositoryStats(
            pocMap.size(),
            tagIndex.size(),
            severityIndex.size(),
            protocolIndex.size(),
            categoryIndex.size(),
            inputTypeIndex.size()
        );
    }
    
    /**
     * 仓库统计信息
     */
    public static class RepositoryStats {
        public final int totalPocs;
        public final int totalTags;
        public final int totalSeverities;
        public final int totalProtocols;
        public final int totalCategories;
        public final int totalInputTypes;
        
        public RepositoryStats(int totalPocs, int totalTags, int totalSeverities, int totalProtocols,
                               int totalCategories, int totalInputTypes) {
            this.totalPocs = totalPocs;
            this.totalTags = totalTags;
            this.totalSeverities = totalSeverities;
            this.totalProtocols = totalProtocols;
            this.totalCategories = totalCategories;
            this.totalInputTypes = totalInputTypes;
        }
        
        @Override
        public String toString() {
            return String.format("RepositoryStats[pocs=%d, tags=%d, severities=%d, protocols=%d, categories=%d, inputTypes=%d]",
                totalPocs, totalTags, totalSeverities, totalProtocols, totalCategories, totalInputTypes);
        }
    }
}

