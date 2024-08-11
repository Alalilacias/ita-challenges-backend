package com.itachallenge.score.filter;

import com.itachallenge.score.docker.DockerContainerHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class DockerJavaFilterTest {

    private DockerJavaFilter dockerJavaFilter;
    private Filter nextFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dockerJavaFilter = new DockerJavaFilter();
        nextFilter = mock(Filter.class);
        dockerJavaFilter.setNext(nextFilter);
    }

    @Test
    void testApply_withValidCode() {

        String validCode = "public class Main { public static void main(String[] args) { System.out.println(\"Hello, World!\"); } }";
        when(nextFilter.apply(validCode)).thenReturn(true);

        try (MockedStatic<DockerContainerHelper> mockedHelper = mockStatic(DockerContainerHelper.class)) {
            GenericContainer<?> mockedContainer = mock(GenericContainer.class);
            mockedHelper.when(DockerContainerHelper::createJavaSandboxContainer).thenReturn(mockedContainer);


            boolean result = dockerJavaFilter.apply(validCode);

            assertTrue(result, "Valid code should pass through DockerJavaFilter");
            verify(nextFilter).apply(validCode);
            mockedHelper.verify(DockerContainerHelper::createJavaSandboxContainer);
        }
    }
}
