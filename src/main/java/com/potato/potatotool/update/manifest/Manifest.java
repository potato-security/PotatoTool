package com.potato.potatotool.update.manifest;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

/**
 * 更新清单数据模型
 * @author Potato
 * @date 2025/10/11
 */
public class Manifest {
    
    @SerializedName("version")
    private String version;
    
    @SerializedName("lastUpdate")
    private String lastUpdate;
    
    @SerializedName("app")
    private AppVersion app;
    
    @SerializedName("resources")
    private List<ResourceItem> resources;

    private transient String sourceUrl;
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getLastUpdate() {
        return lastUpdate;
    }
    
    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
    
    public AppVersion getApp() {
        return app;
    }
    
    public void setApp(AppVersion app) {
        this.app = app;
    }
    
    public List<ResourceItem> getResources() {
        return resources;
    }
    
    public void setResources(List<ResourceItem> resources) {
        this.resources = resources;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
    
    /**
     * 软件版本信息
     */
    public static class AppVersion {
        @SerializedName("version")
        private String version;
        
        @SerializedName("releaseDate")
        private String releaseDate;
        
        @SerializedName("changelog")
        private List<String> changelog;
        
        @SerializedName("required")
        private boolean required;
        
        @SerializedName("files")
        private Map<String, FileInfo> files;  // key: platform (windows-jdk8, windows-jdk11, mac-jdk8, etc.)
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public String getReleaseDate() {
            return releaseDate;
        }
        
        public void setReleaseDate(String releaseDate) {
            this.releaseDate = releaseDate;
        }
        
        public List<String> getChangelog() {
            return changelog;
        }
        
        public void setChangelog(List<String> changelog) {
            this.changelog = changelog;
        }
        
        public boolean isRequired() {
            return required;
        }
        
        public void setRequired(boolean required) {
            this.required = required;
        }
        
        public Map<String, FileInfo> getFiles() {
            return files;
        }
        
        public void setFiles(Map<String, FileInfo> files) {
            this.files = files;
        }
    }
    
    /**
     * 资源项信息
     */
    public static class ResourceItem {
        @SerializedName("name")
        private String name;
        
        @SerializedName("displayName")
        private String displayName;
        
        @SerializedName("version")
        private String version;
        
        @SerializedName("required")
        private boolean required;
        
        @SerializedName("description")
        private String description;
        
        @SerializedName("files")
        private FileDetails files;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public boolean isRequired() {
            return required;
        }
        
        public void setRequired(boolean required) {
            this.required = required;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public FileDetails getFiles() {
            return files;
        }
        
        public void setFiles(FileDetails files) {
            this.files = files;
        }
    }
    
    /**
     * 文件详细信息（用于资源）
     */
    public static class FileDetails {
        @SerializedName("url")
        private UrlInfo url;
        
        @SerializedName("size")
        private long size;
        
        @SerializedName("compressed")
        private boolean compressed;
        
        @SerializedName("compressionType")
        private String compressionType;
        
        @SerializedName("uncompressedSize")
        private long uncompressedSize;
        
        @SerializedName("checksum")
        private Checksum checksum;
        
        @SerializedName("localPath")
        private String localPath;
        
        @SerializedName("minDiskSpace")
        private long minDiskSpace;
        
        public UrlInfo getUrl() {
            return url;
        }
        
        public void setUrl(UrlInfo url) {
            this.url = url;
        }
        
        public long getSize() {
            return size;
        }
        
        public void setSize(long size) {
            this.size = size;
        }
        
        public boolean isCompressed() {
            return compressed;
        }
        
        public void setCompressed(boolean compressed) {
            this.compressed = compressed;
        }
        
        public String getCompressionType() {
            return compressionType;
        }
        
        public void setCompressionType(String compressionType) {
            this.compressionType = compressionType;
        }
        
        public long getUncompressedSize() {
            return uncompressedSize;
        }
        
        public void setUncompressedSize(long uncompressedSize) {
            this.uncompressedSize = uncompressedSize;
        }
        
        public Checksum getChecksum() {
            return checksum;
        }
        
        public void setChecksum(Checksum checksum) {
            this.checksum = checksum;
        }
        
        public String getLocalPath() {
            return localPath;
        }
        
        public void setLocalPath(String localPath) {
            this.localPath = localPath;
        }
        
        public long getMinDiskSpace() {
            return minDiskSpace;
        }
        
        public void setMinDiskSpace(long minDiskSpace) {
            this.minDiskSpace = minDiskSpace;
        }
    }
    
    /**
     * 文件信息（用于软件）
     */
    public static class FileInfo {
        @SerializedName("url")
        private UrlInfo url;
        
        @SerializedName("size")
        private long size;
        
        @SerializedName("checksum")
        private Checksum checksum;
        
        public UrlInfo getUrl() {
            return url;
        }
        
        public void setUrl(UrlInfo url) {
            this.url = url;
        }
        
        public long getSize() {
            return size;
        }
        
        public void setSize(long size) {
            this.size = size;
        }
        
        public Checksum getChecksum() {
            return checksum;
        }
        
        public void setChecksum(Checksum checksum) {
            this.checksum = checksum;
        }
    }
    
    /**
     * URL信息（多源）
     */
    public static class UrlInfo {
        @SerializedName("github")
        private String github;
        
        @SerializedName("mirror")
        private String mirror;
        
        public String getGithub() {
            return github;
        }
        
        public void setGithub(String github) {
            this.github = github;
        }
        
        public String getMirror() {
            return mirror;
        }
        
        public void setMirror(String mirror) {
            this.mirror = mirror;
        }
        
        /**
         * 获取所有可用的URL
         */
        public List<String> getAllUrls() {
            List<String> urls = new java.util.ArrayList<>();
            if (github != null && !github.isEmpty()) {
                urls.add(github);
            }
            if (mirror != null && !mirror.isEmpty()) {
                urls.add(mirror);
            }
            return urls;
        }
    }
    
    /**
     * 校验和信息
     */
    public static class Checksum {
        @SerializedName("sha256")
        private String sha256;
        
        @SerializedName("md5")
        private String md5;
        
        public String getSha256() {
            return sha256;
        }
        
        public void setSha256(String sha256) {
            this.sha256 = sha256;
        }
        
        public String getMd5() {
            return md5;
        }
        
        public void setMd5(String md5) {
            this.md5 = md5;
        }
    }
}
