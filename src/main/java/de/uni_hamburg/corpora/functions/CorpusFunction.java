/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.functions;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.Result;
import de.uni_hamburg.corpora.data.CorpusData;

import java.util.Collection;
import java.util.Map;

/**
 *
 * @author fsnv625
 * @author Herbert Lange
 * @version 20230105
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
