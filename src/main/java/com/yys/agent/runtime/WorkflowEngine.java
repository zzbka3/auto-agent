package com.yys.agent.runtime;

import com.yys.agent.*;
import com.yys.agent.config.*;
import com.yys.agent.context.*;
import com.yys.agent.nodes.*;
import java.util.*;

/**
 * 工作流执行引擎
 */
public class WorkflowEngine {
    
    private WorkflowConfig config;
    private Map<String, AgentNode> nodeRegistry;
    private WorkFlowContext workflowContext;
    private State state;
    private String startNodeId;
    
    public WorkflowEngine() {
        this.nodeRegistry = new HashMap<>();
        this.workflowContext = new WorkFlowContext();
        this.state = State.IDLE;
    }
    
    /**
     * 加载工作流配置
     */
    public void loadWorkflow(WorkflowConfig config) {
        this.config = config;
        this.startNodeId = config.getStartNodeId();
        
        // 初始化全局输入
        Map<String, Object> globalParams = config.getGlobal();
        if (globalParams != null) {
            workflowContext.setGlobalInputs(globalParams);
        }
        
        // 创建节点
        createNodesFromConfig();
        
        state = State.LOADED;
    }
    
    private void createNodesFromConfig() {
        List<NodeConfig> nodes = config.getNodes();
        if (nodes == null) return;
        
        for (NodeConfig nodeConfig : nodes) {
            AgentNode node = createNode(nodeConfig);
            if (node != null) {
                nodeRegistry.put(nodeConfig.getNodeId(), node);
            }
        }
    }
    
    private AgentNode createNode(NodeConfig nodeConfig) {
        String nodeType = nodeConfig.getNodeType();
        AgentNode node = null;

        switch (nodeType) {
            case "SCREENSHOT":
                node = new ScreenshotNode();
                break;
            case "IMAGE_ANALYSIS":
                node = new ImageAnalysisNode();
                break;
            case "IMAGE_MATCH":
                node = new ImageMatchNode();
                break;
            case "SELECT_CLICK_REGION":
                node = new ClickRegionSelectorNode();
                break;
            case "MOUSE_CLICK":
                node = new MouseClickNode();
                break;
            case "END":
                node = new com.yys.agent.nodes.EndNode();
                break;
            default:
                System.err.println("Unknown node type: " + nodeType);
                return null;
        }

        if (node != null) {
            node.initFromConfig(nodeConfig);
        }

        return node;
    }
    
    /**
     * 执行工作流
     */
    public void execute() {
        if (state != State.LOADED && state != State.IDLE) {
            throw new IllegalStateException("Cannot execute in current state: " + state);
        }

        state = State.RUNNING;
        workflowContext.setState(WorkFlowContext.WorkflowState.RUNNING);

        try {
            // 初始化执行文件夹
            String baseDir = workflowContext.getGlobalInput("screenshot_dir", String.class, "./executions");
            String workflowName = config.getName();
            workflowContext.initExecutionDir(baseDir, workflowName);

            // 记录开始时间
            workflowContext.setTemp("workflow_start_time", System.currentTimeMillis());

            workflowContext.log("开始执行工作流: " + config.getName());
            workflowContext.log("工作流描述: " + config.getDescription());

            // 保存执行记录到保留列表
            workflowContext.registerKeepFile(workflowContext.getLogFile().getAbsolutePath());

            executeNode(startNodeId);

            workflowContext.setState(WorkFlowContext.WorkflowState.COMPLETED);
            state = State.COMPLETED;

            workflowContext.log("工作流执行完成");

        } catch (Exception e) {
            workflowContext.setState(WorkFlowContext.WorkflowState.ERROR);
            workflowContext.setErrorMessage(e.getMessage());
            workflowContext.log("工作流执行失败: " + e.getMessage());
            state = State.ERROR;
            throw e;
        } finally {
            // 关闭日志
            workflowContext.closeLog();
        }
    }
    
    private void executeNode(String nodeId) {
        if (nodeId == null) return;

        AgentNode node = nodeRegistry.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Node not found: " + nodeId);
        }

        NodeConfig nodeConfig = config.getNode(nodeId);
        NodeExecutionContext execContext = new NodeExecutionContext(workflowContext, nodeId);

        // 设置执行上下文到节点（用于参数引用解析）
        if (node instanceof com.yys.agent.AbstractAgentNode) {
            ((com.yys.agent.AbstractAgentNode) node).setExecContext(workflowContext);
        }

        // 记录入参（执行前）
        Map<String, Object> inputs = null;
        if (node instanceof com.yys.agent.AbstractAgentNode) {
            com.yys.agent.AbstractAgentNode abstractNode = (com.yys.agent.AbstractAgentNode) node;
            inputs = new java.util.HashMap<>(abstractNode.getParameters());
        }

        // 执行节点
        NodeResult result = node.execute(execContext);

        // 节点输出
        Map<String, Object> outputs = node.getOutputs();

        // 保存节点输出
        workflowContext.putNodeOutputs(nodeId, outputs);

        // 记录执行记录（包含入参和出参）
        workflowContext.addExecutionRecord(nodeId, inputs, outputs, result.isSuccess(), result.getOutput());

        if (!result.isSuccess()) {
            throw new RuntimeException("Node " + nodeId + " failed: " + result.getOutput());
        }

        // 决定下一个节点
        String nextNodeId = determineNextNode(nodeConfig, execContext);
        if (nextNodeId != null) {
            executeNode(nextNodeId);
        }
    }
    
    private String determineNextNode(NodeConfig nodeConfig, NodeExecutionContext ctx) {
        // 1. 节点设置的下一个节点
        String nextFromContext = ctx.getNextNode();
        if (nextFromContext != null) {
            return nextFromContext;
        }
        
        // 2. 配置中的next
        return nodeConfig.getNextNodeId();
    }
    
    public WorkflowConfig getConfig() { return config; }
    public WorkFlowContext getWorkflowContext() { return workflowContext; }
    public State getState() { return state; }
    public Map<String, AgentNode> getNodeRegistry() { return nodeRegistry; }
    
    public enum State {
        IDLE, LOADED, RUNNING, PAUSED, COMPLETED, ERROR
    }
}
