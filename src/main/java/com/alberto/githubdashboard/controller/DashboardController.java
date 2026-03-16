package com.alberto.githubdashboard.controller;

import com.alberto.githubdashboard.model.RepositoryInfo;
import com.alberto.githubdashboard.model.Contributor;
import com.alberto.githubdashboard.model.CommitActivity;
import com.alberto.githubdashboard.model.LanguageStats;
import com.alberto.githubdashboard.model.UserRepository;
import com.alberto.githubdashboard.service.GithubService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private GithubService githubService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam("repoUrl") String repoUrl, Model model) {

        RepositoryInfo repo = null;
        List<Contributor> contributors = null;
        List<CommitActivity> commits = null;
        List<LanguageStats> languages = null;
        List<UserRepository> userRepos = null;

        try {

            String cleanUrl = repoUrl.replace("https://github.com/", "");
            String[] parts = cleanUrl.split("/");

            // Caso 1: URL de usuario
            if(parts.length == 1){

                userRepos = githubService.getUserRepositories(parts[0]);
                model.addAttribute("userRepos", userRepos);

            }

            // Caso 2: URL de repositorio
            if(parts.length >= 2){

                repo = githubService.getRepository(repoUrl);
                contributors = githubService.getContributors(repoUrl);
                commits = githubService.getCommitActivity(repoUrl);
                languages = githubService.getLanguages(repoUrl);

                model.addAttribute("repo", repo);
                model.addAttribute("contributors", contributors);
                model.addAttribute("commits", commits);
                model.addAttribute("languages", languages);
            }

        } catch (Exception e) {

            model.addAttribute("error", "Invalid GitHub URL");

        }

        return "index";
    }
}