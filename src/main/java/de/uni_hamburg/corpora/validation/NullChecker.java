package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.stream.Collectors;

public class NullChecker extends Checker implements CorpusFunction {

    public NullChecker() {
        super(false);
    }

    public NullChecker(boolean hasfixingoption) {
        super(hasfixingoption);
    }

    @Override
    public String getDescription() {
        return "Does nothing and never fails";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) {
        Report stats = new Report();
        // report success
        stats.addCorrect(function,cd,"Success");
        return stats;
    }

    @Override
    public Report function(Corpus c, Boolean fix) {
        Report stats = new Report();
        for (CorpusData cdata : c.getCorpusData()) {
            stats.merge(execute(cdata));
        }
        return stats;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        // Use reflections to get all corpus data classes
        Reflections reflections = new Reflections("de.uni_hamburg.corpora");
        // Get all classes derived from CorpusData
        return reflections.getSubTypesOf(CorpusData.class).stream().filter((c) -> !Modifier.isAbstract(c.getModifiers())).collect(Collectors.toSet());
    }


}
