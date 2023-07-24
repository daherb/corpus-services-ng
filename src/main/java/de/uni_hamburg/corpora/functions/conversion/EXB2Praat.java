/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_hamburg.corpora.functions.conversion;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.convert.PraatConverter;

/**
 *
 * @author Daniel Jettka
 * @author Herbert Lange
 * @version 20230105
 */
public class EXB2Praat {
    
    public EXB2Praat(){
    
    }
    
    
    public String convert(String basicTranscription){
        BasicTranscription bt = TypeConverter.String2BasicTranscription(basicTranscription);
        PraatConverter pc = new PraatConverter();
        return pc.BasicTranscriptionToPraat(bt);
    }
    
}
