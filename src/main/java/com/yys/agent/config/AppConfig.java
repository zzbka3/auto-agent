package com.yys.agent.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 应用配置
 * 读取 application.properties 文件
 */
public class AppConfig {

    private static AppConfig instance;
    private final Properties properties;

    private AppConfig() {
        properties = new Properties();
        loadConfig();
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }

    private void loadConfig() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
                System.out.println("[AppConfig] 配置文件加载成功");
            } else {
                System.out.println("[AppConfig] 未找到配置文件，使用默认配置");
            }
        } catch (IOException e) {
            System.out.println("[AppConfig] 配置文件加载失败: " + e.getMessage());
        }
    }

    /**
     * 获取字符串配置
     */
    public String getString(String key, String defaultValue) {
        String value = properties.getProperty(key, defaultValue);
        // 支持环境变量替换 ${ENV_VAR:default}
        if (value != null && value.startsWith("${") && value.endsWith("}")) {
            String envPart = value.substring(2, value.length() - 1);
            String[] parts = envPart.split(":");
            String envVar = parts[0];
            String envDefault = parts.length > 1 ? parts[1] : "";
            String envValue = System.getenv(envVar);
            value = envValue != null ? envValue : envDefault;
        }
        return value;
    }

    /**
     * 获取 LLM API 类型
     */
    public String getLlmApiType() {
        return getString("llm.api.type", "qwen");
    }

    /**
     * 获取千问 API Key
     */
    public String getQwenApiKey() {
        return getString("llm.qwen.api-key", "");
    }

    /**
     * 获取千问模型
     */
    public String getQwenModel() {
        return getString("llm.qwen.model", "qwen-vl-plus");
    }

    /**
     * 获取 OpenAI API Key
     */
    public String getOpenaiApiKey() {
        return getString("llm.openai.api-key", "");
    }

    /**
     * 获取 OpenAI 模型
     */
    public String getOpenaiModel() {
        return getString("llm.openai.model", "gpt-4o");
    }
}