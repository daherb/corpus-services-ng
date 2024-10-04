/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.publication;

import de.uni_hamburg.corpora.CMDIMetadata;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.CorpusIO;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jdom2.JDOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import static de.uni_hamburg.corpora.utilities.TypeConverter.JdomDocument2W3cDocument;
import java.io.UnsupportedEncodingException;
import javax.xml.xpath.XPathExpressionException;

/**
 * This class registers Handle PIDs for URLs from specific places in CMDI files,
 * and replaces the original URLs with the Handle URLs
 *
 * @author daniel.jettka@uni-hamburg.de
 *
 * Last update
 * @author Herbert Lange
 * @version 20241004
 */
public class HandlePidRegistration extends Publisher implements CorpusFunction {
    
    String cmdiLoc = "";
    
    String EpicApiUser = ""; //e.g. 1008-01 for HZSK
    String EpicApiPass = ""; //e.g. K******* for HZSK
    String HandlePrefix = "11022"; // the default is the HZSK/CLARIN prefix 11022
    String HandleEndpoint = "http://pid.gwdg.de/handles/";
    String HandleUrlBase = "http://hdl.handle.net/";
    
    // names of XML elements in which URLs are found for which Handles shall be retrieved/registered
    String[] ElementNames = {"MdSelfLink", "ResourceRef", "IsPartOf", "PID"};
    
    
    
    public HandlePidRegistration(Properties properties) {
        super(properties);
    }

