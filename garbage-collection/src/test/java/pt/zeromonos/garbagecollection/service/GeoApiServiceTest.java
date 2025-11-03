package pt.zeromonos.garbagecollection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeoApiServiceTest {

    private static final String GEOAPI_URL = "https://json.geoapi.pt/municipios";

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private GeoApiService geoApiService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        geoApiService = new GeoApiService(restTemplate, new ObjectMapper());
    }

    @Test
    void shouldFetchMunicipalitiesFromGeoApi() {
        mockServer.expect(requestTo(GEOAPI_URL))
                .andRespond(withSuccess("""
                        [
                          "porto",
                          {"nome":"Lisboa"},
                          {"municipio":"Sintra"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<String> municipalities = geoApiService.getMunicipalities();

        assertThat(municipalities)
                .containsExactly("Lisboa", "porto", "Sintra");

        mockServer.verify();
    }

    @Test
    void shouldCacheMunicipalitiesUntilTtlExpires() {
        mockServer.expect(requestTo(GEOAPI_URL))
                .andRespond(withSuccess("""
                        [
                          {"nome":"Braga"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<String> firstCall = geoApiService.getMunicipalities();

        List<String> secondCall = geoApiService.getMunicipalities();

        assertThat(secondCall)
                .isSameAs(firstCall)
                .containsExactly("Braga");

        mockServer.verify();
    }

    @Test
    void shouldReturnCachedMunicipalitiesWhenRefreshFails() {
        mockServer.expect(requestTo(GEOAPI_URL))
                .andRespond(withSuccess("""
                        [
                          {"nome":"Aveiro"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(GEOAPI_URL))
                .andRespond(withServerError());

        List<String> cached = geoApiService.getMunicipalities();

        ReflectionTestUtils.setField(Objects.requireNonNull(geoApiService), "cacheExpiry", Instant.now().minusSeconds(1));

        List<String> fallback = geoApiService.getMunicipalities();

        assertThat(fallback)
                .isEqualTo(cached)
                .containsExactly("Aveiro");

        mockServer.verify();
    }
}
