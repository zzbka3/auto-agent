package com.yys.agent.service;

import com.yys.agent.nodes.Rectangle;

/**
 * 图像分析结果
 */
public class AnalysisResult {
    private boolean found;
    private Rectangle region;
    private float confidence;
    private String description;
    
    public AnalysisResult() {}
    
    public AnalysisResult(boolean found) {
        this.found = found;
    }
    
    public AnalysisResult(Rectangle region, float confidence) {
        this.found = region != null;
        this.region = region;
        this.confidence = confidence;
    }
    
    public boolean isFound() { return found; }
    public void setFound(boolean found) { this.found = found; }
    public Rectangle getRegion() { return region; }
    public void setRegion(Rectangle region) { this.region = region; }
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
