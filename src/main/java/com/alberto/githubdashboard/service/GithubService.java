package com.alberto.githubdashboard.service;

import com.alberto.githubdashboard.model.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GithubService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${github.token}")
    private String githubToken;

    private HttpEntity<String> getHeaders(){

        HttpHeaders headers = new HttpHeaders();

        headers.set("Authorization", "Bearer " + githubToken);
        headers.set("Accept", "application/vnd.github+json");

        return new HttpEntity<>(headers);
    }

    public RepositoryInfo getRepository(String repoUrl){

        String[] parts = repoUrl.replace("https://github.com/","").split("/");

        String owner = parts[0];
        String repo = parts[1];

        String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo;

        ResponseEntity<Map> response =
                restTemplate.exchange(apiUrl, HttpMethod.GET, getHeaders(), Map.class);

        Map<String,Object> data = response.getBody();

        RepositoryInfo info = new RepositoryInfo();

        if(data == null){
            return info;
        }

        info.setName((String) data.get("name"));
        info.setDescription((String) data.get("description"));
        info.setStars((Integer) data.get("stargazers_count"));
        info.setForks((Integer) data.get("forks_count"));
        info.setWatchers((Integer) data.get("watchers_count"));
        info.setLanguage((String) data.get("language"));

        Map<String,Object> ownerData = (Map<String,Object>) data.get("owner");

        if(ownerData != null){
            info.setOwner((String) ownerData.get("login"));
        }

        return info;
    }

    public List<Contributor> getContributors(String repoUrl){

        try{

            String[] parts = repoUrl.replace("https://github.com/","").split("/");

            String owner = parts[0];
            String repo = parts[1];

            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/contributors";

            ResponseEntity<List> response =
                    restTemplate.exchange(apiUrl, HttpMethod.GET, getHeaders(), List.class);

            List<Map<String,Object>> data = response.getBody();

            List<Contributor> contributors = new ArrayList<>();

            if(data == null){
                return contributors;
            }

            int limit = Math.min(data.size(),10);

            for(int i = 0; i < limit; i++){

                Map<String,Object> c = data.get(i);

                Contributor contributor = new Contributor();

                contributor.setLogin((String) c.get("login"));
                contributor.setAvatarUrl((String) c.get("avatar_url"));
                contributor.setContributions((Integer) c.get("contributions"));

                contributors.add(contributor);
            }

            return contributors;

        }catch(Exception e){

            System.out.println("Error fetching contributors");

            return new ArrayList<>();
        }
    }

    public List<CommitActivity> getCommitActivity(String repoUrl){

        try{

            String[] parts = repoUrl.replace("https://github.com/","").split("/");

            String owner = parts[0];
            String repo = parts[1];

            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/stats/commit_activity";

            ResponseEntity<List> response =
                    restTemplate.exchange(apiUrl, HttpMethod.GET, getHeaders(), List.class);

            List<Map<String,Object>> data = response.getBody();

            List<CommitActivity> commits = new ArrayList<>();

            if(data == null){
                return commits;
            }

            for(Map<String,Object> c : data){

                CommitActivity activity = new CommitActivity();

                activity.setWeek((Integer) c.get("week"));
                activity.setTotal((Integer) c.get("total"));

                commits.add(activity);
            }

            return commits;

        }catch(Exception e){

            System.out.println("Commit activity not available");

            return new ArrayList<>();
        }
    }

    public List<LanguageStats> getLanguages(String repoUrl){

        try{

            String[] parts = repoUrl.replace("https://github.com/","").split("/");

            String owner = parts[0];
            String repo = parts[1];

            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/languages";

            ResponseEntity<Map> response =
                    restTemplate.exchange(apiUrl, HttpMethod.GET, getHeaders(), Map.class);

            Map<String,Integer> data = response.getBody();

            List<LanguageStats> languages = new ArrayList<>();

            if(data == null){
                return languages;
            }

            double total = data.values()
                    .stream()
                    .mapToDouble(Number::doubleValue)
                    .sum();

            for(Map.Entry<String,Integer> entry : data.entrySet()){

                LanguageStats stat = new LanguageStats();

                stat.setLanguage(entry.getKey());
                stat.setPercentage((entry.getValue()/total)*100);

                languages.add(stat);
            }

            return languages;

        }catch(Exception e){

            System.out.println("Language stats not available");

            return new ArrayList<>();
        }
    }

    public List<UserRepository> getUserRepositories(String username){

        String apiUrl = "https://api.github.com/users/" + username + "/repos";

        List<Map<String,Object>> response =
                restTemplate.getForObject(apiUrl, List.class);

        List<UserRepository> repositories = new ArrayList<>();

        if(response == null){
            return repositories;
        }

        for(Map<String,Object> repo : response){

            UserRepository r = new UserRepository();

            r.setName((String) repo.get("name"));
            r.setStars((Integer) repo.get("stargazers_count"));
            r.setForks((Integer) repo.get("forks_count"));
            r.setLanguage((String) repo.get("language"));
            r.setUrl((String) repo.get("html_url"));

            repositories.add(r);
        }

        return repositories;
    }

}