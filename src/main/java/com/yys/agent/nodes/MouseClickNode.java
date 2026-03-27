package com.yys.agent.nodes;

import com.yys.agent.*;
import com.yys.agent.config.NodeConfig;
import com.yys.agent.context.*;
import java.awt.*;
import java.awt.event.InputEvent;

/**
 * 鼠标点击节点
 * 执行鼠标点击操作
 */
public class MouseClickNode extends AbstractAgentNode {
    
    private ClickType clickType = ClickType.LEFT_CLICK;
    private Integer x;
    private Integer y;
    private int clickCount = 1;
    private int clickInterval = 100;
    
    public MouseClickNode() {
        super("mouse_click", "鼠标点击", "执行鼠标点击", NodeType.MOUSE_CLICK);
    }
    
    public MouseClickNode(String nodeId, String name) {
        super(nodeId, name, "执行鼠标点击", NodeType.MOUSE_CLICK);
    }
    
    @Override
    public void initFromConfig(NodeConfig config) {
        super.initFromConfig(config);
        this.clickType = ClickType.valueOf(getStringParam("click_type", "LEFT_CLICK"));
        this.x = getIntParam("x");
        this.y = getIntParam("y");
        this.clickCount = getIntParam("click_count", 1);
        this.clickInterval = getIntParam("click_interval", 100);
    }
    
    @Override
    public NodeResult execute(NodeExecutionContext execContext) {
        try {
            Robot robot = new Robot();
            
            int targetX, targetY;
            
            // 优先使用固定坐标
            if (x != null && y != null) {
                targetX = x;
                targetY = y;
            } else if (execContext != null) {
                // 从临时变量获取目标区域
                Rectangle region = execContext.getTemp("target_region", Rectangle.class);
                if (region == null) {
                    return NodeResult.failure("未找到点击区域");
                }
                int[] center = region.getCenter();
                targetX = center[0];
                targetY = center[1];
            } else {
                return NodeResult.failure("未指定点击位置");
            }
            
            for (int i = 0; i < clickCount; i++) {
                clickAt(robot, targetX, targetY, clickType);
                if (clickInterval > 0 && i < clickCount - 1) {
                    Thread.sleep(clickInterval);
                }
            }
            
            setOutput("click_x", targetX);
            setOutput("click_y", targetY);
            
            return NodeResult.success("点击坐标: (" + targetX + ", " + targetY + ")");
            
        } catch (Exception e) {
            return NodeResult.failure("鼠标点击失败: " + e.getMessage());
        }
    }
    
    private void clickAt(Robot robot, int x, int y, ClickType type) throws AWTException {
        robot.mouseMove(x, y);
        robot.delay(50);
        
        switch (type) {
            case LEFT_CLICK:
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.delay(50);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                break;
            case RIGHT_CLICK:
                robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                robot.delay(50);
                robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                break;
            case DOUBLE_CLICK:
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.delay(50);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                robot.delay(100);
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.delay(50);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                break;
        }
    }
    
    public void setClickType(ClickType clickType) { this.clickType = clickType; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public void setClickCount(int count) { this.clickCount = count; }
    
    public enum ClickType {
        LEFT_CLICK, RIGHT_CLICK, DOUBLE_CLICK
    }
}
