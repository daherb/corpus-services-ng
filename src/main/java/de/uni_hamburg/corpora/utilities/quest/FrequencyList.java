package de.uni_hamburg.corpora.utilities.quest;

import java.util.*;

/**
 * Helper class implementing a simple frequency list
 * @author bba1792, Dr. Herbert Lange
 * @version 20220328
 */
public class FrequencyList {

    // The map storing the list
    private final Map<String, Integer> freqList = new HashMap<>();

    /**
     * Add a value to the list, increasing its count by 1 if it was already in the list
     * @param k the new value
     */
    public void put(String k) {
        freqList.compute(k,(fk, fv) -> (fv == null) ? 1 : fv + 1);
    }

    /**
     * Adds all values of a collection
     * @param ks the collection of values
     */
    public void putAll(Collection<String> ks) {
        for (String k : ks)
            this.put(k);
    }

    /**
     * Gets the count for a value
     * @param k the value
     * @return its count
     */
    public int get(String k) {
        return freqList.get(k);
    }

    /**
     * Checks if a value is in the frequency list
     * @param k the value
     * @return if it is in the list
     */
    public boolean contains(Object k) {
        return freqList.containsKey(k);
    }

    /**
     * Gets the internal map
     * @return the map
     */
    public Map<String,Integer> getMap() {
        return freqList;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        List<String> keys  = new ArrayList<>(freqList.keySet());
        keys.sort(Comparator.comparing(freqList::get).reversed());
        for (String k : keys) {
            str.append(k);
            str.append(" - ");
            str.append(freqList.get(k));
            str.append("\n");
        }
        return str.toString();
    }

    /**
     * Checks if the list is empty
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return freqList.isEmpty();
    }
}
