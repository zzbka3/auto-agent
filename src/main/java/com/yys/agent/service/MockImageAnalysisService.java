package com.yys.agent.service;

import com.yys.agent.nodes.Rectangle;

/**
 * 模拟图像分析服务
 * 用于测试，实际使用时替换为 LangChain4j Vision 实现
 */
public class MockImageAnalysisService implements ImageAnalysisService {
    
    @Override
    public AnalysisResult analyzeImage(String imagePath, String target) {
        // 模拟返回结果
        // 实际使用时这里会调用 GPT-4V 或其他视觉模型
        
        System.out.println("=== 图像分析 ===");
        System.out.println("图像路径: " + imagePath);
        System.out.println("目标: " + target);
        
        // 模拟识别到一个区域（返回屏幕中心偏上的位置）
        Rectangle region = new Rectangle(500, 300, 200, 80);
        
        AnalysisResult result = new AnalysisResult(region, 0.85f);
        result.setDescription("识别到目标区域: " + target);
        
        System.out.println("识别结果: " + region);
        System.out.println("置信度: 0.85");
        
        return result;
    }
    
    @Override
    public boolean evaluateImage(String imagePath, String question) {
        System.out.println("=== 图像判断 ===");
        System.out.println("图像: " + imagePath);
        System.out.println("问题: " + question);
        
        // 模拟返回
        return true;
    }
}
