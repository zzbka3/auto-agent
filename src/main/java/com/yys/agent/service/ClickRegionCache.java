package com.yys.agent.service;

import com.yys.agent.nodes.Rectangle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 点击区域缓存管理器
 * 用于存储和读取每个场景的点击区域信息
 */
public class ClickRegionCache {

    private final String cacheFilePath;
    private final ObjectMapper objectMapper;
    private Map<String, SceneClickRegion> cache;

    public ClickRegionCache(String cacheFilePath) {
        this.cacheFilePath = cacheFilePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.cache = new HashMap<>();
        loadCache();
    }

    /**
     * 获取点击区域
     * @param sceneId 场景ID
     * @param intent 用户意图（如"战斗按钮"、"开始探索"）
     * @return 点击区域，如果没有记录则返回null
     */
    public Rectangle getClickRegion(String sceneId, String intent) {
        String key = buildKey(sceneId, intent);
        SceneClickRegion region = cache.get(key);
        if (region != null) {
            return region.toRectangle();
        }
        return null;
    }

    /**
     * 保存点击区域
     * @param sceneId 场景ID
     * @param intent 用户意图
     * @param region 点击区域
     * @param description 描述
     */
    public void saveClickRegion(String sceneId, String intent, Rectangle region, String description) {
        String key = buildKey(sceneId, intent);
        SceneClickRegion regionInfo = new SceneClickRegion();
        regionInfo.setSceneId(sceneId);
        regionInfo.setIntent(intent);
        regionInfo.setX(region.getX());
        regionInfo.setY(region.getY());
        regionInfo.setWidth(region.getWidth());
        regionInfo.setHeight(region.getHeight());
        regionInfo.setDescription(description);
        regionInfo.setCreateTime(System.currentTimeMillis());
        regionInfo.setUpdateTime(System.currentTimeMillis());

        cache.put(key, regionInfo);
        saveCache();
    }

    /**
     * 检查是否存在记录
     */
    public boolean hasRecord(String sceneId, String intent) {
        String key = buildKey(sceneId, intent);
        return cache.containsKey(key);
    }

    /**
     * 构建缓存key
     */
    private String buildKey(String sceneId, String intent) {
        return sceneId + "::" + intent;
    }

    /**
     * 加载缓存
     */
    @SuppressWarnings("unchecked")
    private void loadCache() {
        File file = new File(cacheFilePath);
        if (!file.exists()) {
            return;
        }

        try {
            Map<String, Object> data = objectMapper.readValue(file, Map.class);
            if (data != null && data.containsKey("regions")) {
                List<Map<String, Object>> regions = (List<Map<String, Object>>) data.get("regions");
                for (Map<String, Object> r : regions) {
                    SceneClickRegion region = objectMapper.convertValue(r, SceneClickRegion.class);
                    String key = buildKey(region.getSceneId(), region.getIntent());
                    cache.put(key, region);
                }
            }
        } catch (IOException e) {
            System.err.println("加载缓存失败: " + e.getMessage());
        }
    }

    /**
     * 保存缓存
     */
    private void saveCache() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("version", "1.0");
            data.put("updateTime", System.currentTimeMillis());
            data.put("regions", new ArrayList<>(cache.values()));

            File file = new File(cacheFilePath);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            objectMapper.writeValue(file, data);
        } catch (IOException e) {
            System.err.println("保存缓存失败: " + e.getMessage());
        }
    }

    /**
     * 清除所有缓存
     */
    public void clear() {
        cache.clear();
        saveCache();
    }

    /**
     * 清除某个场景的缓存
     */
    public void clearScene(String sceneId) {
        cache.entrySet().removeIf(entry -> entry.getKey().startsWith(sceneId + "::"));
        saveCache();
    }

    // ========== 内部类 ==========

    /**
     * 场景点击区域记录
     */
    public static class SceneClickRegion {
        private String sceneId;
        private String sceneName;
        private String intent;
        private int x;
        private int y;
        private int width;
        private int height;
        private String description;
        private long createTime;
        private long updateTime;

        public Rectangle toRectangle() {
            return new Rectangle(x, y, width, height);
        }

        // Getters and Setters
        public String getSceneId() { return sceneId; }
        public void setSceneId(String sceneId) { this.sceneId = sceneId; }
        public String getSceneName() { return sceneName; }
        public void setSceneName(String sceneName) { this.sceneName = sceneName; }
        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public long getCreateTime() { return createTime; }
        public void setCreateTime(long createTime) { this.createTime = createTime; }
        public long getUpdateTime() { return updateTime; }
        public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
    }
}