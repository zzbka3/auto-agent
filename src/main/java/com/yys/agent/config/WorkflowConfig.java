package com.yys.agent.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流配置
 * 定义整个自动化流程
 */
public class WorkflowConfig {
    
    // 工作流名称
    private String name;
    
    // 工作流描述
    private String description;
    
    // 版本
    private String version;
    
    // 全局参数
    private Map<String, Object> global;
    
    // 节点配置列表
    private List<NodeConfig> nodes;
    
    // 起始节点ID
    private String startNodeId;
    
    // 节点映射（用于快速查找）
    private Map<String, NodeConfig> nodeMap;
    
    public WorkflowConfig() {
        this.global = new HashMap<>();
        this.nodeMap = new HashMap<>();
    }
    
    /**
     * 构建节点映射
     */
    public void buildNodeMap() {
        if (nodes == null) return;
        nodeMap.clear();
        for (NodeConfig node : nodes) {
            nodeMap.put(node.getNodeId(), node);
        }
    }
    
    /**
     * 根据ID获取节点配置
     */
    public NodeConfig getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }
    
    /**
     * 获取全局参数
     */
    public <T> T getGlobalParam(String key, Class<T> type) {
        Object value = global.get(key);
        if (value == null) return null;
        return type.cast(value);
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public Map<String, Object> getGlobal() { return global; }
    public void setGlobal(Map<String, Object> global) { this.global = global; }
    
    public List<NodeConfig> getNodes() { return nodes; }
    public void setNodes(List<NodeConfig> nodes) { 
        this.nodes = nodes;
        buildNodeMap();
    }
    
    public String getStartNodeId() { return startNodeId; }
    public void setStartNodeId(String startNodeId) { this.startNodeId = startNodeId; }
    
    public Map<String, NodeConfig> getNodeMap() { return nodeMap; }
}
