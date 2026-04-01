package com.yys.agent;

/**
 * 节点类型枚举
 */
public enum NodeType {
    SCREENSHOT,      // 截图
    IMAGE_ANALYSIS,  // 图像分析（根据图片判断要点击哪里）
    IMAGE_MATCH,     // 图片匹配（根据截图判断当前场景）
    SELECT_CLICK_REGION, // 选择点击区域（智能判断是使用缓存还是调用大模型）
    MOUSE_CLICK,     // 鼠标点击
    END              // 结束节点
}
