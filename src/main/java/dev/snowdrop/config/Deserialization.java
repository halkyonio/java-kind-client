package dev.snowdrop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

final class Deserialization {
    static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public Deserialization() {
        throw new UnsupportedOperationException("Do not instantiate!");
    }
}
