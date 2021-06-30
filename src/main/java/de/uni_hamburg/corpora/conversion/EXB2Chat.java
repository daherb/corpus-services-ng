/**
 * @File EXB2Chat.java
 */
package de.uni_hamburg.corpora.conversion;

import de.uni_hamburg.corpora.ComaData;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.jdom.JDOMException;
import org.jdom.transform.XSLTransformException;
import org.xml.sax.SAXException;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.segment.CHATSegmentation;
import org.exmaralda.partitureditor.jexmaralda.ListTranscription;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import java.util.Collection;
import java.net.URL;
import de.uni_hamburg.corpora.CorpusIO;
import javax.xml.transform.TransformerException;
import javax.xml.parsers.ParserConfigurationException;
import net.sf.saxon.trans.XPathException;
import javax.xml.xpath.XPathExpressionException;

/**
 *
 * @author Tommi A Pirinen
 */
public class EXB2Chat extends Converter implements CorpusFunction {

    Boolean COMA = false;

    public EXB2Chat() {

    }

    public static final String EXB2CHAT = "exb2chat";

    /*
    * this method takes a CorpusData object, converts it into CHAT and saves it 
    * next to the CorpusData object
    * and gives back a report how it worked
     */
    /**
     *
     * @param cd
     * @return
     * @throws SAXException
     * @throws FSMException
     * @throws XSLTransformException
     * @throws JDOMException
     * @throws IOException
     * @throws Exception
     */
    public Report function(CorpusData cd) throws SAXException,
            FSMException,
            XSLTransformException,
            JDOMException,
            IOException,
            Exception {
        //it cannot be a coma file alone
        return convert(cd);
    }

    public Report function(Corpus c) throws SAXException,
            FSMException,
            XSLTransformException,
            JDOMException,
            IOException,
            Exception {
        COMA = true;
        ComaData comad = c.getComaData();
        return convert(comad);
    }

    public Report convert(CorpusData cod) {
        Report stats = new Report();
        try {
            String basicTranscription = cod.toSaveableString();
            BasicTranscription bt = TypeConverter.String2BasicTranscription(basicTranscription);
            String data = exportCHATTranscript(bt, "UTF-8");
            URL targeturl = new URL(cd.getParentURL() + cd.getFilenameWithoutFileEnding() + ".chat");
            CorpusIO cio = new CorpusIO();
            cio.write(data, targeturl);
            stats.addCorrect(EXB2CHAT, cod, "Conversion of file was successfully saved at " + targeturl);
        } catch (JexmaraldaException je) {
            stats.addException(EXB2CHAT, je, cod, "Input Output Exception");
        } catch (FSMException fe) {
            stats.addException(EXB2CHAT, fe, cod, "Input Output Exception");
        } catch (SAXException se) {
            stats.addException(EXB2CHAT, se, cod, "Input Output Exception");
        } catch (FileNotFoundException fnfe) {
            stats.addException(EXB2CHAT, fnfe, cod, "Input Output Exception");
        } catch (IOException ioe) {
            stats.addException(EXB2CHAT, ioe, cod, "Input Output Exception");
        } catch (XPathException xe) {
            stats.addException(EXB2CHAT, xe, cod, "Input Output Exception");
        } catch (XPathExpressionException xee) {
            stats.addException(EXB2CHAT, xee, cod, "Input Output Exception");
        } catch (ParserConfigurationException pe) {
            stats.addException(EXB2CHAT, pe, cod, "Input Output Exception");
        } catch (TransformerException te) {
            stats.addException(EXB2CHAT, te, cod, "Input Output Exception");
        }
        return stats;
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        try {
            Class cl = Class.forName("de.uni_hamburg.corpora.BasicTranscriptionData");
            IsUsableFor.add(cl);
        } catch (ClassNotFoundException ex) {
            report.addException(ex, "Usable class not found.");
        }
        return IsUsableFor;
    }


    /**
     * export chat transcirpt from org/exmaralda/exmaralda/src/
     */
    String exportCHATTranscript(BasicTranscription bt, String encoding)
            throws JexmaraldaException, FSMException, SAXException,
            FileNotFoundException, IOException {
        // segment the basic transcription and transform it into a list transcription
        CHATSegmentation segmenter = new org.exmaralda.partitureditor.jexmaralda.segment.CHATSegmentation();
        ListTranscription lt = segmenter.BasicToUtteranceList(bt);
        String text = CHATSegmentation.toText(lt);
        return text;
    }

    @Override
    public String getDescription() {
         String description = "This class takes an exb as input and converts it into CHAT format. ";
        return description;
    }

}
