package com.alberto.githubdashboard.controller;

import com.alberto.githubdashboard.model.*;
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

        try {

            String cleanUrl = repoUrl.replace("https://github.com/", "").trim();

            // quitar slash final
            if(cleanUrl.endsWith("/")){
                cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 1);
            }

            String[] parts = cleanUrl.split("/");

            // 🔥 CASO 1: USUARIO
            if(parts.length == 1){

                List<UserRepository> userRepos = githubService.getUserRepositories(parts[0]);
                List<UserRepository> topRepos = githubService.getTopRepositories(parts[0]);

                model.addAttribute("userRepos", userRepos);
                model.addAttribute("topRepos", topRepos);

                return "index";
            }

            // 🔥 CASO 2: REPOSITORIO
            if(parts.length >= 2){

                RepositoryInfo repo = githubService.getRepository(repoUrl);
                List<Contributor> contributors = githubService.getContributors(repoUrl);
                List<CommitActivity> commits = githubService.getCommitActivity(repoUrl);
                List<LanguageStats> languages = githubService.getLanguages(repoUrl);

                model.addAttribute("repo", repo);
                model.addAttribute("contributors", contributors);
                model.addAttribute("commits", commits);
                model.addAttribute("languages", languages);

                return "index";
            }

        } catch (Exception e) {

            model.addAttribute("error", "Invalid GitHub URL");

        }

        return "index";
    }
}