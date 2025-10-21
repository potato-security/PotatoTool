package com.potato.potatotool.utils.core;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleLongProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 国际化管理器 - 支持动态切换语言（使用Property绑定机制）
 * @author Potato
 * @date 2025/10/10
 */
public class I18nManager {
    
    private static I18nManager instance;
    private ResourceBundle currentBundle;
    private final ObjectProperty<Locale> currentLocaleProperty = new SimpleObjectProperty<>();
    private final List<I18nUpdatable> listeners = new CopyOnWriteArrayList<>();
    
    // 用于强制刷新binding的触发器
    private final SimpleLongProperty refreshTrigger = new SimpleLongProperty(0);
    
    // 防止递归触发
    private boolean isUpdating = false;
    
    // 支持的语言
    public static final Locale LOCALE_ZH_CN = new Locale("zh", "CN");
    public static final Locale LOCALE_EN_US = new Locale("en", "US");
    
    private static final String BUNDLE_BASE_NAME = "i18n.messages";
    
    private I18nManager() {
        // 从配置文件读取语言设置
        String savedLanguage = loadLanguageFromConfig();
        Locale locale = parseLocale(savedLanguage);
        currentLocaleProperty.set(locale);
        loadResourceBundle();
        
        // 监听语言变化
        currentLocaleProperty.addListener((obs, oldLocale, newLocale) -> {
            if (newLocale != null && !newLocale.equals(oldLocale) && !isUpdating) {
                isUpdating = true;
                try {
                    loadResourceBundle();
                    saveLanguageToConfig(newLocale);
                    
                    // 强制刷新所有绑定
                    Platform.runLater(() -> {
                        refreshTrigger.set(refreshTrigger.get() + 1);
                        notifyListeners();
                        isUpdating = false;
                    });
                } catch (Exception e) {
                    isUpdating = false;
                    e.printStackTrace();
                }
            }
        });
    }
    
