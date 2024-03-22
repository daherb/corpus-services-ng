/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora;

import java.net.URL;
import java.util.Collection;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.JDOMException;
import org.jdom2.xpath.XPath;
import org.xml.sax.SAXException;

/**
 *
 * @author fsnv625
 */
public class Corpus {

    //only the metadata file, coma or cmdi in most cases, or a list of files
    Collection<Metadata> metadata = new ArrayList<>();
    //the transcriptions
    Collection<ContentData> contentdata = new ArrayList<>();
    Collection<Recording> recording = new ArrayList<>();
    Collection<AdditionalData> additionaldata = new ArrayList<>();
    Collection<AnnotationSpecification> annotationspecification = new ArrayList<>();
    Collection<ConfigParameters> configparameters = new ArrayList<>();
    private Collection<CmdiData> cmdidata = new ArrayList<>();
    Collection<EXMARaLDATranscriptionData> basictranscriptiondata = new ArrayList<>();
    Collection<EXMARaLDASegmentedTranscriptionData> segmentedtranscriptiondata = new ArrayList<>();
    Collection<ELANData> elandata = new ArrayList<>();
    Collection<FlextextData> flextextdata = new ArrayList<>();
    ComaData comadata;
    //all the data together
    Collection<CorpusData> cdc = new HashSet<>();
    URL basedirectory;
    String corpusname;

    public Corpus() {
    }

    public Corpus(URL url) throws JexmaraldaException, URISyntaxException, IOException, ClassNotFoundException, SAXException {
        // TODO this could be problematic
        this(new CorpusIO().read(url, new Report()));
    }

    //only read in the files we need!
    public Corpus(ComaData coma, Collection<Class<? extends CorpusData>> clcds) throws SAXException, JexmaraldaException, URISyntaxException, IOException, ClassNotFoundException, JDOMException {
        CorpusIO cio = new CorpusIO();
        //todo: only read what we need :)
        //cl.isInstance(cd) - needs to be read already for this :/
        //TODO
        //get the needed files from the NSLinks in the coma file as URLs
        // public Collection<URL> URLtoList(URL url)
        Collection<URL> urllist = coma.getReferencedCorpusDataURLs();
        basedirectory = coma.getParentURL();
        corpusname = coma.getCorpusName();
        for (URL url : urllist) {
            CorpusData cddd = cio.readFileURL(url, clcds);
            if (cddd != null && !cdc.contains(cddd)) {
                cdc.add(cddd);
            }
        }
        //Coma is coma is
        comadata = coma;
        // Now sort the corpus data files into categories
        addCorpusDataCollection(cdc);
        //we don't need to check it because we know it
        cdc.add(coma);
    }

    public Corpus(String corpusName, URL baseDir, Collection<CorpusData> cdc) throws MalformedURLException, SAXException, JexmaraldaException {
        this(cdc);
        basedirectory = baseDir ;
        corpusname = corpusName ;
    }

    public Corpus(Collection<CorpusData> cdc) throws MalformedURLException, SAXException, JexmaraldaException {
        this.cdc = cdc ;
        // Now sort the corpus data files into categories
        addCorpusDataCollection(cdc);
        // We don't have a name
        corpusname = "" ;
        // Get the common prefix of all parent urls
        String commonPrefix =
                StringUtils.getCommonPrefix(cdc.stream().map((cd) -> cd.getParentURL().toString()).toArray(String[]::new));
        // Convert to basedirectory
        if (commonPrefix.isEmpty())
            commonPrefix = "file:///" ;
        basedirectory = new URL(commonPrefix) ;
    }

    private void addCorpusDataCollection(Collection<CorpusData> cdc) {
        for (CorpusData cd : cdc) {
            if (cd instanceof ContentData) {
                contentdata.add((ContentData) cd);
                if (cd instanceof EXMARaLDATranscriptionData) {
                    basictranscriptiondata.add((EXMARaLDATranscriptionData) cd);
                } else if (cd instanceof EXMARaLDASegmentedTranscriptionData) {
                    segmentedtranscriptiondata.add((EXMARaLDASegmentedTranscriptionData) cd);
                } else if (cd instanceof ELANData) {
                    elandata.add((ELANData) cd);
                } else if (cd instanceof FlextextData) {
                    flextextdata.add((FlextextData) cd);
                }
            } else if (cd instanceof Recording) {
                recording.add((Recording) cd);
            } else if (cd instanceof AdditionalData) {
                additionaldata.add((AdditionalData) cd);
            } else if (cd instanceof Metadata) {
                //can only be CMDI since it's a coma file...
                metadata.add((Metadata) cd);
                if (cd instanceof CmdiData) {
                    cmdidata.add((CmdiData) cd);
                } else if (cd instanceof AnnotationSpecification) {
                    annotationspecification.add((AnnotationSpecification) cd);
                } else if (cd instanceof ConfigParameters) {
                    configparameters.add((ConfigParameters) cd);
                }
            }
        }
    }

