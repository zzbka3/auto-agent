package com.yys.agent;

import com.yys.agent.config.WorkflowConfig;
import com.yys.agent.config.WorkflowConfigLoader;
import com.yys.agent.context.WorkFlowContext;
import com.yys.agent.runtime.WorkflowEngine;

import java.io.InputStream;

/**
 * 测试入参出参记录 Demo
 */
public class TestInputsOutputsDemo {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("入参出参记录测试 Demo");
        System.out.println("========================================\n");

        try {
            // 加载工作流配置
            WorkflowConfigLoader loader = new WorkflowConfigLoader();
            InputStream is = TestInputsOutputsDemo.class.getClassLoader()
                .getResourceAsStream("workflows/test_inputs_outputs.yaml");

            if (is == null) {
                System.err.println("错误：找不到配置文件 workflows/test_inputs_outputs.yaml");
                return;
            }

            WorkflowConfig config = loader.loadFromStream(is);
            System.out.println("加载工作流: " + config.getName());
            System.out.println("描述: " + config.getDescription());

            // 创建工作流引擎
            WorkflowEngine engine = new WorkflowEngine();
            engine.loadWorkflow(config);

            // 执行工作流
            System.out.println("\n>>> 开始执行...\n");
            System.out.println("注意：观察每个节点的入参和出参信息\n");

            engine.execute();

            // 显示执行文件夹位置
            WorkFlowContext context = engine.getWorkflowContext();
            if (context.getExecutionDir() != null) {
                System.out.println("\n========================================");
                System.out.println("测试完成！");
                System.out.println("========================================");
                System.out.println("\n查看详细记录:");
                System.out.println("  - 执行日志: " + context.getLogFile().getAbsolutePath());
                System.out.println("  - 执行报告: " + context.getExecutionDir().resolve("execution_report.txt"));
                System.out.println("\n提示：查看执行报告可以查看每个节点的详细入参和出参");
            }

        } catch (Exception e) {
            System.err.println("\n执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
