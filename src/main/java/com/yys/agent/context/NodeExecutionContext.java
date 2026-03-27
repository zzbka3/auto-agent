package com.yys.agent.context;

/**
 * 节点执行上下文
 * 每次节点执行时创建，封装节点所需的数据
 */
public class NodeExecutionContext {
    
    private final WorkFlowContext workflowContext;
    private final String nodeId;
    
    // 节点输入（从引用解析后的实际值）
    private java.util.Map<String, Object> inputs;
    
    // 节点输出（执行后填充）
    private java.util.Map<String, Object> outputs;
    
    // 是否停止执行
    private boolean stopped;
    
    // 下一个节点ID（可由节点动态设置）
    private String nextNodeId;
    
    public NodeExecutionContext(WorkFlowContext workflowContext, String nodeId) {
        this.workflowContext = workflowContext;
        this.nodeId = nodeId;
        this.inputs = new java.util.HashMap<>();
        this.outputs = new java.util.HashMap<>();
        this.stopped = false;
    }
    
    // ==================== 输入获取 ====================
    
    /**
     * 获取输入值
     * 自动解析引用
     */
    public <T> T getInput(String key, Class<T> type) {
        // 先从本节点输入获取
        Object value = inputs.get(key);
        if (value != null) {
            return type.cast(value);
        }
        
        // 从工作流全局输入获取
        return workflowContext.getGlobalInput(key, type);
    }
    
    /**
     * 获取节点引用输入
     * 支持解析 ${nodeId:outputKey} 格式的引用
     */
    public Object getReferencedInput(String reference) {
        return workflowContext.resolveReference(reference);
    }
    
    public <T> T getReferencedInput(String reference, Class<T> type) {
        return workflowContext.resolveReference(reference, type);
    }
    
    // ==================== 输出设置 ====================
    
    /**
     * 设置输出
     */
    public void setOutput(String key, Object value) {
        outputs.put(key, value);
    }
    
    /**
     * 批量设置输出
     */
    public void setOutputs(java.util.Map<String, Object> outputs) {
        if (outputs != null) {
            this.outputs.putAll(outputs);
        }
    }
    
    /**
     * 获取输出
     */
    public <T> T getOutput(String key, Class<T> type) {
        Object value = outputs.get(key);
        if (value == null) return null;
        return type.cast(value);
    }
    
    public java.util.Map<String, Object> getOutputs() {
        return outputs;
    }
    
    // ==================== 数据存储 ====================
    
    /**
     * 存储临时变量
     */
    public void setTemp(String key, Object value) {
        workflowContext.setTemp(key, value);
    }
    
    /**
     * 获取临时变量
     */
    public <T> T getTemp(String key, Class<T> type) {
        return workflowContext.getTemp(key, type);
    }
    
    // ==================== 流程控制 ====================
    
    /**
     * 停止执行
     */
    public void stop() {
        this.stopped = true;
    }
    
    public boolean isStopped() {
        return stopped;
    }
    
    /**
     * 设置下一个节点
     */
    public void setNextNode(String nodeId) {
        this.nextNodeId = nodeId;
    }
    
    public String getNextNode() {
        return nextNodeId;
    }
    
    /**
     * 获取工作流上下文
     */
    public WorkFlowContext getWorkflowContext() {
        return workflowContext;
    }
    
    public String getNodeId() {
        return nodeId;
    }
}
