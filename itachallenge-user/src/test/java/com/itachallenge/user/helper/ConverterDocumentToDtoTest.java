package com.itachallenge.user.helper;

import com.itachallenge.user.document.SolutionDocument;
import com.itachallenge.user.document.UserSolutionDocument;
import com.itachallenge.user.dtos.UserScoreDto;
import com.itachallenge.user.dtos.UserSolutionDto;
import com.itachallenge.user.enums.ChallengeStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration
class ConverterDocumentToDtoTest {


    @Autowired
    private ConverterDocumentToDto converter;

    UUID uuid_1 = UUID.randomUUID();
    UUID idUser = UUID.randomUUID();
    UUID idLanguage = UUID.randomUUID();
    UUID idChallenge = UUID.randomUUID();
    String solutionText1 = "Ipsum.. 1";
    String solutionText2 = "Ipsum.. 2";
    SolutionDocument solutionDocument1 = new SolutionDocument(UUID.randomUUID(), solutionText1);
    SolutionDocument solutionDocument2 = new SolutionDocument(UUID.randomUUID(), solutionText2);
    SolutionDocument solutionDocument3 = new SolutionDocument(UUID.randomUUID(), solutionText2);
    List<SolutionDocument> solutionDocumentList = List.of(solutionDocument1, solutionDocument2, solutionDocument3);
    UserSolutionDocument userScoreDocument = new UserSolutionDocument(uuid_1, idUser, idChallenge, idLanguage, true, ChallengeStatus.STARTED, 90, solutionDocumentList);

    SolutionDocument solutionDocument = new SolutionDocument();
    UserSolutionDocument userSolutionDocument = new UserSolutionDocument();


    @DisplayName("Convertir un objeto UserScoreDocument en un objeto UserScoreDto")
    @Test
    void fromUserScoreDocumentToUserScoreDtoTest(){

        Flux<UserScoreDto> result = converter.fromUserScoreDocumentToUserScoreDto(Flux.just(userScoreDocument));

        StepVerifier.create(result)
                .expectNextMatches(userScoreDto -> ValidateUserScoreDto(userScoreDto,userScoreDocument))
                .expectComplete()
                .verify();
    }

    private boolean ValidateUserScoreDto(@NotNull UserScoreDto userScoreDto, @NotNull UserSolutionDocument userScoreDocument) {

        return userScoreDto.getUserId().equals(userScoreDocument.getUserId()) &&
                userScoreDto.getChallengeId().equals(userScoreDocument.getChallengeId()) &&
                userScoreDto.getSolutions().equals(userScoreDocument.getSolutionDocument()) &&
                userScoreDto.getLanguageID().equals(userScoreDocument.getLanguageId());
    }

    @Test
    public void testFromUserSolutionDocumentToUserSolutionDto() {
        solutionDocument.setSolutionText("Sample Solution Text");

        userSolutionDocument.setUserId(UUID.randomUUID());
        userSolutionDocument.setChallengeId(UUID.randomUUID());
        userSolutionDocument.setLanguageId(UUID.randomUUID());
        userSolutionDocument.setStatus(ChallengeStatus.STARTED);
        userSolutionDocument.setSolutionDocument(Collections.singletonList(solutionDocument));

        Flux<UserSolutionDto> userSolutionDtoFlux = fromUserSolutionDocumentToUserSolutionDto(userSolutionDocument);

        StepVerifier.create(userSolutionDtoFlux)
                .expectNextMatches(dto ->
                        dto.getUserId().equals(userSolutionDocument.getUserId().toString()) &&
                                dto.getChallengeId().equals(userSolutionDocument.getChallengeId().toString()) &&
                                dto.getLanguageId().equals(userSolutionDocument.getLanguageId().toString()) &&
                                dto.getStatus().equals(userSolutionDocument.getStatus().toString()) && // Convert status to string
                                dto.getSolutionText().equals(solutionDocument.getSolutionText())
                )
                .expectComplete()
                .verify();
    }

    public Flux<UserSolutionDto> fromUserSolutionDocumentToUserSolutionDto(@NotNull UserSolutionDocument document) {
        return Flux.just(UserSolutionDto.builder()
                .userId(document.getUserId().toString())
                .challengeId(document.getChallengeId().toString())
                .languageId(document.getLanguageId().toString())
                .status(document.getStatus().toString())
                .solutionText(document.getSolutionDocument().get(0).getSolutionText())
                .build());
    }
}

