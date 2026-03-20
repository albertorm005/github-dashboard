package com.alberto.githubdashboard.controller;

import com.alberto.githubdashboard.model.CommitActivity;
import com.alberto.githubdashboard.model.Contributor;
import com.alberto.githubdashboard.model.IssueInfo;
import com.alberto.githubdashboard.model.LanguageStats;
import com.alberto.githubdashboard.model.RepositoryInfo;
import com.alberto.githubdashboard.service.GithubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final GithubService githubService;

    // 🧠 HISTORIAL EN MEMORIA (Limited to 10 items)
    private final List<String> searchHistory = Collections.synchronizedList(new LinkedList<>());

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("history", getRecentHistory());
        return "index";
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam("repoUrl") String repoUrl, Model model) {
        return processAnalysis(repoUrl, model);
    }

    @GetMapping("/analyze")
    public String analyzeGet(@RequestParam("repoUrl") String repoUrl, Model model) {
        return processAnalysis(repoUrl, model);
    }

    private String processAnalysis(String repoUrl, Model model) {
        addToHistory(repoUrl);
        model.addAttribute("searchedUrl", repoUrl);
        model.addAttribute("history", getRecentHistory());

        try {
            String cleanPath = repoUrl.replace("https://github.com/", "").trim();
            if (cleanPath.endsWith("/")) cleanPath = cleanPath.substring(0, cleanPath.length() - 1);
            String[] parts = cleanPath.split("/");

            if (parts.length == 1) {
                // User analysis
                model.addAttribute("userRepos", githubService.getUserRepositories(parts[0]));
                model.addAttribute("topRepos", githubService.getTopRepositories(parts[0]));
            } else if (parts.length >= 2) {
                // Repository analysis (Parallel)
                CompletableFuture<RepositoryInfo> repoFuture = githubService.getRepositoryAsync(repoUrl);
                CompletableFuture<List<Contributor>> contributorsFuture = githubService.getContributorsAsync(repoUrl);
                CompletableFuture<List<CommitActivity>> commitsFuture = githubService.getCommitActivityAsync(repoUrl);
                CompletableFuture<List<LanguageStats>> languagesFuture = githubService.getLanguagesAsync(repoUrl);

                CompletableFuture.allOf(repoFuture, contributorsFuture, commitsFuture, languagesFuture).join();

                model.addAttribute("repo", repoFuture.get());
                model.addAttribute("contributors", contributorsFuture.get());
                model.addAttribute("commits", commitsFuture.get());
                model.addAttribute("languages", languagesFuture.get());
            }
        } catch (Exception e) {
            log.error("Error during analysis of {}", repoUrl, e);
            model.addAttribute("error", "Error analyzing GitHub data: " + e.getMessage());
        }

        return "index";
    }

    @GetMapping("/download")
    @ResponseBody
    public Map<String, Object> downloadJson(@RequestParam String repoUrl) {
        Map<String, Object> data = new HashMap<>();
        try {
            CompletableFuture<RepositoryInfo> repoFuture = githubService.getRepositoryAsync(repoUrl);
            CompletableFuture<List<Contributor>> contributorsFuture = githubService.getContributorsAsync(repoUrl);
            CompletableFuture<List<CommitActivity>> commitsFuture = githubService.getCommitActivityAsync(repoUrl);
            CompletableFuture<List<LanguageStats>> languagesFuture = githubService.getLanguagesAsync(repoUrl);

            CompletableFuture.allOf(repoFuture, contributorsFuture, commitsFuture, languagesFuture).join();

            data.put("repository", repoFuture.get());
            data.put("contributors", contributorsFuture.get());
            data.put("commits", commitsFuture.get());
            data.put("languages", languagesFuture.get());
        } catch (Exception e) {
            data.put("error", "Error fetching data: " + e.getMessage());
        }
        return data;
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf(@RequestParam String repoUrl) {
        try {
            RepositoryInfo repo = githubService.getRepository(repoUrl);
            List<Contributor> contributors = githubService.getContributors(repoUrl);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("GitHub Repository Report").setBold().setFontSize(18));
            document.add(new Paragraph("Repository: " + repo.getName()));
            document.add(new Paragraph("Owner: " + repo.getOwner()));
            document.add(new Paragraph("Description: " + (repo.getDescription() != null ? repo.getDescription() : "N/A")));
            document.add(new Paragraph("Stars: " + repo.getStars() + " | Forks: " + repo.getForks() + " | Watchers: " + repo.getWatchers()));
            document.add(new Paragraph("Main Language: " + (repo.getLanguage() != null ? repo.getLanguage() : "N/A")));

            document.add(new Paragraph("\nTop Contributors:").setBold());
            for (Contributor c : contributors.stream().limit(5).toList()) {
                document.add(new Paragraph(c.getLogin() + " - " + c.getContributions() + " contributions"));
            }

            document.close();

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=report_" + repo.getName() + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(out.toByteArray());
        } catch (Exception e) {
            log.error("Error generating PDF for {}", repoUrl, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/issues")
    public String issues(@RequestParam(value = "label", required = false) String label, Model model) {
        if (label != null && !label.isEmpty()) {
            List<IssueInfo> issues = githubService.searchIssues("label:\"" + label + "\" state:open is:issue");
            model.addAttribute("issues", issues);
            model.addAttribute("currentLabel", label);
        }
        model.addAttribute("searchedLabel", label);
        model.addAttribute("history", searchHistory);
        return "index";
    }

    private void addToHistory(String url) {
        synchronized (searchHistory) {
            searchHistory.remove(url);
            searchHistory.add(0, url);
            if (searchHistory.size() > 10) {
                searchHistory.remove(searchHistory.size() - 1);
            }
        }
    }

    private List<String> getRecentHistory() {
        synchronized (searchHistory) {
            return new ArrayList<>(searchHistory);
        }
    }
}