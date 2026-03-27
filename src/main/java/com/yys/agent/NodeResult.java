package com.yys.agent;

import java.util.List;
import java.util.Map;

/**
 * 节点执行结果
 */
public class NodeResult {
    private boolean success;
    private String output;
    private Map<String, Object> data;
    private String nextNodeId;

    public NodeResult(boolean success, String output) {
        this.success = success;
        this.output = output;
    }

    public static NodeResult success(String output) {
        return new NodeResult(true, output);
    }

    public static NodeResult success(String output, Map<String, Object> data) {
        NodeResult result = new NodeResult(true, output);
        result.data = data;
        return result;
    }

    public static NodeResult failure(String output) {
        return new NodeResult(false, output);
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public String getNextNodeId() { return nextNodeId; }
    public void setNextNodeId(String nextNodeId) { this.nextNodeId = nextNodeId; }
}
