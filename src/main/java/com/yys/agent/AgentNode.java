package com.yys.agent;

import com.yys.agent.config.NodeConfig;
import com.yys.agent.context.NodeExecutionContext;

/**
 * Agent节点接口
 */
public interface AgentNode {
    
    String getNodeId();
    String getName();
    String getDescription();
    NodeType getNodeType();
    
    /**
     * 执行节点
     */
    NodeResult execute(NodeExecutionContext context);
    
    /**
     * 从配置初始化
     */
    default void initFromConfig(NodeConfig config) {}
    
    /**
     * 获取输出
     */
    default java.util.Map<String, Object> getOutputs() {
        return new java.util.HashMap<>();
    }
}