    public Collection<CorpusData> getCorpusData() {
        return cdc;
    }

    public Collection<Metadata> getMetadata() {
        return metadata;
    }

    public Collection<ContentData> getContentdata() {
        return contentdata;
    }

    public Collection<Recording> getRecording() {
        return recording;
    }

    public Collection<AdditionalData> getAdditionaldata() {
        return additionaldata;
    }

    public Collection<AnnotationSpecification> getAnnotationspecification() {
        return annotationspecification;
    }

    public Collection<ConfigParameters> getConfigparameters() {
        return configparameters;
    }

    public Collection<CmdiData> getCmdidata() {
        return cmdidata;
    }

    public Collection<EXMARaLDATranscriptionData> getBasicTranscriptionData() {
        return basictranscriptiondata;
    }

    public Collection<EXMARaLDASegmentedTranscriptionData> getSegmentedTranscriptionData() {
        return segmentedtranscriptiondata;
    }
    
    public Collection<ELANData> getELANData() {
        return elandata;
    }

    public Collection<FlextextData> getFlextextData() {
        return flextextdata;
    }
 
    public ComaData getComaData() {
        return comadata;
    }

    public void setMetadata(Collection<Metadata> metadata) {
        this.metadata = metadata;
    }

    public void setContentdata(Collection<ContentData> contentdata) {
        this.contentdata = contentdata;
    }

    public void setRecording(Collection<Recording> recording) {
        this.recording = recording;
    }

    public void setAdditionaldata(Collection<AdditionalData> additionaldata) {
        this.additionaldata = additionaldata;
    }

    public void setAnnotationspecification(Collection<AnnotationSpecification> annotationspecification) {
        this.annotationspecification = annotationspecification;
    }

    public void setConfigparameters(Collection<ConfigParameters> configparameters) {
        this.configparameters = configparameters;
    }

    public void setCdc(Collection<CorpusData> cdc) {
        this.cdc = cdc;
    }

    public void setCmdidata(Collection<CmdiData> cmdidata) {
        this.cmdidata = cmdidata;
    }

    public void setBasicTranscriptionData(Collection<EXMARaLDATranscriptionData> basictranscriptions) {
        this.basictranscriptiondata = basictranscriptions;
    }

    public void setSegmentedTranscriptionData(Collection<EXMARaLDASegmentedTranscriptionData> segmentedtranscriptions) {
        this.segmentedtranscriptiondata = segmentedtranscriptions;
    }
    
    public void setELANData(Collection<ELANData> elandata) {
        this.elandata = elandata;
    }
    
    public void setFlextextData(Collection<FlextextData> flextextdata) {
        this.flextextdata = flextextdata;
    }

    public void setComaData(ComaData coma) {
        this.comadata = coma;
    }

    public URL getBaseDirectory() {
        return basedirectory;
    }

    public String getCorpusName() {
        return corpusname;
    }

    public void setCorpusName(String s) {
        corpusname = s;
    }

    //TODO make this more sustainable, it is very INEL specific
    String getCorpusSentenceNumber() throws JDOMException {
        XPath xpath = XPath.newInstance("sum(//Transcription/Description/Key[@Name = '# HIAT:u'])");
        double DoubleValue = (double) xpath.selectSingleNode(comadata.getJdom());
        int IntValue = (int) DoubleValue;
        return "" + IntValue;
    }

    String getCorpusTranscriptionNumber() throws JDOMException {
        XPath xpath = XPath.newInstance("count(//Transcription/Description/Key[@Name = 'segmented' and text() = 'false'])");
        double DoubleValue = (double) xpath.selectSingleNode(comadata.getJdom());
        int IntValue = (int) DoubleValue;
        return "" + IntValue;
    }

    String getCorpusSpeakerNumber() throws JDOMException {
        XPath xpath = XPath.newInstance("count(//Speaker)");
        double DoubleValue = (double) xpath.selectSingleNode(comadata.getJdom());
        int IntValue = (int) DoubleValue;
        return "" + IntValue;
    }

    String getCorpusCommunicationNumber() throws JDOMException {
        XPath xpath = XPath.newInstance("count(//Communication)");
        double DoubleValue = (double) xpath.selectSingleNode(comadata.getJdom());
        int IntValue = (int) DoubleValue;
        return "" + IntValue;
    }

    String getCorpusWords() throws JDOMException {
        XPath xpath = XPath.newInstance("sum(//Transcription/Description/Key[@Name = '# HIAT:w'])");
        double DoubleValue = (double) xpath.selectSingleNode(comadata.getJdom());
        int IntValue = (int) DoubleValue;
        return "" + IntValue;
    }
}
