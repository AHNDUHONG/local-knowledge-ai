
package com.ahndu.localknowledgeai.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "RUN_OLLAMA_IT",
        matches = "true"
)
class OllamaIntegrationTest {

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    void chatModelShouldGenerateResponse() {
        String response = chatModel.call(
                "한국의 수도는 어디인가요? 짧게 답해주세요."
        );

        assertThat(response)
                .isNotNull()
                .isNotBlank();

        System.out.println("CHAT TEST PASSED");
    }

    @Test
    void embeddingModelShouldReturn1024Dimensions() {
        float[] embedding = embeddingModel.embed(
                "Local Knowledge AI 임베딩 연결 테스트"
        );

        assertThat(embedding)
                .isNotNull()
                .hasSize(1024);

        System.out.println("EMBEDDING TEST PASSED");
        System.out.println("Dimensions: " + embedding.length);
    }
}
