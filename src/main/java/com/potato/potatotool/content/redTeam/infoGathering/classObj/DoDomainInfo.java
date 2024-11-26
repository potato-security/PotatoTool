package com.potato.potatotool.content.redTeam.infoGathering.classObj;

import com.google.gson.JsonArray;

/**
 * @author Potato
 * @date 2024/11/5 20:49
 */
public class DoDomainInfo {
    private String domain;
    private JsonArray googldLeakage;
    private JsonArray gitRepoLeakage;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public JsonArray getGoogldLeakage() {
        return googldLeakage;
    }

    public void setGoogldLeakage(JsonArray googldLeakage) {
        this.googldLeakage = googldLeakage;
    }

    public JsonArray getGitRepoLeakage() {
        return gitRepoLeakage;
    }

    public void setGitRepoLeakage(JsonArray gitRepoLeakage) {
        this.gitRepoLeakage = gitRepoLeakage;
    }
}
