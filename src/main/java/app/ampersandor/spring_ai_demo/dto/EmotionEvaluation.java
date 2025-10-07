package app.ampersandor.spring_ai_demo.dto;

import app.ampersandor.spring_ai_demo.domain.Emotion;

import java.util.List;

/**
 * Structured projection of the LLM response when it performs sentiment analysis.
 * {@code emotion} captures the bucket while {@code reason} collects supporting evidence.
 */
public record EmotionEvaluation(Emotion emotion, List<String> reason) {}
