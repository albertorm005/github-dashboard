package com.alberto.githubdashboard.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueInfo {
    private String title;
    private String htmlUrl;
    private String repositoryUrl;
    private String repositoryName;
    private String state;
    private List<String> labels;
    private String createdAt;
    private UserInfo user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String login;
        private String avatarUrl;
    }
}
