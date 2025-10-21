package com.potato.potatotool.update;

import com.potato.potatotool.update.manifest.Manifest;

import java.util.List;

/**
 * 更新信息 - 包含检查更新的结果
 * 
 * @author Potato
 * @date 2025/10/11
 */
public class UpdateInfo {
    
    private final boolean appNeedUpdate;      // 软件是否需要更新
    private final Manifest.AppVersion appVersion;  // 远程软件版本信息
    private final String localAppVersion;     // 本地软件版本
    private final List<ResourceUpdate> resourceUpdates;  // 需要更新的资源列表
    private final Manifest manifest;          // 完整清单
    
    public UpdateInfo(boolean appNeedUpdate,
                     Manifest.AppVersion appVersion,
                     String localAppVersion,
                     List<ResourceUpdate> resourceUpdates,
                     Manifest manifest) {
        this.appNeedUpdate = appNeedUpdate;
        this.appVersion = appVersion;
        this.localAppVersion = localAppVersion;
        this.resourceUpdates = resourceUpdates;
        this.manifest = manifest;
    }
    
    /**
     * 是否有任何更新
     */
    public boolean hasAnyUpdate() {
        return appNeedUpdate || !resourceUpdates.isEmpty();
    }
    
    /**
     * 软件是否需要更新
     */
    public boolean isAppNeedUpdate() {
        return appNeedUpdate;
    }
    
    /**
     * 获取远程软件版本信息
     */
    public Manifest.AppVersion getAppVersion() {
        return appVersion;
    }
    
    /**
     * 获取本地软件版本
     */
    public String getLocalAppVersion() {
        return localAppVersion;
    }
    
    /**
     * 获取需要更新的资源列表
     */
    public List<ResourceUpdate> getResourceUpdates() {
        return resourceUpdates;
    }
    
    /**
     * 是否有资源需要更新
     */
    public boolean hasResourceUpdates() {
        return !resourceUpdates.isEmpty();
    }
    
    /**
     * 获取完整清单
     */
    public Manifest getManifest() {
        return manifest;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UpdateInfo{\n");
        sb.append("  软件更新: ").append(appNeedUpdate);
        if (appNeedUpdate && appVersion != null) {
            sb.append(" (").append(localAppVersion).append(" -> ")
              .append(appVersion.getVersion()).append(")");
        }
        sb.append("\n");
        sb.append("  资源更新: ").append(resourceUpdates.size()).append("个\n");
        for (ResourceUpdate ru : resourceUpdates) {
            sb.append("    - ").append(ru.getResourceName())
              .append(" (").append(ru.getLocalVersion())
              .append(" -> ").append(ru.getRemoteVersion())
              .append(")\n");
        }
        sb.append("}");
        return sb.toString();
    }
}

