package com.yys.agent.config;

/**
 * 参数值
 * 支持两种模式：
 * 1. 直接值：{ "value": "具体值" }
 * 2. 引用：{ "refer": "${nodeId:outputKey}" }
 */
public class ParamValue {

    // 直接值
    private Object value;

    // 引用（格式：${nodeId:outputKey} 或 ${global:key} 或 ${temp:key}）
    private String refer;

    public ParamValue() {}

    public ParamValue(Object value) {
        this.value = value;
    }

    public ParamValue(String refer, boolean isRefer) {
        if (isRefer) {
            this.refer = refer;
        } else {
            this.value = refer;
        }
    }

    /**
     * 从 Map 创建 ParamValue
     * Map 格式：{ "value": xxx } 或 { "refer": "${xxx}" }
     */
    public static ParamValue fromMap(Object obj) {
        if (obj == null) {
            return null;
        }

        // 如果是字符串，说明是直接值（旧格式兼容）
        if (obj instanceof String) {
            String str = (String) obj;
            // 检查是否是引用格式
            if (str.startsWith("${") && str.endsWith("}")) {
                ParamValue pv = new ParamValue();
                pv.setRefer(str);
                return pv;
            }
            // 否则是直接值
            ParamValue pv = new ParamValue();
            pv.setValue(str);
            return pv;
        }

        // 如果是 Map，解析 value 或 refer
        if (obj instanceof java.util.Map) {
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
            ParamValue pv = new ParamValue();

            if (map.containsKey("value")) {
                pv.setValue(map.get("value"));
            }
            if (map.containsKey("refer")) {
                pv.setRefer((String) map.get("refer"));
            }

            return pv;
        }

        // 其他类型作为直接值
        ParamValue pv = new ParamValue();
        pv.setValue(obj);
        return pv;
    }

    /**
     * 获取实际值（在运行时解析引用）
     * @param resolver 引用解析器
     * @return 解析后的值
     */
    public Object getResolvedValue(ReferResolver resolver) {
        if (refer != null && resolver != null) {
            return resolver.resolve(refer);
        }
        return value;
    }

    /**
     * 获取原始值（不解析引用）
     */
    public Object getRawValue() {
        return value;
    }

    /**
     * 获取引用字符串
     */
    public String getRefer() {
        return refer;
    }

    /**
     * 是否有引用
     */
    public boolean hasRefer() {
        return refer != null && !refer.isEmpty();
    }

    /**
     * 是否有直接值
     */
    public boolean hasValue() {
        return value != null;
    }

    // Getters and Setters
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
    public void setRefer(String refer) { this.refer = refer; }

    @Override
    public String toString() {
        if (hasRefer()) {
            return "ParamValue{refer='" + refer + "'}";
        }
        return "ParamValue{value=" + value + "}";
    }

    /**
     * 引用解析器接口
     */
    public interface ReferResolver {
        Object resolve(String reference);
    }
}