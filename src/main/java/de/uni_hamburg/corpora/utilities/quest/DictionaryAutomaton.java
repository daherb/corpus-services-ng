package de.uni_hamburg.corpora.utilities.quest;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementation of an automaton representing a dictionary.
 * Currently unoptimized resulting in a kind of prefix tree
 *
 * @author bba1792 Dr. Herbert Lange
 * @version 20210730
 */
public class DictionaryAutomaton {

    // A logger for debugging and error messages
    static Logger logger = Logger.getLogger(DictionaryAutomaton.class.getName());

    // Keeps track of the number of states, used to create new states
    private int stateCount;

    // The alphabet, i.e. all characters encountered in the dictionary
    private final Set<Character> alphabet = new HashSet<>();

    // Initial and accepting states of the automaton
    private final String initialState;
    private final Set<String> acceptingStates = new HashSet<>();

    // The transitions in the automaton from one state to another determined by a character
    HashMap<String, HashMap<Character,String>> transitionsFrom = new HashMap<>();
    HashMap<String, HashMap<Character,String>> transitionsTo = new HashMap<>();

    /**
     * Function to read a file and return a list of lines
     * @param f the file
     * @return the list of lines in a file, empty if the file could not be read or is empty
     */
    public static List<String> readFileAsList(File f) {
//        List<String> lines = new ArrayList<>();
//        try {
//            // Open file
//            BufferedReader br = new BufferedReader(new FileReader(f));
//            // Read all lines and add to list
//            lines.addAll(br.lines().collect(Collectors.toList()));
//            // Return non-empty list if the file is not empty
//            return lines;
//        } catch (IOException e) {
//            logger.log(Level.SEVERE,"Error reading file " + f + ": " + e);
//            // Return empty list
//            return lines ;
//        }
        try {
            return readInputStreamAsList(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE,"Error reading file " + f + ": " + e);
            // Return empty list
            return new ArrayList<>() ;
        }
    }

    /**
     * Function to read a file and return a list of lines
     * @param is the input stream
     * @return the list of lines in a file, empty if the file could not be read or is empty
     */
    public static List<String> readInputStreamAsList(InputStream is) {
        // Open file
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        // Read all lines and add to list
        // Return non-empty list if the file is not empty
        // trim removes spaces in the beginning or the end of the line
        return br.lines().map(String::trim).collect(Collectors.toList());
    }

    /**
     * Constructor constructing the automaton based on a dictionary read from file
     *
     * @param f the dictionary file
     */
    public DictionaryAutomaton(File f) {
        // Read the file and call the other constructor
        this(readFileAsList(f));
    }

    /**
     * Constructor constructing the automaton based on a dictionary read from file
     *
     * @param is the dictionary file
     */
    public DictionaryAutomaton(InputStream is) {
        // Read the file and call the other constructor
        this(readInputStreamAsList(is));
    }
    /**
     * Constructor constructing the automaton basedon a dictionary represented by a list of strings
     *
     * @param dict the dictionary
     */
    public DictionaryAutomaton(List<String> dict) {
        // Initialize start state as state 0
        initialState = "s0";
        // Keep track of the number of states in the automaton to be able to create new ones on demand
        stateCount = 1;
        // For each word, change the automaton to accept it
        for (String w : dict) {
            // Add word to automaton
            addWord(w);
        }

    }

    /**
     * Function to get the initial state
     * @return the initial state
     */
    public String getInitialState() {
        return initialState;
    }

    /**
     * Function to check if a given state is one of the accepting states
     * @param state the state to be checked
     * @return if the state is one of the accepting states
     */
    public boolean isAcceptingState(String state) {
        return acceptingStates.contains(state);
    }

    /**
     * Function to get the complete alphabet known to the automaton
     * @return the alphabet
     */
    public Set<Character> getAlphabet() {
        return alphabet;
    }

