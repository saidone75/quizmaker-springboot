/*
 * Alice's Simple Quiz Maker - fun quizzes for curious minds
 * Copyright (C) 2026 Miss Alice & Saidone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.saidone.quizmaker.service;

import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
@RequiredArgsConstructor
@Slf4j
public class WikimediaImageFinderService implements WikimediaImageSearchService {

    private static final String COMMONS_API = "https://commons.wikimedia.org/w/api.php";
    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]+>");
    private static final Pattern MULTISPACE = Pattern.compile("\\s+");
    private static final Set<String> UNSUPPORTED_FILE_EXTENSIONS = Set.of(".djvu", ".djv");

    private final HttpClient httpClient;
    private final ZooModel<String, float[]> embeddingModel;
    private final ObjectMapper objectMapper;

    @Override
    public String searchImage(String[] keywords) {
        try {
            return findMostRelevantImage(keywords);
        } catch (Exception e) {
            log.warn("Errore durante la ricerca immagine semantica su Wikimedia: {}", e.getMessage());
            return null;
        }
    }

    public String findMostRelevantImage(String[] keywords)
            throws IOException, InterruptedException, TranslateException {
        val result = findMostRelevantImageResult(keywords);
        return result == null ? null : result.imageUrl;
    }

    private ImageResult findMostRelevantImageResult(String[] keywords)
            throws IOException, InterruptedException, TranslateException {
        long startedAt = System.nanoTime();

        val cleanedKeywords = Arrays.stream(
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

        log.info("Avvio ricerca immagine Wikimedia | originalKeywords={} | cleanedKeywords={} | count={}",
                Arrays.toString(Optional.ofNullable(keywords).orElse(new String[0])),
                cleanedKeywords,
                cleanedKeywords.size());

        val dedup = new LinkedHashMap<String, Candidate>();
        val queryBatches = buildSearchQueryBatches(cleanedKeywords);
        log.debug("Strategie query (ordine esecuzione): {}",
                queryBatches.stream()
                        .flatMap(List::stream)
                        .map(q -> String.format("%s=%s", q.label, q.query))
                        .toList());

        for (int batchIndex = 0; batchIndex < queryBatches.size(); batchIndex++) {
            val batch = queryBatches.get(batchIndex);
            val batchStart = System.nanoTime();
            val dedupBeforeBatch = dedup.size();

            for (int i = 0; i < batch.size(); i++) {
                val querySpec = batch.get(i);
                val queryStart = System.nanoTime();
                log.debug("Eseguo query [{}]: {}", querySpec.label, querySpec.query);
                val titles = searchFileTitles(querySpec.query, 20);
                log.debug("Query [{}] ({}/{}) ha prodotto {} titoli: {}",
                        querySpec.label, i + 1, batch.size(), titles.size(), titles);
                val details = fetchImageDetails(titles);
                val dedupBeforeQuery = dedup.size();
                for (val c : details) {
                    dedup.putIfAbsent(c.title, c);
                }
                long elapsedMs = Duration.ofNanos(System.nanoTime() - queryStart).toMillis();
                log.debug("Query [{}] ha prodotto {} candidati, nuovi={}, deduplicatiTotali={}, elapsedMs={}",
                        querySpec.label, details.size(), dedup.size() - dedupBeforeQuery, dedup.size(), elapsedMs);
            }

            long batchElapsedMs = Duration.ofNanos(System.nanoTime() - batchStart).toMillis();
            int newInBatch = dedup.size() - dedupBeforeBatch;
            log.debug("Batch query {} completato: nuoviCandidati={}, deduplicatiTotali={}, elapsedMs={}",
                    batchIndex + 1, newInBatch, dedup.size(), batchElapsedMs);

            if (dedup.isEmpty()) {
                log.debug("Nessun candidato nel batch {}, provo batch successivo più permissivo", batchIndex + 1);
                continue;
            }
            if (batchIndex < queryBatches.size() - 1) {
                log.debug("Interrompo espansione query: trovati {} candidati nel batch {}",
                        dedup.size(), batchIndex + 1);
            }
            break;
        }

        if (dedup.isEmpty()) {
            log.info("Nessuna immagine candidata trovata per keyword: {}", cleanedKeywords);
            return null;
        }

        val candidates = new ArrayList<>(dedup.values());

        for (val c : candidates) {
            c.lexicalScore = lexicalScore(c, cleanedKeywords);
        }
        log.debug("Calcolato lexicalScore per {} candidati", candidates.size());

        val queryText = String.join(" ", cleanedKeywords);
        log.debug("Testo query per embedding: '{}'", queryText);

        // Predictor creato per richiesta: più semplice e sicuro
        try (val predictor = embeddingModel.newPredictor()) {
            float[] queryEmbedding = predictor.predict(queryText);
            for (val c : candidates) {
                float[] candEmbedding = predictor.predict(c.semanticText);
                c.semanticScore = cosineSimilarity(queryEmbedding, candEmbedding);
                c.totalScore = 0.60 * c.lexicalScore + 0.40 * normalizeSemantic(c.semanticScore);
                c.rationale = buildRationale(c, cleanedKeywords);
            }
        }

        candidates.sort(Comparator.comparingDouble((Candidate c) -> c.totalScore).reversed());
        val best = candidates.getFirst();
        val topCandidates = candidates.stream()
                .limit(3)
                .map(c -> String.format(
                        "%s [total=%s, lexical=%s, semantic=%s, mime=%s]",
                        c.title,
                        round(c.totalScore),
                        round(c.lexicalScore),
                        round(c.semanticScore),
                        c.mime
                ))
                .toList();
        val totalMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
        log.info("Miglior immagine trovata | title={} | imageUrl={} | totalScore={} | elapsedMs={}",
                best.title, best.imageUrl, round(best.totalScore), totalMs);
        log.debug("Top 3 candidati: {}", topCandidates);
        log.debug("Rationale migliore candidato: {}", best.rationale);

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

    List<List<SearchQuerySpec>> buildSearchQueryBatches(List<String> keywords) {
        val joined = String.join(" ", keywords);
        val andJoined = String.join(" AND ", keywords);
        val orJoined = String.join(" OR ", keywords);

        val titleOnly = keywords.stream()
                .map(k -> String.format("intitle:%s", quoteIfNeeded(k)))
                .collect(Collectors.joining(" "));

        val titleBoost = keywords.stream()
                .limit(2)
                .map(k -> String.format("intitle:%s", quoteIfNeeded(k)))
                .collect(Collectors.joining(" "));

        val strict = List.of(
                new SearchQuerySpec("and", String.format("filemime:image %s", andJoined)),
                new SearchQuerySpec("title", String.format("filemime:image %s", titleOnly)),
                new SearchQuerySpec("title-boost", String.format("filemime:image %s %s", titleBoost, joined))
        );
        val medium = List.of(
                new SearchQuerySpec("phrase", String.format("filemime:image \"%s\"", joined)),
                new SearchQuerySpec("plain", String.format("filemime:image %s", joined))
        );
        val broad = List.of(
                new SearchQuerySpec("or", String.format("filemime:image %s", orJoined))
        );
        return List.of(strict, medium, broad);
    }

    private List<String> searchFileTitles(String srsearch, int limit)
            throws IOException, InterruptedException {

        val url = String.format(
                "%s?action=query&format=json&list=search&srnamespace=6&srlimit=%s&srsearch=%s",
                COMMONS_API,
                limit,
                encode(srsearch)
        );
        log.trace("Chiamata searchFileTitles URL: {}", url);

        val root = getJson(url);
        val results = root.path("query").path("search");

        val out = new ArrayList<String>();
        if (results.isArray()) {
            for (val node : results) {
                val title = node.path("title").asText();
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

        val url = String.format(
                "%s?action=query&format=json&prop=imageinfo%%7Cinfo&inprop=url&titles=%s&iiprop=url%%7Cmime%%7Cextmetadata&iiurlwidth=500",
                COMMONS_API,
                encode(String.join("|", titles))
        );
        log.trace("Chiamata fetchImageDetails URL: {}", url);

        val root = getJson(url);
        val pages = root.path("query").path("pages");

        val out = new ArrayList<Candidate>();
        val it = pages.elements();
        while (it.hasNext()) {
            val page = it.next();
            val imageinfo = page.path("imageinfo");
            if (!imageinfo.isArray() || imageinfo.isEmpty()) {
                continue;
            }

            val ii = imageinfo.get(0);
            val ext = ii.path("extmetadata");

            val c = new Candidate();
            c.title = page.path("title").asText();
            c.pageUrl = page.path("fullurl").asText(
                    String.format("https://commons.wikimedia.org/wiki/%s", c.title.replace(" ", "_"))
            );
            c.imageUrl = ii.path("url").asText(null);
            c.thumbnailUrl = ii.path("thumburl").asText(c.imageUrl);
            c.mime = ii.path("mime").asText("");

            if (isUnsupportedMedia(c.title, c.mime, c.imageUrl)) {
                log.debug("Scarto file non supportato: title={}, mime={}, imageUrl={}", c.title, c.mime, c.imageUrl);
                continue;
            }

            val semanticText = String.join(" | ",
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
        val title = normalize(safeTitle(c.title));
        val text = normalize(c.semanticText);

        double score = 0.0;
        int matched = 0;

        for (val kw : keywords) {
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
        val matched = keywords.stream()
                .filter(k -> containsTokenish(normalize(c.semanticText), k)
                        || containsTokenish(c.normalizedTitle, k))
                .toList();

        return String.format(
                "matched=%s, mime=%s, lexical=%s, semantic=%s, total=%s",
                matched,
                c.mime,
                round(c.lexicalScore),
                round(c.semanticScore),
                round(c.totalScore)
        );
    }

    private JsonNode getJson(String url) throws IOException, InterruptedException {
        val request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", "WikimediaImageFinder/1.0")
                .timeout(Duration.ofSeconds(30))
                .build();

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
        return objectMapper.readTree(response.body());
    }

    private static String meta(JsonNode extmetadata, String key) {
        return extmetadata.path(key).path("value").asText("");
    }

    private static String safeTitle(String title) {
        return title != null && title.startsWith("File:") ? title.substring(5) : Objects.toString(title, "");
    }

    private static String cleanHtml(String s) {
        if (s == null) return "";
        val noTags = HTML_TAGS.matcher(s).replaceAll(" ");
        return MULTISPACE.matcher(noTags).replaceAll(" ").trim();
    }

    private static String normalize(String s) {
        if (s == null) return "";
        val n = Normalizer.normalize(s, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ");
        return MULTISPACE.matcher(n).replaceAll(" ").trim();
    }

    private static boolean containsTokenish(String text, String kw) {
        val nText = normalize(text);
        val nKw = normalize(kw);
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
        return s.contains(" ") ? String.format("\"%s\"", s) : s;
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static double round(double d) {
        return Math.round(d * 1000.0) / 1000.0;
    }

    static boolean isUnsupportedMedia(String title, String mime, String imageUrl) {
        val loweredMime = Objects.toString(mime, "").toLowerCase(Locale.ROOT);
        if (loweredMime.contains("djvu") || loweredMime.contains("vnd.djvu")) {
            return true;
        }

        val titleOrUrl = (Objects.toString(title, "") + " " + Objects.toString(imageUrl, ""))
                .toLowerCase(Locale.ROOT);
        return UNSUPPORTED_FILE_EXTENSIONS.stream().anyMatch(titleOrUrl::contains);
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

    record SearchQuerySpec(String label, String query) {
    }

    private record ImageResult(
            String title,
            String pageUrl,
            String imageUrl,
            String thumbnailUrl,
            String mime,
            double lexicalScore,
            double semanticScore,
            double totalScore,
            String rationale
    ) {
    }
}
