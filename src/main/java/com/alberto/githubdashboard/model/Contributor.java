package com.alberto.githubdashboard.model;

import lombok.Data;

@Data
public class Contributor {

    private String login;
    private int contributions;
    private String avatarUrl;
}