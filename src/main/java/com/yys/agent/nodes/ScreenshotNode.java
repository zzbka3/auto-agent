package com.yys.agent.nodes;

import com.yys.agent.*;
import com.yys.agent.config.NodeConfig;
import com.yys.agent.context.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.nio.file.Path;

/**
 * 截图节点
 * 截取当前屏幕图像
 */
public class ScreenshotNode extends AbstractAgentNode {
    
    private String saveDir = "./screenshots";
    private boolean saveToFile = true;
    private String filenamePattern = "screenshot_{timestamp}";
    private String format = "png";
    
    public ScreenshotNode() {
        super("screenshot", "截图", "截取当前屏幕", NodeType.SCREENSHOT);
    }
    
    public ScreenshotNode(String nodeId, String name) {
        super(nodeId, name, "截取当前屏幕", NodeType.SCREENSHOT);
    }
    
    @Override
    public void initFromConfig(NodeConfig config) {
        super.initFromConfig(config);
        this.saveDir = getStringParam("save_dir", "./screenshots");
        this.saveToFile = getBoolParam("save_to_file", true);
        this.filenamePattern = getStringParam("filename_pattern", "screenshot_{timestamp}");
        this.format = getStringParam("format", "png");
    }
    
    @Override
    public NodeResult execute(NodeExecutionContext execContext) {
        clearOutputs();

        try {
            java.awt.Robot robot = new java.awt.Robot();
            java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            java.awt.Rectangle captureRect = new java.awt.Rectangle(screenSize);

            BufferedImage image = robot.createScreenCapture(captureRect);
            String screenshotPath = null;

            if (saveToFile && execContext != null) {
                // 使用执行文件夹中的截图子目录
                Path screenshotsDir = execContext.getWorkflowContext().getExecutionSubDir("screenshots");
                String filename = generateFilename();
                File outputFile = screenshotsDir.resolve(filename).toFile();
                ImageIO.write(image, format, outputFile);
                screenshotPath = outputFile.getAbsolutePath();

                // 记录到日志
                execContext.getWorkflowContext().log("截图保存: " + filename);

                // 标记为临时文件（可以清理）
                execContext.getWorkflowContext().registerTempFile(screenshotPath);

                setOutput("screenshot_path", screenshotPath);
                setOutput("screenshot_filename", filename);
                setOutput("image_width", image.getWidth());
                setOutput("image_height", image.getHeight());

                if (execContext != null) {
                    execContext.setTemp("current_screenshot", screenshotPath);
                }
            }

            return NodeResult.success("截图完成" + (screenshotPath != null ? ": " + screenshotPath : ""));

        } catch (Exception e) {
            return NodeResult.failure("截图失败: " + e.getMessage());
        }
    }
    
    private String generateFilename() {
        String filename = filenamePattern;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
        filename = filename.replace("{timestamp}", sdf.format(new Date()));
        filename = filename.replace("{uuid}", UUID.randomUUID().toString().substring(0, 8));
        if (!filename.contains(".")) {
            filename += "." + format;
        }
        return filename;
    }
    
    public void setSaveDir(String saveDir) { this.saveDir = saveDir; }
}
