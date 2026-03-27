package com.yys.agent.service;

import com.yys.agent.nodes.Rectangle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 基于大模型的图像分析服务
 * 使用 OkHttp 直接调用 OpenAI Vision API (GPT-4o)
 */
public class LlmImageAnalysisService implements ImageAnalysisService {

    private final String apiKey;
    private final String modelName;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * 使用默认配置创建服务
     * 需要设置 OPENAI_API_KEY 环境变量
     */
    public LlmImageAnalysisService() {
        this(System.getenv("OPENAI_API_KEY"), "gpt-4o");
    }

    /**
     * 指定模型创建服务
     * @param apiKey API Key
     * @param modelName 模型名称（如 gpt-4o, gpt-4o-2024-05-13）
     */
    public LlmImageAnalysisService(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AnalysisResult analyzeImage(String imagePath, String target) {
        try {
            // 读取图像并获取尺寸
            BufferedImage image = ImageIO.read(new File(imagePath));
            int width = image.getWidth();
            int height = image.getHeight();

            // 将图像转为 Base64
            String imageBase64 = encodeImageToBase64(image);

            // 构建 prompt
            String promptText = buildAnalysisPrompt(width, height, target);

            // 调用大模型
            String response = callVisionApi(promptText, imageBase64);

            // 解析返回结果
            return parseLlmResponse(response, target);

        } catch (Exception e) {
            System.err.println("图像分析失败: " + e.getMessage());
            e.printStackTrace();
            return new AnalysisResult(false);
        }
    }

    @Override
    public boolean evaluateImage(String imagePath, String question) {
        try {
            BufferedImage image = ImageIO.read(new File(imagePath));
            int width = image.getWidth();
            int height = image.getHeight();
            String imageBase64 = encodeImageToBase64(image);

            String promptText = buildEvaluationPrompt(width, height, question);
            String response = callVisionApi(promptText, imageBase64);

            // 简单判断返回值
            return response.toLowerCase().contains("true")
                    || response.toLowerCase().contains("yes")
                    || response.toLowerCase().contains("是")
                    || response.toLowerCase().contains("有");

        } catch (Exception e) {
            System.err.println("图像判断失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 调用 OpenAI Vision API
     */
    private String callVisionApi(String prompt, String imageBase64) throws IOException {
        // 构建请求体
        String requestBodyJson = String.format("""
            {
                "model": "%s",
                "messages": [
                    {
                        "role": "user",
                        "content": [
                            {"type": "text", "text": "%s"},
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": "data:image/png;base64,%s"
                                }
                            }
                        ]
                    }
                ],
                "max_tokens": 1000
            }
            """, modelName, prompt.replace("\"", "\\\""), imageBase64);

        RequestBody requestBody = RequestBody.create(
                requestBodyJson,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API call failed: " + response);
            }

            String responseBody = response.body().string();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode choices = rootNode.get("choices");

            if (choices != null && choices.isArray() && choices.size() > 0) {
                return choices.get(0).get("message").get("content").asText();
            }

            return "";
        }
    }

    /**
     * 将图像编码为 Base64
     */
    private String encodeImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * 构建图像分析 prompt
     */
    private String buildAnalysisPrompt(int width, int height, String target) {
        return "你是一个屏幕图像分析助手。用户希望你在屏幕截图中找到特定目标的位置。\n\n"
                + "屏幕信息：\n"
                + "- 屏幕宽度: " + width + " 像素\n"
                + "- 屏幕高度: " + height + " 像素\n"
                + "- 坐标原点: 左上角 (0, 0)\n"
                + "- X轴向右增加\n"
                + "- Y轴向下增加\n\n"
                + "用户要查找的目标: \"" + target + "\"\n\n"
                + "请分析上面的截图图片，找出\"" + target + "\"的位置。\n\n"
                + "请按以下 JSON 格式返回结果（只返回 JSON，不要其他内容）：\n"
                + "{\n"
                + "    \"found\": true/false,\n"
                + "    \"x\": 左上角X坐标,\n"
                + "    \"y\": 左上角Y坐标,\n"
                + "    \"width\": 区域宽度,\n"
                + "    \"height\": 区域高度,\n"
                + "    \"description\": \"对这个区域的描述\"\n"
                + "}\n\n"
                + "如果找到了目标，设置 found 为 true 并给出精确的坐标和区域。\n"
                + "如果没找到，设置 found 为 false。";
    }

    /**
     * 构建图像判断 prompt
     */
    private String buildEvaluationPrompt(int width, int height, String question) {
        return "你是一个屏幕图像分析助手。请根据截图回答用户的问题。\n\n"
                + "屏幕信息：\n"
                + "- 屏幕宽度: " + width + " 像素\n"
                + "- 屏幕高度: " + height + " 像素\n\n"
                + "问题: \"" + question + "\"\n\n"
                + "请根据截图回答这个问题。返回 \"true\" 或 \"false\"，不需要其他解释。";
    }

    /**
     * 解析大模型返回的结果
     */
    private AnalysisResult parseLlmResponse(String response, String target) {
        try {
            // 尝试提取 JSON
            response = response.trim();
            // 去掉可能的 markdown 代码块标记
            if (response.startsWith("```")) {
                response = response.replaceAll("```json", "").replaceAll("```", "").trim();
            }

            // 使用正则提取 JSON 内容
            Pattern jsonPattern = Pattern.compile("\\{[^{}]*\\}");
            Matcher matcher = jsonPattern.matcher(response);
            String jsonStr = null;
            while (matcher.find()) {
                jsonStr = matcher.group();
                if (jsonStr.contains("found") && jsonStr.contains("x")) {
                    break;
                }
            }

            if (jsonStr == null) {
                // 备用方案：直接解析坐标
                return parseCoordinatesFromText(response, target);
            }

            // 解析 JSON
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            boolean found = jsonNode.has("found") && jsonNode.get("found").asBoolean();
            int x = jsonNode.has("x") ? jsonNode.get("x").asInt() : 0;
            int y = jsonNode.has("y") ? jsonNode.get("y").asInt() : 0;
            int w = jsonNode.has("width") ? jsonNode.get("width").asInt() : 0;
            int h = jsonNode.has("height") ? jsonNode.get("height").asInt() : 0;

            if (found && x >= 0 && y >= 0) {
                Rectangle region = new Rectangle(x, y, w, h);
                float confidence = 0.9f;
                AnalysisResult result = new AnalysisResult(region, confidence);
                result.setDescription("大模型识别到: " + target);
                return result;
            }

            return new AnalysisResult(false);

        } catch (Exception e) {
            System.err.println("解析大模型响应失败: " + e.getMessage());
            return parseCoordinatesFromText(response, target);
        }
    }

    /**
     * 从文本中提取坐标（备用方案）
     */
    private AnalysisResult parseCoordinatesFromText(String text, String target) {
        try {
            // 尝试匹配 "x: 数字" 或 "X: 数字" 等模式
            Pattern pattern = Pattern.compile("(?:x|X|坐标|position)[\\s:]*(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher xMatcher = pattern.matcher(text);
            int x = -1, y = -1;

            if (xMatcher.find()) {
                x = Integer.parseInt(xMatcher.group(1));
            }
            if (xMatcher.find()) {
                y = Integer.parseInt(xMatcher.group(1));
            }

            if (x >= 0 && y >= 0) {
                // 默认区域大小
                Rectangle region = new Rectangle(x, y, 100, 50);
                AnalysisResult result = new AnalysisResult(region, 0.8f);
                result.setDescription("大模型识别到坐标: (" + x + ", " + y + ")");
                return result;
            }

            // 检查是否未找到
            if (text.toLowerCase().contains("未找到")
                    || text.toLowerCase().contains("not found")
                    || text.toLowerCase().contains("找不到")) {
                return new AnalysisResult(false);
            }

        } catch (Exception e) {
            System.err.println("解析坐标失败: " + e.getMessage());
        }

        return new AnalysisResult(false);
    }
}