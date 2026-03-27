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

    // 节点参数（支持对象格式：{value: xxx} 或 {refer: ${xxx}}）
    private Map<String, ParamValue> paramValues;

    // 节点输出定义
    private Map<String, String> outputs;

    // 下一个节点ID（用于线性流程）
    private String nextNodeId;

    // 条件分支（用于条件节点）
    private Map<String, String> branches;

    // 输入引用（用于从其他节点获取数据）
    private List<String> inputs;

    public NodeConfig() {
        this.paramValues = new HashMap<>();
        this.outputs = new HashMap<>();
        this.branches = new HashMap<>();
        this.inputs = new ArrayList<>();
    }

    /**
     * 获取参数值对象（支持引用解析）
     */
    public ParamValue getParamValue(String key) {
        return paramValues.get(key);
    }

    /**
     * 获取所有 ParamValue
     */
    public Map<String, ParamValue> getParamValues() {
        return paramValues;
    }

    public void setParamValues(Map<String, ParamValue> paramValues) {
        this.paramValues = paramValues != null ? paramValues : new HashMap<>();
    }

    // 获取字符串参数（自动解析引用）
    public String getStringParam(String key) {
        ParamValue pv = paramValues.get(key);
        if (pv == null) return null;
        if (pv.hasRefer()) {
            // 引用在运行时解析，这里只返回 value
            return pv.getRawValue() != null ? String.valueOf(pv.getRawValue()) : null;
        }
        return pv.getRawValue() != null ? String.valueOf(pv.getRawValue()) : null;
    }

    // 获取整数参数
    public Integer getIntParam(String key) {
        ParamValue pv = paramValues.get(key);
        if (pv == null) return null;
        Object value = pv.getRawValue();
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 获取布尔参数
    public Boolean getBoolParam(String key) {
        ParamValue pv = paramValues.get(key);
        if (pv == null) return null;
        Object value = pv.getRawValue();
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(String.valueOf(value));
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
                ", paramValues=" + paramValues +
                ", outputs=" + outputs +
                '}';
    }
}
