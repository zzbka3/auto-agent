package com.yys.agent.service;

import com.yys.agent.nodes.Rectangle;

/**
 * 测试大模型图像分析服务
 */
public class LlmImageAnalysisServiceTest {

    public static void main(String[] args) {
        System.out.println("=== 大模型图像分析服务测试 ===\n");

        // 测试1: 简单文本对话测试（不依赖图像）
        System.out.println("【测试1】简单文本对话测试");
        testSimpleChat();

        // 测试2: 分析图像
        System.out.println("\n【测试2】图像分析测试");
//        testAnalyzeImage();

        System.out.println("\n=== 测试完成 ===");
    }

    /**
     * 简单测试 API 调用（不依赖图像）
     */
    static void testSimpleChat() {
        try {
            LlmImageAnalysisService service = new LlmImageAnalysisService();
            System.out.println("发送测试请求...");

            // 直接调用内部方法测试
            String response = service.testApiCall("你好，你是谁");

            System.out.println("收到响应: " + response);

            if (response != null && response.length() > 0) {
                System.out.println("API 调用成功!");
            } else {
                System.out.println("API 返回为空");
            }
        } catch (Exception e) {
            System.err.println("API 调用失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试图像分析功能
     */
    static void testAnalyzeImage() {
        // 查找最近的截图
        String screenshotDir = "./screenshots";
        java.io.File dir = new java.io.File(screenshotDir);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("截图目录不存在: " + screenshotDir);
            return;
        }

        // 获取最新的截图文件
        java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
        if (files == null || files.length == 0) {
            System.out.println("未找到截图文件");
            return;
        }

        // 按修改时间排序，取最新的
        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        String imagePath = files[0].getAbsolutePath();

        System.out.println("使用截图: " + imagePath);

        // 测试多个目标
        String[] targets = {"探索入口", "按钮", "文本"};

        for (String target : targets) {
            System.out.println("\n--- 查找目标: " + target + " ---");
            try {
                ImageAnalysisService service = new LlmImageAnalysisService();
                AnalysisResult result = service.analyzeImage(imagePath, target);

                if (result.isFound()) {
                    Rectangle region = result.getRegion();
                    System.out.println("找到目标!");
                    System.out.println("  区域: " + region);
                    System.out.println("  描述: " + result.getDescription());
                    System.out.println("  置信度: " + result.getConfidence());
                } else {
                    System.out.println("未找到目标");
                }
            } catch (Exception e) {
                System.err.println("分析失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 测试图像评估功能
     */
    static void testEvaluateImage() {
        String screenshotDir = "./screenshots";
        java.io.File dir = new java.io.File(screenshotDir);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("截图目录不存在: " + screenshotDir);
            return;
        }

        java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
        if (files == null || files.length == 0) {
            System.out.println("未找到截图文件");
            return;
        }

        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        String imagePath = files[0].getAbsolutePath();

        System.out.println("使用截图: " + imagePath);

        // 测试多个问题
        String[] questions = {
            "是否有红色的按钮",
            "画面是否清晰",
            "是否有战斗场景"
        };

        for (String question : questions) {
            System.out.println("\n--- 问题: " + question + " ---");
            try {
                ImageAnalysisService service = new LlmImageAnalysisService();
                boolean result = service.evaluateImage(imagePath, question);
                System.out.println("回答: " + (result ? "是" : "否"));
            } catch (Exception e) {
                System.err.println("评估失败: " + e.getMessage());
            }
        }
    }
}