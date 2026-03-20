package com.alberto.githubdashboard.service;

import com.alberto.githubdashboard.exception.GithubApiException;
import com.alberto.githubdashboard.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class GithubService {

    private final RestTemplate restTemplate;

    @Value("${github.token}")
    private String githubToken;

    private HttpEntity<String> getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (githubToken != null && !githubToken.isEmpty()) {
            headers.set("Authorization", "Bearer " + githubToken);
        }
        headers.set("Accept", "application/vnd.github+json");
        return new HttpEntity<>(headers);
    }

    private String[] parseRepoUrl(String repoUrl) {
        try {
            String cleanUrl = repoUrl.replace("https://github.com/", "").trim();
            if (cleanUrl.endsWith("/")) {
                cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 1);
            }
            String[] parts = cleanUrl.split("/");
            if (parts.length < 2) {
                throw new GithubApiException("Invalid GitHub repository URL: " + repoUrl);
            }
            return parts;
        } catch (Exception e) {
            throw new GithubApiException("Error parsing repository URL: " + e.getMessage());
        }
    }

    @Cacheable(value = "repositories", key = "#p0")
    public RepositoryInfo getRepository(String repoUrl) {
        String[] parts = parseRepoUrl(repoUrl);
        String owner = parts[0];
        String repo = parts[1];
        String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo;

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(apiUrl, HttpMethod.GET, getHeaders(), new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> data = response.getBody();
            if (data == null) throw new GithubApiException("No data found for repository: " + repoUrl);

            RepositoryInfo info = new RepositoryInfo();
            info.setName((String) data.get("name"));
            info.setDescription((String) data.get("description"));
            info.setStars((Integer) data.get("stargazers_count"));
            info.setForks((Integer) data.get("forks_count"));
            info.setWatchers((Integer) data.get("watchers_count"));
            info.setLanguage((String) data.get("language"));

            @SuppressWarnings("unchecked")
            Map<String, Object> ownerData = (Map<String, Object>) data.get("owner");
            if (ownerData != null) {
                info.setOwner((String) ownerData.get("login"));
            }
            return info;
        } catch (Exception e) {
            log.error("Error fetching repository: {}", repoUrl, e);
            throw new GithubApiException("Failed to fetch repository info from GitHub API");
        }
    }

    @Cacheable(value = "contributors", key = "#p0")
    public List<Contributor> getContributors(String repoUrl) {
        String[] parts = parseRepoUrl(repoUrl);
        String owner = parts[0];
        String repo = parts[1];
        String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/contributors";

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(apiUrl, HttpMethod.GET, getHeaders(), new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {});
            List<Map<String, Object>> data = response.getBody();
            if (data == null) return Collections.emptyList();

            return data.stream().limit(10).map(c -> {
                Contributor contributor = new Contributor();
                contributor.setLogin((String) c.get("login"));
                contributor.setAvatarUrl((String) c.get("avatar_url"));
                contributor.setContributions((Integer) c.get("contributions"));
                return contributor;
            }).toList();
        } catch (Exception e) {
            log.warn("Error fetching contributors for {}", repoUrl);
            return Collections.emptyList();
        }
    }

    @Cacheable(value = "commitActivity", key = "#p0")
    public List<CommitActivity> getCommitActivity(String repoUrl) {
        String[] parts = parseRepoUrl(repoUrl);
        String owner = parts[0];
        String repo = parts[1];
        String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/stats/commit_activity";

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(apiUrl, HttpMethod.GET, getHeaders(), new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {});
            List<Map<String, Object>> data = response.getBody();
            if (data == null) return Collections.emptyList();

            return data.stream().map(c -> {
                CommitActivity activity = new CommitActivity();
                activity.setWeek((Integer) c.get("week"));
                activity.setTotal((Integer) c.get("total"));
                return activity;
            }).toList();
        } catch (Exception e) {
            log.warn("Commit activity not available for {}", repoUrl);
            return Collections.emptyList();
        }
    }

    @Cacheable(value = "languages", key = "#p0")
    public List<LanguageStats> getLanguages(String repoUrl) {
        String[] parts = parseRepoUrl(repoUrl);
        String owner = parts[0];
        String repo = parts[1];
        String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/languages";

        try {
            ResponseEntity<Map<String, Integer>> response = restTemplate.exchange(apiUrl, HttpMethod.GET, getHeaders(), new org.springframework.core.ParameterizedTypeReference<Map<String, Integer>>() {});
            Map<String, Integer> data = response.getBody();
            if (data == null) return Collections.emptyList();

            double total = data.values().stream().mapToDouble(Number::doubleValue).sum();

            return data.entrySet().stream().map(entry -> {
                LanguageStats stat = new LanguageStats();
                stat.setLanguage(entry.getKey());
                stat.setPercentage((entry.getValue() / total) * 100);
                return stat;
            }).toList();
        } catch (Exception e) {
            log.warn("Language stats not available for {}", repoUrl);
            return Collections.emptyList();
        }
    }

    @Cacheable(value = "userRepos", key = "#p0")
    public List<UserRepository> getUserRepositories(String username) {
        String apiUrl = "https://api.github.com/users/" + username + "/repos";
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(apiUrl, HttpMethod.GET, getHeaders(), new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {});
            List<Map<String, Object>> responseList = response.getBody();
            if (responseList == null) return Collections.emptyList();

            return responseList.stream().map(this::mapToUserRepository).toList();
        } catch (Exception e) {
            log.error("Error fetching user repositories: {}", username, e);
            throw new GithubApiException("Failed to fetch user repositories from GitHub API");
        }
    }

    @Cacheable(value = "issueSearch", key = "#p0")
    public List<IssueInfo> searchIssues(String query) {
        String apiUrl = "https://api.github.com/search/issues?q=" + query + "&sort=created&order=desc";
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    getHeaders(),
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("items")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
                return items.stream().map(this::mapToIssueInfo).toList();
            }
        } catch (Exception e) {
            log.error("Error searching issues with query {}: {}", query, e.getMessage());
        }
        return List.of();
    }

    private IssueInfo mapToIssueInfo(Map<String, Object> map) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> labelsMap = (List<Map<String, Object>>) map.get("labels");
        List<String> labels = labelsMap.stream()
                .map(l -> (String) l.get("name"))
                .toList();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) map.get("user");
        IssueInfo.UserInfo userInfo = new IssueInfo.UserInfo(
                (String) userMap.get("login"),
                (String) userMap.get("avatar_url")
        );

        // Extract repo name from repository_url: https://api.github.com/repos/owner/repo
        String repoUrl = (String) map.get("repository_url");
        String repoName = repoUrl != null ? repoUrl.substring(repoUrl.lastIndexOf("repos/") + 6) : "Unknown";

        return IssueInfo.builder()
                .title((String) map.get("title"))
                .htmlUrl((String) map.get("html_url"))
                .repositoryUrl(repoUrl)
                .repositoryName(repoName)
                .state((String) map.get("state"))
                .labels(labels)
                .createdAt((String) map.get("created_at"))
                .user(userInfo)
                .build();
    }

    public List<UserRepository> getTopRepositories(String username) {
        List<UserRepository> repos = new ArrayList<>(getUserRepositories(username));
        repos.sort((a, b) -> Integer.compare(b.getStars(), a.getStars()));
        return repos.stream().limit(5).toList();
    }

    private UserRepository mapToUserRepository(Map<String, Object> repo) {
        UserRepository r = new UserRepository();
        r.setName((String) repo.get("name"));
        r.setStars((Integer) repo.get("stargazers_count"));
        r.setForks((Integer) repo.get("forks_count"));
        r.setLanguage((String) repo.get("language"));
        r.setUrl((String) repo.get("html_url"));
        return r;
    }

    // Async wrappers for parallelism
    @Async
    public CompletableFuture<RepositoryInfo> getRepositoryAsync(String repoUrl) {
        return CompletableFuture.completedFuture(getRepository(repoUrl));
    }

    @Async
    public CompletableFuture<List<Contributor>> getContributorsAsync(String repoUrl) {
        return CompletableFuture.completedFuture(getContributors(repoUrl));
    }

    @Async
    public CompletableFuture<List<CommitActivity>> getCommitActivityAsync(String repoUrl) {
        return CompletableFuture.completedFuture(getCommitActivity(repoUrl));
    }

    @Async
    public CompletableFuture<List<LanguageStats>> getLanguagesAsync(String repoUrl) {
        return CompletableFuture.completedFuture(getLanguages(repoUrl));
    }
}