package app.ampersandor.spring_ai_demo.dto;

import app.ampersandor.spring_ai_demo.domain.Emotion;

import java.util.List;

public record EmotionEvaluation(Emotion emotion, List<String> reason) {}
