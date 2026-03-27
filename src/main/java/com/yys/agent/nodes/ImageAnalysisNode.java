package com.yys.agent.nodes;

import com.yys.agent.*;
import com.yys.agent.config.NodeConfig;
import com.yys.agent.context.*;
import com.yys.agent.service.ImageAnalysisService;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 图像分析节点
 */
public class ImageAnalysisNode extends AbstractAgentNode {
    
    private ImageAnalysisService analysisService;
    private String target;
    private float confidence = 0.7f;
    
    public ImageAnalysisNode() {
        super("image_analysis", "图像分析", "分析图像识别目标区域", NodeType.IMAGE_ANALYSIS);
    }
    
    public ImageAnalysisNode(String nodeId, String name, String target) {
        super(nodeId, name, "分析图像识别目标区域", NodeType.IMAGE_ANALYSIS);
        this.target = target;
    }
    
    @Override
    public void initFromConfig(NodeConfig config) {
        super.initFromConfig(config);
        this.target = getStringParam("target");
        this.confidence = getFloatParam("confidence", 0.7f);
    }
    
    public void setAnalysisService(ImageAnalysisService service) {
        this.analysisService = service;
    }
    
    @Override
    public NodeResult execute(NodeExecutionContext execContext) {
        clearOutputs();
        
        try {
            String screenshotPath = getScreenshotPath(execContext);
            if (screenshotPath == null) {
                return NodeResult.failure("未找到截图");
            }
            
            if (analysisService == null) {
                return NodeResult.failure("未配置图像分析服务");
            }
            
            com.yys.agent.service.AnalysisResult result = analysisService.analyzeImage(screenshotPath, target);
            
            if (result == null || !result.isFound()) {
                setOutput("found", false);
                return NodeResult.failure("未找到目标: " + target);
            }
            
            setOutput("found", true);
            setOutput("region", result.getRegion());
            setOutput("confidence", result.getConfidence());
            setOutput("target", target);
            setOutput("description", result.getDescription());
            
            if (execContext != null) {
                execContext.setTemp("target_region", result.getRegion());
                execContext.setTemp("target_found", true);
                execContext.setTemp("last_target", target);
            }
            
            return NodeResult.success("识别到目标: " + target, 
                java.util.Map.of("region", result.getRegion(), "confidence", result.getConfidence()));
                
        } catch (Exception e) {
            return NodeResult.failure("图像分析失败: " + e.getMessage());
        }
    }
    
    private String getScreenshotPath(NodeExecutionContext execContext) {
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
    
    public void setTarget(String target) { this.target = target; }
}
