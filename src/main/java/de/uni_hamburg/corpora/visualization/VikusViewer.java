/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.visualization;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusIO;
import de.uni_hamburg.corpora.Report;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathFactory;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
import org.xml.sax.SAXException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opencsv.CSVReader;
import de.uni_hamburg.corpora.ComaData;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URL;

import org.jdom2.Attribute;
import org.jdom2.Element;

/**
 *
 * @author anne
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class VikusViewer extends Visualizer {

    private static final String CONFIG_PATH = "/vikus-viewer/config.json";
    private static final String DATA_PATH = "/vikus-viewer/data.csv";
    private static final String INFO_PATH = "/vikus-viewer/info.md";
    private static final String TIMELINE_PATH = "/vikus-viewer/timeline.csv";
    private static final String AUDIO_IMAGE_PATH = "/vikus-viewer/sound.jpg";
    ArrayList<String> keywordblacklist = new ArrayList<>();
    URL vikusviewerurl;
    String licence;
    String version;
    String corpusPrefix;
    String title;
    String description;
    ArrayList<String> allDistinctYears = new ArrayList<>();
    private final XPathFactory xpathFactory = new JaxenXPathFactory();

    public VikusViewer(Properties properties) {
        super(properties);
    }

    @Override
    public Report function(CorpusData cd) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report stats = new Report();
        ComaData coma = (ComaData) cd;
        keywordBlacklist();
        vikusviewerurl = new URL(cd.getParentURL() + "resources/vikus-viewer");
        File vikusviewerfolder = new File((vikusviewerurl).getFile());
        if (!vikusviewerfolder.exists()) {
            //the curation folder it not there and needs to be created
            vikusviewerfolder.mkdirs();
        }

        Element comadescription = coma.getCorpusDescription();
        Element descriptioncoma =
            new XPathBuilder<>("descendant::Key[@Name='DC:description']", Filters.element()).compileWith(xpathFactory).evaluateFirst(comadescription);
        description = descriptioncoma.getText();
        Element elcorpusPrefix =
                new XPathBuilder<>("descendant::Key[@Name='hzsk:corpusPrefix']", Filters.element()).compileWith(xpathFactory).evaluateFirst(comadescription);
        corpusPrefix = elcorpusPrefix.getText();
        Element eltitle =
                new XPathBuilder<>("descendant::Key[@Name='DC:title']", Filters.element()).compileWith(xpathFactory).evaluateFirst(comadescription);
        title = eltitle.getText();
        Element elversion =
                new XPathBuilder<>("descendant::Key[@Name='hzsk:corpusVersion']", Filters.element()).compileWith(xpathFactory).evaluateFirst(comadescription);
        version = elversion.getText();
        Element ellicence =
                new XPathBuilder<>("descendant::Key[@Name='DC:rights']", Filters.element()).compileWith(xpathFactory).evaluateFirst(comadescription);
        licence = ellicence.getText();
        stats.merge(createDataCSV(cd));
        stats.merge(createConfigJSON(cd));
        stats.merge(createInfoMD(cd));
        stats.merge(createTimelineCSV(cd));
        return stats;
    }

    public void keywordBlacklist() {
        keywordblacklist.add("and");
        keywordblacklist.add("a");
        keywordblacklist.add("the");
        keywordblacklist.add("i");
        keywordblacklist.add("in");
        keywordblacklist.add("are");
        keywordblacklist.add("is");
        keywordblacklist.add("how");
        keywordblacklist.add("an");
        keywordblacklist.add("on");
        keywordblacklist.add("of");
        keywordblacklist.add("my");
        keywordblacklist.add("with");
        keywordblacklist.add("at");
        keywordblacklist.add("...");
    }

    public Report createDataCSV(CorpusData cd) throws FileNotFoundException, IOException, JDOMException {
        //id,keywords,year,_dialect,_country,_region,_settlement,_language,_speaker,_transcription,_scorehtml,_listhtml,_pdf,_audio,_genre,_description
        //"sketch,drawing",1890,Ket,Russia,Tomsk Oblast,sel,https://corpora.uni-hamburg.de/hzsk/de/islandora/object/transcript:selkup-0.1_AR_1965_RestlessNight_transl/datastream/EXB/AR_1965_RestlessNight_transl.exb,https://corpora.uni-hamburg.de/hzsk/de/islandora/object/file:selkup-0.1_KFN_1965_BearHunting1_nar/datastream/PDF/KFN_1965_BearHunting1_nar.pdf,https://corpora.uni-hamburg.de/hzsk/de/islandora/object/recording:selkup-0.1_DN_196X_Bread_nar/datastream/MP3/DN_196X_Bread_nar.mp3,flk,Male Torso,KAI_1965_OldWitch_flk
        Report stats = new Report();
        CSVReader reader;
        CorpusIO cio = new CorpusIO();
        reader = new CSVReader(new InputStreamReader(getClass().getResourceAsStream(DATA_PATH)), ',');
        List<String[]> data = reader.readAll();
        //create Row ForCommunications
        ComaData coma = (ComaData) cd;
        String transrepourl = "https://corpora.uni-hamburg.de/repository/transcript:" + corpusPrefix + "-" + version + "_";
        String filerepourl = "https://corpora.uni-hamburg.de/repository/file:" + corpusPrefix + "-" + version + "_";
        String recrepourl = "https://corpora.uni-hamburg.de/repository/recording:" + corpusPrefix + "-" + version + "_";
        for (Element communication : coma.getCommunications()) {
            String[] comrow = new String[16];
            //id
            Attribute id =
                    new XPathBuilder<>("@Name", Filters.attribute()).compileWith(xpathFactory).evaluateFirst(communication);
            comrow[0] = id.getValue();
            //keyword - year, genre, Title splitted by spaces
            Element year =
                    new XPathBuilder<>("descendant::Description/Key[contains(@Name,'Date of recording')]", Filters.element()).compileWith(xpathFactory).evaluateFirst(communication);
            System.out.println(year.getText());
            if (!allDistinctYears.contains(year.getText())) {
                allDistinctYears.add(year.getText());
            }
            Element descriptiondesc =
                    new XPathBuilder<>("descendant::Description/Key[contains(@Name,'Title')]", Filters.element()).compileWith(xpathFactory).evaluateFirst(communication);

            Element genre =
                    new XPathBuilder<>("descendant::Description/Key[contains(@Name,'Genre')]", Filters.element()).compileWith(xpathFactory).evaluateFirst(communication);
            System.out.println(genre.getText());
            Element settlement =
                    new XPathBuilder<>("descendant::Location/Description/Key[contains(@Name,'Settlement')]", Filters.element()).compileWith(xpathFactory).evaluateFirst(communication);
            if(settlement==null){
                settlement = new Element("Settlement");
            }
            System.out.println(settlement.getText());
            Element speaker =
                    new XPathBuilder<>("descendant::Description/Key[contains(@Name,'Speakers')]", Filters.element()).compileWith(xpathFactory).evaluateFirst(communication);
            System.out.println(speaker.getText());
            String keywords = "\"";
            if (descriptiondesc != null) {
                System.out.println(descriptiondesc.getText());

                for (String s : descriptiondesc.getText().split(" ")) {
                    if (!keywordblacklist.contains(s.toLowerCase())) {
                        keywords += s + ",";
                    }
                }
            }
            keywords += year.getText() + "," + genre.getText() + "," + settlement.getText() + "," + speaker.getText() + "\"";
            comrow[1] = keywords;
            //year - Description Date of Recording
            comrow[2] = cleanForCSV(year.getText());
            //dialect
            Element dialect =
                    new XPathBuilder<>("descendant::Description/Key[contains(@Name,'Dialect')]", Filters.element()).compileWith(xpathFactory).evaluateFirst(communication);
            System.out.println(dialect.getText());
            comrow[3] = cleanForCSV(dialect.getText());
            //country
            Element country =
                    new XPathBuilder<>("descendant::Location/Description/Key[contains(@Name,'Country')]", Filters.element()).compileWith(xpathFactory).evaluateFirst(communication);
            if(country==null){
                country = new Element("Country");
            }
            System.out.println(country.getText());
            comrow[4] = cleanForCSV(country.getText());
            //region
            Element region =
                    new XPathBuilder<>("descendant::Location/Description/Key[contains(@Name,'Region')]", Filters.element()).compileWith(xpathFactory).evaluateFirst(communication);
            if(region==null){
                region = new Element("Region");
            }
            System.out.println(region.getText());
            comrow[5] = cleanForCSV(region.getText());
            //settlement
            comrow[6] = cleanForCSV(settlement.getText());
            //language
            Element language =
                    new XPathBuilder<>("descendant::Language/LanguageCode", Filters.element()).compileWith(xpathFactory).evaluateFirst(communication);
            System.out.println(language.getText());
            comrow[7] = cleanForCSV(language.getText());
            //speaker
            comrow[8] = "\"" + speaker.getText() + "\"";
            //transcription url
            //needs to look like https://corpora.uni-hamburg.de/repository/transcript:selkup-1.0_DN_196X_Bread_nar/EXB/DN_196X_Bread_nar.exb 
            String transcrurl = transrepourl + id.getValue() + "/EXB/" + id.getValue() + ".exb";
            //Element transcription = (Element) XPath.selectSingleNode(communication, "descendant::Transcription/NSLink");
            //System.out.println(transcription.getText());
            //comrow[8] = transcription.getText();
            comrow[9] = transcrurl;
            //scorehtml url
            //needs to look like 
            //https://corpora.uni-hamburg.de/repository/transcript:selkup-1.0_AGS_1964_SnakeInMouth_flk/SCORE/AGS_1964_SnakeInMouth_flk-score.html 
            String scoreurl = transrepourl + id.getValue() + "/SCORE/" + id.getValue() + "-score.html";
            comrow[10] = scoreurl;
            //listhtml url
            //needs to look like 
            //https://corpora.uni-hamburg.de/repository/transcript:selkup-1.0_AGS_1964_SnakeInMouth_flk/LIST/AGS_1964_SnakeInMouth_flk-list.html
            String listurl = transrepourl + id.getValue() + "/LIST/" + id.getValue() + "-list.html";
            comrow[11] = listurl;
            //pdf url
            Element pdf =
                    new XPathBuilder<>("descendant::File[mimetype='application/pdf']/relPath", Filters.element()).compileWith(xpathFactory).evaluateFirst(communication);
            //audio url
            Element audio =
                    new XPathBuilder<>("descendant::Recording/Media/NSLink", Filters.element()).compileWith(xpathFactory).evaluateFirst(communication);
            //check for cases with no audio and no pdf or both!
            String pdfrurl = filerepourl + id.getValue() + "/PDF/" + id.getValue() + ".pdf";
            String audiourl = recrepourl + id.getValue() + "/MP3/" + id.getValue() + ".mp3";
            Element transcription =
                    new XPathBuilder<>("descendant::Transcription/NSLink", Filters.element()).compileWith(xpathFactory).evaluateFirst(communication);
            URL imageLocation = null;
            if (transcription != null) {
                imageLocation = new URL(cd.getParentURL() + transcription.getText().replaceFirst("[.][^.]+$", "") + ".jpg");
            } else {
                stats.addCritical(function, cd, id.getValue() + ": No transcription linked in communication in the coma file!");
            }
            if (pdf == null && audio == null) {
                comrow[12] = "np pdf";
                comrow[13] = "no audio";
                stats.addCritical(function, cd, id.getValue() + ": No audio or pdf linked in communication in the coma file!");
            } else if (pdf != null && audio != null) {
                //we have both - add both links but don't add an audio image
                comrow[12] = pdfrurl;
                comrow[13] = audiourl;
                stats.addCritical(function, cd, id.getValue() + ": Audio AND pdf linked in communication in the coma file!");
            } else if (pdf != null) {
                comrow[12] = pdfrurl;
                comrow[13] = "no audio";
                //TODO
                //now we need an image jpeg of the first page
                //cio.copyInternalBinaryFile(PDF_IMAGE_PATH, imageLocation); 
            } else {
                comrow[12] = "no pdf";
                comrow[13] = audiourl;
                //now save the audio image in the folder with the correct name
                if (imageLocation != null) {
                    cio.copyInternalBinaryFile(AUDIO_IMAGE_PATH, imageLocation);
                }
            }
            //genre
            System.out.println(genre.getText());
            comrow[14] = genre.getText();
            //description
            if (descriptiondesc != null) {
                String descdesc = "\"" + descriptiondesc.getText() +  "\"";
                comrow[15] = descdesc;
            }
            data.add(comrow);
        }
        String newdata = "";
        for (String[] row : data) {
            newdata += String.join(",", row) + "\n";
            //first row = keys
            //other rows = values
        }
        //now save the string array as csv
        URL configJSONlocation = new URL(vikusviewerurl + "/data.csv");
        cio.write(newdata, configJSONlocation);
        stats.addCorrect(function, cd, "vikus-viewer config successfully created at " + configJSONlocation.toString());
        return stats;
    }

    public Report createConfigJSON(CorpusData cd) throws JDOMException, IOException {
        Report stats = new Report();
        CorpusIO cio = new CorpusIO();
        String config = cio.readInternalResourceAsString(CONFIG_PATH);
        JsonElement jelement = new JsonParser().parse(config);
        JsonObject jobject = jelement.getAsJsonObject();
        jobject = jobject.getAsJsonObject("project");
        jobject.addProperty("name", title + " " + version);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJsonString = gson.toJson(jelement);
        //System.out.println(prettyJsonString);
        //now save it pretty printed
        URL configJSONlocation = new URL(vikusviewerurl + "/config.json");
        cio.write(prettyJsonString, configJSONlocation);
        stats.addCorrect(function, cd, "vikus-viewer config successfully created at " + configJSONlocation.toString());
        return stats;
    }

    public Report createInfoMD(CorpusData cd) throws JDOMException, IOException {
        Report stats = new Report();
        CorpusIO cio = new CorpusIO();
        String info = cio.readInternalResourceAsString(INFO_PATH);
        String corpusnameandversion = title + " " + version;
        info = info.replaceAll("_CORPUSNAME_", corpusnameandversion);
        //_DESCRIPTION_  <Key Name="DC:description">
        info = info.replaceAll("_DESCRIPTION_", description);
        //_LICENCE_      <Key Name="DC:rights">CC BY-NC-SA 4.0</Key>
        info = info.replaceAll("_LICENCE_", licence);
        //now save the string array as csv
        URL infoMDlocation = new URL(vikusviewerurl + "/info.md");
        cio.write(info, infoMDlocation);
        stats.addCorrect(function, cd, "vikus-viewer info.md successfully created at " + infoMDlocation.toString());
        return stats;
    }

    public Report createTimelineCSV(CorpusData cd) throws FileNotFoundException, IOException, JDOMException {
        //year,titel,text,extra,link,kategorie
        //1864,Early work,"Vincent begins drawing his surroundings early, at the age of 11 here.","The family van Gogh lives in the small town Zundert in the South of the Netherlands. Vincent later visits a middle school in Tilburg, where he lives far from his family. Despite his good grades, he leaves school in 1868, aged 15. From now on, he works for the international art firm Goupil & Cie.",,
        Report stats = new Report();
        CSVReader reader;
        CorpusIO cio = new CorpusIO();
        reader = new CSVReader(new InputStreamReader(getClass().getResourceAsStream(TIMELINE_PATH)), ',');
        Collections.sort(allDistinctYears);
        List<String[]> time = reader.readAll();
        for (String year : allDistinctYears) {
            String[] timerow = new String[6];
            timerow[0] = year;
            timerow[1] = "";
            timerow[2] = "";
            timerow[3] = "";
            timerow[4] = "";
            timerow[5] = "";
            time.add(timerow);
        }
        String newtime = "";
        for (String[] row : time) {
            newtime += String.join(",", row) + "\n";
        }
        //now save the string array as csv
        URL timelineCSVlocation = new URL(vikusviewerurl + "/timeline.csv");
        cio.write(newtime, timelineCSVlocation);
        stats.addCorrect(function, cd, "vikus-viewer config successfully created at " + timelineCSVlocation.toString());
        return stats;
    }

    @Override
    public Report function(Corpus c) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report stats;
        cd = c.getComaData();
        stats = function(cd);
        return stats;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        try {
            Class cl = Class.forName("de.uni_hamburg.corpora.ComaData");
            IsUsableFor.add(cl);
        } catch (ClassNotFoundException ex) {
            report.addException(ex, "Usable class not found.");
        }
        return IsUsableFor;
    }
    
    public String cleanForCSV(String s){
        s = s.replace(',', ' ');
        s = s.replace('"', ' ');
        s = s.replace('\'', ' ');
        return s;
    }

    @Override
    public String getDescription() {
        String description = "This class creates an config files needed "
                + "for the vikus-viewer software. ";
        return description;
    }

}
