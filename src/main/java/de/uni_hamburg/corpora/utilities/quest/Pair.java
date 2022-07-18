package de.uni_hamburg.corpora.utilities.quest;

import java.util.Objects;

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

    @Override
    public String toString() {
        return "<" +
                fst +
                ", " + snd +
                '>';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return fst.equals(pair.fst) && snd.equals(pair.snd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fst, snd);
    }
}
