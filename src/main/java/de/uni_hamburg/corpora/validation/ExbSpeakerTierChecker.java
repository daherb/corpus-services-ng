package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.EXMARaLDATranscriptionData;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
 * A class that checks speaker attributes in tiers of EXB files correspond to 
 * values in the speaker table. For files with only one speaker, it corrects them.
 * @author bay7303
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class ExbSpeakerTierChecker extends Checker implements CorpusFunction {
    
    public ExbSpeakerTierChecker(Properties props) {
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
        ArrayList<String> speakerIds = new ArrayList();
        
        for (int i=0; i<speakerTable.getLength(); i++) {
            Element speakerEl = (Element) speakerTable.item(i);
            String speakerId = speakerEl.getAttribute("id");
            speakerIds.add(speakerId);
        }
        
        NodeList tierList = doc.getElementsByTagName("tier");
        
        for (int i=0; i<tierList.getLength(); i++) {
            Element tier = (Element) tierList.item(i);
            String speakerTier = tier.getAttribute("speaker");
            String tierId = tier.getAttribute("id");
            if (!speakerIds.contains(speakerTier)) {
                if (fix && speakerIds.size() == 1 ) {
                    tier.setAttribute("speaker", speakerIds.get(0));
                    String message = "Speaker attribute was fixed in tier " + tierId + ", the updated value is " + speakerIds.get(0);
                    stats.addFix(function, cd, message);                    
                } else {
                    String message = "No corresponding id in the speaker table was found for speaker attribute of the tier " + tierId;
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
        String description = "A class that checks speaker attributes in tiers of EXB files correspond to \n" +
        "values in the speaker table. For files with only one speaker, it corrects them.";
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