    @Override
    public Report function(CorpusData cd)
            throws SAXException, IOException, ParserConfigurationException {
        
        Report stats = new Report();     
        
        CMDIMetadata cmdi = (CMDIMetadata) cd;
        Document doc = JdomDocument2W3cDocument(cmdi.getJdom());
        
        //optional, but recommended
        //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();

        for (int x = 0; x < ElementNames.length; x++) {
            NodeList nodes = root.getElementsByTagName(ElementNames[x]);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i).getFirstChild();
                if (node == null) {
                    continue;
                }
                String oldURL = node.getTextContent();
                
                // test if the URL is already a Handle
                if(oldURL.matches("^\\s*(https?://)?hdl\\.handle\\.net/.*$")){
                    stats.addWarning(function, cd, "URL is already a Handle PID: " + oldURL);
                } 
                // if URL is not already a Handle
                else{

                    String newURL = oldURL;
                    String partIdentifier = "";
                    if(oldURL.matches("^.+/[A-Z0-9]{2,6}$") && !oldURL.endsWith("/CMDI")){
                        int endIndex = oldURL.lastIndexOf("/");
                        if(endIndex != -1){
                            partIdentifier = "@"+oldURL.substring(endIndex + 1);
                            oldURL = oldURL.substring(0, endIndex);
                        }
                    }

                    /* get existing PID for this url */
                    String existingHandle = getPID(oldURL);

                    /* there is a handle pid registered for this url already*/
                    if(existingHandle != null && !existingHandle.equals("")){
                        newURL = HandleUrlBase + HandlePrefix + "/" + existingHandle + partIdentifier;
                        newURL = newURL.replaceAll("[\\s\\n]+", "");
                        stats.addNote(function, cd, "Retrieved existing Handle PID for " + oldURL + ":\n" + newURL);                        
                        System.out.println("Retrieved existing Handle PID for " + oldURL + ":\n" + newURL);
                    }
                    /* for this url a new pid has to be registered */
                    else{
                        String newHandle = registerPID(oldURL);
                        newURL = HandleUrlBase + HandlePrefix + "/" + newHandle + partIdentifier;
                        newURL = newURL.replaceAll("[\\s\\n]+", "");
                        stats.addNote(function, cd, "Registered new Handle PID for " + oldURL + ":\n" + newURL);                      
                        System.out.println("Registered new Handle PID for " + oldURL + ":\n" + newURL);
                    }
                    
                    node.setNodeValue(newURL);
                }                
            }
        }
                
            
        String newCmdiXmlInStringOneLine = TypeConverter.W3cDocument2String(doc);

        //make sure XML prolog is on own line (needed for jOAI provider)
        //insert comment about automatic processing into file (between last processing instruction and directly following comment)
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date today = Calendar.getInstance().getTime();
        String newCmdiXmlInString = newCmdiXmlInStringOneLine.replaceAll("\\?>\\s*<!\\-\\-", "?>\n<!--Handle PIDs generated by HandlePIDs.java on "+df.format(today)+"-->\n<!--");

                
        try {
            CorpusIO cio = new CorpusIO();
            cd.updateUnformattedString(newCmdiXmlInString);
            cio.write(cd, cd.getURL());
            stats.addFix(function, cd, "Handle PIDs were retrieved and file was updated.");
        } catch (UnsupportedEncodingException ex) {
            stats.addCritical(function, cd, "UnsupportedEncodingException: " + ex);
        } catch (JDOMException ex) {
            stats.addCritical(function, cd, "JDOMException: " + ex);
        } catch (TransformerException ex) {
            stats.addCritical(function, cd, "TransformerException: " + ex);
        }
                
        return stats;
    }

    
    public String getPID(String handleURL)
            throws IOException{

        //http://pid.gwdg.de/handles/11022?URL=http://www.corpora.uni-hamburg.de/repository
        
        String authString = EpicApiUser + ":" + EpicApiPass;
        String authStringEnc = Base64.getEncoder().encodeToString(authString.getBytes("UTF-8"));

        URL url = new URL(HandleEndpoint + HandlePrefix + "?URL=" + handleURL);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
        urlConnection.setRequestProperty("Accept", "text/plain");
        try {
            int rc = urlConnection.getResponseCode();
            InputStream is = urlConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);

            int numCharsRead;
            char[] charArray = new char[1024];
            StringBuffer sb = new StringBuffer();
            while ((numCharsRead = isr.read(charArray)) > 0) {
                    sb.append(charArray, 0, numCharsRead);
            }

        return sb.toString();
        } catch(FileNotFoundException fnfe) {
            System.out.println("FileNotFound? " + fnfe.toString());
            return null;
        }
    }

    public String registerPID(String handleURL)
            throws ParserConfigurationException, SAXException, IOException{

        // "Accept:application/json" -H "Content-Type:application/json" -X POST --data '[{"type":"URL","parsed_data":"http://www.example.com/cmdi"}]' "http://pid.gwdg.de/handles/11022/"

        //http://pid.gwdg.de/handles/11022?URL=http://www.corpora.uni-hamburg.de/repository
        
        String authString = EpicApiUser + ":" + EpicApiPass;
        String authStringEnc = Base64.getEncoder().encodeToString(authString.getBytes("UTF-8"));

        URL object=new URL(HandleEndpoint + HandlePrefix + "/");
        HttpURLConnection con = (HttpURLConnection) object.openConnection();
        con.setRequestProperty("Authorization", "Basic " + authStringEnc);
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/xml");
        con.setRequestMethod("POST");

        OutputStreamWriter wr= new OutputStreamWriter(con.getOutputStream());
        wr.write("[{\"type\":\"URL\",\"parsed_data\":\""+handleURL+"\"}]");
        wr.flush();

        //display what returns the POST request
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(),"utf-8"));
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();

        String responseInString = ""+sb.toString();
        System.out.println(responseInString);
        Document responseAsXml = TypeConverter.String2W3cDocument(responseInString);
        Element root = responseAsXml.getDocumentElement();
        NodeList nList = root.getElementsByTagName("dd");
        String handlePID = nList.item(0).getFirstChild().getTextContent();
        handlePID = handlePID.replaceAll("[\\s\\n]+", "");

        return handlePID;
    }
    
    public void setUser(String user){
        EpicApiUser = user;
    }
       
    public void setPass(String pass){
        EpicApiPass = pass;
    }
    
    public void setHandlePrefix(String prefix){
        HandlePrefix = prefix;
    }
    

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        try {
            Class cl = Class.forName("de.uni_hamburg.corpora.CMDIMetadata");
            IsUsableFor.add(cl);
        } catch (ClassNotFoundException ex) {
            report.addException(ex, " usable class not found");
        }
        return IsUsableFor;
    }

    /**Default function which returns a two/three line description of what 
     * this class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class loads CMDI data and retrieves already existing or newly registered "
                + "Handle PIDs for URLs from specific XML elements.";
        return description;
    }

    @Override
    public Report function(Corpus c) throws SAXException, IOException, ParserConfigurationException {
        Report stats = new Report();
        for(CMDIMetadata cmdid : c.getCmdidata()){
            stats.merge(function(cmdid));
        }
        return stats;
    }

}
