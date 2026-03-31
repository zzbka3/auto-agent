package com.yys.agent;

/**
 * 节点类型枚举
 */
public enum NodeType {
    SCREENSHOT,      // 截图
    IMAGE_ANALYSIS,  // 图像分析（根据图片判断要点击哪里）
    MOUSE_CLICK,     // 鼠标点击
    END              // 结束节点
}
