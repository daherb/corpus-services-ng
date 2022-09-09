package de.uni_hamburg.corpora.utilities.quest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A class providing means of segmenting a string into given chunks
 * @author bba1792, Dr. Herbert Lange
 * @version 20220909
 */
public class StringSegmentation {

    // The list of segments as a result of the last segmentation
    private ArrayList<String> segments;

    /**
     * Default constructor
     */
    public StringSegmentation() {

    }

    /**
     * Method to check if a word is segmentable into chunks
     * @param token the word to be segmented
     * @param chunks candidates for chunks
     * @return if the word can be segmented
     */
    public boolean segmentWord(String token, List<String> chunks) {
        // Filter out blank strings, remove duplicates and sort by length
        chunks = new ArrayList<>(chunks.stream().filter((s) -> !s.isEmpty())
                .collect(Collectors.toSet()));
        chunks.sort(Comparator.comparingInt(String::length).reversed());
        // List of states to remember and initial state with nothing matched and all remaining
        List<Pair<List<String>,String>> states = new ArrayList<>();
        List<String> matched = new ArrayList<>();
        String remaining;
        states.add(new Pair<>(matched,token));
        // Continue as long as we have states
        while (!states.isEmpty()) {
            // Get the first remaining state and recover both matched and remaining
            Pair<List<String>, String> state = states.remove(0);
            matched = state.getFirst().stream().collect(Collectors.toList());
            remaining = state.getSecond();
            // Check all possible chunks if remaining string starts with it
            for (String c : chunks) {
                if (remaining.startsWith(c)) {
                    // Copy matched and add new chunk
                    List<String> newMatched = matched.stream().collect(Collectors.toList());
                    newMatched.add(c);
                    // Remove matched prefix from remaining
                    String newRemaining = remaining.replaceFirst(Pattern.quote(c), "");
                    // If nothing remains we are done
                    if (newRemaining.isEmpty()) {
                        segments = new ArrayList<>();
                        segments.addAll(newMatched);
                        return true;
                    }
                    else {
                        // Otherwise, store where to continue
                        states.add(new Pair<>(newMatched, newRemaining));
                    }
                }
            }
        }
        // Return if failed
        return false;
    }

    public List<String> getSegments() {
        return segments;
    }
}
