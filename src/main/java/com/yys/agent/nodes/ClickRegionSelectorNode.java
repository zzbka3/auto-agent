package com.yys.agent.nodes;

import com.yys.agent.*;
import com.yys.agent.config.NodeConfig;
import com.yys.agent.context.*;
import com.yys.agent.service.ClickRegionCache;
import com.yys.agent.service.ImageAnalysisService;
import com.yys.agent.service.AnalysisResult;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 选择点击区域节点
 * 根据场景和用户意图判断是使用缓存还是调用大模型
 */
public class ClickRegionSelectorNode extends AbstractAgentNode {

    // 点击区域缓存
    private ClickRegionCache cache;

    // 图像分析服务（用于大模型分析）
    private ImageAnalysisService analysisService;

    // 缓存文件路径
    private String cacheFilePath;

    // 用户意图
    private String intent;

    // 场景ID参数名（从上下文获取）
    private String sceneIdParamName = "matched_scene_id";

    public ClickRegionSelectorNode() {
        super("select_click_region", "选择点击区域", "智能选择点击区域", NodeType.SELECT_CLICK_REGION);
    }

    public ClickRegionSelectorNode(String nodeId, String name) {
        super(nodeId, name, "智能选择点击区域", NodeType.SELECT_CLICK_REGION);
    }

    @Override
    public void initFromConfig(NodeConfig config) {
        super.initFromConfig(config);

        // 解析缓存文件路径
        this.cacheFilePath = getStringParam("cache_file", "./data/click_regions.json");

        // 解析用户意图
        this.intent = getStringParam("intent");

        // 解析场景ID参数名
        String paramName = getStringParam("scene_id_param");
        if (paramName != null && !paramName.isEmpty()) {
            this.sceneIdParamName = paramName;
        }

        // 初始化缓存
        initCache();

        // 初始化分析服务
        initAnalysisService();
    }

    /**
     * 初始化缓存
     */
    private void initCache() {
        // 解析缓存路径（支持引用）
        String resolvedPath = resolveStringValue(cacheFilePath);
        if (resolvedPath != null && execContext != null) {
            // 如果是相对路径，放在执行目录下
            if (!resolvedPath.startsWith("/") && !resolvedPath.matches("^[A-Z]:")) {
                Path baseDir = execContext.getExecutionDir();
                if (baseDir != null) {
                    resolvedPath = baseDir.resolve(resolvedPath).toString();
                }
            }
        }
        this.cache = new ClickRegionCache(resolvedPath);
    }

    /**
     * 初始化分析服务
     */
    private void initAnalysisService() {
        // 可以从配置获取服务类型，或者使用默认的
        // 这里暂时使用 Mock 服务，生产环境可以传入真实服务
        this.analysisService = new com.yys.agent.service.MockImageAnalysisService();
    }

    public void setAnalysisService(ImageAnalysisService service) {
        this.analysisService = service;
    }

    public void setCache(ClickRegionCache cache) {
        this.cache = cache;
    }

    @Override
    public NodeResult execute(NodeExecutionContext execContext) {
        clearOutputs();

        try {
            // 1. 获取当前场景ID
            String sceneId = getSceneId(execContext);
            if (sceneId == null || sceneId.isEmpty()) {
                return NodeResult.failure("未找到场景ID");
            }

            // 2. 获取用户意图
            String userIntent = getUserIntent(execContext);
            if (userIntent == null || userIntent.isEmpty()) {
                userIntent = this.intent;
            }
            if (userIntent == null || userIntent.isEmpty()) {
                return NodeResult.failure("未指定用户意图");
            }

            // 3. 检查缓存
            boolean fromCache = false;
            com.yys.agent.nodes.Rectangle region = null;

            if (cache != null && cache.hasRecord(sceneId, userIntent)) {
                region = cache.getClickRegion(sceneId, userIntent);
                fromCache = true;
                execContext.getWorkflowContext().log("使用缓存的点击区域: 场景=" + sceneId + ", 意图=" + userIntent);
            }

            // 4. 如果没有缓存，调用大模型
            if (region == null) {
                region = analyzeWithLlm(execContext, sceneId, userIntent);
                if (region != null) {
                    // 保存到缓存
                    if (cache != null) {
                        cache.saveClickRegion(sceneId, userIntent, region, "大模型识别");
                        execContext.getWorkflowContext().log("保存点击区域到缓存: 场景=" + sceneId + ", 意图=" + userIntent);
                    }
                    fromCache = false;
                }
            }

            // 5. 返回结果
            if (region != null) {
                setOutput("region", region);
                setOutput("x", region.getX());
                setOutput("y", region.getY());
                setOutput("width", region.getWidth());
                setOutput("height", region.getHeight());
                setOutput("scene_id", sceneId);
                setOutput("intent", userIntent);
                setOutput("from_cache", fromCache);

                // 保存到临时变量
                execContext.setTemp("target_region", region);
                execContext.setTemp("target_found", true);

                String source = fromCache ? "缓存" : "大模型";
                return NodeResult.success("确定点击区域: (" + region.getX() + ", " + region.getY() + ", " +
                        region.getWidth() + "x" + region.getHeight() + "), 来源: " + source,
                        java.util.Map.of("region", region, "from_cache", fromCache));
            } else {
                return NodeResult.failure("无法确定点击区域");
            }

        } catch (Exception e) {
            return NodeResult.failure("选择点击区域失败: " + e.getMessage());
        }
    }

