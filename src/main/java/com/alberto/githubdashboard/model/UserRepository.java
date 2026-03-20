package com.alberto.githubdashboard.model;

import lombok.Data;

@Data
public class UserRepository {

    private String name;
    private int stars;
    private int forks;
    private String language;
    private String url;
}