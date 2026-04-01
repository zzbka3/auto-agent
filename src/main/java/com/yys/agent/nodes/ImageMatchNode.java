package com.yys.agent.nodes;

import com.yys.agent.*;
import com.yys.agent.config.NodeConfig;
import com.yys.agent.context.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 图片匹配节点
 * 根据当前截图与预定义的场景图片进行相似度匹配，判断当前处于哪个场景
 */
public class ImageMatchNode extends AbstractAgentNode {

    // 场景配置：场景ID -> 场景配置
    private Map<String, SceneConfig> scenes;

    // 默认匹配阈值
    private float defaultThreshold = 0.7f;

    public ImageMatchNode() {
        super("image_match", "图片匹配", "根据截图匹配当前场景", NodeType.IMAGE_MATCH);
        this.scenes = new HashMap<>();
    }

    public ImageMatchNode(String nodeId, String name) {
        super(nodeId, name, "根据截图匹配当前场景", NodeType.IMAGE_MATCH);
        this.scenes = new HashMap<>();
    }

    @Override
    public void initFromConfig(NodeConfig config) {
        super.initFromConfig(config);
        this.defaultThreshold = getFloatParam("default_threshold", 0.7f);
        initScenesFromConfig(config);
    }

    /**
     * 从配置初始化场景
     * 配置格式示例：
     * scenes:
     *   - scene_id: "battle_page"
     *     scene_name: "战斗页面"
     *     reference_image: "./images/battle.png"
     *     threshold: 0.8
     *   - scene_id: "main_menu"
     *     scene_name: "主菜单"
     *     reference_image: "./images/menu.png"
     *     threshold: 0.8
     */
    @SuppressWarnings("unchecked")
    private void initScenesFromConfig(NodeConfig config) {
        Object scenesObj = parameters.get("scenes");
        if (scenesObj instanceof List) {
            List<Map<String, Object>> scenesList = (List<Map<String, Object>>) scenesObj;
            for (Map<String, Object> sceneMap : scenesList) {
                SceneConfig scene = new SceneConfig();
                scene.setSceneId((String) sceneMap.get("scene_id"));
                scene.setSceneName((String) sceneMap.get("scene_name"));
                scene.setReferenceImage((String) sceneMap.get("reference_image"));
                Object threshold = sceneMap.get("threshold");
                if (threshold instanceof Number) {
                    scene.setThreshold(((Number) threshold).floatValue());
                } else {
                    scene.setThreshold(defaultThreshold);
                }
                scenes.put(scene.getSceneId(), scene);
            }
        }
    }

