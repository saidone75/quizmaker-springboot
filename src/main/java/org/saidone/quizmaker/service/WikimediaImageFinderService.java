package org.saidone.quizmaker.service;

import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class WikimediaImageFinderService {

    private static final String COMMONS_API = "https://commons.wikimedia.org/w/api.php";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]+>");
    private static final Pattern MULTISPACE = Pattern.compile("\\s+");

    private final HttpClient httpClient;
    private final ZooModel<String, float[]> embeddingModel;

    public WikimediaImageFinderService(ZooModel<String, float[]> embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public ImageResult findMostRelevantImage(String[] keywords)
            throws IOException, InterruptedException, TranslateException {

        List<String> cleanedKeywords = Arrays.stream(
                        Optional.ofNullable(keywords).orElse(new String[0]))
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(WikimediaImageFinderService::normalize)
                .distinct()
                .toList();

        if (cleanedKeywords.isEmpty()) {
            throw new IllegalArgumentException("Nessuna keyword valida");
        }

        List<String> searchQueries = buildSearchQueries(cleanedKeywords);

        Map<String, Candidate> dedup = new LinkedHashMap<>();
        for (String query : searchQueries) {
            List<String> titles = searchFileTitles(query, 20);
            List<Candidate> details = fetchImageDetails(titles);
            for (Candidate c : details) {
                dedup.putIfAbsent(c.title, c);
            }
        }

        if (dedup.isEmpty()) {
            return null;
        }

        List<Candidate> candidates = new ArrayList<>(dedup.values());

        for (Candidate c : candidates) {
            c.lexicalScore = lexicalScore(c, cleanedKeywords);
        }

        String queryText = String.join(" ", cleanedKeywords);

        // Predictor creato per richiesta: più semplice e sicuro.
        try (Predictor<String, float[]> predictor = embeddingModel.newPredictor()) {
            float[] queryEmbedding = predictor.predict(queryText);

            for (Candidate c : candidates) {
                float[] candEmbedding = predictor.predict(c.semanticText);
                c.semanticScore = cosineSimilarity(queryEmbedding, candEmbedding);
                c.totalScore = 0.60 * c.lexicalScore + 0.40 * normalizeSemantic(c.semanticScore);
                c.rationale = buildRationale(c, cleanedKeywords);
            }
        }

        candidates.sort(Comparator.comparingDouble((Candidate c) -> c.totalScore).reversed());
        Candidate best = candidates.get(0);

        return new ImageResult(
                best.title,
                best.pageUrl,
                best.imageUrl,
                best.thumbnailUrl,
                best.mime,
                best.lexicalScore,
                best.semanticScore,
                best.totalScore,
                best.rationale
        );
    }

    private List<String> buildSearchQueries(List<String> keywords) {
        String joined = String.join(" ", keywords);

        String titleBoost = keywords.stream()
                .limit(2)
                .map(k -> "intitle:" + quoteIfNeeded(k))
                .collect(Collectors.joining(" "));

        List<String> queries = new ArrayList<>();
        queries.add("filemime:image " + titleBoost + " " + joined);
        queries.add("filemime:image \"" + joined + "\"");
        queries.add("filemime:image " + joined);
        return queries;
    }

    private List<String> searchFileTitles(String srsearch, int limit)
            throws IOException, InterruptedException {

        String url = COMMONS_API
                + "?action=query"
                + "&format=json"
                + "&list=search"
                + "&srnamespace=6"
                + "&srlimit=" + limit
                + "&srsearch=" + encode(srsearch);

        JsonNode root = getJson(url);
        JsonNode results = root.path("query").path("search");

        List<String> out = new ArrayList<>();
        if (results.isArray()) {
            for (JsonNode node : results) {
                String title = node.path("title").asText();
                if (title.startsWith("File:")) {
                    out.add(title);
                }
            }
        }
        return out;
    }

    private List<Candidate> fetchImageDetails(List<String> titles)
            throws IOException, InterruptedException {

        if (titles.isEmpty()) {
            return List.of();
        }

        String url = COMMONS_API
                + "?action=query"
                + "&format=json"
                + "&prop=imageinfo|info"
                + "&inprop=url"
                + "&titles=" + encode(String.join("|", titles))
                + "&iiprop=url|mime|extmetadata"
                + "&iiurlwidth=500";

        JsonNode root = getJson(url);
        JsonNode pages = root.path("query").path("pages");

        List<Candidate> out = new ArrayList<>();
        Iterator<JsonNode> it = pages.elements();
        while (it.hasNext()) {
            JsonNode page = it.next();
            JsonNode imageinfo = page.path("imageinfo");
            if (!imageinfo.isArray() || imageinfo.isEmpty()) {
                continue;
            }

            JsonNode ii = imageinfo.get(0);
            JsonNode ext = ii.path("extmetadata");

            Candidate c = new Candidate();
            c.title = page.path("title").asText();
            c.pageUrl = page.path("fullurl").asText("https://commons.wikimedia.org/wiki/" + c.title.replace(" ", "_"));
            c.imageUrl = ii.path("url").asText(null);
            c.thumbnailUrl = ii.path("thumburl").asText(c.imageUrl);
            c.mime = ii.path("mime").asText("");

            String semanticText = String.join(" | ",
                    cleanHtml(safeTitle(c.title)),
                    cleanHtml(meta(ext, "ObjectName")),
                    cleanHtml(meta(ext, "ImageDescription")),
                    cleanHtml(meta(ext, "Categories")),
                    cleanHtml(meta(ext, "Artist")),
                    cleanHtml(meta(ext, "Credit")),
                    cleanHtml(meta(ext, "LicenseShortName"))
            );

            c.normalizedTitle = normalize(safeTitle(c.title));
            c.semanticText = semanticText;

            if (c.imageUrl != null && !c.imageUrl.isBlank()) {
                out.add(c);
            }
        }

        return out;
    }

    private double lexicalScore(Candidate c, List<String> keywords) {
        String title = normalize(safeTitle(c.title));
        String text = normalize(c.semanticText);

        double score = 0.0;
        int matched = 0;

        for (String kw : keywords) {
            boolean inTitle = containsTokenish(title, kw);
            boolean inText = containsTokenish(text, kw);

            if (inTitle) score += 10;
            if (inText) score += 4;
            if (inTitle || inText) matched++;
        }

        if (title.contains(String.join(" ", keywords))) {
            score += 8;
        }
        if (matched == keywords.size()) {
            score += 12;
        }

        if (c.mime.startsWith("image/jpeg") || c.mime.startsWith("image/png") || c.mime.startsWith("image/webp")) {
            score += 2;
        }
        if (c.mime.contains("svg")) {
            score -= 2;
        }

        score -= penaltyIfContains(text, "logo", 8);
        score -= penaltyIfContains(text, "icon", 7);
        score -= penaltyIfContains(text, "diagram", 7);
        score -= penaltyIfContains(text, "coat of arms", 8);
        score -= penaltyIfContains(text, "flag", 6);
        score -= penaltyIfContains(text, "map", 5);

        return Math.max(score, 0.0);
    }

    private String buildRationale(Candidate c, List<String> keywords) {
        List<String> matched = keywords.stream()
                .filter(k -> containsTokenish(normalize(c.semanticText), k)
                        || containsTokenish(c.normalizedTitle, k))
                .toList();

        return "matched=" + matched
                + ", mime=" + c.mime
                + ", lexical=" + round(c.lexicalScore)
                + ", semantic=" + round(c.semanticScore)
                + ", total=" + round(c.totalScore);
    }

    private JsonNode getJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", "WikimediaImageFinder/1.0")
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
        return MAPPER.readTree(response.body());
    }

    private static String meta(JsonNode extmetadata, String key) {
        return extmetadata.path(key).path("value").asText("");
    }

    private static String safeTitle(String title) {
        return title != null && title.startsWith("File:") ? title.substring(5) : Objects.toString(title, "");
    }

    private static String cleanHtml(String s) {
        if (s == null) return "";
        String noTags = HTML_TAGS.matcher(s).replaceAll(" ");
        return MULTISPACE.matcher(noTags).replaceAll(" ").trim();
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ");
        return MULTISPACE.matcher(n).replaceAll(" ").trim();
    }

    private static boolean containsTokenish(String text, String kw) {
        String nText = normalize(text);
        String nKw = normalize(kw);
        return nText.contains(nKw);
    }

    private static double penaltyIfContains(String text, String token, double penalty) {
        return containsTokenish(text, token) ? penalty : 0.0;
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static double normalizeSemantic(double cosine) {
        return (cosine + 1.0) * 50.0; // [-1,1] -> [0,100]
    }

    private static String quoteIfNeeded(String s) {
        return s.contains(" ") ? "\"" + s + "\"" : s;
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static double round(double d) {
        return Math.round(d * 1000.0) / 1000.0;
    }

    private static class Candidate {
        String title;
        String pageUrl;
        String imageUrl;
        String thumbnailUrl;
        String mime;
        String normalizedTitle;
        String semanticText;
        double lexicalScore;
        double semanticScore;
        double totalScore;
        String rationale;
    }

    public record ImageResult(
            String title,
            String pageUrl,
            String imageUrl,
            String thumbnailUrl,
            String mime,
            double lexicalScore,
            double semanticScore,
            double totalScore,
            String rationale
    ) {}
}