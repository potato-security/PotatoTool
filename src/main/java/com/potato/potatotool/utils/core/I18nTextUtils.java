package com.potato.potatotool.utils.core;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * 无 JavaFX 依赖的国际化读取工具，供纯逻辑链路和无头测试使用。
 */
public final class I18nTextUtils {

    private static final Locale LOCALE_ZH_CN = new Locale("zh", "CN");
    private static final Locale LOCALE_EN_US = new Locale("en", "US");
    private static final String BUNDLE_BASE_NAME = "i18n.messages";

    private I18nTextUtils() {
    }

    public static String getString(String key, Object... args) {
        String pattern = getPattern(key);
        if (args == null || args.length == 0) {
            return pattern;
        }
        return MessageFormat.format(pattern, args);
    }

    private static String getPattern(String key) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(
                    BUNDLE_BASE_NAME,
                    resolveLocale(),
                    new UTF8Control()
            );
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        } catch (Exception ignored) {
        }
        return key;
    }

    private static Locale resolveLocale() {
        try {
            JsonObject config = (JsonObject) Constants.getOutsideConfig(null);
            if (config != null && config.has(ConfigConstants.LANGUAGE)) {
                String language = config.get(ConfigConstants.LANGUAGE).getAsString();
                if ("en_US".equalsIgnoreCase(language)) {
                    return LOCALE_EN_US;
                }
            }
        } catch (Exception ignored) {
        }
        return LOCALE_ZH_CN;
    }

    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName,
                                        Locale locale,
                                        String format,
                                        ClassLoader loader,
                                        boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            if (!"java.properties".equals(format)) {
                return super.newBundle(baseName, locale, format, loader, reload);
            }

            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            InputStream stream;
            if (reload) {
                java.net.URL url = loader.getResource(resourceName);
                if (url == null) {
                    return null;
                }
                java.net.URLConnection connection = url.openConnection();
                if (connection == null) {
                    return null;
                }
                connection.setUseCaches(false);
                stream = connection.getInputStream();
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }

            if (stream == null) {
                return null;
            }

            try {
                return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
            } finally {
                stream.close();
            }
        }
    }
}
