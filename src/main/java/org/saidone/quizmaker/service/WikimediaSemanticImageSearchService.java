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

import ai.djl.inference.Predictor;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WikimediaSemanticImageSearchService implements WikimediaImageSearchService {

    private static final String COMMONS_API = "https://commons.wikimedia.org/w/api.php";
    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]+>");
    private static final Pattern MULTISPACE = Pattern.compile("\\s+");
    private static final Pattern TOKEN_SPLIT = Pattern.compile("\\s+");
    private static final Set<String> UNSUPPORTED_FILE_EXTENSIONS = Set.of(".djvu", ".djv");
    private static final double PRIMARY_KEYWORD_TITLE_WEIGHT = 1.25;
    private static final double PRIMARY_KEYWORD_TEXT_WEIGHT = 1.15;
    private static final int STRICT_BATCH_LIMIT = 20;
    private static final int MEDIUM_BATCH_LIMIT = 30;
    private static final int BROAD_BATCH_LIMIT = 40;
    private static final int MAX_QUERY_LIMIT = 50;
    private static final int TARGET_CANDIDATE_POOL_SIZE = 60;
    private static final long MAX_EMBEDDING_CACHE_BYTES = 10L * 1024L * 1024L;

    private final HttpClient httpClient;
    private final ZooModel<String, float[]> embeddingModel;
    private final ObjectMapper objectMapper;

    @Override
    public String searchImage(String[] keywords) {
        try {
            return findMostRelevantImage(keywords);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Ricerca immagine semantica su Wikimedia interrotta: {}", e.getMessage());
            return null;
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

        val cleanedKeywords = normalizeKeywords(keywords);
        logSearchStart(keywords, cleanedKeywords);

        val dedup = collectCandidates(cleanedKeywords);
        if (dedup.isEmpty()) {
            log.info("Nessuna immagine candidata trovata per keyword: {}", cleanedKeywords);
            return null;
        }

        val candidates = new ArrayList<>(dedup.values());
        calculateLexicalScores(candidates, cleanedKeywords);
        calculateSemanticScores(candidates, cleanedKeywords);
        return buildResult(candidates, startedAt);
    }

    private List<String> normalizeKeywords(String[] keywords) {
        val cleanedKeywords = Arrays.stream(
                        Optional.ofNullable(keywords).orElse(new String[0]))
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(WikimediaSemanticImageSearchService::normalize)
                .distinct()
                .toList();
        if (cleanedKeywords.isEmpty()) {
            throw new IllegalArgumentException("Nessuna keyword valida");
        }
        return cleanedKeywords;
    }

    private void logSearchStart(String[] keywords, List<String> cleanedKeywords) {
        log.info("Avvio ricerca immagine Wikimedia | originalKeywords={} | cleanedKeywords={} | count={}",
                Arrays.toString(Optional.ofNullable(keywords).orElse(new String[0])),
                cleanedKeywords,
                cleanedKeywords.size());
    }

    private LinkedHashMap<String, Candidate> collectCandidates(List<String> cleanedKeywords)
            throws IOException, InterruptedException {
        val dedup = new LinkedHashMap<String, Candidate>();
        val queryBatches = buildSearchQueryBatches(cleanedKeywords);
        log.debug("Strategie query (ordine esecuzione): {}",
                queryBatches.stream()
                        .flatMap(List::stream)
                        .map(q -> String.format("%s=%s", q.label, q.query))
                        .toList());

        for (int batchIndex = 0; batchIndex < queryBatches.size(); batchIndex++) {
            val batch = queryBatches.get(batchIndex);
            executeBatch(batch, batchIndex, cleanedKeywords.size(), dedup);
            if (!shouldContinueWithNextBatch(dedup, batchIndex, queryBatches.size())) {
                break;
            }
        }
        return dedup;
    }

    private void executeBatch(List<SearchQuerySpec> batch, int batchIndex, int keywordCount, Map<String, Candidate> dedup)
            throws IOException, InterruptedException {
        val batchStart = System.nanoTime();
        val dedupBeforeBatch = dedup.size();

        val queryTasks = new ArrayList<CompletableFuture<QueryExecutionResult>>();
        for (int i = 0; i < batch.size(); i++) {
            val queryIndex = i;
            queryTasks.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return executeQueryAndCollect(batch, batchIndex, queryIndex, keywordCount);
                } catch (IOException e) {
                    throw new CompletionException(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CompletionException(e);
                }
            }));
        }

        for (int i = 0; i < queryTasks.size(); i++) {
            val queryFuture = queryTasks.get(i);
            try {
                val queryResult = queryFuture.join();
                val dedupBeforeQuery = dedup.size();
                for (val c : queryResult.details()) {
                    dedup.putIfAbsent(c.title, c);
                }
                log.debug("Query [{}] ha prodotto {} candidati, nuovi={}, deduplicatiTotali={}, elapsedMs={}",
                        queryResult.querySpec().label(),
                        queryResult.details().size(),
                        dedup.size() - dedupBeforeQuery,
                        dedup.size(),
                        queryResult.elapsedMs());
            } catch (CompletionException e) {
                if (e.getCause() instanceof InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ie;
                }
                if (e.getCause() instanceof IOException ioe) {
                    throw ioe;
                }
                throw e;
            }
        }

        long batchElapsedMs = Duration.ofNanos(System.nanoTime() - batchStart).toMillis();
        int newInBatch = dedup.size() - dedupBeforeBatch;
        log.debug("Batch query {} completato: nuoviCandidati={}, deduplicatiTotali={}, elapsedMs={}",
                batchIndex + 1, newInBatch, dedup.size(), batchElapsedMs);
    }

    private QueryExecutionResult executeQueryAndCollect(List<SearchQuerySpec> batch, int batchIndex, int queryIndex, int keywordCount)
            throws IOException, InterruptedException {
        val querySpec = batch.get(queryIndex);
        val limit = resolveQueryLimit(batchIndex, queryIndex, keywordCount);
        val queryStart = System.nanoTime();
        log.debug("Eseguo query [{}] con srlimit={}: {}", querySpec.label, limit, querySpec.query);
        val titles = searchFileTitles(querySpec.query, limit);
        log.debug("Query [{}] ({}/{}) ha prodotto {} titoli: {}",
                querySpec.label, queryIndex + 1, batch.size(), titles.size(), titles);
        val details = fetchImageDetails(titles);
        long elapsedMs = Duration.ofNanos(System.nanoTime() - queryStart).toMillis();
        return new QueryExecutionResult(querySpec, details, elapsedMs);
    }

    private boolean shouldContinueWithNextBatch(Map<String, Candidate> dedup, int batchIndex, int batchCount) {
        if (batchIndex >= batchCount - 1) {
            return false;
        }
        if (dedup.size() >= TARGET_CANDIDATE_POOL_SIZE) {
            log.debug("Interrompo espansione query: raggiunto target candidati={} nel batch {}",
                    dedup.size(), batchIndex + 1);
            return false;
        }
        log.debug("Proseguo con batch successivo: candidati raccolti={} dopo batch {}",
                dedup.size(), batchIndex + 1);
        return true;
    }

    private void calculateLexicalScores(List<Candidate> candidates, List<String> cleanedKeywords) {
        for (val c : candidates) {
            c.lexicalScore = lexicalScore(c, cleanedKeywords);
        }
        log.debug("Calcolato lexicalScore per {} candidati", candidates.size());
    }

    private void calculateSemanticScores(List<Candidate> candidates, List<String> cleanedKeywords)
            throws TranslateException {
        val queryText = String.join(" ", cleanedKeywords);
        log.debug("Testo query per embedding: '{}'", queryText);

        try (val predictor = embeddingModel.newPredictor()) {
            val embeddingCache = new BoundedEmbeddingCache(MAX_EMBEDDING_CACHE_BYTES);
            float[] queryEmbedding = getOrComputeEmbedding(predictor, embeddingCache, queryText);
            for (val c : candidates) {
                val descriptionText = fallbackDescriptionText(c);
                float[] descriptionEmbedding = getOrComputeEmbedding(predictor, embeddingCache, descriptionText);
                float[] candEmbedding = getOrComputeEmbedding(predictor, embeddingCache, c.semanticText);
                c.descriptionSemanticScore = cosineSimilarity(queryEmbedding, descriptionEmbedding);
                c.semanticScore = cosineSimilarity(queryEmbedding, candEmbedding);
                c.totalScore = computeTotalScore(c);
                c.rationale = buildRationale(c, cleanedKeywords);
            }
        }
    }

    private static float[] getOrComputeEmbedding(Predictor<String, float[]> predictor,
                                                 BoundedEmbeddingCache embeddingCache,
                                                 String text) throws TranslateException {
        val key = Objects.toString(text, "");
        val cached = embeddingCache.get(key);
        if (cached != null) {
            return cached;
        }
        val computed = predictor.predict(key);
        embeddingCache.put(key, computed);
        return computed;
    }

    private ImageResult buildResult(List<Candidate> candidates, long startedAt) {
        candidates.sort(Comparator.comparingDouble((Candidate c) -> c.totalScore).reversed());
        val best = candidates.getFirst();
        val topCandidates = candidates.stream()
                .limit(3)
                .map(c -> String.format(
                        "%s [total=%s, lexical=%s, semanticDesc=%s, semantic=%s, mime=%s]",
                        c.title,
                        round(c.totalScore),
                        round(c.lexicalScore),
                        round(c.descriptionSemanticScore),
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
                best.descriptionSemanticScore,
                best.semanticScore,
                best.totalScore,
                best.rationale
        );
    }

    private static String fallbackDescriptionText(Candidate c) {
        return c.descriptionText == null || c.descriptionText.isBlank()
                ? c.semanticText
                : c.descriptionText;
    }

    private static double computeTotalScore(Candidate c) {
        val descriptionAvailable = hasMeaningfulDescription(c.descriptionText);
        val lexicalWeight = hasStrongLexicalSignal(c.lexicalScore) ? 0.45 : 0.35;
        val descriptionWeight = descriptionAvailable ? (0.20 + (0.20 * descriptionQuality(c.descriptionText))) : 0.0;
        val semanticWeight = 1.0 - lexicalWeight - descriptionWeight;
        return lexicalWeight * c.lexicalScore
                + descriptionWeight * normalizeSemantic(c.descriptionSemanticScore)
                + semanticWeight * normalizeSemantic(c.semanticScore);
    }

    private static boolean hasStrongLexicalSignal(double lexicalScore) {
        return lexicalScore >= 24.0;
    }

    private static double descriptionQuality(String descriptionText) {
        val normalized = normalize(descriptionText);
        if (normalized.isBlank()) {
            return 0.0;
        }
        val tokenCount = normalized.split(" ").length;
        return Math.min(tokenCount, 20) / 20.0;
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

            c.descriptionText = String.join(" | ",
                    cleanHtml(meta(ext, "ObjectName")),
                    cleanHtml(meta(ext, "ImageDescription"))
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

        val keywordMatch = scoreKeywords(title, text, keywords);
        double score = keywordMatch.score()
                + scoreExactTitlePhrase(title, keywords)
                + scoreAllKeywordsMatched(keywordMatch.matchedCount(), keywords.size())
                + scoreMime(c.mime)
                - scorePenaltyTerms(text);

        return Math.max(score, 0.0);
    }

    private KeywordMatchScore scoreKeywords(String title, String text, List<String> keywords) {
        double score = 0.0;
        int matched = 0;

        for (int i = 0; i < keywords.size(); i++) {
            val kw = keywords.get(i);
            val inTitle = containsTokenish(title, kw);
            val inText = containsTokenish(text, kw);
            score += scoreKeywordHit(inTitle, inText, i == 0);
            if (inTitle || inText) {
                matched++;
            }
        }

        return new KeywordMatchScore(score, matched);
    }

    private double scoreKeywordHit(boolean inTitle, boolean inText, boolean primaryKeyword) {
        double score = 0.0;
        if (inTitle) {
            score += primaryKeyword ? 10 * PRIMARY_KEYWORD_TITLE_WEIGHT : 10;
        }
        if (inText) {
            score += primaryKeyword ? 4 * PRIMARY_KEYWORD_TEXT_WEIGHT : 4;
        }
        return score;
    }

    private double scoreExactTitlePhrase(String title, List<String> keywords) {
        return title.contains(String.join(" ", keywords)) ? 8 : 0;
    }

    private double scoreAllKeywordsMatched(int matched, int keywordCount) {
        return matched == keywordCount ? 12 : 0;
    }

    private double scoreMime(String mime) {
        double score = 0.0;
        if (mime.startsWith("image/jpeg") || mime.startsWith("image/png") || mime.startsWith("image/webp")) {
            score += 2;
        }
        if (mime.contains("svg")) {
            score -= 2;
        }
        return score;
    }

    private double scorePenaltyTerms(String text) {
        return penaltyIfContains(text, "logo", 8)
                + penaltyIfContains(text, "icon", 7)
                + penaltyIfContains(text, "diagram", 7)
                + penaltyIfContains(text, "coat of arms", 8)
                + penaltyIfContains(text, "flag", 6)
                + penaltyIfContains(text, "map", 5);
    }

    private String buildRationale(Candidate c, List<String> keywords) {
        val matched = keywords.stream()
                .filter(k -> containsTokenish(normalize(c.semanticText), k)
                        || containsTokenish(c.normalizedTitle, k))
                .toList();

        return String.format(
                "matched=%s, mime=%s, lexical=%s, semanticDescription=%s, semantic=%s, total=%s",
                matched,
                c.mime,
                round(c.lexicalScore),
                round(c.descriptionSemanticScore),
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

    static boolean containsTokenish(String text, String kw) {
        val nText = normalize(text);
        val nKw = normalize(kw);
        if (nText.isBlank() || nKw.isBlank()) {
            return false;
        }
        if (nText.equals(nKw)) {
            return true;
        }

        val textTokens = TOKEN_SPLIT.splitAsStream(nText)
                .filter(token -> !token.isBlank())
                .toList();
        val keywordTokens = TOKEN_SPLIT.splitAsStream(nKw)
                .filter(token -> !token.isBlank())
                .toList();

        if (keywordTokens.isEmpty()) {
            return false;
        }
        if (keywordTokens.size() == 1) {
            return textTokens.contains(keywordTokens.getFirst());
        }
        return containsExactTokenSequence(textTokens, keywordTokens);
    }

    private static boolean containsExactTokenSequence(List<String> textTokens, List<String> keywordTokens) {
        if (keywordTokens.size() > textTokens.size()) {
            return false;
        }
        for (int i = 0; i <= textTokens.size() - keywordTokens.size(); i++) {
            boolean allMatched = true;
            for (int j = 0; j < keywordTokens.size(); j++) {
                if (!textTokens.get(i + j).equals(keywordTokens.get(j))) {
                    allMatched = false;
                    break;
                }
            }
            if (allMatched) {
                return true;
            }
        }
        return false;
    }

    private static double penaltyIfContains(String text, String token, double penalty) {
        return containsTokenish(text, token) ? penalty : 0.0;
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0;
        double na = 0;
        double nb = 0;
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

    private static boolean hasMeaningfulDescription(String descriptionText) {
        val normalized = normalize(descriptionText);
        if (normalized.isBlank()) {
            return false;
        }
        return normalized.split(" ").length >= 3;
    }

    private static String quoteIfNeeded(String s) {
        return s.contains(" ") ? String.format("\"%s\"", s) : s;
    }

    private int resolveQueryLimit(int batchIndex, int queryIndex, int keywordCount) {
        int baseLimit = switch (batchIndex) {
            case 0 -> STRICT_BATCH_LIMIT;
            case 1 -> MEDIUM_BATCH_LIMIT;
            default -> BROAD_BATCH_LIMIT;
        };

        if (keywordCount <= 2) {
            baseLimit += 10;
        }
        if (queryIndex == 0) {
            baseLimit += 5;
        }
        return Math.min(baseLimit, MAX_QUERY_LIMIT);
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
        String descriptionText;
        String semanticText;
        double lexicalScore;
        double descriptionSemanticScore;
        double semanticScore;
        double totalScore;
        String rationale;
    }

    private record KeywordMatchScore(double score, int matchedCount) {
    }

    private static class BoundedEmbeddingCache {
        private final long maxBytes;
        private final LinkedHashMap<String, float[]> cache = new LinkedHashMap<>(16, 0.75f, true);
        private long currentBytes = 0L;

        private BoundedEmbeddingCache(long maxBytes) {
            this.maxBytes = maxBytes;
        }

        private float[] get(String key) {
            return cache.get(key);
        }

        private void put(String key, float[] embedding) {
            if (embedding == null) {
                return;
            }

            val newEntrySize = estimateEntrySize(key, embedding);
            if (newEntrySize > maxBytes) {
                cache.clear();
                currentBytes = 0L;
                return;
            }

            val previous = cache.remove(key);
            if (previous != null) {
                currentBytes -= estimateEntrySize(key, previous);
            }

            cache.put(key, embedding);
            currentBytes += newEntrySize;
            evictIfNeeded();
        }

        private void evictIfNeeded() {
            val iterator = cache.entrySet().iterator();
            while (currentBytes > maxBytes && iterator.hasNext()) {
                val eldest = iterator.next();
                currentBytes -= estimateEntrySize(eldest.getKey(), eldest.getValue());
                iterator.remove();
            }
        }

        private static long estimateEntrySize(String key, float[] embedding) {
            long keyBytes = Objects.toString(key, "").getBytes(StandardCharsets.UTF_8).length;
            long embeddingBytes = (long) embedding.length * Float.BYTES;
            return keyBytes + embeddingBytes + 64L;
        }
    }

    private record QueryExecutionResult(SearchQuerySpec querySpec, List<Candidate> details, long elapsedMs) {
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
            double descriptionSemanticScore,
            double semanticScore,
            double totalScore,
            String rationale
    ) {
    }
}
