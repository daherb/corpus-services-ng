package de.uni_hamburg.corpora.conversion;

import com.opencsv.CSVReader;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.xml.sax.SAXException;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 *
 * @author fsnv625
 *
 * this class can be used from the command line to insert data in a csv file
 * into an existing coma file there needs to be a header with information of the
 * information in the columns the first line has to consist of the sigle of the
 * speaker or name of the communication the metadata should be assigned to
 */
public class AddCSVMetadataToComa extends Converter implements CorpusFunction {

    private String comaFile;
    private String csvFile;
    private Document coma;
    private String SpeakerOrCommunication;
    private Boolean IsSpeaker;

    /**
     * creates a new instance of AddCSVMetadataToComa
     */
    public AddCSVMetadataToComa(String corpusPath) {
    }

    public AddCSVMetadataToComa() {
    }

    /**
     * creates a new instance of AddCSVMetadataToComa
     */
    public AddCSVMetadataToComa(String corpusPath, String csvPath, String SpeakerOrCommunication) {
        this.comaFile = corpusPath;
        this.csvFile = csvPath;
        this.SpeakerOrCommunication = SpeakerOrCommunication;
        if (SpeakerOrCommunication.equals("speaker")) {
            IsSpeaker = true;
        }
        if (SpeakerOrCommunication.equals("communication")) {
            IsSpeaker = false;
        }
    }

    /**
     * Default check function which calls the exceptionalCheck function so that
     * the primal functionality of the feature can be implemented, and
     * additionally checks for parser configuration, SAXE and IO exceptions.
     */
    public Report check(CorpusData cd) throws SAXException, JexmaraldaException {
        Report stats = new Report();
        try {
            stats = function(cd);
        } catch (ParserConfigurationException pce) {
            stats.addException(function, pce, cd, "Unknown parsing error");
        } catch (IOException ioe) {
            stats.addException(function, ioe, cd, "Unknown file reading error");
        } catch (JDOMException ex) {
             stats.addException(function, ex, cd, "Unknown JDOM error");
        }
        return stats;
    }

