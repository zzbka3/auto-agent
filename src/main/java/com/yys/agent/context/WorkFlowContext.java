package com.yys.agent.context;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * 工作流全局上下文
 * 存储全局输入和所有节点的执行结果
 *
 * 数据存储结构：
 * - globalInputs: 全局输入（YAML配置转换）
 * - nodeOutputs: 每个节点的输出（key: nodeId）
 * - tempVariables: 临时变量（运行时产生）
 * - executionDir: 执行文件夹（本次执行的所有文件都放在这里）
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

    // 执行文件夹（本次执行的所有文件都放在这里）
    private Path executionDir;

    // 日志文件
    private File logFile;

    // 临时文件列表（用于清理）
    private List<String> tempFiles;

    // 保留的文件列表（不清理）
    private List<String> keepFiles;

    // 日志写入器
    private PrintWriter logWriter;

    public WorkFlowContext() {
        this.globalInputs = new HashMap<>();
        this.nodeOutputs = new HashMap<>();
        this.tempVariables = new HashMap<>();
        this.executionHistory = new ArrayList<>();
        this.state = WorkflowState.INIT;
        this.tempFiles = new ArrayList<>();
        this.keepFiles = new ArrayList<>();
    }

    // ==================== 执行文件夹管理 ====================

    /**
     * 初始化执行文件夹
     * @param baseDir 基础目录
     * @param workflowName 工作流名称
     */
    public void initExecutionDir(String baseDir, String workflowName) {
        try {
            // 创建文件夹：workflow_name_20240331_143052_123
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = sdf.format(new Date());
            String dirName = workflowName + "_" + timestamp;
            this.executionDir = Paths.get(baseDir, dirName);

            // 创建文件夹
            Files.createDirectories(executionDir);

            // 创建日志文件
            this.logFile = executionDir.resolve("execution.log").toFile();
            this.logWriter = new PrintWriter(new FileWriter(logFile, true));

            log("执行文件夹创建成功: " + executionDir);

        } catch (IOException e) {
            System.err.println("创建执行文件夹失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取执行文件夹路径
     */
    public Path getExecutionDir() {
        return executionDir;
    }

    /**
     * 获取执行文件夹的子目录路径
     */
    public Path getExecutionSubDir(String subDirName) {
        try {
            Path subDir = executionDir.resolve(subDirName);
            Files.createDirectories(subDir);
            return subDir;
        } catch (IOException e) {
            System.err.println("创建子目录失败: " + e.getMessage());
            return executionDir;
        }
    }

    /**
     * 创建一个文件并返回其路径
     */
    public Path createFile(String fileName) throws IOException {
        Path filePath = executionDir.resolve(fileName);
        Files.createFile(filePath);
        return filePath;
    }

    /**
     * 记录临时文件（结束时可以清理）
     */
    public void registerTempFile(String filePath) {
        if (!tempFiles.contains(filePath)) {
            tempFiles.add(filePath);
        }
    }

    /**
     * 记录需要保留的文件（不清理）
     */
    public void registerKeepFile(String filePath) {
        if (!keepFiles.contains(filePath)) {
            keepFiles.add(filePath);
        }
    }

    /**
     * 清理临时文件
     */
    public void cleanupTempFiles() {
        log("开始清理临时文件...");
        int cleaned = 0;
        for (String filePath : tempFiles) {
            try {
                // 如果在保留列表中，跳过
                if (keepFiles.contains(filePath)) {
                    log("跳过保留文件: " + filePath);
                    continue;
                }

                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    Files.delete(path);
                    cleaned++;
                    log("已删除: " + filePath);
                }
            } catch (IOException e) {
                log("删除文件失败: " + filePath + ", 错误: " + e.getMessage());
            }
        }
        log("清理完成，共删除 " + cleaned + " 个文件");
    }

    // ==================== 日志记录 ====================

    /**
     * 写入日志
     */
    public void log(String message) {
        if (logWriter != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String timestamp = sdf.format(new Date());
            logWriter.println("[" + timestamp + "] " + message);
            logWriter.flush();
        }
        System.out.println(message);
    }

    /**
     * 关闭日志文件
     */
    public void closeLog() {
        if (logWriter != null) {
            logWriter.close();
            logWriter = null;
        }
    }

    public File getLogFile() {
        return logFile;
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
     * 添加执行记录（旧版，兼容性保留）
     */
    public void addExecutionRecord(String nodeId, boolean success, String message) {
        addExecutionRecord(nodeId, null, null, success, message);
    }

    /**
     * 添加执行记录（新版，包含入参和出参）
     * @param nodeId 节点ID
     * @param inputs 节点入参
     * @param outputs 节点出参
     * @param success 是否成功
     * @param message 执行消息
     */
    public void addExecutionRecord(String nodeId, Map<String, Object> inputs,
                                  Map<String, Object> outputs, boolean success, String message) {
        ExecutionRecord record = new ExecutionRecord(nodeId, inputs, outputs, success, message);
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
        private Map<String, Object> inputs;    // 节点入参
        private Map<String, Object> outputs;   // 节点出参

        // 构造方法1：旧版（兼容）
        public ExecutionRecord(String nodeId, boolean success, String message) {
            this(nodeId, null, null, success, message);
        }

        // 构造方法2：新版（包含入参出参）
        public ExecutionRecord(String nodeId, Map<String, Object> inputs,
                          Map<String, Object> outputs, boolean success, String message) {
            this.nodeId = nodeId;
            this.timestamp = System.currentTimeMillis();
            this.inputs = inputs;
            this.outputs = outputs;
            this.success = success;
            this.message = message;
        }

        public String getNodeId() { return nodeId; }
        public long getTimestamp() { return timestamp; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, Object> getInputs() { return inputs; }
        public Map<String, Object> getOutputs() { return outputs; }
    }
}
