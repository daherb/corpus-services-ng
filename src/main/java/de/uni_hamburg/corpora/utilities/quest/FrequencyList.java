package de.uni_hamburg.corpora.utilities.quest;

import java.util.*;

public class FrequencyList {

    private final Map<String, Integer> freqList = new HashMap<>();

    public void put(String k) {
        freqList.compute(k,(fk, fv) -> (fv == null) ? 1 : fv + 1);
    }

    public void putAll(Collection<String> ks) {
        for (String k : ks)
            this.put(k);
    }

    public int get(String k) {
        return freqList.get(k);
    }

    public Map<String,Integer> getMap() {
        return freqList;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        List<String> keys  = new ArrayList<>(freqList.keySet());
        keys.sort(Comparator.comparing(freqList::get));
        for (String k : keys) {
            str.append(k);
            str.append(" - ");
            str.append(freqList.get(k));
            str.append("\n");
        }
        return str.toString();
    }
}
