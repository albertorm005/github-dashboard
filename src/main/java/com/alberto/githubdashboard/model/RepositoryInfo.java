package com.alberto.githubdashboard.model;

import lombok.Data;

@Data
public class RepositoryInfo {

    private String name;
    private String description;
    private String owner;
    private int stars;
    private int forks;
    private int watchers;
    private String language;
}