package com.potato.potatotool.content.redTeam.infoGathering;

import java.util.*;

/**
 * @author Potato
 * @date 2024/10/18 11:43
 */
public class AssetObj {
    private Set<String> domain = new HashSet<>();
    private Set<String> ipc = new HashSet<>();

    public Set<String> getDomain() {
        return domain;
    }

    public AssetObj setDomain(Set<String> domain) {
        this.domain = domain;
        return this;
    }
    public AssetObj addDomain(String domain) {
        this.domain.add(domain.replace("*.", ""));
        return this;
    }

    public Set<String> getIcp() {
        return ipc;
    }
    public AssetObj setIcp(Set<String> ipc) {
        this.ipc = ipc;
        return this;
    }
    public AssetObj addIcp(String ipc) {
        this.ipc.add(ipc.replaceAll("-.*", ""));
        return this;
    }

}
