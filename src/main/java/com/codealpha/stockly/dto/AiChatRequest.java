package com.codealpha.stockly.dto;

import java.util.ArrayList;
import java.util.List;

public class AiChatRequest {

    private String message;

    private List<AiChatMessage> history = new ArrayList<>();

    public AiChatRequest() {
    }

    public AiChatRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<AiChatMessage> getHistory() {
        return history;
    }

    public void setHistory(List<AiChatMessage> history) {
        this.history = history;
    }
}