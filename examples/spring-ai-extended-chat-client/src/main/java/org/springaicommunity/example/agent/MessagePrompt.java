package org.springaicommunity.example.agent;

/**
 * Request payload for {@link ExtendedChatController#handleChat}.
 *
 * <p>Either {@code prompt} or {@code message} may be provided. {@code prompt} takes
 * precedence; {@code message} is used as a fallback when {@code prompt} is absent.
 */
public record MessagePrompt(String prompt, String message) {
}