    public static I18nManager getInstance() {
        if (instance == null) {
            synchronized (I18nManager.class) {
                if (instance == null) {
                    instance = new I18nManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 从配置文件加载语言设置
     */
    private String loadLanguageFromConfig() {
        try {
            // 清空缓存，确保读取最新配置
            Constants.cachedConfig = null;
            
            // 获取整个配置对象
            JsonObject config = (JsonObject) Constants.getOutsideConfig(null);
            
            if (config != null && config.has(ConfigConstants.LANGUAGE)) {
                return config.get(ConfigConstants.LANGUAGE).getAsString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "zh_CN"; // 默认中文
    }
    
    /**
     * 解析语言字符串为 Locale
     */
    private Locale parseLocale(String language) {
        if (language == null) {
            return LOCALE_ZH_CN;
        }
        
        switch (language) {
            case "en_US":
                return LOCALE_EN_US;
            case "zh_CN":
            default:
                return LOCALE_ZH_CN;
        }
    }
    
    /**
     * 加载资源文件（使用UTF-8编码）
     */
    private void loadResourceBundle() {
        try {
            currentBundle = ResourceBundle.getBundle(
                BUNDLE_BASE_NAME, 
                currentLocaleProperty.get(),
                new UTF8Control()
            );
        } catch (Exception e) {
            System.err.println("加载资源文件失败: " + e.getMessage());
            // 回退到默认语言
            try {
                currentBundle = ResourceBundle.getBundle(
                    BUNDLE_BASE_NAME, 
                    LOCALE_ZH_CN,
                    new UTF8Control()
                );
            } catch (Exception ex) {
                System.err.println("回退到默认语言失败: " + ex.getMessage());
            }
        }
    }
    
    /**
     * 自定义ResourceBundle.Control以支持UTF-8编码
     */
    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            // 只处理properties文件
            if (!"java.properties".equals(format)) {
                return super.newBundle(baseName, locale, format, loader, reload);
            }
            
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            
            InputStream stream = null;
            if (reload) {
                java.net.URL url = loader.getResource(resourceName);
                if (url != null) {
                    java.net.URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            
            if (stream != null) {
                try {
                    // 使用UTF-8编码读取properties文件
                    return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                } finally {
                    stream.close();
                }
            }
            
            return null;
        }
    }
    
    /**
     * 切换语言
     * @param locale 新的语言环境
     */
    public void switchLanguage(Locale locale) {
        if (locale != null && !locale.equals(currentLocaleProperty.get())) {
            currentLocaleProperty.set(locale);
        }
    }
    
    /**
     * 切换语言（通过语言代码字符串）
     * @param languageCode 语言代码，如 "zh_CN" 或 "en_US"
     */
    public void switchLanguage(String languageCode) {
        Locale locale = parseLocale(languageCode);
        switchLanguage(locale);
    }
    
    /**
     * 保存语言设置到配置文件
     */
    private void saveLanguageToConfig(Locale locale) {
        String languageCode = locale.getLanguage() + "_" + locale.getCountry();
        Constants.saveConfig(ConfigConstants.LANGUAGE, languageCode);
        Constants.cachedConfig = null;
    }
    
    /**
     * 获取国际化文本
     * @param key 资源键
     * @return 对应的文本，如果找不到则返回键本身
     */
    public String getString(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        try {
            return currentBundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }
    
    /**
     * 获取格式化的国际化文本
     * @param key 资源键
     * @param args 格式化参数
     * @return 格式化后的文本
     */
    public String getString(String key, Object... args) {
        try {
            String pattern = currentBundle.getString(key);
            return MessageFormat.format(pattern, args);
        } catch (MissingResourceException e) {
            return key;
        }
    }
    
    /**
     * 创建一个绑定到资源键的StringBinding
     * 当语言切换时，绑定会自动更新
     * @param key 资源键
     * @return StringBinding对象
     */
    public StringBinding createBinding(String key) {
        return new StringBinding() {
            {
                // 同时绑定到locale和刷新触发器，确保立即更新
                bind(currentLocaleProperty);
                bind(refreshTrigger);
            }
            
            @Override
            protected String computeValue() {
                return getString(key);
            }
        };
    }
    
    /**
     * 创建一个带格式化参数的StringBinding
     * @param key 资源键
     * @param args 格式化参数
     * @return StringBinding对象
     */
    public StringBinding createBinding(String key, Object... args) {
        return new StringBinding() {
            {
                // 同时绑定到locale和刷新触发器，确保立即更新
                bind(currentLocaleProperty);
                bind(refreshTrigger);
            }
            
            @Override
            protected String computeValue() {
                return getString(key, args);
            }
        };
    }
    
    /**
     * 获取当前语言环境
     */
    public Locale getCurrentLocale() {
        return currentLocaleProperty.get();
    }
    
    /**
     * 获取语言属性（用于绑定）
     */
    public ObjectProperty<Locale> localeProperty() {
        return currentLocaleProperty;
    }
    
    /**
     * 获取当前语言代码
     */
    public String getCurrentLanguageCode() {
        Locale locale = currentLocaleProperty.get();
        return locale.getLanguage() + "_" + locale.getCountry();
    }
    
    /**
     * 注册语言切换监听器
     * @param listener 实现了 I18nUpdatable 接口的对象
     */
    public void registerListener(I18nUpdatable listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * 注销监听器
     * @param listener 要注销的监听器
     */
    public void unregisterListener(I18nUpdatable listener) {
        listeners.remove(listener);
    }
    
    /**
     * 通知所有监听器更新UI
     */
    private void notifyListeners() {
        Platform.runLater(() -> {
            for (I18nUpdatable listener : listeners) {
                try {
                    listener.updateLanguage();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
     * 判断当前是否为中文环境
     */
    public boolean isChineseLocale() {
        return LOCALE_ZH_CN.equals(currentLocaleProperty.get());
    }
    
    /**
     * 判断当前是否为英文环境
     */
    public boolean isEnglishLocale() {
        return LOCALE_EN_US.equals(currentLocaleProperty.get());
    }
}
