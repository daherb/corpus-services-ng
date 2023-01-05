/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora;

import java.util.Collection;
import java.util.Map;

/**
 *
 * @author fsnv625
 */
public interface CorpusFunction {

public Result execute(CorpusData cd);
 
public Result execute(Corpus c);

public Result execute(CorpusData cd, boolean fix);

public Result execute(Corpus c, boolean fix);

public Collection<Class<? extends CorpusData>> getIsUsableFor() ;

public Map<String,String> getParameters();

public String getDescription();

public String getFunction();

public Boolean getCanFix();
}
