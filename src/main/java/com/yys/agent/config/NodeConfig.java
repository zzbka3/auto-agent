package com.yys.agent.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点配置
 * 每个节点在流程中需要有参数
 */
public class NodeConfig {
    
    // 节点ID
    private String nodeId;
    
    // 节点类型
    private String nodeType;
    
    // 节点名称
    private String name;
    
    // 节点描述
    private String description;
    
    // 节点参数（键值对）
    private Map<String, Object> parameters;
    
    // 节点输出定义
    private Map<String, String> outputs;
    
    // 下一个节点ID（用于线性流程）
    private String nextNodeId;
    
    // 条件分支（用于条件节点）
    private Map<String, String> branches;
    
    // 输入引用（用于从其他节点获取数据）
    private List<String> inputs;
    
    public NodeConfig() {
        this.parameters = new HashMap<>();
        this.outputs = new HashMap<>();
        this.branches = new HashMap<>();
        this.inputs = new ArrayList<>();
    }
    
    // 获取参数
    public <T> T getParam(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value == null) return null;
        return type.cast(value);
    }
    
    // 获取参数（带默认值）
    public <T> T getParam(String key, Class<T> type, T defaultValue) {
        T value = getParam(key, type);
        return value != null ? value : defaultValue;
    }
    
    // 获取字符串参数
    public String getStringParam(String key) {
        return getParam(key, String.class);
    }
    
    // 获取整数参数
    public Integer getIntParam(String key) {
        return getParam(key, Integer.class);
    }
    
    // 获取布尔参数
    public Boolean getBoolParam(String key) {
        return getParam(key, Boolean.class);
    }
    
    // Getters and Setters
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    
    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    
    public Map<String, String> getOutputs() { return outputs; }
    public void setOutputs(Map<String, String> outputs) { this.outputs = outputs; }
    
    public String getNextNodeId() { return nextNodeId; }
    public void setNextNodeId(String nextNodeId) { this.nextNodeId = nextNodeId; }
    
    public Map<String, String> getBranches() { return branches; }
    public void setBranches(Map<String, String> branches) { this.branches = branches; }
    
    public List<String> getInputs() { return inputs; }
    public void setInputs(List<String> inputs) { this.inputs = inputs; }
    
    @Override
    public String toString() {
        return "NodeConfig{" +
                "nodeId='" + nodeId + '\'' +
                ", nodeType='" + nodeType + '\'' +
                ", name='" + name + '\'' +
                ", parameters=" + parameters +
                ", outputs=" + outputs +
                '}';
    }
}
