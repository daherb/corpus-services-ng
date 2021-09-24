/**
 * @file ComaErrorChecker.java
 *
 * Collection of checks for cmdi errors for HZSK repository purposes.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.CmdiData;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import static de.uni_hamburg.corpora.utilities.TypeConverter.JdomDocument2W3cDocument;

/**
 * A class that can load cmdi data and check for potential problems with HZSK
 * repository depositing.
 */
public class CmdiChecker extends Checker implements CorpusFunction {

    ValidatorSettings settings;

    public CmdiChecker() {
        //no fix available
        super(false);
    }

    private boolean isUrlHandleOrHzsk(String url) {
        if ((url.startsWith("http://hdl.handle.net/11022/")) ||
                (url.startsWith("https://corpora.uni-hamburg.de/repository/")) ||
                (url.startsWith("http://annis.corpora.uni-hamburg.de"))) {
            return true;
        } else {
            return false;
        }
    }

    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, IOException, ParserConfigurationException {
        CmdiData cmdi = (CmdiData) cd;
        Document doc = JdomDocument2W3cDocument(cmdi.getJdom());
        NodeList rps = doc.getElementsByTagName("ResourceProxy");
        Report stats = new Report();
        boolean hasLandingPage = false;
        for (int i = 0; i < rps.getLength(); i++) {
            Element rpe = (Element) rps.item(i);
            NodeList restypes = rpe.getElementsByTagName("ResourceType");
            Element restype = (Element) restypes.item(0);
            if (restype.getTextContent().equals("LandingPage")) {
                hasLandingPage = true;
                stats.addCorrect(function, cd, "Good resource type LandingPage");
            } else if (restype.getTextContent().equals("Resource")) {
                stats.addCorrect(function,  cd, 
                    "Good resource type Resource");
            } else if (restype.getTextContent().equals("SearchPage")) {
                stats.addCorrect(function,  cd, 
                    "Good resource type SearchPage");
            } else if (restype.getTextContent().equals("SearchService")) {
                stats.addCorrect(function,  cd, 
                    "Good resource type SearchService");
            } else if (restype.getTextContent().equals("Metadata")) {
                stats.addCorrect(function,  cd, 
                    "Good resource type Metadata");
            } else {
                stats.addWarning(function,  cd, 
                        "Unrecognised resource type "
                        + restype.getTextContent());
            }
            NodeList resrefs = rpe.getElementsByTagName("ResourceRef");
            Element resref = (Element) resrefs.item(0);
            String url = resref.getTextContent();
            if (!isUrlHandleOrHzsk(url)) {
                stats.addCritical(function, cd, 
                        "Invalid URL for reesource proxy:"
                        + url +
                        "URLs should start with http://hdl.handle.net... or "
                        + "https://corpora.uni-hamburg.de/repository/...");
            } else {
                stats.addCorrect(function,  cd, "Good resource proxy URL " + url);
            }
        }
        if (!hasLandingPage) {
            stats.addCritical(function,  cd,  "Missing landing page");
        } else {
            stats.addCorrect(function,  cd,  "Good landing page found");
        }
        NodeList gis = doc.getElementsByTagName("GeneralInfo");
        for (int i = 0; i < gis.getLength(); i++) {
            Node ginode = gis.item(i);
            if (ginode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element gi = (Element) ginode;
            NodeList childs = gi.getChildNodes();
            boolean englishTitle = false;
            boolean englishDesc = false;
            boolean legalOwner = false;
            boolean pidFound = false;
            for (int j = 0; j < childs.getLength(); j++) {
                Node n = childs.item(j);
                if (n.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element e = (Element) n;
                if (e.getTagName().equals("PID")) {
                    if (!isUrlHandleOrHzsk(e.getTextContent())) {
                        stats.addCritical(function,  cd, "Invalid URL for PID:"
                                + e.getTextContent() +
                                "URLs should start with "
                                + "http://hdl.handle.net... or "
                                + "https://corpora.uni-hamburg.de/repository/...");
                    } else {
                        stats.addCorrect(function, cd, "Good PID URL: "
                                + e.getTextContent());
                    }
                    pidFound = true;
                } else if (e.getTagName().equals("Description")) {
                    if (e.getAttribute("xml:lang").equals("en") || e.getAttribute("xml:lang").equals("eng")) {
                        englishDesc = true;
                        stats.addCorrect(function, cd, "English Description present");
                    }
                } else if (e.getTagName().equals("Title")) {
                    if (e.getAttribute("xml:lang").equals("en") || e.getAttribute("xml:lang").equals("eng")) {
                        englishTitle = true;
                        stats.addCorrect(function,  cd,  "English title present");
                    }
                } else if (e.getTagName().equals("LegalOwner")) {
                    legalOwner = true;
                    stats.addCorrect(function,  cd,  "LegalOwner present");
                } else {
                    System.out.println("DEBUG: GeneralInfo/" + e.getTagName());
                    // pass
                }
            }
            if (!englishTitle) {
                stats.addWarning(function, cd,  "English title missing from General Info "
                        + "(needed by FCS for example)");
            }
            if (!englishDesc) {
                stats.addWarning(function, cd,  "English Description missing from General Info "
                        + "(needed by FCS for example)");
            }
            if (!pidFound) {
                stats.addCritical(function,  cd,  "PID missing");
            }
        }
        NodeList cis = doc.getElementsByTagName("CorpusInfo");
        for (int i = 0; i < cis.getLength(); i++) {
            Node cinode = cis.item(i);
            if (cinode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element ci = (Element) cis.item(i);
            checkCorpusInfo(ci, stats, cd);
        }
        return stats;
    }

    private void checkCorpusInfo(Element ci, Report stats, CorpusData cd) {
        NodeList childs = ci.getChildNodes();
        boolean corpusType = false;
        boolean genre = false;
        boolean modality = false;
        boolean annotationTypes = false;
        boolean timeCoverage = false;
        for (int i = 0; i < childs.getLength(); i++) {
            Node n = childs.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element e = (Element) n;
            if (e.getTagName().equals("CorpusContext")) {
                NodeList cts = e.getElementsByTagName("CorpusType");
                if (cts.getLength() != 0) {
                    corpusType = true;
                }
            } else if (e.getTagName().equals("SubjectLanguages")) {
                checkSubjectLanguages(e, stats, cd);
            } else if (e.getTagName().equals("Coverage")) {
                NodeList tcs = e.getElementsByTagName("TimeCoverage");
                if (tcs.getLength() != 0) {
                    timeCoverage = true;
                }
                checkCoverage(e, stats, cd);
            } else if (e.getTagName().equals("Content")) {
                NodeList genres = e.getElementsByTagName("Genre");
                if (genres.getLength() != 0) {
                    genre = true;
                }
                NodeList modalities = e.getElementsByTagName("Modalities");
                if (modalities.getLength() != 0) {
                    modality = true;
                }
            } else {
                //
                System.out.println("DEBUG: CorpusInfo/" + e.getTagName());
            }
        }
        if (!corpusType) {
            stats.addCritical(function,  cd,  "Corpus type is needed for repo web pages");
        } else {
            stats.addCorrect(function,  cd,  "Corpus type included");
        }
        if (!genre) {
            stats.addCritical(function,  cd,  "Genre is needed for repo web pages");
        } else {
            stats.addCorrect(function,  cd,  "Genre included");
        }
        if (!modality) {
            stats.addCritical(function, cd,  "Modality is needed for repo web pages");
        } else {
            stats.addCorrect(function,  cd,  "modality included");
        }
        if (!timeCoverage) {
            stats.addWarning(function,  cd,  "time coverage is missing (recommended for VLO)");
        }
    }

    private void checkCoverage(Element coverage, Report stats, CorpusData cd) {
        NodeList timeCoverages = coverage.getElementsByTagName("TimeCoverage");
        for (int i = 0; i < timeCoverages.getLength(); i++) {
            Node n = timeCoverages.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element e = (Element) n;
            String tc = e.getTextContent();
            if (tc.matches("[0-9]+/[0-9]+")) {
                stats.addCorrect(function,  cd,  "Good time coverage");
            } else {
                stats.addCritical(function,  cd,  "TimeCoverage should be YYYY/YYYY for VLO");
            }
        }

    }

    private void checkSubjectLanguages(Element sls, Report stats, CorpusData cd) {
        NodeList langs = sls.getElementsByTagName("Language");
        for (int i = 0; i < langs.getLength(); i++) {
            Node n = langs.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element e = (Element) n;
            NodeList childs = e.getElementsByTagName("LanguageName");
            boolean engFound = false;
            for (int j = 0; j < childs.getLength(); j++) {
                Element lang = (Element) childs.item(j);
                if (lang.getAttribute("xml:lang").equals("eng")) {
                    engFound = true;
                }
            }
            if (!engFound) {
                stats.addCritical(function,  cd,  "Each subject language must have @xml:lang eng "
                        + "filled in");
            } else {
                stats.addCorrect(function,  cd,  "Goog language data");
            }
        }
    }


    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(CmdiData.class);
    }

    /**Default function which returns a two/three line description of what 
     * this class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class loads cmdi data and check for potential "
                + "problems with HZSK repository depositing.";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException {
        Report stats = new Report();
        for(CmdiData cmdid : c.getCmdidata()){
            stats.merge(function(cmdid, false));
        }
        return stats;
    }

}
