package com.itachallenge.challenge.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itachallenge.challenge.dto.ChallengeDto;
import com.itachallenge.challenge.dto.GenericResultDto;
import com.itachallenge.challenge.exception.ChallengeNotFoundException;
import com.itachallenge.challenge.service.IChallengeService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@WebFluxTest(ChallengeController.class)
class ChallengeControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private IChallengeService challengeService;

    @MockBean
    private DiscoveryClient discoveryClient;

    @MockBean
    private ChallengeController challengeController;

    @Test
    void test() {
        // Arrange
        List<ServiceInstance> instances = Arrays.asList(
                new DefaultServiceInstance("instanceId", "itachallenge-challenge", "localhost", 8080, false),
                new DefaultServiceInstance("instanceId", "itachallenge-user", "localhost", 8081, false)
        );
        when(discoveryClient.getInstances("itachallenge-challenge")).thenReturn(instances);
        when(discoveryClient.getInstances("itachallenge-user")).thenReturn(Collections.singletonList(instances.get(1)));

        // Act & Assert
        webTestClient.get().uri("/itachallenge/api/v1/challenge/test")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Hello from ITA Challenge!!!");
    }

    @Test
    void getOneChallenge_ValidId_ChallengeReturned() {
        // Arrange
        String challengeId = "valid-challenge-id";
        GenericResultDto<ChallengeDto> expectedResult = new GenericResultDto<>();
        expectedResult.setInfo(0, 1, 1, new ChallengeDto[]{new ChallengeDto()});

        when(challengeService.getChallengeById(challengeId)).thenReturn(Mono.just(expectedResult));

        // Act & Assert
        webTestClient.get()
                .uri("/itachallenge/api/v1/challenge/challenges/{challengeId}", challengeId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GenericResultDto.class)
                .value(dto -> {
                    assert dto != null;
                    assert dto.getCount() == 1;
                    assert dto.getResults() != null;
                    assert dto.getResults().length == 1;
                });
    }

    @Test
    void removeResourcesById_ValidId_ResourceDeleted() {
        // Arrange
        String resourceId = "valid-resource-id";
        GenericResultDto<String> expectedResult = new GenericResultDto<>();
        expectedResult.setInfo(0, 1, 1, new String[]{"resource deleted correctly"});

        when(challengeService.removeResourcesByUuid(resourceId)).thenReturn(Mono.just(expectedResult));

        // Act & Assert
        webTestClient.delete()
                .uri("/itachallenge/api/v1/challenge/resources/{idResource}", resourceId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GenericResultDto.class)
                .value(dto -> {
                    assert dto != null;
                    assert dto.getCount() == 1;
                    assert dto.getResults() != null;
                    assert dto.getResults().length == 1;
                });
    }

    @Test
    void getAllChallenges_ChallengesExist_ChallengesReturned() {
        // Arrange
        GenericResultDto<ChallengeDto> expectedResult = new GenericResultDto<>();
        expectedResult.setInfo(0, 2, 2, new ChallengeDto[]{new ChallengeDto(), new ChallengeDto()});

        when(challengeService.getAllChallenges()).thenReturn(Mono.just(expectedResult));

        // Act & Assert
        webTestClient.get()
                .uri("/itachallenge/api/v1/challenge/challenges")
                .exchange()
                .expectStatus().isOk()
                .expectBody(GenericResultDto.class)
                .value(dto -> {
                    assert dto != null;
                    assert dto.getCount() == 2;
                    assert dto.getResults() != null;
                    assert dto.getResults().length == 2;
                });
    }

    @Test
    void testGetChallenges() {
        // Arrange
        ChallengeDto challenge1 = new ChallengeDto();
        ChallengeDto challenge2 = new ChallengeDto();

        Set<String> languages = new HashSet<>();
        languages.add("Javascript");
        Set<String> levels = new HashSet<>();
        levels.add("Medium");

        ChallengeDto[] challenges = {challenge1, challenge2};

        GenericResultDto<ChallengeDto> expectedResult = new GenericResultDto<>();
        expectedResult.setInfo(0, 5, challenges.length, challenges);

        when(challengeService.getChallengesByLanguagesAndLevel(languages, levels)).thenReturn(Mono.just(expectedResult));

        webTestClient.get()
                .uri("/itachallenge/api/v1/challenge/filtered-challenges")
                .exchange()
                .expectStatus().isOk()
                .expectBody(GenericResultDto.class);
    }

    @Test
    void testGetChallenges_NoChallengesFound() {
        // Arrange
        Set<String> languages = new HashSet<>();
        languages.add("Not_languages");
        Set<String> levels = new HashSet<>();
        levels.add("Not_levels");

        when(challengeService.getChallengesByLanguagesAndLevel(languages, levels)).thenReturn(Mono.error(new ChallengeNotFoundException("No challenges found for the given filters.")));

        StepVerifier.create(challengeController.getChallenges(languages, levels))
                .expectError(ChallengeNotFoundException.class);

        verifyNoInteractions(challengeService);
    }

}
