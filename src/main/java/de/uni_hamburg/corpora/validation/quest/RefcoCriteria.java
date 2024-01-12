package de.uni_hamburg.corpora.validation.quest;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Representation of the complete information defined in the RefCo spreadsheet
 * @author bba1792
 * @version 20220822
 */
public class RefcoCriteria {
    /**
     * A pair of information with potentially associated notes, used e.g. in the Overview table
     */
    public static class InformationNotes {
        public InformationNotes(String information, String notes) {
            this.information = information;
            this.notes = notes;
        }
        @XmlElement(required=true)
        String information ;
        @XmlElement(required=true)
        String notes;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InformationNotes that = (InformationNotes) o;
            return Objects.equals(information, that.information) && Objects.equals(notes, that.notes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(information, notes);
        }

        public String getInformation() {
            return information;
        }

        public String getNotes() {
            return notes;
        }

        public void setInformation(String information) {
            this.information = information;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    /**
     * Representation of information in the CorpusComposition table consisting of e.g.
     * speaker information, location and date
     */
    public static class Session {
        @XmlElement(required=true)
        String sessionName ;
        @XmlElement(required=true)
        String fileNames;
        @XmlElement(required=true)
        String speakerNames;
        @XmlElement(required=true)
        String speakerAges;
        @XmlElement(required=true)
        String speakerGender ;
        @XmlElement(required=true)
        String recordingLocation ;
        @XmlElement(required=true)
        String recordingDate ;
        @XmlElement(required=true)
        String genre ; // is this a controlled vocabulary?

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Session session = (Session) o;
            return Objects.equals(sessionName, session.sessionName) && Objects.equals(fileNames, session.fileNames) && Objects.equals(speakerNames, session.speakerNames) && Objects.equals(speakerAges, session.speakerAges) && Objects.equals(speakerGender, session.speakerGender) && Objects.equals(recordingLocation, session.recordingLocation) && Objects.equals(recordingDate, session.recordingDate) && Objects.equals(genre, session.genre);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sessionName, fileNames, speakerNames, speakerAges, speakerGender, recordingLocation, recordingDate, genre);
        }

        public String getSessionName() {
            return sessionName;
        }

        public String getFileNames() {
            return fileNames;
        }

        public String getSpeakerNames() {
            return speakerNames;
        }

        public String getSpeakerAges() {
            return speakerAges;
        }

        public String getSpeakerGender() {
            return speakerGender;
        }

        public String getRecordingLocation() {
            return recordingLocation;
        }

        public String getRecordingDate() {
            return recordingDate;
        }

        public String getGenre() {
            return genre;
        }

        public void setSessionName(String sessionName) {
            this.sessionName = sessionName;
        }

        public void setFileNames(String fileNames) {
            this.fileNames = fileNames;
        }

        public void setSpeakerNames(String speakerNames) {
            this.speakerNames = speakerNames;
        }

        public void setSpeakerAges(String speakerAges) {
            this.speakerAges = speakerAges;
        }

        public void setSpeakerGender(String speakerGender) {
            this.speakerGender = speakerGender;
        }

        public void setRecordingLocation(String recordingLocation) {
            this.recordingLocation = recordingLocation;
        }

        public void setRecordingDate(String recordingDate) {
            this.recordingDate = recordingDate;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }
    }


