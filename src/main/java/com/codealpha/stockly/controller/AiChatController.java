package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.AiChatRequest;
import com.codealpha.stockly.dto.AiChatResponse;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.service.AiChatService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public AiChatResponse chat(
            @RequestBody AiChatRequest request,
            Authentication authentication
    ) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "Authenticated user not found."
            );
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof User user)) {

            throw new IllegalStateException(
                    "Authenticated principal is not a Stockly user."
            );
        }

        String response = aiChatService.chat(
                user,
                request.getMessage(),
                request.getHistory()
        );

        return new AiChatResponse(response);
    }
}