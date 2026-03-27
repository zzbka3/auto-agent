package com.yys.agent.service;

import com.yys.agent.nodes.Rectangle;

/**
 * 图像分析服务接口
 */
public interface ImageAnalysisService {
    
    /**
     * 分析图像，返回要点击的区域
     * @param imagePath 图像路径
     * @param target 目标描述（如"探索入口"、"敌人"等）
     * @return 分析结果
     */
    AnalysisResult analyzeImage(String imagePath, String target);
    
    /**
     * 判断图像是否包含某个内容
     * @param imagePath 图像路径
     * @param question 判断问题（如"是否有胜利标志"）
     * @return 判断结果
     */
    boolean evaluateImage(String imagePath, String question);
}
