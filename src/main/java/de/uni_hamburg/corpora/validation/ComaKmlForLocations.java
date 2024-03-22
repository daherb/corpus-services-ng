package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.jdom2.JDOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A class that identifies and lists fields which contain location information;
 * creates a list of different location names; gets geo-coordinates for the
 * location names via Google API.
 */
public class ComaKmlForLocations extends Checker implements CorpusFunction {

    String comaLoc = "";
    String kmlFile;
    HashMap<String, String> birthPlace; // hash map for holding the birthplaces of speakers
    HashMap<String, String> domicile; // hash map for storing the residences of speakers 
    HashMap<String, String> commLocation; // hash map for holding locations where the communications took place
    HashMap<String, String> lngLat; // hash map for holding coordinates of locations

    final String KEYBIRTHPLACE = "1a Place of birth";
    final String KEYBIRTHPLACELL = "1c Place of birth (LngLat)";
    final String KEYREGION = "2 Region";
    final String KEYCOUNTRY = "3 Country";
    final String KEYDOMICILE = "7a Domicile";
    final String KEYDOMICILELL = "7c Domicile (LngLat)";
    final String KEYOTHER = "8a Other information";
    final String KEYCOUNTRYBARE = "Country";
    final String KEYREGIONBARE = "Region";
    final String KEYSETTLEMENT = "Settlement";
    final String KEYSETTLEMENTLL = "Settlement (LngLat)";

    public ComaKmlForLocations(Properties properties) {
        //fix available
        super(true,properties);
    }

