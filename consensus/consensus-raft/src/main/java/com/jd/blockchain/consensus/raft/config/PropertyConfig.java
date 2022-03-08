package com.jd.blockchain.consensus.raft.config;

import org.springframework.util.ReflectionUtils;
import utils.PropertiesUtils;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class PropertyConfig {

    private static final Map<Class<? extends PropertyConfig>, Map<Field, ConfigProperty>> CLASS_FIELD_MAP = new HashMap<>();

    static {
        loadConfig(RaftConfig.class);
        loadConfig(RaftNetworkConfig.class);
        loadConfig(RaftConsensusConfig.class);
        loadConfig(RaftNodeConfig.class);
    }

    public void init(Properties prop) {
        Map<Field, ConfigProperty> m = CLASS_FIELD_MAP.get(this.getClass());
        for (Map.Entry<Field, ConfigProperty> entry : m.entrySet()) {
            Field field = entry.getKey();
            ConfigProperty configProperty = entry.getValue();

            String propValue = PropertiesUtils.getOptionalProperty(prop, configProperty.value());
            if (propValue == null) {
                continue;
            }
            Object parsedValue = configProperty.type().parseValue(propValue);
            ReflectionUtils.setField(field, this, parsedValue);
        }
    }

    public Properties convert() {
        Properties properties = new Properties();
        Map<Field, ConfigProperty> m = CLASS_FIELD_MAP.get(this.getClass());
        for (Map.Entry<Field, ConfigProperty> entry : m.entrySet()) {
            Field field = entry.getKey();
            ConfigProperty configProperty = entry.getValue();

            Object value = ReflectionUtils.getField(field, this);
            if (value != null) {
                properties.setProperty(configProperty.value(), configProperty.type().toStr(value));
            }
        }
        return properties;
    }


    public void setValue(Properties properties, String key, int value) {
        properties.setProperty(key, String.valueOf(value));
    }

    public void setValue(Properties properties, String key, boolean value) {
        properties.setProperty(key, String.valueOf(value));
    }

    public void setValue(Properties properties, String key, String value) {
        properties.setProperty(key, value);
    }

    private static void loadConfig(Class<? extends PropertyConfig> clazz) {
        if (CLASS_FIELD_MAP.containsKey(clazz)) {
            return;
        }
        Map<Field, ConfigProperty> m = new HashMap<>();
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            ConfigProperty configProperty = field.getDeclaredAnnotation(ConfigProperty.class);
            if (configProperty != null) {
                ReflectionUtils.makeAccessible(field);
                m.put(field, configProperty);
            }
        }

        CLASS_FIELD_MAP.put(clazz, m);
    }

    @Target({ElementType.FIELD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    protected @interface ConfigProperty {
        String value() default "";

        ConfigPropertyType type() default ConfigPropertyType.INT;
    }

    protected enum ConfigPropertyType {
        INT {
            @Override
            Object parseValue(String value) {
                return Integer.valueOf(value);
            }
        },
        LONG {
            @Override
            Object parseValue(String value) {
                return Long.valueOf(value);
            }
        },
        STRING {
            @Override
            Object parseValue(String value) {
                return value;
            }
        },
        DOUBLE {
            @Override
            Object parseValue(String value) {
                return Double.valueOf(value);
            }
        },
        BOOLEAN {
            @Override
            Object parseValue(String value) {
                return Boolean.valueOf(value);
            }
        };

        abstract Object parseValue(String value);

        public String toStr(Object value) {
            return value.toString();
        }


    }


}
