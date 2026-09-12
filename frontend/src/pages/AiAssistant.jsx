import { useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { Bot, Send, Trash2, Sparkles, User } from "lucide-react";

import api from "../services/api";

import "./AiAssistant.css";

function AiAssistant() {
  const [messages, setMessages] = useState([
    {
      role: "assistant",
      content:
        "Hello! I'm Stockly AI. I can help you understand your virtual portfolio, holdings, stock concepts, technical indicators, risk, and diversification.\n\nWhat would you like to know?",
    },
  ]);

  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);

  const sendMessage = async () => {
    const message = input.trim();

    if (!message || loading) {
      return;
    }

    const userMessage = {
      role: "user",
      content: message,
    };

    /*
     * Keep the conversation that already exists on screen.
     *
     * The backend will add the current message separately,
     * so we don't include userMessage in history here.
     */
    const conversationHistory = messages.map((item) => ({
      role: item.role,
      content: item.content,
    }));

    setMessages((previous) => [
      ...previous,
      userMessage,
    ]);

    setInput("");
    setLoading(true);

    try {
      const response = await api.post("/ai/chat", {
        message,
        history: conversationHistory,
      });

      const aiMessage = {
        role: "assistant",
        content:
          response.data?.message ||
          "I couldn't generate a response right now.",
      };

      setMessages((previous) => [
        ...previous,
        aiMessage,
      ]);

    } catch (error) {
      console.error("AI chat error:", error);

      const errorMessage = {
        role: "assistant",
        content:
          error?.response?.data?.message ||
          "Unable to connect to Stockly AI right now. Please try again.",
      };

      setMessages((previous) => [
        ...previous,
        errorMessage,
      ]);

    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      sendMessage();
    }
  };

  const clearChat = () => {
    setMessages([
      {
        role: "assistant",
        content:
          "Chat cleared. What would you like to know about your Stockly portfolio?",
      },
    ]);
  };

  const suggestedQuestions = [
    "Give me a summary of my portfolio",
    "Which holding contributes most to my P/L?",
    "What is my biggest portfolio concentration?",
    "Explain RSI in simple terms",
  ];

  return (
    <div className="ai-assistant-page">

      <div className="ai-assistant-container">

        {/* HEADER */}
        <header className="ai-assistant-header">

          <div className="ai-assistant-heading">

            <div className="ai-assistant-logo">
              <Bot size={23} />
            </div>

            <div>
              <div className="ai-assistant-title-row">
                <h1>Stockly AI</h1>

                <span className="ai-online-status">
                  <span className="ai-status-dot" />
                  AI Assistant
                </span>
              </div>

              <p>
                Your intelligent virtual trading assistant
              </p>
            </div>

          </div>

          <button
            type="button"
            className="ai-clear-button"
            onClick={clearChat}
            disabled={loading}
          >
            <Trash2 size={15} />
            Clear Chat
          </button>

        </header>


        {/* DISCLAIMER */}
        <div className="ai-disclaimer">

          <Sparkles size={16} />

          <p>
            Stockly AI provides educational insights using your
            current virtual trading data. It does not provide
            financial advice or guaranteed predictions.
          </p>

        </div>


        {/* CHAT */}
        <section className="ai-chat-card">

          <div className="ai-messages">

            {messages.map((message, index) => (

              <div
                key={index}
                className={`ai-message-row ${
                  message.role === "user"
                    ? "ai-message-user"
                    : "ai-message-assistant"
                }`}
              >

                <div className="ai-message-avatar">

                  {message.role === "user" ? (
                    <User size={16} />
                  ) : (
                    <Bot size={16} />
                  )}

                </div>

                <div className="ai-message-bubble">

                  {message.role === "assistant" ? (
                    <ReactMarkdown
                      remarkPlugins={[remarkGfm]}
                    >
                      {message.content}
                    </ReactMarkdown>
                  ) : (
                    <p>{message.content}</p>
                  )}

                </div>

              </div>

            ))}


            {/* LOADING */}
            {loading && (

              <div className="ai-message-row ai-message-assistant">

                <div className="ai-message-avatar">
                  <Bot size={16} />
                </div>

                <div className="ai-message-bubble ai-loading-bubble">

                  <span />
                  <span />
                  <span />

                </div>

              </div>

            )}

          </div>


          {/* SUGGESTIONS */}
          {messages.length === 1 && !loading && (

            <div className="ai-suggestions">

              <div className="ai-suggestions-label">
                Try asking
              </div>

              <div className="ai-suggestion-list">

                {suggestedQuestions.map((question) => (

                  <button
                    key={question}
                    type="button"
                    onClick={() => {
                      setInput(question);
                    }}
                  >
                    {question}
                  </button>

                ))}

              </div>

            </div>

          )}


          {/* INPUT */}
          <div className="ai-input-section">

            <div className="ai-input-wrapper">

              <textarea
                value={input}
                onChange={(event) =>
                  setInput(event.target.value)
                }
                onKeyDown={handleKeyDown}
                placeholder="Ask Stockly AI about your portfolio..."
                rows={1}
                disabled={loading}
              />

              <button
                type="button"
                className="ai-send-button"
                onClick={sendMessage}
                disabled={!input.trim() || loading}
                aria-label="Send message"
              >
                <Send size={17} />
              </button>

            </div>

            <div className="ai-input-hint">
              Press Enter to send · Shift + Enter for a new line
            </div>

          </div>

        </section>

      </div>

    </div>
  );
}

export default AiAssistant;