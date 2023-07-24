<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" exclude-result-prefixes="xs" version="2.0">

    <!-- 
    This stylesheet can be used when changes need to be made in Flex after there are already annotations in the 
    imported exb.
    The parameter "new-exb" needs to be the Path to the newly exported exb from Flex (with flextext import in Partitur-Editor).
    The stylesheet needs to be applied on the old exb that already has annotations, translations or other changes in there.
    It only works if the timeline wasn't changed. 
    -->
    <xsl:variable name="document-uri" select="document-uri(.)"/>
    <xsl:variable name="filename" select="(tokenize($document-uri, '/'))[last()]"/>
    <xsl:variable name="path" select="substring-before(document-uri(.), $filename)"/>

    <xsl:param name="new-exb" select="concat('flex2exb-conversion/', $filename)"/>
    <xsl:variable name="new-exb-filepath" select="concat($path, $new-exb)"/>
    <xsl:variable name="new-exb-document" select="document($new-exb-filepath)"/>


    <!-- Identity template, copies everything as is -->
    <xsl:template match="@* | node()">
        <xsl:choose>
            <xsl:when test="/basic-transcription/basic-body/common-timeline = $new-exb-document/basic-transcription/basic-body/common-timeline">
                <xsl:copy>
                    <xsl:apply-templates select="@* | node()"/>
                </xsl:copy>
            </xsl:when>
            <xsl:otherwise>
                Attention! There are changes in the timeline!
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Override for target element -->
    <xsl:template match="tier[@id = 'mb' or @id = 'mp' or @id = 'ge' or @id = 'gg' or @id = 'gr' or @id = 'mc' or @id = 'hn' or @id = 'ps']">
        <xsl:choose>
            <xsl:when test="/basic-transcription/basic-body/common-timeline = $new-exb-document/basic-transcription/basic-body/common-timeline">
                <!--    /basic-transcription/basic-body/common-timeline
    das hier muss identisch sein bei beiden exbs-->
                <xsl:variable name="tier-id" select="@id"/>
                <xsl:copy-of select="$new-exb-document//tier[@id = $tier-id][node()]"/>
                <!-- Copy the element -->
                <!--    <xsl:copy>
            <!-\- And everything inside it -\->
            <xsl:apply-templates select="@* | *"/> 
            <!-\- Add new node (or whatever else you wanna do) -\->
            <ud-information attribute-name="korrigiert">ja</ud-information>
            <ud-information attribute-name="Dateiname"><xsl:value-of select="$newfilename"/></ud-information> -->
            </xsl:when>
            <xsl:otherwise>
           Attention! There are changes in the timeline!
        </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
