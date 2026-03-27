package com.yys.agent.context;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 工作流全局上下文
 * 存储全局输入和所有节点的执行结果
 * 
 * 数据存储结构：
 * - globalInputs: 全局输入（YAML配置转换）
 * - nodeOutputs: 每个节点的输出（key: nodeId）
 * - tempVariables: 临时变量（运行时产生）
 */
public class WorkFlowContext {
    
    // 全局输入（来自YAML配置）
    private Map<String, Object> globalInputs;
    
    // 节点输出（key: nodeId, value: 节点的输出Map）
    private Map<String, Map<String, Object>> nodeOutputs;
    
    // 临时变量（运行时产生，不属于任何节点）
    private Map<String, Object> tempVariables;
    
    // 执行历史
    private List<ExecutionRecord> executionHistory;
    
    // 当前执行状态
    private WorkflowState state;
    
    // 错误信息
    private String errorMessage;
    
    public WorkFlowContext() {
        this.globalInputs = new HashMap<>();
        this.nodeOutputs = new HashMap<>();
        this.tempVariables = new HashMap<>();
        this.executionHistory = new ArrayList<>();
        this.state = WorkflowState.INIT;
    }
    
    // ==================== 全局输入 ====================
    
    /**
     * 设置全局输入
     */
    public void setGlobalInput(String key, Object value) {
        globalInputs.put(key, value);
    }
    
    /**
     * 获取全局输入
     */
    @SuppressWarnings("unchecked")
    public <T> T getGlobalInput(String key, Class<T> type) {
        Object value = globalInputs.get(key);
        if (value == null) return null;
        return type.cast(value);
    }
    
    /**
     * 获取全局输入（带默认值）
     */
    public <T> T getGlobalInput(String key, Class<T> type, T defaultValue) {
        T value = getGlobalInput(key, type);
        return value != null ? value : defaultValue;
    }
    
    public Map<String, Object> getGlobalInputs() {
        return globalInputs;
    }
    
    public void setGlobalInputs(Map<String, Object> globalInputs) {
        this.globalInputs = globalInputs;
    }
    
    // ==================== 节点输出 ====================
    
    /**
     * 保存节点输出
     */
    public void putNodeOutput(String nodeId, String key, Object value) {
        Map<String, Object> outputs = nodeOutputs.computeIfAbsent(nodeId, k -> new HashMap<>());
        outputs.put(key, value);
    }
    
    /**
     * 保存节点的所有输出
     */
    public void putNodeOutputs(String nodeId, Map<String, Object> outputs) {
        if (outputs != null) {
            nodeOutputs.put(nodeId, new HashMap<>(outputs));
        }
    }
    
    /**
     * 获取节点输出
     */
    public <T> T getNodeOutput(String nodeId, String key, Class<T> type) {
        Map<String, Object> outputs = nodeOutputs.get(nodeId);
        if (outputs == null) return null;
        Object value = outputs.get(key);
        if (value == null) return null;
        return type.cast(value);
    }
    
    /**
     * 获取节点所有输出
     */
    public Map<String, Object> getNodeOutputs(String nodeId) {
        Map<String, Object> outputs = nodeOutputs.get(nodeId);
        return outputs != null ? new HashMap<>(outputs) : new HashMap<>();
    }
    
    /**
     * 获取所有节点的输出
     */
    public Map<String, Map<String, Object>> getAllNodeOutputs() {
        return new HashMap<>(nodeOutputs);
    }
    
    // ==================== 临时变量 ====================
    
    /**
     * 设置临时变量
     */
    public void setTemp(String key, Object value) {
        tempVariables.put(key, value);
    }
    
    /**
     * 获取临时变量
     */
    @SuppressWarnings("unchecked")
    public <T> T getTemp(String key, Class<T> type) {
        Object value = tempVariables.get(key);
        if (value == null) return null;
        return type.cast(value);
    }
    
    /**
     * 获取临时变量（带默认值）
     */
    public <T> T getTemp(String key, Class<T> type, T defaultValue) {
        T value = getTemp(key, type);
        return value != null ? value : defaultValue;
    }
    
    public Map<String, Object> getTempVariables() {
        return tempVariables;
    }
    
    // ==================== 数据引用 ====================
    
    /**
     * 解析数据引用
     * 支持格式：
     * - ${global:key} - 全局输入
     * - ${nodeId:outputKey} - 节点输出
     * - ${temp:key} - 临时变量
     * - ${nodeId} - 节点输出（默认output）
     */
    public Object resolveReference(String reference) {
        if (reference == null || !reference.startsWith("${") || !reference.endsWith("}")) {
            return reference;
        }
        
        String inner = reference.substring(2, reference.length() - 1);
        String[] parts = inner.split(":", 2);
        
        if (parts.length == 1) {
            // 可能是节点ID引用整个输出
            return getNodeOutputs(parts[0]).get("output");
        }
        
        String prefix = parts[0];
        String key = parts[1];
        
        switch (prefix) {
            case "global":
                return getGlobalInput(key, Object.class);
            case "temp":
                return getTemp(key, Object.class);
            default:
                // 默认为节点输出
                return getNodeOutput(prefix, key, Object.class);
        }
    }
    
    /**
     * 解析引用（带类型）
     */
    public <T> T resolveReference(String reference, Class<T> type) {
        Object value = resolveReference(reference);
        if (value == null) return null;
        return type.cast(value);
    }
    
    // ==================== 执行历史 ====================
    
    /**
     * 添加执行记录
     */
    public void addExecutionRecord(String nodeId, boolean success, String message) {
        ExecutionRecord record = new ExecutionRecord(nodeId, success, message);
        executionHistory.add(record);
    }
    
    public List<ExecutionRecord> getExecutionHistory() {
        return executionHistory;
    }
    
    // ==================== 状态管理 ====================
    
    public WorkflowState getState() {
        return state;
    }
    
    public void setState(WorkflowState state) {
        this.state = state;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * 工作流状态
     */
    public enum WorkflowState {
        INIT,       // 初始状态
        RUNNING,    // 运行中
        PAUSED,     // 暂停
        COMPLETED,  // 完成
        ERROR       // 错误
    }
    
    /**
     * 执行记录
     */
    public static class ExecutionRecord {
        private String nodeId;
        private long timestamp;
        private boolean success;
        private String message;
        
        public ExecutionRecord(String nodeId, boolean success, String message) {
            this.nodeId = nodeId;
            this.timestamp = System.currentTimeMillis();
            this.success = success;
            this.message = message;
        }
        
        public String getNodeId() { return nodeId; }
        public long getTimestamp() { return timestamp; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
