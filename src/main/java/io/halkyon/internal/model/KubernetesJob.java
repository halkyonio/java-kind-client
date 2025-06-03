package io.halkyon.internal.model;

import java.util.List;

public class KubernetesJob {
    private String name;
    private String image;
    private List<String> command;
    private List<String> args;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getCommand() {
        return command;
    }

    public void setCommand(List<String> command) {
        this.command = command;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    @Override
    public String toString() {
        return "KubernetesJobConfig{" +
            "name='" + name + '\'' +
            ", image='" + image + '\'' +
            ", command=" + command +
            ", args=" + args +
            '}';
    }
}
