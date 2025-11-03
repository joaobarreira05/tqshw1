package pt.zeromonos.garbagecollection.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class GeoApiService {

    private static final Logger logger = LoggerFactory.getLogger(GeoApiService.class);
    private static final String GEOAPI_URL = "https://json.geoapi.pt/municipios";
    private static final Duration CACHE_TTL = Duration.ofHours(12);
    private static final Duration FALLBACK_CACHE_TTL = Duration.ofMinutes(15);
    private static final List<String> DEFAULT_MUNICIPALITIES = List.of(
            "Lisboa", "Porto", "Coimbra", "Faro", "Braga", "Aveiro", "Sintra"
    );

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ReentrantLock refreshLock = new ReentrantLock();

    private volatile List<String> cachedMunicipalities = Collections.emptyList();
    private volatile Instant cacheExpiry = Instant.EPOCH;

    public GeoApiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<String> getMunicipalities() {
        Instant now = Instant.now();
        if (now.isBefore(cacheExpiry) && !cachedMunicipalities.isEmpty()) {
            return cachedMunicipalities;
        }

        refreshLock.lock();
        try {
            if (Instant.now().isBefore(cacheExpiry) && !cachedMunicipalities.isEmpty()) {
                return cachedMunicipalities;
            }

            ResponseEntity<String> response = restTemplate.getForEntity(GEOAPI_URL, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<String> municipalities = extractMunicipalities(response.getBody());

                if (!municipalities.isEmpty()) {
                    cachedMunicipalities = municipalities;
                    cacheExpiry = Instant.now().plus(CACHE_TTL);
                    logger.info("Fetched {} municipalities from GeoAPI.", municipalities.size());
                    return cachedMunicipalities;
                }

                logger.warn("GeoAPI responded with an empty municipality list.");
            } else {
                logger.warn("GeoAPI responded with status {}. Keeping previous cache.", response.getStatusCode());
            }
        } catch (RestClientException | JsonProcessingException ex) {
            logger.error("Failed to fetch municipalities from GeoAPI", ex);
        } finally {
            refreshLock.unlock();
        }

        if (cachedMunicipalities.isEmpty()) {
            logger.warn("Municipality cache is empty after attempting GeoAPI refresh. Returning fallback list.");
            cachedMunicipalities = DEFAULT_MUNICIPALITIES;
            cacheExpiry = Instant.now().plus(FALLBACK_CACHE_TTL);
        } else {
            logger.info("Returning {} cached municipalities after GeoAPI failure.", cachedMunicipalities.size());
        }

        return cachedMunicipalities;
    }

    private List<String> extractMunicipalities(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);

        if (!root.isArray()) {
            if (root.has("municipios") && root.get("municipios").isArray()) {
                root = root.get("municipios");
            } else {
                return Collections.emptyList();
            }
        }

        List<String> names = new ArrayList<>();
        for (JsonNode node : root) {
            Optional<String> extracted = nodeToName(node);
            extracted.ifPresent(names::add);
        }

        return names.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toUnmodifiableList());
    }

    private Optional<String> nodeToName(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }

        if (node.isTextual()) {
            return Optional.ofNullable(node.asText());
        }

        if (node.hasNonNull("nome")) {
            return Optional.ofNullable(node.get("nome").asText());
        }

        if (node.hasNonNull("municipio")) {
            return Optional.ofNullable(node.get("municipio").asText());
        }

        return Optional.empty();
    }
}