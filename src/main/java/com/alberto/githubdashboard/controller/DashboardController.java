package com.alberto.githubdashboard.controller;

import com.alberto.githubdashboard.model.RepositoryInfo;
import com.alberto.githubdashboard.service.GithubService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class DashboardController {

    private final GithubService githubService;

    public DashboardController(GithubService githubService) {
        this.githubService = githubService;
    }

    @GetMapping("/")
    public String home(){

        return "index";
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam String repoUrl, Model model){

        RepositoryInfo repo = githubService.getRepository(repoUrl);

        var contributors = githubService.getContributors(repoUrl);
        var commits = githubService.getCommitActivity(repoUrl);
        var languages = githubService.getLanguages(repoUrl);

        model.addAttribute("repo", repo);
        model.addAttribute("contributors", contributors);
        model.addAttribute("commits", commits);
        model.addAttribute("languages", languages);

        System.out.println(commits);
        System.out.println(languages);

        return "index";
    }

}