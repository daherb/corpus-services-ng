/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora;

import java.util.Collection;

/**
 *
 * @author fsnv625
 */
public interface CorpusFunction {

public Report execute(CorpusData cd);
 
public Report execute(Corpus c);

public Report execute(CorpusData cd, boolean fix);

public Report execute(Corpus c, boolean fix);

public Collection<Class<? extends CorpusData>> getIsUsableFor() ;

public String getDescription();

public String getFunction();

public Boolean getCanFix();
}
