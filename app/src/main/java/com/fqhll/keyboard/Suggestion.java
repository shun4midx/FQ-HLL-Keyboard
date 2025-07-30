package com.fqhll.keyboard;

public class Suggestion {
    public final String[] suggestions;
    public final double[] scores;
    public Suggestion(String[] suggestions, double[] scores) {
        this.suggestions = suggestions;
        this.scores = scores;
    }
}