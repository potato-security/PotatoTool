package me.gv7.woodpecker.plugin.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class WPSettings {
    private Map<String, Object> _settings;

    public WPSettings(Map<String, Object> settings) {
        this._settings = settings;
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String defaultValue) {
        if (_settings.containsKey(key)) {
            return _settings.get(key).toString();
        } else {
            return defaultValue;
        }
    }

    public int getInteger(String key) {
        return getInteger(key, null);
    }


    public boolean getBoolean(String key, boolean defaultValue) {
        if (_settings.containsKey(key)) {
            String obj = _settings.get(key).toString().toLowerCase();
            if (obj.equals("true") || obj.equals("1")) {
                return true;
            } else if (obj.equals("false") || obj.equals("0")) {
                return false;
            } else {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    public Integer getInteger(String key, Integer defaultValue) {
        if (_settings.containsKey(key)) {
            return Integer.parseInt(_settings.get(key).toString());
        } else {
            return defaultValue;
        }
    }

    public byte[] getFileContent(String key) {
        if (_settings.containsKey(key)) {
            try {
                return Files.readAllBytes(Paths.get(_settings.get(key).toString()));
            } catch (Exception ex) {
                return null;
            }
        } else {
            return null;
        }
    }
}