    /**
     * Function to add a word to the dictionary
     * @param word the word to be added
     */
    public void addWord(String word) {
        // Always start from the initial state
        String state = initialState;
        // The state we use if we use to follow transitions, null indicates that we haven't a matching transition in
        // the current state
        String newState = null ;
        // No transitions to start state
        transitionsTo.put(state,new HashMap<>());
        for (Character c : word.toCharArray()) {
            // Keep track of all characters
            alphabet.add(c);
            // Try to follow a transition
            // logger.log(Level.INFO,c + " - " + state);
            newState = transition(state,c);
            // If we fail (new state is null), we need to create a new state and add a transition to it
            if (newState == null) {
                // Create new state
                newState = "s" + stateCount;
                stateCount += 1;
                // Push the new transition
                transitionsFrom.get(state).put(c,newState);
                // Create entry if necessary
                if (!transitionsTo.containsKey(newState)) {
                    transitionsTo.put(newState,new HashMap<>());
                }
                transitionsTo.get(newState).put(c,state);
            }
            state = newState ;
        }
        // After adding a complete word, add the final state to the accepting states
        if (newState != null) {
            // No transitions at the end of the word
            if (!transitionsFrom.containsKey(newState))
                transitionsFrom.put(newState,new HashMap<>());
            acceptingStates.add(newState);
        }
    }

    /**
     * Function to match a string with the automaton
     * @param s the string
     * @return if the string can be matched using the automaton
     */
    public boolean match(String s) {
        String state = initialState;
        for (Character c : s.toCharArray()) {
            String newState = transition(state,c);
            // If we fail to transition we know we failed to match
            if (newState == null)
                return false;
            else
                state = newState ;
        }
        // Check if the final state is an accepting state
        return acceptingStates.contains(state);
    }

    /**
     * Function to follow a transition in the automaton from one state to another determined by the next character.
     * As a side effect create new empty hash maps for missing transitions
     *
     * @param state the first state
     * @param input the input character
     * @return the new state, or null if no valid transition for the character in the state
     */
    public String transition(String state, Character input) {
        HashMap<Character,String> currentTransitions = transitionsFrom.get(state);
        if (currentTransitions == null)
        {
            transitionsFrom.put(state,new HashMap<>());
            return null;
        }
        return currentTransitions.get(input);
    }

    /**
     * Class to keep track of the state when segemneting a word
     */
    private static class State {
        // Start of the segment
        int startIndex ;
        // The segment/substring
        String segment;
        // End of the segment
        int endIndex;

        /**
         * Function to convert a State object into a string representation
         * @return the string representation of the State object
         */
        @Override
        public java.lang.String toString() {
            return "State{" +
                    "startIndex=" + startIndex +
                    ", segment='" + segment + '\'' +
                    ", endIndex=" + endIndex +
                    '}';
        }

        /**
         * Constructor setting all fields
         * @param startIndex the segment start index
         * @param word the segment
         * @param endIndex the segment end index
         */
        public State(int startIndex, String word, int endIndex) {
            this.startIndex = startIndex;
            this.segment = word;
            this.endIndex = endIndex;
        }
    }

