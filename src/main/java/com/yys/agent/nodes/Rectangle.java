package com.yys.agent.nodes;

/**
 * 矩形区域，用于描述屏幕上的点击区域
 */
public class Rectangle {
    private int x;
    private int y;
    private int width;
    private int height;
    
    public Rectangle() {}
    
    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    /**
     * 获取矩形中心点
     */
    public int[] getCenter() {
        return new int[]{x + width / 2, y + height / 2};
    }
    
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    
    @Override
    public String toString() {
        return "Rectangle{x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "}";
    }
}
