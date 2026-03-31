package com.yys.agent.nodes;

import com.yys.agent.*;
import com.yys.agent.config.NodeConfig;
import com.yys.agent.context.NodeExecutionContext;
import com.yys.agent.context.WorkFlowContext;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 结束节点
 * 标记工作流的结束点，执行清理操作，保留执行记录
 */
public class EndNode extends AbstractAgentNode {

    private String message = "工作流执行完成";
    private boolean cleanupTempFiles = true;
    private boolean saveExecutionReport = true;

    public EndNode() {
        super("end", "结束", "工作流结束节点", NodeType.END);
    }

    public EndNode(String nodeId, String name) {
        super(nodeId, name, "工作流结束节点", NodeType.END);
    }

    @Override
    public void initFromConfig(NodeConfig config) {
        super.initFromConfig(config);
        this.message = getStringParam("message", "工作流执行完成");
        this.cleanupTempFiles = getBoolParam("cleanup_temp_files", true);
        this.saveExecutionReport = getBoolParam("save_execution_report", true);
    }

    @Override
    public NodeResult execute(NodeExecutionContext execContext) {
        clearOutputs();

        try {
            // 设置输出信息
            setOutput("message", message);
            setOutput("status", "completed");

            if (execContext != null) {
                WorkFlowContext context = execContext.getWorkflowContext();

                // 记录结束时间
                long endTime = System.currentTimeMillis();
                long startTime = context.getTemp("workflow_start_time", Long.class, endTime);
                long duration = endTime - startTime;

                context.setTemp("workflow_end_time", endTime);
                context.setTemp("workflow_duration", duration);

                // 获取执行统计信息
                int nodeCount = context.getExecutionHistory().size();
                setOutput("executed_nodes", nodeCount);
                setOutput("workflow_name", context.getGlobalInputs().get("workflow_name"));
                setOutput("duration_ms", duration);
                setOutput("duration_seconds", duration / 1000.0);

                // 输出统计信息到日志
                context.log("\n========================================");
                context.log(message);
                context.log("========================================");
                context.log("执行统计:");
                context.log("  - 执行节点数: " + nodeCount);
                context.log("  - 执行时长: " + (duration / 1000.0) + " 秒");
                context.log("  - 开始时间: " + new Date(startTime));
                context.log("  - 结束时间: " + new Date(endTime));
                context.log("  - 执行文件夹: " + context.getExecutionDir());

                // 输出每个节点的入参和出参
                context.log("\n节点执行详情:");
                context.log("----------------------------------------");
                for (WorkFlowContext.ExecutionRecord record : context.getExecutionHistory()) {
                    String status = record.isSuccess() ? "✓" : "✗";
                    context.log(status + " 节点: " + record.getNodeId() + " - " + record.getMessage());

                    // 显示入参
                    if (record.getInputs() != null && !record.getInputs().isEmpty()) {
                        context.log("  入参:");
                        for (Map.Entry<String, Object> entry : record.getInputs().entrySet()) {
                            context.log("    " + entry.getKey() + ": " + entry.getValue());
                        }
                    }

                    // 显示出参
                    if (record.getOutputs() != null && !record.getOutputs().isEmpty()) {
                        context.log("  出参:");
                        for (Map.Entry<String, Object> entry : record.getOutputs().entrySet()) {
                            context.log("    " + entry.getKey() + ": " + entry.getValue());
                        }
                    }
                    context.log("----------------------------------------");
                }
                context.log("\n========================================\n");

                // 保存执行报告
                if (saveExecutionReport && context.getExecutionDir() != null) {
                    saveExecutionReport(context);
                }

                // 清理临时文件
                if (cleanupTempFiles) {
                    context.cleanupTempFiles();
                }
            }

            System.out.println("\n========================================");
            System.out.println(message);
            System.out.println("========================================");
            System.out.println("执行记录已保存到: " +
                (execContext != null ? execContext.getWorkflowContext().getLogFile().getAbsolutePath() : "N/A"));

            return NodeResult.success(message);

        } catch (Exception e) {
            return NodeResult.failure("结束节点执行失败: " + e.getMessage());
        }
    }

    /**
     * 保存执行报告
     */
    private void saveExecutionReport(WorkFlowContext context) {
        try {
            Path reportFile = context.getExecutionDir().resolve("execution_report.txt");

            try (FileWriter writer = new FileWriter(reportFile.toFile())) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                long endTime = System.currentTimeMillis();
                long startTime = context.getTemp("workflow_start_time", Long.class, endTime);

                writer.write("========================================\n");
                writer.write("工作流执行报告\n");
                writer.write("========================================\n\n");
                writer.write("工作流名称: " + context.getGlobalInputs().get("workflow_name") + "\n");
                writer.write("开始时间: " + sdf.format(new Date(startTime)) + "\n");
                writer.write("结束时间: " + sdf.format(new Date(endTime)) + "\n");
                writer.write("执行时长: " + ((endTime - startTime) / 1000.0) + " 秒\n");
                writer.write("执行文件夹: " + context.getExecutionDir() + "\n\n");

                writer.write("执行历史:\n");
                writer.write("----------------------------------------\n");
                for (WorkFlowContext.ExecutionRecord record : context.getExecutionHistory()) {
                    String status = record.isSuccess() ? "[成功]" : "[失败]";
                    writer.write(status + " " + record.getNodeId() + ": " + record.getMessage() + "\n");

                    // 显示入参
                    if (record.getInputs() != null && !record.getInputs().isEmpty()) {
                        writer.write("  入参:\n");
                        for (Map.Entry<String, Object> entry : record.getInputs().entrySet()) {
                            writer.write("    " + entry.getKey() + " = " + entry.getValue() + "\n");
                        }
                    }

                    // 显示出参
                    if (record.getOutputs() != null && !record.getOutputs().isEmpty()) {
                        writer.write("  出参:\n");
                        for (Map.Entry<String, Object> entry : record.getOutputs().entrySet()) {
                            writer.write("    " + entry.getKey() + " = " + entry.getValue() + "\n");
                        }
                    }
                    writer.write("----------------------------------------\n");
                }
                writer.write("========================================\n");

                // 标记为保留文件
                context.registerKeepFile(reportFile.toAbsolutePath().toString());
            }

            context.log("执行报告已保存: execution_report.txt");

        } catch (IOException e) {
            context.log("保存执行报告失败: " + e.getMessage());
        }
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCleanupTempFiles(boolean cleanup) {
        this.cleanupTempFiles = cleanup;
    }

    public void setSaveExecutionReport(boolean save) {
        this.saveExecutionReport = save;
    }
}
