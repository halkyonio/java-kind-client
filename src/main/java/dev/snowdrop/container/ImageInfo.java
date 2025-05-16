package dev.snowdrop.container;

import java.util.Map;

public class ImageInfo {
    public String id;
    public String digest;
    public String tags;
    public Map<String, String> labels;
    public String[] env;
    public String platform;
}
