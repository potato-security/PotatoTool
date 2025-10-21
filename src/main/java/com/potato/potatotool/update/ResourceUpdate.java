package com.potato.potatotool.update;

import com.potato.potatotool.update.manifest.Manifest;

/**
 * 资源更新信息
 * 
 * @author Potato
 * @date 2025/10/11
 */
public class ResourceUpdate {
    
    private final Manifest.ResourceItem resource;  // 资源项信息
    private final String localVersion;             // 本地版本
    private final String remoteVersion;            // 远程版本

    public ResourceUpdate(Manifest.ResourceItem resource, 
                         String localVersion, 
                         String remoteVersion) {
        this.resource = resource;
        this.localVersion = localVersion;
        this.remoteVersion = remoteVersion;
    }
    
    /**
     * 获取资源项信息
     */
    public Manifest.ResourceItem getResource() {
        return resource;
    }
    
    /**
     * 获取资源名称
     */
    public String getResourceName() {
        return resource.getName();
    }
    
    /**
     * 获取资源显示名称
     */
    public String getDisplayName() {
        return resource.getDisplayName();
    }
    
    /**
     * 获取本地版本
     */
    public String getLocalVersion() {
        return localVersion == null ? "未安装" : localVersion;
    }
    
    /**
     * 获取远程版本
     */
    public String getRemoteVersion() {
        return remoteVersion;
    }
    
    /**
     * 是否是新安装（本地不存在）
     */
    public boolean isNewInstall() {
        return localVersion == null || localVersion.isEmpty();
    }
    
    /**
     * 获取文件大小
     */
    public long getFileSize() {
        if (resource.getFiles() != null) {
            return resource.getFiles().getSize();
        }
        return 0;
    }
    
    /**
     * 获取描述信息
     */
    public String getDescription() {
        return resource.getDescription();
    }
    
    @Override
    public String toString() {
        return String.format("ResourceUpdate{name='%s', %s -> %s}",
                getResourceName(), getLocalVersion(), remoteVersion);
    }
}

