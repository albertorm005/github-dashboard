package com.alberto.githubdashboard.controller;

import com.alberto.githubdashboard.model.*;
import com.alberto.githubdashboard.service.GithubService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.List;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.element.*;

@Controller
public class DashboardController {

    @Autowired
    private GithubService githubService;

    // 🧠 HISTORIAL EN MEMORIA
    private List<String> searchHistory = new ArrayList<>();

    // =============================
    // HOME
    // =============================
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // =============================
    // ANALYZE POST
    // =============================
    @PostMapping("/analyze")
    public String analyze(@RequestParam("repoUrl") String repoUrl, Model model) {

        return processAnalysis(repoUrl, model);
    }

    // =============================
    // ANALYZE GET (para historial)
    // =============================
    @GetMapping("/analyze")
    public String analyzeGet(@RequestParam("repoUrl") String repoUrl, Model model) {

        return processAnalysis(repoUrl, model);
    }

    // =============================
    // LÓGICA CENTRAL
    // =============================
    private String processAnalysis(String repoUrl, Model model) {

        try {

            String cleanUrl = repoUrl.replace("https://github.com/", "").trim();

            if (cleanUrl.endsWith("/")) {
                cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 1);
            }

            String[] parts = cleanUrl.split("/");

            // 🔥 guardar URL
            model.addAttribute("searchedUrl", repoUrl);

            // 🔥 guardar historial
            searchHistory.add(repoUrl);
            model.addAttribute("history", searchHistory);

            // =============================
            // 👤 USUARIO
            // =============================
            if (parts.length == 1) {

                List<UserRepository> userRepos = githubService.getUserRepositories(parts[0]);
                List<UserRepository> topRepos = githubService.getTopRepositories(parts[0]);

                model.addAttribute("userRepos", userRepos);
                model.addAttribute("topRepos", topRepos);

                return "index";
            }

            // =============================
            // 📦 REPOSITORIO
            // =============================
            if (parts.length >= 2) {

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

    // =============================
    // 📥 JSON
    // =============================
    @GetMapping("/download")
    @ResponseBody
    public Map<String, Object> downloadJson(@RequestParam String repoUrl) {

        Map<String, Object> data = new HashMap<>();

        try {
            data.put("repository", githubService.getRepository(repoUrl));
            data.put("contributors", githubService.getContributors(repoUrl));
            data.put("commits", githubService.getCommitActivity(repoUrl));
            data.put("languages", githubService.getLanguages(repoUrl));
        } catch (Exception e) {
            data.put("error", "Error fetching data");
        }

        return data;
    }

    // =============================
    // 📄 PDF
    // =============================
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
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Name: " + repo.getName()));
            document.add(new Paragraph("Description: " + repo.getDescription()));
            document.add(new Paragraph("Stars: " + repo.getStars()));
            document.add(new Paragraph("Forks: " + repo.getForks()));
            document.add(new Paragraph("Watchers: " + repo.getWatchers()));

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Top Contributors:").setBold());

            for (int i = 0; i < Math.min(5, contributors.size()); i++) {
                Contributor c = contributors.get(i);

                document.add(new Paragraph(
                        c.getLogin() + " - " + c.getContributions() + " contributions"
                ));
            }

            document.close();

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(out.toByteArray());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}