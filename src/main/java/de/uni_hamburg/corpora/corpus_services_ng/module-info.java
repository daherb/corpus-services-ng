module corpus_services_ng {
    requires java.logging;
    requires java.sql;

    requires transitive java.datatransfer;
    requires transitive java.desktop;
    requires transitive java.xml;

    requires transitive invenio.java.api;
    requires transitive datacite.java.api;

//    requires java.xml.bind;
    requires jakarta.xml.bind;
    requires jakarta.activation;
    requires EXMARaLDA;
    requires ELAN;

    requires bagit;
    requires jdom;
    requires org.jdom2;
    requires com.fasterxml.jackson.module.jsonSchema;
    requires com.fasterxml.jackson.dataformat.xml;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires jakarta.servlet;
    requires org.apache.xmpbox;
    requires preflight;
    requires org.apache.pdfbox;
    requires com.google.gson;
    requires languagetool;
    requires org.apache.commons.cli;
    requires org.apache.commons.text;
    requires commons.io;
    requires com.helger.collection;
    requires opencsv;
    requires ini4j;
    requires com.google.common;
    requires jmimemagic;
        
    exports de.idsmannheim.lza.conversion;
    exports de.idsmannheim.lza.publication;
    exports de.idsmannheim.lza.utilities.publication;
    exports de.idsmannheim.lza.utilities.publication.mapper;
    exports de.idsmannheim.lza.validation;
    exports de.uni_hamburg.corpora;
    exports de.uni_hamburg.corpora.conversion;
    exports de.uni_hamburg.corpora.publication;
    exports de.uni_hamburg.corpora.statistics;
    exports de.uni_hamburg.corpora.swing;
    exports de.uni_hamburg.corpora.utilities;
    exports de.uni_hamburg.corpora.utilities.quest;
    exports de.uni_hamburg.corpora.validation;
    exports de.uni_hamburg.corpora.validation.quest;
    exports de.uni_hamburg.corpora.visualization;

}