    /**
     * Representation of information in the AnnotationTiers table consisting of e.g.
     * tier functions and languages
     */
    public static class Tier {
        @XmlElement(required=true)
        String tierName ;
        @XmlElement(required=true)
        List<String> tierFunctions;
        @XmlElement(required=true)
        String segmentationStrategy ;
        @XmlElement(required=true)
        String languages ;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tier tier = (Tier) o;
            return Objects.equals(tierName, tier.tierName) && Objects.equals(tierFunctions, tier.tierFunctions) && Objects.equals(segmentationStrategy, tier.segmentationStrategy) && Objects.equals(languages, tier.languages);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tierName, tierFunctions, segmentationStrategy, languages);
        }

        public String getTierName() {
            return tierName;
        }

        public List<String> getTierFunctions() {
            return tierFunctions;
        }

        public String getSegmentationStrategy() {
            return segmentationStrategy;
        }

        public String getLanguages() {
            return languages;
        }

        public void setTierName(String tierName) {
            this.tierName = tierName;
        }

        public void setTierFunctions(List<String> tierFunctions) {
            this.tierFunctions = tierFunctions;
        }

        public void setSegmentationStrategy(String segmentationStrategy) {
            this.segmentationStrategy = segmentationStrategy;
        }

        public void setLanguages(String languages) {
            this.languages = languages;
        }
    }

    /**
     * Representation of information in the Transcriptions table consisting of e.g.
     * the list of valid graphemes used in transcription tiers
     */
    public static class Transcription {
        @XmlElement(required=true)
        String grapheme ;
        @XmlElement(required=true)
        String linguisticValue ;
        @XmlElement(required=true)
        String linguisticConvention ;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Transcription that = (Transcription) o;
            return Objects.equals(grapheme, that.grapheme) && Objects.equals(linguisticValue, that.linguisticValue) && Objects.equals(linguisticConvention, that.linguisticConvention);
        }

        @Override
        public int hashCode() {
            return Objects.hash(grapheme, linguisticValue, linguisticConvention);
        }

        public String getGrapheme() {
            return grapheme;
        }

        public String getLinguisticValue() {
            return linguisticValue;
        }

        public String getLinguisticConvention() {
            return linguisticConvention;
        }

        public void setGrapheme(String grapheme) {
            this.grapheme = grapheme;
        }

        public void setLinguisticValue(String linguisticValue) {
            this.linguisticValue = linguisticValue;
        }

        public void setLinguisticConvention(String linguisticConvention) {
            this.linguisticConvention = linguisticConvention;
        }
    }

    /**
     * Representation of information in the Glosses table consisting of e.g.
     * list of expected glosses and the tiers they are valid in
     */
    public static class Gloss {
        @XmlElement(required=true)
        String gloss ;
        @XmlElement(required=true)
        String meaning ;
        @XmlElement
        String comments ;
        @XmlElement(required=true)
        String tiers ;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Gloss gloss1 = (Gloss) o;
            return Objects.equals(gloss, gloss1.gloss) && Objects.equals(meaning, gloss1.meaning) && Objects.equals(comments, gloss1.comments) && Objects.equals(tiers, gloss1.tiers);
        }

        @Override
        public int hashCode() {
            return Objects.hash(gloss, meaning, comments, tiers);
        }

        public String getGloss() {
            return gloss;
        }

        public String getMeaning() {
            return meaning;
        }

        public String getComments() {
            return comments;
        }

        public String getTiers() {
            return tiers;
        }

        public void setGloss(String gloss) {
            this.gloss = gloss;
        }

        public void setMeaning(String meaning) {
            this.meaning = meaning;
        }

        public void setComments(String comments) {
            this.comments = comments;
        }

        public void setTiers(String tiers) {
            this.tiers = tiers;
        }
    }

    /**
     * Representation of information in the Punctuation table consisting of e.g.
     * valid punctuation characters and the tiers they are valid in
     */
    public static class Punctuation {
        @XmlElement(required=true)
        String character ;
        @XmlElement(required=true)
        String meaning ;
        @XmlElement
        String comments ;
        @XmlElement(required=true)
        String tiers ;
        @XmlElement(required=true)
        String function;

        public void setCharacter(String character) {
            this.character = character;
        }

        public void setMeaning(String meaning) {
            this.meaning = meaning;
        }

        public void setComments(String comments) {
            this.comments = comments;
        }

        public void setTiers(String tiers) {
            this.tiers = tiers;
        }

        public void setFunction(String function) {
            this.function = function;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Punctuation that = (Punctuation) o;
            return Objects.equals(character, that.character) && Objects.equals(meaning, that.meaning) && Objects.equals(comments, that.comments) && Objects.equals(tiers, that.tiers) && Objects.equals(function, that.function);
        }

        @Override
        public int hashCode() {
            return Objects.hash(character, meaning, comments, tiers, function);
        }

        public String getCharacter() {
            return character;
        }

        public String getMeaning() {
            return meaning;
        }

        public String getComments() {
            return comments;
        }

        public String getTiers() {
            return tiers;
        }

        public String getFunction() {
            return function;
        }
    }

    // Tab: Overview
    // Corpus information
    @XmlElement(required=true)
    String corpusTitle ;
    @XmlElement(required=true)
    String subjectLanguages ;
    @XmlElement(required=true)
    String archive ;
    @XmlElement(required=true)
    String persistentId ; // should be an url to either a doi or handle
    @XmlElement(required=true)
    String annotationLicense ;
    @XmlElement(required=true)
    String recordingLicense ;
    @XmlElement(required=true)
    String creatorName ;
    @XmlElement(required=true)
    String creatorContact ; // usually mail address
    @XmlElement(required=true)
    String creatorInstitution ;
    // Certification information
    @XmlElement(required=true)
    InformationNotes refcoVersion ;
    // Quantitative Summary
    @XmlElement(required=true)
    InformationNotes numberSessions ;
    @XmlElement(required=true)
    InformationNotes numberTranscribedWords ;
    @XmlElement(required=true)
    InformationNotes numberAnnotatedWords ;
    // Tab: Corpus Compositions
    @XmlElementWrapper(name="sessions")
    @XmlElement(required=true,name="session")
    public
    ArrayList<Session> sessions = new ArrayList<>() ;
    // Tab: Annotation Tiers
    @XmlElementWrapper(name="tiers")
    @XmlElement(required = true,name="tier")
    ArrayList<Tier> tiers = new ArrayList<>() ;
    // Tab: Transcriptions
    @XmlElementWrapper(name="transcriptions")
    @XmlElement(required = true, name="transcription")
    ArrayList<Transcription> transcriptions = new ArrayList<>() ;
    // Tab: Glosses
    @XmlElementWrapper(name="glosses")
    @XmlElement(required = true, name="gloss")
    ArrayList<Gloss> glosses = new ArrayList<>() ;
    // Tab: Punctuation
    @XmlElementWrapper(name="punctuations")
    @XmlElement(required = true, name="punctuation")
    ArrayList<Punctuation> punctuations = new ArrayList<>() ;

    public String getCorpusTitle() {
        return corpusTitle;
    }

    public String getSubjectLanguages() {
        return subjectLanguages;
    }

    public String getArchive() {
        return archive;
    }

    public String getPersistentId() {
        return persistentId;
    }

    public String getAnnotationLicense() {
        return annotationLicense;
    }

    public String getRecordingLicense() {
        return recordingLicense;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public String getCreatorContact() {
        return creatorContact;
    }

    public String getCreatorInstitution() {
        return creatorInstitution;
    }

    public InformationNotes getRefcoVersion() {
        return refcoVersion;
    }

    public InformationNotes getNumberSessions() {
        return numberSessions;
    }

    public InformationNotes getNumberTranscribedWords() {
        return numberTranscribedWords;
    }

    public InformationNotes getNumberAnnotatedWords() {
        return numberAnnotatedWords;
    }

    public ArrayList<Session> getSessions() {
        return sessions;
    }

    public ArrayList<Tier> getTiers() {
        return tiers;
    }

    public ArrayList<Transcription> getTranscriptions() {
        return transcriptions;
    }

    public ArrayList<Gloss> getGlosses() {
        return glosses;
    }

    public ArrayList<Punctuation> getPunctuations() {
        return punctuations;
    }

    public void setCorpusTitle(String corpusTitle) {
        this.corpusTitle = corpusTitle;
    }

    public void setSubjectLanguages(String subjectLanguages) {
        this.subjectLanguages = subjectLanguages;
    }

    public void setArchive(String archive) {
        this.archive = archive;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public void setAnnotationLicense(String annotationLicense) {
        this.annotationLicense = annotationLicense;
    }

    public void setRecordingLicense(String recordingLicense) {
        this.recordingLicense = recordingLicense;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public void setCreatorContact(String creatorContact) {
        this.creatorContact = creatorContact;
    }

    public void setCreatorInstitution(String creatorInstitution) {
        this.creatorInstitution = creatorInstitution;
    }

    public void setRefcoVersion(InformationNotes refcoVersion) {
        this.refcoVersion = refcoVersion;
    }

    public void setNumberSessions(InformationNotes numberSessions) {
        this.numberSessions = numberSessions;
    }

    public void setNumberTranscribedWords(InformationNotes numberTranscribedWords) {
        this.numberTranscribedWords = numberTranscribedWords;
    }

    public void setNumberAnnotatedWords(InformationNotes numberAnnotatedWords) {
        this.numberAnnotatedWords = numberAnnotatedWords;
    }

    public void setSessions(ArrayList<Session> sessions) {
        this.sessions = sessions;
    }

    public void setTiers(ArrayList<Tier> tiers) {
        this.tiers = tiers;
    }

    public void setTranscriptions(ArrayList<Transcription> transcriptions) {
        this.transcriptions = transcriptions;
    }

    public void setGlosses(ArrayList<Gloss> glosses) {
        this.glosses = glosses;
    }

    public void setPunctuations(ArrayList<Punctuation> punctuations) {
        this.punctuations = punctuations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefcoCriteria that = (RefcoCriteria) o;
        return Objects.equals(corpusTitle, that.corpusTitle) && Objects.equals(subjectLanguages,
                that.subjectLanguages) && Objects.equals(archive, that.archive) && Objects.equals(persistentId,
                that.persistentId) && Objects.equals(annotationLicense, that.annotationLicense) && Objects.equals(recordingLicense, that.recordingLicense) && Objects.equals(creatorName, that.creatorName) && Objects.equals(creatorContact, that.creatorContact) && Objects.equals(creatorInstitution, that.creatorInstitution) && Objects.equals(refcoVersion, that.refcoVersion) && Objects.equals(numberSessions, that.numberSessions) && Objects.equals(numberTranscribedWords, that.numberTranscribedWords) && Objects.equals(numberAnnotatedWords, that.numberAnnotatedWords) && // Objects.equals(translationLanguages, that.translationLanguages) &&
                Objects.equals(sessions, that.sessions) && Objects.equals(tiers, that.tiers) && Objects.equals(transcriptions, that.transcriptions) && Objects.equals(glosses, that.glosses) && Objects.equals(punctuations, that.punctuations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(corpusTitle, subjectLanguages, archive, persistentId, annotationLicense,
                recordingLicense, creatorName, creatorContact, creatorInstitution, refcoVersion, numberSessions,
                numberTranscribedWords, numberAnnotatedWords, // translationLanguages,
                sessions, tiers, transcriptions, glosses, punctuations);
    }
}
