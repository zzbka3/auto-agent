package com.yys.agent;

import com.yys.agent.config.*;
import com.yys.agent.context.*;
import com.yys.agent.runtime.*;
import com.yys.agent.service.*;
import com.yys.agent.nodes.*;
import java.io.InputStream;
import java.util.*;

/**
 * 运行示例
 * 演示如何创建和执行自动探索工作流
 */
public class Demo {
    
    public static void main(String[] args) {
        // 示例1: 从YAML加载执行（推荐）
        try {
            runFromYaml();
        } catch (Exception e) {
            System.err.println("YAML加载失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 示例2: 编程方式运行（仅用于测试）
        // runProgrammatic();
    }
    
    /**
     * 编程方式运行
     */
    static void runProgrammatic() {
        System.out.println("========================================");
        System.out.println("阴阳师自动探索 Agent Demo");
        System.out.println("========================================\n");
        
        // 1. 创建工作流配置
        WorkflowConfig config = new WorkflowConfig();
        config.setName("阴阳师探索");
        config.setStartNodeId("screenshot");
        
        // 全局输入
        Map<String, Object> globalParams = new HashMap<>();
        globalParams.put("screenshot_dir", "./screenshots");
        globalParams.put("log_level", "INFO");
        config.setGlobal(globalParams);
        
        // 2. 创建节点配置
        List<NodeConfig> nodeConfigs = new ArrayList<>();
        
        // 节点1: 截图
        NodeConfig screenshotConfig = createNodeConfig("screenshot", "SCREENSHOT", "截图", 
            Map.of("save_dir", "./screenshots", "save_to_file", true), "analyze");
        
        // 节点2: 图像分析
        NodeConfig analyzeConfig = createNodeConfig("analyze", "IMAGE_ANALYSIS", "分析图像",
            Map.of("target", "探索入口", "confidence", 0.7), "click");
        
        // 节点3: 鼠标点击
        NodeConfig clickConfig = createNodeConfig("click", "MOUSE_CLICK", "点击",
            Map.of("click_count", 1), null);
        
        nodeConfigs.add(screenshotConfig);
        nodeConfigs.add(analyzeConfig);
        nodeConfigs.add(clickConfig);
        
        config.setNodes(nodeConfigs);
        
        // 3. 创建引擎
        WorkflowEngine engine = new WorkflowEngine();
        engine.loadWorkflow(config);
        
        // 4. 设置图像分析服务（模拟）
        ImageAnalysisNode analyzeNode = (ImageAnalysisNode) engine.getNodeRegistry().get("analyze");
        analyzeNode.setAnalysisService(new MockImageAnalysisService());
        
        // 5. 执行
        System.out.println("\n>>> 开始执行工作流...\n");
        
        try {
            engine.execute();
            
            // 6. 输出结果
            printResults(engine.getWorkflowContext());
            
        } catch (Exception e) {
            System.err.println("执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 从YAML文件加载运行
     */
    static void runFromYaml() throws Exception {
        System.out.println("========================================");
        System.out.println("从 YAML 文件加载工作流");
        System.out.println("========================================\n");
        
        // 加载配置
        WorkflowConfigLoader loader = new WorkflowConfigLoader();
        InputStream is = Demo.class.getClassLoader()
            .getResourceAsStream("workflows/simple_exploration.yaml");
        
        if (is == null) {
            System.out.println("YAML文件未找到，使用编程方式运行");
            runProgrammatic();
            return;
        }
        
        WorkflowConfig config = loader.loadFromStream(is);
        System.out.println("加载工作流: " + config.getName());
        
        // 创建引擎
        WorkflowEngine engine = new WorkflowEngine();
        engine.loadWorkflow(config);
        
        // 设置分析服务
        ImageAnalysisNode analyzeNode = (ImageAnalysisNode) engine.getNodeRegistry().get("analyze");
        analyzeNode.setAnalysisService(new MockImageAnalysisService());
        
        // 执行
        System.out.println("\n>>> 开始执行...\n");
        
        try {
            engine.execute();
            printResults(engine.getWorkflowContext());
        } catch (Exception e) {
            System.err.println("执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建节点配置辅助方法
     */
    static NodeConfig createNodeConfig(String id, String type, String name,
                                       Map<String, Object> params, String next) {
        NodeConfig config = new NodeConfig();
        config.setNodeId(id);
        config.setNodeType(type);
        config.setName(name);

        // 将参数转换为 ParamValue 格式
        Map<String, ParamValue> paramValues = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            paramValues.put(entry.getKey(), ParamValue.fromMap(entry.getValue()));
        }
        config.setParamValues(paramValues);

        config.setNextNodeId(next);
        return config;
    }
    
    /**
     * 打印执行结果
     */
    static void printResults(WorkFlowContext context) {
        System.out.println("\n========================================");
        System.out.println("执行结果");
        System.out.println("========================================");
        
        // 执行历史
        System.out.println("\n--- 执行历史 ---");
        for (WorkFlowContext.ExecutionRecord record : context.getExecutionHistory()) {
            String status = record.isSuccess() ? "[OK]" : "[FAIL]";
            System.out.println(status + " " + record.getNodeId() + ": " + record.getMessage());
        }
        
        // 节点输出
        System.out.println("\n--- 节点输出 ---");
        for (Map.Entry<String, Map<String, Object>> entry : context.getAllNodeOutputs().entrySet()) {
            System.out.println("节点 " + entry.getKey() + ":");
            for (Map.Entry<String, Object> output : entry.getValue().entrySet()) {
                System.out.println("  " + output.getKey() + " = " + output.getValue());
            }
        }
        
        // 临时变量
        System.out.println("\n--- 临时变量 ---");
        for (Map.Entry<String, Object> entry : context.getTempVariables().entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
        
        System.out.println("\n========================================");
        System.out.println("工作流执行完成!");
        System.out.println("========================================");
    }
}