    /**
     * The primary functionality of the class; it accepts the coma file and
     * writes the CSV data into it.
     */
    public Report function(CorpusData cd)
            throws SAXException, IOException, ParserConfigurationException, JexmaraldaException, JDOMException {
        Report stats = new Report(); //create a new report
        this.comaFile = cd.getURL().getPath(); // set the path of the coma file
        List<String[]> allElements = readData(); // read the data from the csv file
        // put the elements in the report
        for (String[] row : allElements) {
            System.out.println(Arrays.toString(row));
            stats.addNote(function, cd, Arrays.toString(row));
        }
        System.out.println(Arrays.toString(allElements.get(0)));
        stats.addNote(function, cd, Arrays.toString(allElements.get(0)));
        System.out.println(allElements.get(0)[0]);
        stats.addNote(function, cd, allElements.get(0)[0]);

        coma = org.exmaralda.common.jdomutilities.IOUtilities.readDocumentFromLocalFile(comaFile);
        //add the key and value to speaker/description or communication/description
        for (int i = 1; i < allElements.size(); i++) {
            for (int a = 1; a < allElements.get(i).length; a++) {
                if (IsSpeaker) {
                    //the place is the xpath where it should be inserted
                    String place = "//Speaker[Sigle/text()=\"" + allElements.get(i)[0] + "\"]/Description";
                    System.out.println(place);
                    stats.addNote(function, cd, place);
                    XPath p = XPath.newInstance(place);
                    //System.out.println(p.selectSingleNode(coma));
                    Object o = p.selectSingleNode(coma);
                    if (o != null) {
                        Element desc = (Element) o;
                        //the new Key element that is inserted
                        Element key = new Element("Key");
                        desc.addContent(key);
                        key.setAttribute("Name", allElements.get(0)[a]);
                        System.out.println(desc.getAttributes());
                        stats.addNote(function, cd, Arrays.toString(desc.getAttributes().toArray()));
                        key.setText(allElements.get(i)[a]);
                    }
                }
                if (!IsSpeaker) {
                    //the place is the xpath where it should be inserted
                    String place = "//Communication[@Name=\"" + allElements.get(i)[0] + "\"]/Description";
                    System.out.println(place);
                    stats.addNote(function, cd, place);
                    XPath p = XPath.newInstance(place);
                    System.out.println(p.selectSingleNode(coma));
                    stats.addNote(function, cd, p.selectSingleNode(coma).toString());
                    Object o = p.selectSingleNode(coma);
                    if (o != null) {
                        Element desc = (Element) o;
                        //the new Key element that is inserted
                        Element key = new Element("Key");
                        desc.addContent(key);
                        key.setAttribute("Name", allElements.get(0)[a]);
                        System.out.println(desc.getAttributes());
                        stats.addNote(function, cd, Arrays.toString(desc.getAttributes().toArray()));
                        key.setText(allElements.get(i)[a]);
                    }
                }
            }
        }
        //save the coma file!
        Writer fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(comaFile), "UTF8"));
        XMLOutputter serializer = new XMLOutputter();
        serializer.output(coma, fileWriter);
        stats.addNote(function, cd, "The data in the csv file has been added into the coma.");
        return stats;
    }

    /**
     * gets the csv data and puts it into the coma file
     */
    public void inputData() throws IOException, JDOMException {
        insertDataIntoComa(readData());
    }

    /**
     * reads the data from the csv file
     */
    public List<String[]> readData() throws FileNotFoundException, IOException {
        CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
        List<String[]> allElements = null;
        allElements = reader.readAll();
        return allElements;
    }

    /**
     * puts the data into the coma file
     */
    public void insertDataIntoComa(List<String[]> allElements) throws JDOMException, IOException {

        for (String[] row : allElements) {
            System.out.println(Arrays.toString(row));

            //first row = keys
            //other rows = values
            //first column = communication or speaker name
        }
        System.out.println(Arrays.toString(allElements.get(0)));

        System.out.println(allElements.get(0)[0]);
        coma = org.exmaralda.common.jdomutilities.IOUtilities.readDocumentFromLocalFile(comaFile);
        //add the key and value to speaker/description or communication/description
        for (int i = 1; i < allElements.size(); i++) {
            for (int a = 1; a < allElements.get(i).length; a++) {
                if (IsSpeaker) {
                    //the place is the xpath where it should be inserted
                    String place = "//Speaker[Sigle/text()=\"" + allElements.get(i)[0] + "\"]/Description";
                    System.out.println(place);
                    XPath p = XPath.newInstance(place);
                    //System.out.println(p.selectSingleNode(coma));
                    Object o = p.selectSingleNode(coma);
                    if (o != null) {
                        Element desc = (Element) o;
                        //the new Key element that is inserted
                        Element key = new Element("Key");
                        desc.addContent(key);
                        key.setAttribute("Name", allElements.get(0)[a]);
                        System.out.println(desc.getAttributes());
                        key.setText(allElements.get(i)[a]);
                    }
                }
                if (!IsSpeaker) {
                    //the place is the xpath where it should be inserted
                    String place = "//Communication[@Name=\"" + allElements.get(i)[0] + "\"]/Description";
                    System.out.println(place);
                    XPath p = XPath.newInstance(place);
                    System.out.println(p.selectSingleNode(coma));
                    Object o = p.selectSingleNode(coma);
                    if (o != null) {
                        Element desc = (Element) o;
                        //the new Key element that is inserted
                        Element key = new Element("Key");
                        desc.addContent(key);
                        key.setAttribute("Name", allElements.get(0)[a]);
                        System.out.println(desc.getAttributes());
                        key.setText(allElements.get(i)[a]);
                    }
                }
            }
        }
        //save the coma file!
        Writer fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(comaFile), "UTF8"));
        XMLOutputter serializer = new XMLOutputter();
        serializer.output(coma, fileWriter);
    }

    //@Override
    public String getXpathToTranscriptions() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    //@Override
    public void process(String filename) throws JexmaraldaException, SAXException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    // sets the CSV file path which is provided as input
    public void setCSVFilePath(String path) {
        this.csvFile = path;
    }

    // set what sort of data the csv file contain which will eventually be added to the coma
    public void setSpeakerOrCommunication(String spOrCommInput) {
        if (spOrCommInput.equals("speaker")) {
            IsSpeaker = true;
        }
        if (spOrCommInput.equals("communication")) {
            IsSpeaker = false;
        }
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        try {
            Class cl = Class.forName("de.uni_hamburg.corpora.ComaData");
            IsUsableFor.add(cl);
        } catch (ClassNotFoundException ex) {
            report.addException(ex, "unknown class not found error");
        }
        return IsUsableFor;
    }

    @Override
    public String getDescription() {
        String description = "this class can be used from the command line to insert data in a csv file "
                + "  into an existing coma file there needs to be a header with information of the "
                + "  information in the columns the first line has to consist of the sigle of the "
                + "  speaker or name of the communication the metadata should be assigned to";
        return description;
    }

    @Override
    public Report function(Corpus c) throws Exception, NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


}
