package io.halkyon.config.model;

public class User {
    private String name;
    private UserSpec user;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserSpec getUser() {
        return user;
    }

    public void setUser(UserSpec user) {
        this.user = user;
    }
}
