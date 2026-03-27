package com.yys.agent.config;

import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.util.*;

/**
 * 工作流配置加载器
 * 从YAML文件加载工作流配置
 */
public class WorkflowConfigLoader {
    
    private final Yaml yaml;
    
    public WorkflowConfigLoader() {
        // 不使用 Constructor，让 SnakeYAML 只解析为 Map，完全手动处理
        this.yaml = new Yaml();
    }
    
    /**
     * 从文件加载工作流配置
     */
    public WorkflowConfig loadFromFile(String filePath) throws IOException {
        try (InputStream inputStream = new FileInputStream(filePath)) {
            return loadFromStream(inputStream);
        }
    }
    
    /**
     * 从输入流加载工作流配置
     */
    public WorkflowConfig loadFromStream(InputStream inputStream) {
        // 自定义加载，处理嵌套的Map结构
        Map<String, Object> root = yaml.load(inputStream);
        return parseWorkflowConfig(root);
    }
    
    /**
     * 从字符串加载工作流配置
     */
    public WorkflowConfig loadFromString(String yamlContent) {
        Map<String, Object> root = yaml.load(yamlContent);
        return parseWorkflowConfig(root);
    }
    
    /**
     * 解析工作流配置
     */
    @SuppressWarnings("unchecked")
    private WorkflowConfig parseWorkflowConfig(Map<String, Object> root) {
        WorkflowConfig config = new WorkflowConfig();
        
        // 基本信息
        config.setName((String) root.get("name"));
        config.setDescription((String) root.get("description"));
        config.setVersion((String) root.get("version"));
        config.setStartNodeId((String) root.get("start_node"));
        
        // 全局参数
        Map<String, Object> globalMap = (Map<String, Object>) root.get("global");
        if (globalMap != null) {
            config.setGlobal(globalMap);
        }
        
        // 节点配置
        List<Map<String, Object>> nodesList = (List<Map<String, Object>>) root.get("nodes");
        if (nodesList != null) {
            List<NodeConfig> nodeConfigs = new ArrayList<>();
            for (Map<String, Object> nodeMap : nodesList) {
                NodeConfig nodeConfig = parseNodeConfig(nodeMap);
                nodeConfigs.add(nodeConfig);
            }
            config.setNodes(nodeConfigs);
        }
        
        return config;
    }
    
    /**
     * 解析节点配置
     */
    @SuppressWarnings("unchecked")
    private NodeConfig parseNodeConfig(Map<String, Object> nodeMap) {
        NodeConfig nodeConfig = new NodeConfig();
        
        nodeConfig.setNodeId((String) nodeMap.get("id"));
        nodeConfig.setNodeType((String) nodeMap.get("type"));
        nodeConfig.setName((String) nodeMap.get("name"));
        nodeConfig.setDescription((String) nodeMap.get("description"));
        nodeConfig.setNextNodeId((String) nodeMap.get("next"));
        
        // 参数
        Map<String, Object> paramsMap = (Map<String, Object>) nodeMap.get("params");
        if (paramsMap != null) {
            nodeConfig.setParameters(paramsMap);
        }
        
        // 输出
        Map<String, Object> outputsMap = (Map<String, Object>) nodeMap.get("outputs");
        if (outputsMap != null) {
            Map<String, String> outputs = new HashMap<>();
            for (Map.Entry<String, Object> entry : outputsMap.entrySet()) {
                outputs.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            nodeConfig.setOutputs(outputs);
        }
        
        // 输入引用
        List<Object> inputsList = (List<Object>) nodeMap.get("inputs");
        if (inputsList != null) {
            List<String> inputs = new ArrayList<>();
            for (Object obj : inputsList) {
                inputs.add(String.valueOf(obj));
            }
            nodeConfig.setInputs(inputs);
        }
        
        // 分支（用于条件节点）
        Map<String, Object> branchesMap = (Map<String, Object>) nodeMap.get("branches");
        if (branchesMap != null) {
            Map<String, String> branches = new HashMap<>();
            for (Map.Entry<String, Object> entry : branchesMap.entrySet()) {
                branches.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            nodeConfig.setBranches(branches);
        }
        
        // 循环节点
        List<Object> loopNodesList = (List<Object>) nodeMap.get("loop_nodes");
        if (loopNodesList != null) {
            // 处理循环节点（后续实现）
            List<String> loopNodes = new ArrayList<>();
            for (Object obj : loopNodesList) {
                loopNodes.add(String.valueOf(obj));
            }
            // 可以存储在参数中
            nodeConfig.getParameters().put("loop_nodes", loopNodes);
        }
        
        return nodeConfig;
    }
    
    /**
     * 加载多个工作流配置文件
     */
    public List<WorkflowConfig> loadAllFromDirectory(String directoryPath) throws IOException {
        List<WorkflowConfig> configs = new ArrayList<>();
        
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            return configs;
        }
        
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    WorkflowConfig config = loadFromFile(file.getAbsolutePath());
                    configs.add(config);
                } catch (Exception e) {
                    System.err.println("Failed to load workflow from " + file.getName() + ": " + e.getMessage());
                }
            }
        }
        
        return configs;
    }
}
