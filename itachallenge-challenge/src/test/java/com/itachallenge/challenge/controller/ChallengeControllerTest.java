package com.itachallenge.challenge.controller;

import com.itachallenge.challenge.dtos.ChallengeDto;
import com.itachallenge.challenge.services.IChallengeService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class ChallengeControllerTest {
    //region ATTRIBUTES
    private final static String VALID_ID = "dcacb291-b4aa-4029-8e9b-284c8ca80296";
    private final static String INVALID_ID = "123456789";
    private final static String MESSAGE_INVALID_ID = "Invalid ID format.";
    private final static String MESSAGE_ILLEGAL_ARGUMENT_EXCEPTION = "ID challenge: " + VALID_ID + " does not exist in the database";
    private final static String MESSAGE_BAD_REQUEST = "400 BAD_REQUEST";
    private final static String MESSAGE_INTERNAL_SERVER_ERROR = "INTERNAL SERVER ERROR " + HttpStatus.INTERNAL_SERVER_ERROR.value();
    private final String CHALLENGE_BASE_URL = "/itachallenge/api/v1/challenge";

    @Autowired
    private WebTestClient webTestClient;
    @MockBean
    private IChallengeService challengeService;
    @InjectMocks
    private ChallengeController challengeController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Test EndPoint: test")
    void TestEndPoint_test(){
        final String URI_TEST = "/test";
        webTestClient.get()
                .uri(CHALLENGE_BASE_URL + URI_TEST)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(s -> s, equalTo("Hello from ITA Challenge!!!"));
    }

    @Test
    void testGetOneChallengeValidUUID() {
        ChallengeDto challenge = new ChallengeDto();

        when(challengeService.isValidUUID(VALID_ID)).thenReturn(true);
        when(challengeService.getChallengeId(UUID.fromString(VALID_ID))).thenReturn(Mono.just(challenge));

        ResponseEntity<Mono<ChallengeDto>> response = challengeController.getOneChallenge(VALID_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(challenge, response.getBody().block());

        verifyService();
    }

    @Test
    void testGetOneChallengeNotValidUUID() {
        when(challengeService.isValidUUID(INVALID_ID)).thenReturn(false);

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> {
            challengeController.getOneChallenge(INVALID_ID);
        });

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getStatusCode().value());
        assertEquals(MESSAGE_INVALID_ID, exception.getReason());

        verify(challengeService, times(1)).isValidUUID(INVALID_ID);
        //cuando el id es invalido no se llama al método getChallengeId
        verify(challengeService, times(0)).getChallengeId(UUID.fromString(VALID_ID));
    }

    @Test
    void testGetOneChallengeEmpty(){
        when(challengeService.isValidUUID(VALID_ID)).thenReturn(true);
        when(challengeService.getChallengeId(UUID.fromString(VALID_ID))).thenReturn(Mono.empty());

        Mono<ChallengeDto> challenge = challengeController.getOneChallenge(VALID_ID).getBody();

        StepVerifier.create(challenge)
                .expectError(ResponseStatusException.class)
                .verify();

        verifyService();
    }

    @Test
    void testGetOneChallengeIllegalArgumentException() {
        IllegalArgumentException exception = new IllegalArgumentException(MESSAGE_ILLEGAL_ARGUMENT_EXCEPTION);

        when(challengeService.isValidUUID(VALID_ID)).thenReturn(true);
        when(challengeService.getChallengeId(any())).thenReturn(Mono.error(exception));

        ResponseEntity<Mono<ChallengeDto>> responseEntity = challengeController.getOneChallenge(VALID_ID);
        Mono<ChallengeDto> challenge = responseEntity.getBody();

        StepVerifier.create(challenge)
                .expectErrorMatches(error -> error instanceof ResponseStatusException
                        && ((ResponseStatusException) error).getStatusCode() == HttpStatus.BAD_REQUEST
                        && error.getMessage().contains(MESSAGE_BAD_REQUEST)
                        && error.getMessage().contains(MESSAGE_ILLEGAL_ARGUMENT_EXCEPTION))
                .verify();

        verifyService();
    }

    @Test
    void testGetOneChallengeException(){
        when(challengeService.isValidUUID(VALID_ID)).thenReturn(true);
        when(challengeService.getChallengeId(any(UUID.class))).thenThrow(new RuntimeException(MESSAGE_INTERNAL_SERVER_ERROR));

        ResponseEntity<Mono<ChallengeDto>> response = challengeController.getOneChallenge(VALID_ID);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        verifyService();
    }

    private void verifyService(){
        verify(challengeService, times(1)).isValidUUID(VALID_ID);
        verify(challengeService, times(1)).getChallengeId(UUID.fromString(VALID_ID));
    }

}