    @Override
    public NodeResult execute(NodeExecutionContext execContext) {
        clearOutputs();

        try {
            // 获取当前截图路径
            String screenshotPath = getCurrentScreenshot(execContext);
            if (screenshotPath == null) {
                return NodeResult.failure("未找到当前截图");
            }

            // 执行图片匹配
            MatchResult matchResult = matchImage(screenshotPath, execContext);

            // 设置输出
            setOutput("matched", matchResult.isMatched());
            setOutput("scene_id", matchResult.getMatchedSceneId());
            setOutput("scene_name", matchResult.getMatchedSceneName());
            setOutput("confidence", matchResult.getConfidence());

            // 保存到临时变量，供后续节点使用
            if (execContext != null) {
                execContext.setTemp("matched_scene_id", matchResult.getMatchedSceneId());
                execContext.setTemp("matched_scene_name", matchResult.getMatchedSceneName());
                execContext.setTemp("matched_confidence", matchResult.getConfidence());
                execContext.setTemp("image_matched", matchResult.isMatched());
            }

            if (matchResult.isMatched()) {
                return NodeResult.success("匹配到场景: " + matchResult.getMatchedSceneName() +
                        ", 置信度: " + String.format("%.2f", matchResult.getConfidence()));
            } else {
                return NodeResult.failure("未匹配到任何场景, 最高置信度: " +
                        String.format("%.2f", matchResult.getConfidence()));
            }

        } catch (Exception e) {
            return NodeResult.failure("图片匹配失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前截图路径
     */
    private String getCurrentScreenshot(NodeExecutionContext execContext) {
        if (execContext != null) {
            // 优先从临时变量获取
            String path = execContext.getTemp("current_screenshot", String.class);
            if (path != null) return path;

            // 从执行历史中查找最近的截图节点输出
            var history = execContext.getWorkflowContext().getExecutionHistory();
            for (int i = history.size() - 1; i >= 0; i--) {
                String nodeId = history.get(i).getNodeId();
                String output = execContext.getWorkflowContext().getNodeOutput(nodeId, "screenshot_path", String.class);
                if (output != null) return output;
            }
        }
        return null;
    }

    /**
     * 执行图片匹配
     */
    private MatchResult matchImage(String screenshotPath, NodeExecutionContext execContext) throws IOException {
        // 加载当前截图
        BufferedImage currentImage = ImageIO.read(new File(screenshotPath));
        if (currentImage == null) {
            throw new IOException("无法读取截图: " + screenshotPath);
        }

        MatchResult bestMatch = new MatchResult();
        bestMatch.setMatched(false);

        // 遍历所有场景进行匹配
        for (SceneConfig scene : scenes.values()) {
            String refImagePath = resolvePath(scene.getReferenceImage(), execContext);
            File refFile = new File(refImagePath);

            if (!refFile.exists()) {
                continue;
            }

            BufferedImage referenceImage = ImageIO.read(refFile);
            if (referenceImage == null) {
                continue;
            }

            // 计算相似度
            float similarity = calculateSimilarity(currentImage, referenceImage);

            // 记录日志
            if (execContext != null) {
                execContext.getWorkflowContext().log("场景[" + scene.getSceneName() + "] 相似度: " + String.format("%.2f", similarity));
            }

            // 检查是否匹配
            if (similarity >= scene.getThreshold() && similarity > bestMatch.getConfidence()) {
                bestMatch.setMatched(true);
                bestMatch.setMatchedSceneId(scene.getSceneId());
                bestMatch.setMatchedSceneName(scene.getSceneName());
                bestMatch.setConfidence(similarity);
                bestMatch.setReferenceImagePath(refImagePath);
            }
        }

        return bestMatch;
    }

    /**
     * 计算两张图片的相似度（简单实现：基于像素比较）
     */
    private float calculateSimilarity(BufferedImage img1, BufferedImage img2) {
        // 将图片缩放到相同大小进行比较
        int width = 100;
        int height = 100;

        BufferedImage scaled1 = scaleImage(img1, width, height);
        BufferedImage scaled2 = scaleImage(img2, width, height);

        // 转换为灰度并比较
        int[][] gray1 = toGrayMatrix(scaled1);
        int[][] gray2 = toGrayMatrix(scaled2);

        // 计算相似度
        double totalDiff = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                totalDiff += Math.abs(gray1[y][x] - gray2[y][x]);
            }
        }

        // 归一化到 0-1，差异越小相似度越高
        double maxDiff = 255 * width * height;
        double similarity = 1.0 - (totalDiff / maxDiff);

        return (float) similarity;
    }

    /**
     * 缩放图片
     */
    private BufferedImage scaleImage(BufferedImage image, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = scaled.createGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }

    /**
     * 转换为灰度矩阵
     */
    private int[][] toGrayMatrix(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] matrix = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                // 灰度转换公式
                matrix[y][x] = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            }
        }

        return matrix;
    }

    /**
     * 解析路径（支持引用）
     */
    private String resolvePath(String path, NodeExecutionContext execContext) {
        if (path != null && path.startsWith("${") && path.endsWith("}")) {
            String inner = path.substring(2, path.length() - 1);
            if (inner.startsWith("global.")) {
                String key = inner.substring(7);
                return execContext.getWorkflowContext().getGlobalInput(key, String.class);
            }
        }
        return path;
    }

    /**
     * 添加场景
     */
    public void addScene(SceneConfig scene) {
        scenes.put(scene.getSceneId(), scene);
    }

    /**
     * 获取场景
     */
    public SceneConfig getScene(String sceneId) {
        return scenes.get(sceneId);
    }

    public Map<String, SceneConfig> getScenes() {
        return scenes;
    }

    // ========== 内部类 ==========

    /**
     * 场景配置
     */
    public static class SceneConfig {
        private String sceneId;
        private String sceneName;
        private String referenceImage;
        private float threshold = 0.7f;

        public String getSceneId() { return sceneId; }
        public void setSceneId(String sceneId) { this.sceneId = sceneId; }
        public String getSceneName() { return sceneName; }
        public void setSceneName(String sceneName) { this.sceneName = sceneName; }
        public String getReferenceImage() { return referenceImage; }
        public void setReferenceImage(String referenceImage) { this.referenceImage = referenceImage; }
        public float getThreshold() { return threshold; }
        public void setThreshold(float threshold) { this.threshold = threshold; }
    }

    /**
     * 匹配结果
     */
    public static class MatchResult {
        private boolean matched;
        private String matchedSceneId;
        private String matchedSceneName;
        private float confidence;
        private String referenceImagePath;

        public boolean isMatched() { return matched; }
        public void setMatched(boolean matched) { this.matched = matched; }
        public String getMatchedSceneId() { return matchedSceneId; }
        public void setMatchedSceneId(String matchedSceneId) { this.matchedSceneId = matchedSceneId; }
        public String getMatchedSceneName() { return matchedSceneName; }
        public void setMatchedSceneName(String matchedSceneName) { this.matchedSceneName = matchedSceneName; }
        public float getConfidence() { return confidence; }
        public void setConfidence(float confidence) { this.confidence = confidence; }
        public String getReferenceImagePath() { return referenceImagePath; }
        public void setReferenceImagePath(String referenceImagePath) { this.referenceImagePath = referenceImagePath; }
    }
}