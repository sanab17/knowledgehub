package com.enterprise.knowledgehub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Unit tests for ChatController to verify route mapping and security contexts.
 */
@WebMvcTest(ChatController.class)
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatModel chatModel;

    @MockBean
    private VectorStore vectorStore;

    @Test
    @WithMockUser(username = "employee1", roles = "EMPLOYEE")
    public void testShowChatPageAuthorized() throws Exception {
        mockMvc.perform(get("/chat"))
                .andExpect(status().isOk())
                .andExpect(view().name("chat"));
    }

    @Test
    public void testShowChatPageUnauthorizedRedirects() throws Exception {
        mockMvc.perform(get("/chat"))
                .andExpect(status().isUnauthorized());
    }
}
