package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.EXMARaLDATranscriptionData;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.jdom2.JDOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A class that checks IDs in speaker tables of EXB files correspond to 
 * respective abbreviations. If there are mismatches, it corrects them.
 * @author bay7303
 *
 * Last updated
 * @author Herbert Lange
 * @version 20241004
 */
public class ExbSpeakerTableChecker extends Checker implements CorpusFunction {
    
    public ExbSpeakerTableChecker(Properties props) {
        super(true,props);
    }
    
    
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(EXMARaLDATranscriptionData.class);
    }
    
    @Override
    public Report function(CorpusData cd, Boolean fix) throws IOException, SAXException, TransformerException, ParserConfigurationException, JDOMException {
        Report stats = new Report();
        Document doc = null;
        EXMARaLDATranscriptionData ecd = new EXMARaLDATranscriptionData();
        ecd = (EXMARaLDATranscriptionData) cd;
        doc = TypeConverter.JdomDocument2W3cDocument(ecd.getJdom());
        
        NodeList speakerTable = doc.getElementsByTagName("speaker");
        
        for (int i=0; i<speakerTable.getLength(); i++) {
            Element speakerEl = (Element) speakerTable.item(i);
            String speakerId = speakerEl.getAttribute("id");
            NodeList ab = speakerEl.getElementsByTagName("abbreviation");
            Element abbr = (Element) ab.item(0);
            String speakerAb = abbr.getTextContent();
                      
            if (!speakerAb.equals(speakerId)) {                
                if (fix) {
                    speakerEl.setAttribute("id", speakerAb);
                    String message = "Speaker id was fixed in speaker table, the updated value is " + speakerAb;
                    stats.addFix(function, cd, message);
                } else {
                    String message = "Speaker abbreviation " + speakerAb + " and speaker id " + speakerId + " do not match in the speaker table";
                    stats.addCritical(function, cd, message);
                }
            }
        }
        
        String result = TypeConverter.W3cDocument2String(doc);
        CorpusIO cio = new CorpusIO();
        if (fix) {
            cd.updateUnformattedString(result);
            cio.write(cd, cd.getURL());
        }
        
        return stats;
    }
    
    @Override
    public String getDescription() {
        String description = "A class that checks IDs in speaker tables of EXB files correspond to \n" +
        "respective abbreviations. If there are mismatches, it corrects them.";
        return description;
    }
    
    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, JDOMException {
        Report stats = new Report();
        for (CorpusData cdata : c.getBasicTranscriptionData()) {
            stats.merge(function(cdata, fix));
        }
        return stats;
    }
}
