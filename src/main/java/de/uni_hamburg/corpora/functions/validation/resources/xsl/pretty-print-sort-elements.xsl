<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : pretty-print-sort-elements.xsl
    Created on : 19. Juni 2019, 10:25
    Author     : Daniel Jettka
    Description:
        Sorting elements in EXMARaLDA file for better pretty print (and better git diff later)
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" version="2.0">

    <!-- GENERAL copy rules -->

    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>
    
    <xsl:template match="*">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="text()|comment()|processing-instruction()">
        <xsl:copy-of select="."/>
    </xsl:template>

    <!-- SPECIFIC sorting rules -->
    
    <xsl:template match="/Corpus/CorpusData">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:for-each select="Communication">
                <xsl:sort select="@Name"/>
                <xsl:apply-templates select="."/>
            </xsl:for-each>
            <xsl:for-each select="Speaker">
                <xsl:sort select="Sigle"/>
                <xsl:apply-templates select="."/>
            </xsl:for-each>
            <xsl:call-template name="children-sorted-by-name">
                <xsl:with-param name="ignore-elements" select="'Communication', 'Speaker'"/>
            </xsl:call-template>
        </xsl:copy>
    </xsl:template>
        
    <xsl:template match="/Corpus/CorpusData/Communication">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <!-- specific order for metadata elements -->
            <xsl:copy-of select="Setting, Description, Language, Location"/>
            <!-- Transcription elements -->
            <xsl:for-each select="Transcription">
                <xsl:sort select="Filename"/>
                <xsl:copy>
                    <xsl:copy-of select="@*"/>
                    <xsl:copy-of select="Name, Filename, NSLink"/>
                    <xsl:for-each select="Description">
                        <xsl:call-template name="description-with-sorted-keys"/>                        
                    </xsl:for-each>
                    <xsl:copy-of select="Availability"/>
                    <xsl:call-template name="children-sorted-by-name">
                        <xsl:with-param name="ignore-elements" select="'Name', 'Filename', 'NSLink', 'Description', 'Availability'"/>
                    </xsl:call-template>
                </xsl:copy>
            </xsl:for-each>
            <!-- Recording elements -->
            <xsl:for-each select="Recording">
                <xsl:copy>
                    <xsl:copy-of select="@*"/>
                    <xsl:copy-of select="Name, RecordingDuration"/>
                    <xsl:for-each select="Media">
                        <xsl:sort select="Filename"/>   
                        <xsl:copy>
                            <xsl:copy-of select="@*"/>                     
                            <xsl:call-template name="children-sorted-by-name"/>
                        </xsl:copy>
                    </xsl:for-each>
                    <xsl:copy-of select="Description"/>
                    <xsl:call-template name="children-sorted-by-name">
                        <xsl:with-param name="ignore-elements" select="'Name', 'RecordingDuration', 'Media', 'Description'"/>
                    </xsl:call-template>
                </xsl:copy>
            </xsl:for-each>
            <!-- File elements -->
            <xsl:for-each select="File">
                <xsl:copy>
                    <xsl:copy-of select="@*"/>
                    <xsl:call-template name="children-sorted-by-name"/>
                </xsl:copy>
            </xsl:for-each>
            <!-- other elements -->
            <xsl:call-template name="children-sorted-by-name">
                <xsl:with-param name="ignore-elements" select="'Setting', 'Description', 'Language', 'Location', 'Transcription', 'Recording', 'File'"/>
            </xsl:call-template>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="/Corpus/CorpusData/Speaker">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:copy-of select="Sigle, Pseudo, KnownHuman, Sex"/>
            <!-- Language elements -->
            <xsl:for-each select="Language">
                <xsl:sort select="@Type"/>
                <xsl:copy-of select="."/>
            </xsl:for-each>
            <!-- Location elements -->
            <xsl:for-each select="Location">
                <xsl:sort select="@Type"/>
                <xsl:copy-of select="."/>
            </xsl:for-each>
            <!-- Description element -->
            <xsl:copy-of select="Description"/>
            <!-- other elements -->
            <xsl:call-template name="children-sorted-by-name">
                <xsl:with-param name="ignore-elements" select="'Sigle', 'Pseudo', 'KnownHuman', 'Sex', 'Location', 'Language', 'Description'"/>
            </xsl:call-template>
        </xsl:copy>
    </xsl:template>
    
    <!-- generalized templates -->
    
    <xsl:template name="description-with-sorted-keys">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:for-each select="Key">
                <xsl:sort select="@Name"/>
                <xsl:copy-of select="."/>
            </xsl:for-each>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template name="children-sorted-by-name">
        <xsl:param name="ignore-elements" as="xs:string*"/>
        <xsl:for-each select="*[not(local-name()=$ignore-elements)]">
            <xsl:sort select="local-name()"/>
            <xsl:copy-of select="."/>
        </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>
