package de.uni_hamburg.corpora.utilities.quest;

/**
 * Class implementing a simple pair of values
 * @param <T> the type of the first field
 * @param <U> the type of the second field
 */
public class Pair<T,U> {
    private T fst;
    private U snd;

    public Pair(T fst, U snd) {
        this.fst = fst;
        this.snd = snd;
    }

    public T getFirst() {
        return fst;
    }

    public U getSecond() {
        return snd;
    }

    public void setFirst(T fst) {
        this.fst = fst;
    }

    public void setSecond(U snd) {
        this.snd = snd;
    }
}
