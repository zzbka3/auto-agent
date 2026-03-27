package com.yys.agent;

import com.yys.agent.config.NodeConfig;
import com.yys.agent.config.ParamValue;
import com.yys.agent.context.WorkFlowContext;
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

    // 执行上下文（运行时设置）
    protected WorkFlowContext execContext;

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

        // 只使用 ParamValue 格式的参数
        if (config.getParamValues() != null) {
            for (Map.Entry<String, ParamValue> entry : config.getParamValues().entrySet()) {
                this.parameters.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 设置执行上下文（由引擎在执行时调用）
     */
    public void setExecContext(WorkFlowContext context) {
        this.execContext = context;
    }

    /**
     * 解析参数值，支持引用
     * 引用格式：
     * - ${nodeId:outputKey} - 引用某节点的输出
     * - ${global:key} - 引用全局输入
     * - ${temp:key} - 引用临时变量
     * - ${nodeId} - 引用节点默认输出
     */
    protected Object resolveValue(Object value) {
        if (value == null) return null;

        // 如果是 ParamValue 对象
        if (value instanceof ParamValue) {
            ParamValue pv = (ParamValue) value;
            if (pv.hasRefer()) {
                return resolveReference(pv.getRefer());
            }
            return pv.getRawValue();
        }

        // 如果是字符串，检查是否是引用格式
        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.startsWith("${") && strValue.endsWith("}")) {
                return resolveReference(strValue);
            }
        }

        return value;
    }

    /**
     * 解析引用字符串
     */
    protected Object resolveReference(String reference) {
        if (execContext != null) {
            return execContext.resolveReference(reference);
        }
        return reference;
    }

    /**
     * 解析字符串参数值，支持引用
     */
    protected String resolveStringValue(Object value) {
        Object resolved = resolveValue(value);
        return resolved != null ? String.valueOf(resolved) : null;
    }

    // 参数获取（支持引用）
    protected <T> T getParam(String key, Class<T> type) {
        // 先尝试从 parameters 获取
        Object value = parameters.get(key);
        Object resolved = resolveValue(value);
        if (resolved == null) return null;
        return type.cast(resolved);
    }

    protected String getStringParam(String key) {
        return resolveStringValue(parameters.get(key));
    }

    protected String getStringParam(String key, String defaultValue) {
        String value = getStringParam(key);
        return value != null ? value : defaultValue;
    }

    protected Integer getIntParam(String key) {
        Object value = parameters.get(key);
        Object resolved = resolveValue(value);
        if (resolved == null) return null;
        if (resolved instanceof Integer) return (Integer) resolved;
        if (resolved instanceof Number) return ((Number) resolved).intValue();
        try {
            return Integer.parseInt(String.valueOf(resolved));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected Integer getIntParam(String key, Integer defaultValue) {
        Integer value = getIntParam(key);
        return value != null ? value : defaultValue;
    }

    protected Boolean getBoolParam(String key) {
        Object value = parameters.get(key);
        Object resolved = resolveValue(value);
        if (resolved == null) return null;
        if (resolved instanceof Boolean) return (Boolean) resolved;
        return Boolean.parseBoolean(String.valueOf(resolved));
    }

    protected Boolean getBoolParam(String key, Boolean defaultValue) {
        Boolean value = getBoolParam(key);
        return value != null ? value : defaultValue;
    }

    protected Float getFloatParam(String key) {
        Object value = parameters.get(key);
        Object resolved = resolveValue(value);
        if (resolved == null) return null;
        if (resolved instanceof Float) return (Float) resolved;
        if (resolved instanceof Double) return ((Double) resolved).floatValue();
        if (resolved instanceof Number) return ((Number) resolved).floatValue();
        try {
            return Float.parseFloat(String.valueOf(resolved));
        } catch (NumberFormatException e) {
            return null;
        }
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
