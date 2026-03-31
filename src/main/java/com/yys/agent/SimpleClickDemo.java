package com.yys.agent;

import com.yys.agent.config.WorkflowConfig;
import com.yys.agent.config.WorkflowConfigLoader;
import com.yys.agent.context.WorkFlowContext;
import com.yys.agent.runtime.WorkflowEngine;

import java.io.InputStream;

/**
 * 简单点击10次Demo
 * 在固定位置点击10次，每次间隔2秒
 */
public class SimpleClickDemo {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("简单点击10次 Demo");
        System.out.println("========================================\n");

        try {
            // 1. 加载工作流配置
            WorkflowConfigLoader loader = new WorkflowConfigLoader();
            InputStream is = SimpleClickDemo.class.getClassLoader()
                .getResourceAsStream("workflows/simple_click_10times.yaml");

            if (is == null) {
                System.err.println("错误：找不到配置文件 workflows/simple_click_10times.yaml");
                return;
            }

            WorkflowConfig config = loader.loadFromStream(is);
            System.out.println("加载工作流: " + config.getName());
            System.out.println("描述: " + config.getDescription());

            // 2. 创建工作流引擎
            WorkflowEngine engine = new WorkflowEngine();
            engine.loadWorkflow(config);

            // 3. 执行工作流
            System.out.println("\n>>> 开始执行...\n");
            System.out.println("提示：请确保点击位置 (1000, 500) 是您想要点击的目标位置");
            System.out.println("如需修改坐标，请编辑 src/main/resources/workflows/simple_click_10times.yaml 文件\n");

            long startTime = System.currentTimeMillis();

            engine.execute();

            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000;

            // 4. 输出执行结果
            System.out.println("\n========================================");
            System.out.println("执行完成！");
            System.out.println("总耗时: " + duration + " 秒");
            System.out.println("点击次数: 10 次");
            System.out.println("点击间隔: 2 秒");
            System.out.println("========================================");

            // 显示执行文件夹和日志位置
            WorkFlowContext context = engine.getWorkflowContext();
            if (context.getExecutionDir() != null) {
                System.out.println("\n执行文件夹: " + context.getExecutionDir());
                System.out.println("执行日志: " + context.getLogFile().getAbsolutePath());
                System.out.println("执行报告: " + context.getExecutionDir().resolve("execution_report.txt"));
            }

        } catch (Exception e) {
            System.err.println("\n执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