    /**
     * Function to check if a word can be segmented into words from the dictionary
     *
     * @param word the word to be segmented
     * @return the list of segments and null if the segmentation fails
     */
    public List<String> segmentWord(String word) {
        // Stack to keep track of the process and for backtracking
        Stack<State> frontier = new Stack<>() ;
        // Another stack to keep track of the segments we encountered, used to construct the segmentation
        Stack<State> segmentStack = new Stack<>();
        // Push some initial state with a blank segment
        frontier.push(new State(0, "", 0));
        // Loop until either done (early return) or out of options
        while (!frontier.isEmpty()) {
            State state = frontier.pop();
            // Debug
            // logger.log(Level.INFO, "Old state: " + state.toString() + " Stack size: " + states.size());
            int newStart = state.endIndex ;
            int newEnd = state.endIndex ;
            int index = newStart ;
            String newSegment = "";
            // The state in the dictionary automaton
            String daState = initialState;
            Stack<State> newStates = new Stack<>() ;
            while (index < word.length()) {
                daState = transition(daState,word.charAt(index)) ;
                newSegment += word.charAt(index) ;
                index += 1 ;
                if (acceptingStates.contains(daState)) {
                    State newState = new State(newStart, newSegment, index);
                    newEnd = index ;
                    newStates.push(newState) ;
                    // DEBUG
                    //logger.log(Level.INFO, "New state: " + newState);
                    segmentStack.push(newState);
                }
            }
            // We can continue from the current state
            if (!newStates.isEmpty()) {
                // Put all the new states on top of the stack
                frontier.addAll(newStates);
                // We are actually done, construct the list and return it
                if (newEnd == word.length()) {
                    return stackToList(segmentStack, word);
                }
            }
        }
        // if we fail to segment we return an empty list
        return null ; // Collections.EMPTY_LIST ;
    }

    /**
     * Function to check if a word is segmentable using the dictionary
     * @param word the word to be checked
     * @return if the word can be segmented
     */
    public boolean checkSegmentableWord(String word) {
        List<String> l = segmentWord(word) ;
        return l != null && !l.isEmpty() ;
    }

    /**
     * Function to convert a stack of states into a list of segments the word can be split into completely
     * @param segmentStates the stack of states encountered
     * @param word the word to be segmented
     * @return the list of segments
     */
    private List<String> stackToList(Stack<State> segmentStates, String word) {
        // DEBUG
        //logger.log(Level.INFO,"Segments: \n"+ segmentStates.stream().map(State::toString).reduce((s1,s2) -> s1 + "\n" + s2));
        LinkedList<String> strings = new LinkedList<>() ;
        if (segmentStates.isEmpty())
            return Collections.emptyList() ;
        State  s = segmentStates.pop();
        // Try to find the last segment
        while (s.endIndex != word.length() && !segmentStates.isEmpty()) {
            s = segmentStates.pop();
            // DEBUG
            //logger.log(Level.INFO, "Dropping " + s);
        }
        // DEBUG
        // logger.log(Level.INFO, "Got " + s.segment);
        strings.addFirst(s.segment);
        int currentStart = s.startIndex ;
        while (!segmentStates.isEmpty()) {
            s = segmentStates.pop();
            // DEBUG
            // logger.log(Level.INFO, "Looking at " + s);
            if (s.endIndex == currentStart) {
                strings.addFirst(s.segment);
                currentStart = s.startIndex ;
            }
            // Done early when encountering the start
            if (currentStart == 0)
                return strings ;
        }
        // If we are done matching we return the strings otherwise the empty list
        if (currentStart == 0)
            return strings ;
        else
            return Collections.emptyList() ;
    }

    public void saveDot(String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph dictAutomaton {\n");
        sb.append("\tstart [shape=point];\n");
        for (int i = 0; i < this.stateCount ; i++) {
            String state = "s" + i;
            // Mark accepting states
            if (this.isAcceptingState(state)) {
                sb.append(String.format("\t%s [shape=doublecircle];\n",state));
            }
            // Or regular states
            else {
                sb.append(String.format("\t%s [shape=circle];\n",state));
            }
            // Connect initial state
            if (state.equals(this.initialState))
                sb.append(String.format("\tstart -> %s;\n",state));
            Map<Character,String> transitions = this.transitionsFrom.get(state);
            if (transitions != null) {
                for (Map.Entry<Character, String> transition : transitions.entrySet()) {
                    // Connect states
                    sb.append(String.format("\t%s -> %s [label=\"%s\"];\n", state, transition.getValue(), transition.getKey()));
                }
            }
        }
        sb.append("}");
        BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
        bw.write(sb.toString());
        bw.close();
    }
}