    /**
     * 获取场景ID
     */
    private String getSceneId(NodeExecutionContext execContext) {
        if (execContext != null) {
            // 优先从临时变量获取
            String sceneId = execContext.getTemp(sceneIdParamName, String.class);
            if (sceneId != null) return sceneId;

            // 从上下文获取
            sceneId = execContext.getTemp("matched_scene_id", String.class);
            if (sceneId != null) return sceneId;

            // 从节点输出获取
            var outputs = execContext.getWorkflowContext().getAllNodeOutputs();
            for (var entry : outputs.entrySet()) {
                Object sid = entry.getValue().get("scene_id");
                if (sid != null) return sid.toString();
            }
        }
        return null;
    }

    /**
     * 获取用户意图
     */
    private String getUserIntent(NodeExecutionContext execContext) {
        // 先尝试从参数获取
        String intent = getStringParam("intent");
        if (intent != null && !intent.isEmpty()) {
            return intent;
        }

        // 从临时变量获取
        if (execContext != null) {
            intent = execContext.getTemp("user_intent", String.class);
            if (intent != null) return intent;
        }

        return null;
    }

    /**
     * 使用大模型分析点击区域
     */
    private com.yys.agent.nodes.Rectangle analyzeWithLlm(NodeExecutionContext execContext, String sceneId, String intent) {
        try {
            // 获取当前截图
            String screenshotPath = getCurrentScreenshot(execContext);
            if (screenshotPath == null) {
                execContext.getWorkflowContext().log("未找到当前截图");
                return null;
            }

            // 调用大模型分析
            String prompt = buildPrompt(sceneId, intent);
            if (analysisService == null) {
                execContext.getWorkflowContext().log("未配置图像分析服务");
                return null;
            }

            AnalysisResult result = analysisService.analyzeImage(screenshotPath, prompt);

            if (result != null && result.isFound() && result.getRegion() != null) {
                execContext.getWorkflowContext().log("大模型识别成功: " + result.getDescription());
                return result.getRegion();
            }

            execContext.getWorkflowContext().log("大模型未能识别到目标区域");
            return null;

        } catch (Exception e) {
            execContext.getWorkflowContext().log("大模型分析失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 构建分析提示
     */
    private String buildPrompt(String sceneId, String intent) {
        // 这里可以设计更复杂的prompt
        return "在当前" + sceneId + "场景下，用户想要" + intent + "，请找出需要点击的位置区域";
    }

    /**
     * 获取当前截图路径
     */
    private String getCurrentScreenshot(NodeExecutionContext execContext) {
        if (execContext != null) {
            String path = execContext.getTemp("current_screenshot", String.class);
            if (path != null) return path;

            var history = execContext.getWorkflowContext().getExecutionHistory();
            for (int i = history.size() - 1; i >= 0; i--) {
                String nodeId = history.get(i).getNodeId();
                String output = execContext.getWorkflowContext().getNodeOutput(nodeId, "screenshot_path", String.class);
                if (output != null) return output;
            }
        }
        return null;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public void setCacheFilePath(String cacheFilePath) {
        this.cacheFilePath = cacheFilePath;
    }
}