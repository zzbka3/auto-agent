package com.yys.agent;

import com.yys.agent.config.NodeConfig;
import java.util.HashMap;
import java.util.Map;

/**
 * 节点抽象基类
 */
public abstract class AbstractAgentNode implements AgentNode {
    
    protected String nodeId;
    protected String name;
    protected String description;
    protected NodeType nodeType;
    protected Map<String, Object> parameters;
    protected Map<String, Object> outputs;
    
    protected AbstractAgentNode(String nodeId, String name, String description, NodeType nodeType) {
        this.nodeId = nodeId;
        this.name = name;
        this.description = description;
        this.nodeType = nodeType;
        this.parameters = new HashMap<>();
        this.outputs = new HashMap<>();
    }
    
    @Override
    public String getNodeId() { return nodeId; }
    @Override
    public String getName() { return name; }
    @Override
    public String getDescription() { return description; }
    @Override
    public NodeType getNodeType() { return nodeType; }
    
    @Override
    public void initFromConfig(NodeConfig config) {
        this.nodeId = config.getNodeId();
        this.name = config.getName();
        this.description = config.getDescription();
        if (config.getParameters() != null) {
            this.parameters.putAll(config.getParameters());
        }
    }
    
    // 参数获取
    protected <T> T getParam(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value == null) return null;
        return type.cast(value);
    }
    
    protected String getStringParam(String key) {
        return getParam(key, String.class);
    }
    
    protected String getStringParam(String key, String defaultValue) {
        String value = getStringParam(key);
        return value != null ? value : defaultValue;
    }
    
    protected Integer getIntParam(String key) {
        return getParam(key, Integer.class);
    }
    
    protected Integer getIntParam(String key, Integer defaultValue) {
        Integer value = getIntParam(key);
        return value != null ? value : defaultValue;
    }
    
    protected Boolean getBoolParam(String key) {
        return getParam(key, Boolean.class);
    }
    
    protected Boolean getBoolParam(String key, Boolean defaultValue) {
        Boolean value = getBoolParam(key);
        return value != null ? value : defaultValue;
    }
    
    protected Float getFloatParam(String key) {
        Object value = parameters.get(key);
        if (value == null) return null;
        if (value instanceof Float) return (Float) value;
        if (value instanceof Double) return ((Double) value).floatValue();
        if (value instanceof Number) return ((Number) value).floatValue();
        return null;
    }
    
    protected Float getFloatParam(String key, Float defaultValue) {
        Float value = getFloatParam(key);
        return value != null ? value : defaultValue;
    }
    
    protected void setParam(String key, Object value) {
        parameters.put(key, value);
    }
    
    protected void setOutput(String key, Object value) {
        outputs.put(key, value);
    }
    
    @Override
    public Map<String, Object> getOutputs() {
        return outputs;
    }
    
    protected void clearOutputs() {
        outputs.clear();
    }
}