    /**
     * Main functionality of the feature;
     */
    @Override
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, TransformerConfigurationException, TransformerException, XPathExpressionException {
        Report stats = new Report();
        try {

            if (kmlFile != null) {
                stats = getCoordinates();
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(TypeConverter.String2InputStream(cd.toSaveableString())); // get the file as a document
                NodeList communications = doc.getElementsByTagName("Communication"); // get all the communications in the corpus
                NodeList speakers = doc.getElementsByTagName("Speaker"); // get all the speakers in the corpus

                if (birthPlace == null) {
                    birthPlace = new HashMap<>();
                }
                if (domicile == null) {
                    domicile = new HashMap<>();
                }
                if (commLocation == null) {
                    commLocation = new HashMap<>();
                }
                for (int i = 0; i < speakers.getLength(); i++) { //iterate through speakers
                    Element speaker = (Element) speakers.item(i);
                    Element sigle = (Element) speaker.getElementsByTagName("Sigle").item(0);
                    String sigleString = sigle.getTextContent();
                    NodeList locations = speaker.getElementsByTagName("Location");
                    String languageCode = speaker.getElementsByTagName("LanguageCode").item(0).getTextContent();
                    for (int j = 0; j < locations.getLength(); j++) {
                        Element location = (Element) locations.item(j);
                        if (location.getAttribute("Type").equals("Basic biogr. data")) {
                            NodeList keys = location.getElementsByTagName("Key");
                            String placeOfBirth = "";
                            String region = null;
                            String country = null;
                            String domicileStr = "";
                            boolean coorFlag = false;
                            boolean domCoor = false;
                            Element ref = null;
                            Element domRef = null;
                            for (int k = 0; k < keys.getLength(); k++) {
                                Element key = (Element) keys.item(k);
                                switch (key.getAttribute("Name")) {
                                    case KEYBIRTHPLACE:
                                        placeOfBirth = key.getTextContent();
                                        break;
                                    case KEYBIRTHPLACELL:
                                        coorFlag = true;
                                        break;
                                    case KEYREGION:
                                        region = key.getTextContent();
                                        ref = key;
                                        break;
                                    case KEYCOUNTRY:
                                        country = key.getTextContent();
                                        break;
                                    case KEYDOMICILE:
                                        domicileStr = key.getTextContent();
                                        break;
                                    case KEYDOMICILELL:
                                        domCoor = true;
                                        break;
                                    case KEYOTHER:
                                        domRef = key;
                                        break;
                                }
                            }
                            if (!placeOfBirth.equals("...") && !placeOfBirth.equals("")) {
                                if (placeOfBirth.endsWith("(?)")) {
                                    placeOfBirth = placeOfBirth.substring(0, placeOfBirth.indexOf(" (?)"));
                                }
                                if (placeOfBirth.endsWith("`")) {
                                    placeOfBirth = placeOfBirth.substring(0, placeOfBirth.indexOf("`"));
                                }
                                
                                // test for existing coordinates
                                String coordinates = "";
                                if(lngLat.containsKey(domicileStr + "-" + languageCode)){
                                    coordinates = lngLat.get(domicileStr + "-" + languageCode);
                                } else if(lngLat.containsKey(domicileStr + "-")){
                                    coordinates = lngLat.get(domicileStr + "-");
                                }
                                
                                if (coorFlag == false && (!coordinates.equals(""))) {
                                    Element coordinatesKey = doc.createElement("Key");
                                    coordinatesKey.setAttribute("Name", KEYBIRTHPLACELL);
                                    coordinatesKey.setTextContent(coordinates);                                    
                                    Element loc = (Element) location.getElementsByTagName("Description").item(0);
                                    loc.insertBefore(coordinatesKey, ref);
                                    String message = "Added Key " + KEYBIRTHPLACELL + ": " + coordinatesKey + ") from KML (" + kmlFile + ") " + domicileStr + "' "
                                            + "for speaker '" + sigleString + "'";
                                    stats.addFix(function, cd, message);
                                } else if (!lngLat.containsKey(placeOfBirth + "-" + languageCode)) {
                                    String message = "KML (" + kmlFile + ") does not contain the birthplace '" + placeOfBirth + "' "
                                            + "from speaker '" + sigleString + "'";
                                    System.out.println(message);
                                    stats.addWarning(function, cd, message);
                                }
                            }
                            
                            if (!domicileStr.equals("...") && !domicileStr.equals("")) {
                                if (domicileStr.endsWith("(?)")) {
                                    domicileStr = domicileStr.substring(0, domicileStr.indexOf(" (?)"));
                                }
                                if (domicileStr.endsWith("`")) {
                                    domicileStr = domicileStr.substring(0, domicileStr.indexOf("`"));
                                }
                                
                                // test for existing coordinates
                                String coordinates = "";
                                if(lngLat.containsKey(domicileStr + "-" + languageCode)){
                                    coordinates = lngLat.get(domicileStr + "-" + languageCode);
                                } else if(lngLat.containsKey(domicileStr + "-")){
                                    coordinates = lngLat.get(domicileStr + "-");
                                }
                        
                                if (domCoor == false && (!coordinates.equals(""))) {
                                    Element coordinatesKey = doc.createElement("Key");
                                    coordinatesKey.setAttribute("Name", KEYDOMICILELL);
                                    coordinatesKey.setTextContent(coordinates);
                                    Element loc = (Element) location.getElementsByTagName("Description").item(0);
                                    loc.insertBefore(coordinatesKey, domRef);
                                    String message = "Added Key " + KEYDOMICILELL + ": " + coordinatesKey + ") from KML (" + kmlFile + ") " + domicileStr + "' "
                                            + "for speaker '" + sigleString + "'";
                                    stats.addFix(function, cd, message);
                                } else if (!lngLat.containsKey(domicileStr + "-" + languageCode)) {
                                    String message = "KML (" + kmlFile + ") does not contain the domicile '" + domicileStr + "' "
                                            + "from speaker '" + sigleString + "'";
                                    System.out.println(message);
                                    stats.addWarning(function, cd, message);
                                }
                            }
                            birthPlace.put(sigleString, new String(placeOfBirth + ", " + region + ", " + country));
                            domicile.put(sigleString, domicileStr);
                            break;
                        }
                    }
                }
                for (int i = 0; i < communications.getLength(); i++) { //iterate through communications
                    Element communication = (Element) communications.item(i);
                    Element location = (Element) communication.getElementsByTagName("Location").item(0); // get the location of the communication
                    String communicationID = communication.getAttribute("Id"); // get communication id
                    String communicationName = communication.getAttribute("Name"); // get communication name
                    NodeList keys = location.getElementsByTagName("Key");
                    String country = "";
                    String region = "";
                    String settlement = "";
                    String languageCode = communication.getElementsByTagName("LanguageCode").item(0).getTextContent();
                    boolean coorFlag = false;
                    for (int j = 0; j < keys.getLength(); j++) {
                        Element key = (Element) keys.item(j);
                        switch (key.getAttribute("Name")) {
                            case KEYCOUNTRYBARE:
                                country = key.getTextContent();
                                break;
                            case KEYREGIONBARE:
                                region = key.getTextContent();
                                break;
                            case KEYSETTLEMENT:
                                settlement = key.getTextContent();
                                break;
                            case KEYSETTLEMENTLL:
                                coorFlag = true;
                                break;
                        }
                    }
                    if (!settlement.equals("...") && !settlement.equals("")) {
                        if (settlement.endsWith("(?)")) {
                            settlement = settlement.substring(0, settlement.indexOf(" (?)"));
                        }
                        if (settlement.endsWith("`")) {
                            settlement = settlement.substring(0, settlement.indexOf("`"));
                        }
                        
                        // test for existing coordinates
                        String coordinates = "";
                        if(lngLat.containsKey(settlement + "-" + languageCode)){
                            coordinates = lngLat.get(settlement + "-" + languageCode);
                        } else if(lngLat.containsKey(settlement + "-")){
                            coordinates = lngLat.get(settlement + "-");
                        }
                        
                        if (coorFlag == false && (!coordinates.equals(""))) {
                            Element coordinatesKey = doc.createElement("Key");
                            coordinatesKey.setAttribute("Name", KEYSETTLEMENTLL);
                            coordinatesKey.setTextContent(coordinates);                                    
                            Element loc = (Element) location.getElementsByTagName("Description").item(0);
                            loc.appendChild(coordinatesKey);
                            String message = "Added Key " + KEYSETTLEMENTLL + ": " + coordinatesKey + ") from KML (" + kmlFile + ") "
                                    + "for communication '" + communicationName + "'";
                            stats.addFix(function, cd, message);
                        } else if ((!lngLat.containsKey(settlement + "-" + languageCode)) && (!lngLat.containsKey(settlement + "-_any_"))) {
                            String message = "KML (" + kmlFile + ") does not contain the settlement '" + settlement + "' "
                                    + "from communication '" + communicationName + "'";
                            System.out.println(message);
                            stats.addWarning(function, cd, message);
                        }
                    }
                    commLocation.put(communicationID, new String(settlement + ", " + region + ", " + country));
                }
                if (fix) {
                    CorpusIO cio = new CorpusIO();
                    cd.updateUnformattedString(TypeConverter.W3cDocument2String(doc));
                    cio.write(cd, cd.getURL());
                }
            } else {
                stats.addCritical(function, "No KML file path supplied");
            }
        } catch (JDOMException ex) {
            Logger.getLogger(ComaKmlForLocations.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            stats.addException(function, ex, cd, "The KML file could not be parsed.");
        } catch (TransformerException ex) {
            stats.addException(function, ex, cd, "Unknown Transformer error.");
        } catch (XPathExpressionException ex) {
            stats.addException(function, ex, cd, "Unknown XPath error.");
        } catch (IOException ex) {
            stats.addException(function, ex, cd, "The KML file could not be parsed.");
        } catch (URISyntaxException ex) {
            stats.addException(function, ex, cd, "URI syntax Exception.");
        }
        return stats; // return the report with warnings
    }

    // sets the KML file path which is provided as input
    public void setKMLFilePath(String path) {
        this.kmlFile = path;
    }

    // the method for getting coordinates of locations in the kml file
    public Report getCoordinates() throws ParserConfigurationException, SAXException, IOException, JDOMException, URISyntaxException {
        Report stats = new Report();
        Document doc = null;
        CorpusIO cio = new CorpusIO();
        if (kmlFile != null) {
            URL url = Paths.get(kmlFile).toUri().toURL();
            String kmlString = cio.readExternalResourceAsString(url.toString());
            if (kmlString != null) {
                doc = TypeConverter.String2W3cDocument(kmlString);
                if (lngLat == null) {
                    lngLat = new HashMap<>();
                }
                if (doc != null) {
                    NodeList placeMarks = doc.getElementsByTagName("Placemark");
                    for (int i = 0; i < placeMarks.getLength(); i++) { //iterate through place marks
                        Element placeMark = (Element) placeMarks.item(i);
                        Element name = (Element) placeMark.getElementsByTagName("name").item(0);
                        String nameOfPlace = name.getTextContent();
                        String language = "";
                        NodeList data = placeMark.getElementsByTagName("Data");
                        for (int j = 0; j < data.getLength(); j++) {
                            Element datum = (Element) data.item(j);
                            if (datum.getAttribute("name").equals("lang")) {
                                Element value = (Element) datum.getElementsByTagName("value").item(0);
                                language = value.getTextContent();
                            }
                        }
                        String coordinatesWithAltitude = placeMark.getElementsByTagName("coordinates").item(0).getTextContent();
                        String coordinate = coordinatesWithAltitude.trim().substring(0, coordinatesWithAltitude.trim().lastIndexOf(","));
                        lngLat.put(nameOfPlace + "-" + language, coordinate);
                    }
                } else {
                    stats.addCritical(function, "KML file cannot be read");
                }
            } else {
                stats.addCritical(function, "KML file cannot be read");
            }
        } else {
            stats.addCritical(function, "No KML file path supplied");
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
        return Collections.singleton(ComaData.class);
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class identifies and lists fields which contain"
                + " location information; creates a list of different location names;"
                + " gets geo-coordinates for the location names via Google API.";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException, TransformerException, TransformerConfigurationException, XPathExpressionException {
        Report stats = new Report();
        cd = c.getComaData();
        stats = function(cd, fix);
        return stats;
    }

}
