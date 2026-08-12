package com.enterprise.knowledgehub.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Controller to handle semantic corporate chat queries against vectorized document database (RAG).
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    @GetMapping("/chat")
    public String showChatPage(Model model) {
        model.addAttribute("activePage", "chat");
        return "chat";
    }

    public static record ChatChunk(String content) {}

    @GetMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<ChatChunk> streamChat(@RequestParam("message") String message) {
        log.info("RAG Query: User asked: '{}'", message);

        try {
            // 1. Retrieve relevant contexts from pgvector store
            List<Document> similarDocs = vectorStore.similaritySearch(
                    SearchRequest.query(message).withTopK(4).withSimilarityThreshold(0.3)
            );

            String context = similarDocs.isEmpty() ? "No relevant document context found." :
                    similarDocs.stream()
                            .map(Document::getContent)
                            .collect(Collectors.joining("\n---\n"));

            // 2. Build the system/user instruction prompt
            String systemInstruction = """
                    You are KnowledgeHub AI, a secure corporate virtual assistant.
                    Your goal is to answer the user's questions accurately based ONLY on the retrieved document segments provided below.
                    If the answer cannot be found in the context segments, state: "I cannot find this information in the portal documents."
                    Be professional, direct, and summarize the answers clearly. Always cite the document title(s) if present in the metadata.
                    
                    Document context segments:
                    ---
                    %s
                    ---
                    """.formatted(context);

            String promptText = systemInstruction + "\nUser Question: " + message + "\nAnswer:";
            Prompt prompt = new Prompt(promptText);

            // 3. Call model streaming and extract content tokens
            return chatModel.stream(prompt)
                    .map(response -> {
                        if (response.getResult() != null && response.getResult().getOutput() != null) {
                            return new ChatChunk(response.getResult().getOutput().getContent());
                        }
                        return new ChatChunk("");
                    })
                    .filter(chunk -> chunk.content() != null);

        } catch (Exception e) {
            log.error("RAG Error: Failed to execute chat streaming query. Error: {}", e.getMessage(), e);
            return Flux.just(new ChatChunk("Error: Failed to fetch answer from AI model. Please verify that your OpenAI API Key environment variable is configured correctly."));
        }
    }
}
