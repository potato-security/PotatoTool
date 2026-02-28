package com.potato.potatotool.content.redTeam.vulnScanner.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * POC 清单数据结构
 * 
 * 云端和本地都使用此格式管理 POC 文件列表
 * 
 * 清单格式示例：
 * {
 *   "version": "1.0.0",
 *   "lastUpdate": "2025-01-15T10:30:00Z",
 *   "directories": [
 *     {
 *       "path": "nucleiPoc/http/cves",
 *       "files": [
 
 *         {"name": "CVE-2024-0002.yaml", "version": "1.0.1", "md5": "def456...", "action": "update", "size": 2345}
 *       ]
 *     }
 *   ],
 *   "deleted": [
 *     {"id": "nucleiPoc/http/cves/old.yaml", "md5": "...", "version": "1.0.0"}
 *   ]
 * }
 * 
 * @author Potato
 * @date 2025/12/19
 */
@Data
public class PocManifest {
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private String version;
    
    @SerializedName("baseUrl")
    private String baseUrl;
    
    @SerializedName("lastUpdate")
    private String lastUpdate;
    
    private List<Directory> directories = new ArrayList<>();
    
    private List<DeletedEntry> deleted = new ArrayList<>();
    
    /**
     * 目录结构
     */
    @Data
    public static class Directory {
        private String path;
        private List<FileEntry> files = new ArrayList<>();
    }
    
    /**
     * 文件条目
     */
    @Data
    public static class FileEntry {
        private String name;
        private String version;
        private String md5;
        private Action action = Action.ADD;
        private long size;
        
        public String getFullPath(String dirPath) {
            if (dirPath == null || dirPath.isEmpty() || ".".equals(dirPath)) {
                return name;
            }
            return dirPath + "/" + name;
        }
    }
    
    /**
     * 操作类型
     */
    public enum Action {
        @SerializedName("add")
        ADD,
        
        @SerializedName("update")
        UPDATE,
        
        @SerializedName("delete")
        DELETE,
        
        @SerializedName("keep")
        KEEP
    }
    
    /**
     * 删除条目
     */
    @Data
    public static class DeletedEntry {
        private String id;        // POC 完整路径标识
        private String path;      // 目录路径
        private String name;      // 文件名
        private String version;   // 版本号
        private String md5;       // 校验和
        private Long size;        // 文件大小
        
        /**
         * 获取有效的 ID
         */
        public String getEffectiveId() {
            if (id != null && !id.isEmpty()) {
                return id;
            }
            if (path != null && name != null) {
                if (path.isEmpty() || ".".equals(path)) {
                    return name;
                }
                return path + "/" + name;
            }
            return null;
        }
    }
    
    /**
     * 获取所有文件条目（扁平化）
     */
    public List<FileEntryWithPath> getAllFiles() {
        List<FileEntryWithPath> allFiles = new ArrayList<>();
        for (Directory dir : directories) {
            for (FileEntry file : dir.getFiles()) {
                allFiles.add(new FileEntryWithPath(dir.getPath(), file));
            }
        }
        return allFiles;
    }
    
    /**
     * 获取所有文件的 Map（path -> FileEntry）
     */
    public Map<String, FileEntryWithPath> getFileMap() {
        Map<String, FileEntryWithPath> map = new HashMap<>();
        for (Directory dir : directories) {
            for (FileEntry file : dir.getFiles()) {
                String fullPath = file.getFullPath(dir.getPath());
                map.put(fullPath, new FileEntryWithPath(dir.getPath(), file));
            }
        }
        return map;
    }
    
    /**
     * 带路径的文件条目
     */
    @Data
    public static class FileEntryWithPath {
        private final String dirPath;
        private final FileEntry file;
        
        public FileEntryWithPath(String dirPath, FileEntry file) {
            this.dirPath = dirPath;
            this.file = file;
        }
        
        public String getFullPath() {
            return file.getFullPath(dirPath);
        }
        
        public String getDownloadUrl(String baseUrl) {
            String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            return base + getFullPath();
        }
    }
    
    /**
     * 从 JSON 解析
     */
    public static PocManifest fromJson(String json) {
        return gson.fromJson(json, PocManifest.class);
    }
    
    /**
     * 从 Reader 解析
     */
    public static PocManifest fromReader(Reader reader) {
        return gson.fromJson(reader, PocManifest.class);
    }
    
    /**
     * 转换为 JSON
     */
    public String toJson() {
        return gson.toJson(this);
    }
    
    /**
     * 计算与远程清单的差异
     * 
     * 注意：toDelete 只根据云端 deleted 字段判断，不是"本地有云端无"
     * 
     * @param remote 远程清单
     * @return 差异结果
     */
    public DiffResult diff(PocManifest remote) {
        DiffResult result = new DiffResult();
        
        Map<String, FileEntryWithPath> localMap = this.getFileMap();
        Map<String, FileEntryWithPath> remoteMap = remote.getFileMap();
        
        // 找出需要新增的（远程有，本地没有）
        for (Map.Entry<String, FileEntryWithPath> entry : remoteMap.entrySet()) {
            String path = entry.getKey();
            FileEntryWithPath remoteFile = entry.getValue();
            
            if (!localMap.containsKey(path)) {
                result.getToAdd().add(remoteFile);
            } else {
                // 检查是否需要更新（md5 不同）
                FileEntryWithPath localFile = localMap.get(path);
                if (!isSameMd5(localFile, remoteFile)) {
                    result.getToUpdate().add(remoteFile);
                }
            }
        }
        
        // 删除只根据云端 deleted 字段判断
        if (remote.getDeleted() != null) {
            for (DeletedEntry entry : remote.getDeleted()) {
                String effectiveId = entry.getEffectiveId();
                if (effectiveId != null) {
                    result.getDeletedEntries().add(entry);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 基于 md5 判断文件是否相同
     * 云端清单必定有 md5，直接比较即可
     */
    private boolean isSameMd5(FileEntryWithPath local, FileEntryWithPath remote) {
        String localMd5 = local.getFile().getMd5();
        String remoteMd5 = remote.getFile().getMd5();
        
        if (remoteMd5 != null && localMd5 != null) {
            return remoteMd5.equals(localMd5);
        }
        
        // 无法判断，认为需要更新
        return false;
    }
    
    /**
     * 差异结果
     */
    @Data
    public static class DiffResult {
        private List<FileEntryWithPath> toAdd = new ArrayList<>();
        private List<FileEntryWithPath> toUpdate = new ArrayList<>();
        private List<DeletedEntry> deletedEntries = new ArrayList<>();  // 云端显式删除的条目
        
        /**
         * 获取所有需要删除的 POC ID（只根据云端 deleted 字段）
         */
        public List<String> getAllDeleteIds() {
            List<String> ids = new ArrayList<>();
            for (DeletedEntry entry : deletedEntries) {
                String id = entry.getEffectiveId();
                if (id != null && !ids.contains(id)) {
                    ids.add(id);
                }
            }
            return ids;
        }
        
        public boolean hasChanges() {
            return !toAdd.isEmpty() || !toUpdate.isEmpty() || !deletedEntries.isEmpty();
        }
        
        public int getTotalChanges() {
            return toAdd.size() + toUpdate.size() + deletedEntries.size();
        }
        
        @Override
        public String toString() {
            return String.format("DiffResult{add=%d, update=%d, deletedEntries=%d}",
                    toAdd.size(), toUpdate.size(), deletedEntries.size());
        }
    }
}
