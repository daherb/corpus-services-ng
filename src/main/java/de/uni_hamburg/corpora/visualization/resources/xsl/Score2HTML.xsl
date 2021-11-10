<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:exmaralda="http://www.exmaralda.org/xml"
    xmlns:string="https://corpora.uni-hamburg.de/hzsk/xmlns/string"
    xmlns:hzsk-pi="https://corpora.uni-hamburg.de/hzsk/xmlns/processing-instruction" exclude-result-prefixes="#all" version="2.0">
    <xsl:output encoding="UTF-8" method="xml" omit-xml-declaration="yes"/>


    <xsl:key name="tier-title-by-category" match="@title" use="../@category"/>

    <!-- ********************** -->
    <!-- Parameters Declaration -->
    <!-- ********************** -->

    <xsl:param name="TRANSCRIPTION_ID" as="xs:string?" required="no"/>
    <xsl:param name="COMMUNICATION_ID" as="xs:string?" required="no"/>
    <xsl:param name="RECORDING_PATH"
        select="
            (for $type in ($SUPPORTED_VIDEO_TYPES, $SUPPORTED_AUDIO_TYPES)
            return
                //referenced-file/@url[ends-with(lower-case(.), $type)])[1]"
        as="xs:string?" required="no"/>
    <xsl:param name="RECORDING_TYPE" select="tokenize($RECORDING_PATH, '\.')[last()]" as="xs:string?" required="no"/>
    <xsl:param name="EMAIL_ADDRESS" select="'corpora@uni-hamburg.de'" as="xs:string?" required="no"/>
    <xsl:param name="WEBSERVICE_NAME" select="'ScoreHTML'" as="xs:string?" required="no"/>
    <xsl:param name="HZSK_WEBSITE" select="'https://corpora.uni-hamburg.de/'" as="xs:string?" required="no"/>
    <xsl:param name="STYLES" as="xs:string?" required="no"/>
    <!-- The displayed name of the transcription -->
    <!-- occurs, for example in the navigation bar -->
    <xsl:param name="TRANSCRIPTION_NAME" select="//transcription-name" as="xs:string?" required="no"/>
        <!-- The displayed name of the corpus -->
    <!-- occurs, for example in the navigation bar -->
    <xsl:param name="CORPUS_NAME" select="//project-name" as="xs:string?" required="no"/>
    <xsl:variable name="SUPPORTED_VIDEO_TYPES" select="('webm', 'mpeg', 'mpg')" as="xs:string+"/>
    <!-- sorted by favourited format -->
    <xsl:variable name="SUPPORTED_AUDIO_TYPES" select="('mp3', 'ogg', 'wav')" as="xs:string+"/>
    <!-- sorted by favourited format -->

    <!-- ********************* -->
    <!-- Variables Declaration -->
    <!-- ********************* -->





    <!-- the base of the filename from which the names of all linked files are derived -->
    <xsl:variable name="BASE_FILENAME">
        <xsl:value-of select="//referenced-file[1]/@url"/>
        <!-- <xsl:value-of select="//ud-information[@attribute-name='Code']"/> -->
    </xsl:variable>

    <!-- the path to the folder with resources -->
    <xsl:variable name="TOP_LEVEL_PATH" as="xs:string" select="'https://corpora.uni-hamburg.de/drupal/sites/default/files/visualization/'"/>

    <xsl:variable name="DATASTREAM_VIDEO" as="xs:string?" select="$RECORDING_PATH"/>

    <xsl:variable name="DATASTREAM_AUDIO" as="xs:string?" select="$RECORDING_PATH"/>

    <!-- the name of the project which owns the corpus -->
    <xsl:variable name="PROJECT_NAME" as="xs:string" select="'EXMARaLDA'"/>

    <!-- the URL of the project which owns the corpus -->
    <xsl:variable name="PROJECT_URL" as="xs:string" select="'http://www.exmaralda.org/'"/>

    <!-- whether or not the transcription contains video -->
    <xsl:variable name="HAS_VIDEO" as="xs:boolean" select="lower-case($RECORDING_TYPE) = $SUPPORTED_VIDEO_TYPES"/>

    <!-- whether or not the transcription contains audio -->
    <xsl:variable name="HAS_AUDIO" as="xs:boolean" select="lower-case($RECORDING_TYPE) = $SUPPORTED_AUDIO_TYPES"/>

    <!-- Titles of tiers by category -->
    <xsl:variable name="TIER_TITLES">
        <tier category="k" title="Commentary tier"/>
        <tier category="nn" title="NN tier"/>
        <tier category="pause" title="Pause tier"/>
        <tier category="en" title="English tranlsation tier"/>
        <tier category="de" title="German translation tier"/>
        <tier category="nr" title="News tier"/>
        <tier category="cs" title="Code switch tier"/>
        <tier category="pho" title="Phonetics tier"/>
        <tier category="sup" title="intonation tier"/>
        <tier category="nv" title="non-verbal actions tier"/>
    </xsl:variable>


    <!-- ******************************************************************************************************************************************** -->

    <!-- ************************ -->
    <!--    Top level template   -->
    <!-- ************************ -->

    <xsl:template match="/">
        <html>
            <head>
                <title>
                    <xsl:value-of select="concat($CORPUS_NAME, ': ', $TRANSCRIPTION_NAME)"/>
                </title>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>

                <xsl:choose>
                    <xsl:when test="not($STYLES = '/* EMTPY TIER FORMAT TABLE!!! */')">
                        <style><xsl:value-of select="$STYLES"/></style>
                        <!-- placeholder for css, inserted later by Java -->
                        <style><hzsk-pi:include>/css/ScoreFormat.css</hzsk-pi:include></style>
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- placeholder for css, inserted later by Java -->
                        <style><hzsk-pi:include>/css/ScoreFormat.css</hzsk-pi:include></style>
                    </xsl:otherwise>
                </xsl:choose>
                <!-- placeholder for css, inserted later by Java -->
                <style><hzsk-pi:include>/css/VisualizationFormat.css</hzsk-pi:include></style>

                <!-- placeholder for js script, inserted later by Java -->
                <script><hzsk-pi:include>/js/timelight-0.1.min.js</hzsk-pi:include></script>
                <script><hzsk-pi:include>/js/jsfunctions.js</hzsk-pi:include></script>
            </head>
            <body>
                <xsl:call-template name="MAKE_TITLE"/>
                <div id="content">
                    <div id="controls">
                        <xsl:call-template name="MAKE_PLAYER_DIV"/>
                        <xsl:call-template name="MAKE_TIER_DISPLAY_CONTROL"/>
                        <xsl:call-template name="MAKE_WEB_SERVICE_INFO"/>
                        <!-- <xsl:call-template name="MAKE_DOWNLOAD_FILES_CONTROL"/> -->
                        <!-- <xsl:call-template name="MAKE_FOOTER"/>-->
                    </div>
                    <div id="main">
                        <div id="transcription">
                            <xsl:apply-templates select="//it-bundle"/>
                            <!-- I removed multiple breaks from here, one seemed to be enough (I'm not exactly sure if even that is needed, but I guess it does no harm -Niko) -->
                            <p>
                                <br/>
                            </p>
                        </div>
                    </div>
                </div>
            </body>
        </html>
    </xsl:template>

    <!-- ************************ -->
    <!--     one partitur area    -->
    <!-- ************************ -->

    <xsl:template match="it-bundle">
        <!-- the numbering for this partitur area -->
        <span class="pno">
            <xsl:for-each select="anchor">
                <a name="{.}"/>
            </xsl:for-each>
            <xsl:value-of select="concat('[', position(), ']')"/>
        </span>

        <!-- the table representing the actual partitur area -->
        <table class="p" width="{round(1.4*//table-width/@table-width)}">
            <xsl:apply-templates select="sync-points"/>
            <xsl:apply-templates select="it-line"/>
        </table>
    </xsl:template>


    <!-- ********************************* -->
    <!-- syncpoints aka timeline items -->
    <!-- ********************************* -->

    <xsl:template match="sync-points">
        <tr>
            <!-- one empty cell above the tier labels -->
            <td class="snc-emp"/>
            <!-- now the real syncPoints -->
            <xsl:apply-templates select="sync-point"/>
        </tr>
    </xsl:template>

    <!-- ********************************************* -->
    <!-- an individual syncpoint aka timeline item -->
    <!-- ********************************************* -->
    <xsl:template match="sync-point">
        <td class="snc">
            <!-- anchor for media playback -->
            <xsl:if test="//tli[@id = current()/@id]/@time">
                <xsl:variable name="TIME" select="0 + //tli[@id = current()/@id]/@time"/>
                <a onclick="jump('{format-number(($TIME + 0.03), '#.##')}');">
                    <img class="media invert" title="{exmaralda:FORMAT_TIME($TIME)}&#x0020;-&#x0020;Click to start player" src="{$TOP_LEVEL_PATH}pbn.gif"/>
                </a>
            </xsl:if>
        </td>
    </xsl:template>

    <!-- ****************************** -->
    <!-- an individual it-line aka tier -->
    <!-- ****************************** -->
    <xsl:template match="it-line">
        <tr class="{//tier[@id=current()/@formatref]/@category}" name="{//tier[@id=current()/@formatref]/@category}"/>

        <xsl:variable name="itLinePosition" select="position()" as="xs:integer"/>

        <!-- aply the template for the tier label -->
        <xsl:apply-templates select="it-label"/>

        <xsl:for-each select="../sync-points/sync-point">

            <xsl:variable name="Pos" select="1 + count(preceding-sibling::*)" as="xs:integer"/>

            <xsl:variable name="interval_is_covered">
                <xsl:for-each select="../../it-line[$itLinePosition + 0]/it-chunk">
                    <xsl:variable name="startPos" select="1 + count(../../sync-points/sync-point[@id = current()/@start-sync]/preceding-sibling::*)" as="xs:integer"/>
                    <xsl:variable name="endPos" select="1 + count(../../sync-points/sync-point[@id = current()/@end-sync]/preceding-sibling::*)" as="xs:integer"/>
                    <xsl:if test="$startPos + 0 &lt;= $Pos + 0 and $endPos + 0 &gt; $Pos + 0">X</xsl:if>
                </xsl:for-each>
            </xsl:variable>

            <xsl:choose>
                <!-- case where there is no event at or across the current timepoint -->
                <xsl:when test="not(contains($interval_is_covered, 'X'))">
                    <td>
                        <xsl:attribute name="class">
                            <!-- TODO: check why this is so complex - don't we have the variable's value already? -->
                            <xsl:variable name="CATEGORY" select="//tier[@id = current()/../../it-line[$itLinePosition + 0]/@formatref]/@category"/>
                            <!-- TODO: check why this is so complex , use parameters maybe -->
                            <xsl:if
                                test="($CATEGORY != 'k' and count(current()/../../it-line[$itLinePosition + 0]/following-sibling::*) = 0) or ($CATEGORY != 'k' and //tier[@id = current()/../../it-line[$itLinePosition + 0]/following-sibling::*[1]/@formatref]/@category = 'k')">
                                <xsl:text>b </xsl:text>
                            </xsl:if>
                            <xsl:if test="$CATEGORY != 'k' and count(current()/following-sibling::*) = 0">
                                <xsl:text>r </xsl:text>
                            </xsl:if>
                            <xsl:text>emp</xsl:text>
                        </xsl:attribute>

                        <!-- if this is the last entry in that row: stretch it! -->
                        <xsl:if test="count(current()/following-sibling::*) = 0">
                            <xsl:attribute name="width">100%</xsl:attribute>
                        </xsl:if>
                    </td>
                </xsl:when>

                <!-- case where there IS an event at the current timepoint -->
                <xsl:otherwise>
                    <xsl:apply-templates select="../../it-line[$itLinePosition + 0]/it-chunk[@start-sync = current()/@id]"/>
                </xsl:otherwise>

            </xsl:choose>
        </xsl:for-each>

    </xsl:template>

    <xsl:template match="it-label">
        <xsl:variable name="CATEGORY" select="//tier[@id = current()/../@formatref]/@category"/>
        <xsl:element name="td">
            <xsl:attribute name="class">
                <!-- check if it is the last tier in the partitur frame -->
                <xsl:if test="($CATEGORY != 'k' and count(../following-sibling::*) = 0) or ($CATEGORY != 'k' and //tier[@id = current()/../following-sibling::*[1]/@formatref]/@category = 'k')">
                    <xsl:text>b </xsl:text>
                </xsl:if>
                <!-- check whether it is a main or a subordinate tier -->
                <xsl:choose>
                    <xsl:when test="//tier[@id = current()/../@formatref]/@category = 'v'">tlm</xsl:when>
                    <xsl:otherwise>tlo</xsl:otherwise>
                </xsl:choose>
                <!-- TODO: check if this can/should be parameterized -->
                <xsl:if test="//tier[@id = current()/../@formatref]/@category = 'k'">
                    <xsl:text> nlb</xsl:text>
                </xsl:if>
            </xsl:attribute>

            <!-- the tooltip title for this tier -->
            <xsl:attribute name="title">
                <xsl:variable name="SPEAKER_ID" select="//tier[@id = current()/../@formatref]/@speaker" as="xs:string"/>
                <xsl:value-of select="key('tier-title-by-category', $CATEGORY, $TIER_TITLES)"/>
            </xsl:attribute>

            <xsl:value-of select="run/text()"/>

            <!-- two non-breaking spaces behind the tier-label -->
            <xsl:if test="not(string-length(run/text()) = 0)">
                <xsl:text>&#x00A0;&#x00A0;</xsl:text>
            </xsl:if>

        </xsl:element>
    </xsl:template>

    <xsl:template match="it-chunk">
        <xsl:variable name="CATEGORY" select="//tier[@id = current()/../@formatref]/@category"/>
        <xsl:variable name="cellspan"
            select="count(../../sync-points/sync-point[@id = current()/@end-sync]/preceding-sibling::*) - count(../../sync-points/sync-point[@id = current()/@start-sync]/preceding-sibling::*)"/>
        <xsl:variable name="tiercategory" select="//tier[@id = current()/@formatref]/@category"/>

        <td colspan="{$cellspan}">
            <xsl:attribute name="class">
                <xsl:if test="($CATEGORY != 'k' and count(../following-sibling::*) = 0) or ($CATEGORY != 'k' and //tier[@id = current()/../following-sibling::*[1]/@formatref]/@category = 'k')">
                    <xsl:text>b </xsl:text>
                </xsl:if>
                <xsl:value-of select="$tiercategory"/>
            </xsl:attribute>

            <xsl:attribute name="data-tl">
                <xsl:variable name="TIMESTART" select="0 + //tli[@id = current()/@start-sync]/@time"/>
                <xsl:variable name="TIMEEND">
                    <xsl:choose>
                        <xsl:when test="number(//tli[@id = current()/@end-sync]/@time) = number(//tli[@id = current()/@end-sync]/@time)">
                            <!-- One needs to adjust the 0 so that the previous annotation would not get highlighted when one is started by clicking play icon -->
                            <xsl:value-of select="0 + //tli[@id = current()/@end-sync]/@time"/>
                        </xsl:when>
                        <xsl:when
                            test="number(//tli[@id = current()/../../following-sibling::it-bundle[1]/it-line[1]/it-chunk[1]/@end-sync]/@time) = number(//tli[@id = current()/../../following-sibling::it-bundle[1]/it-line[1]/it-chunk[1]/@end-sync]/@time)">
                            <xsl:value-of select="0 + //tli[@id = current()/../../following-sibling::it-bundle[1]/it-line[1]/it-chunk[1]/@end-sync]/@time"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="0 + max(//tli/@time)"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:if test="not(format-number($TIMESTART, '#.##') = 'NaN') and not(format-number($TIMEEND, '#.##') = 'NaN')">
                    <xsl:value-of select="concat(format-number($TIMESTART, '#.##'), '-', format-number($TIMEEND, '#.##'))"/>
                </xsl:if>
            </xsl:attribute>

            <xsl:apply-templates/>
        </td>
    </xsl:template>

    <!-- *********************************************************************************** -->
    <!-- *********************************************************************************** -->
    <!-- ************************** HTML Templates ***************************************** -->
    <!-- *********************************************************************************** -->
    <!-- *********************************************************************************** -->

    <xsl:template name="MAKE_TITLE">
        <div id="head">
            <span id="document-title">
                <xsl:value-of select="$TRANSCRIPTION_NAME"/>
            </span>
        </div>
    </xsl:template>

    <xsl:template name="MAKE_PLAYER_DIV">
        <div id="mediaplayer" class="sidebarcontrol">
            <xsl:if test="$HAS_VIDEO">
                <video controls="controls" width="320" height="240" data-tlid="media">
                    <source src="{$DATASTREAM_VIDEO}" type="video/{$RECORDING_TYPE}"/>
                    <xsl:if test="not(starts-with($RECORDING_PATH, 'http'))">
                        <source src="https://corpora.uni-hamburg.de/hzsk/de/islandora/object/recording:{replace(lower-case($CORPUS_NAME), '\s+corpus\s+', '-')}_{$TRANSCRIPTION_NAME}/datastream/{upper-case($RECORDING_TYPE)}/{$RECORDING_PATH}" type="video/{$RECORDING_TYPE}"/>
                    </xsl:if>
                </video>
            </xsl:if>
            <xsl:if test="not($HAS_VIDEO) and ($HAS_AUDIO)">
                <audio controls="controls" data-tlid="media">
                    <source src="{$DATASTREAM_AUDIO}" type="audio/{$RECORDING_TYPE}"/>
                    <xsl:if test="not(starts-with($RECORDING_PATH, 'http'))">
                        <source src="https://corpora.uni-hamburg.de/hzsk/de/islandora/object/recording:{replace(lower-case($CORPUS_NAME), '\s+corpus\s+', '-')}_{$TRANSCRIPTION_NAME}/datastream/{upper-case($RECORDING_TYPE)}/{$RECORDING_PATH}" type="audio/{$RECORDING_TYPE}"/>
                    </xsl:if>
                </audio>
            </xsl:if>
        </div>
    </xsl:template>

    <xsl:template name="MAKE_TIER_DISPLAY_CONTROL">
        <div class="sidebarcontrol">
            <div class="collapse_box" id="tier_display">
                <div class="collapse_title">
                    <!--<img alt="Minimize_grey" class="collapse_icon" src="../../resources/minusButton.png" /> -->
					Tier display
				</div>
                <div class="collapse_content">
                    <xsl:for-each-group select="//tier/@category" group-by=".">
                        <xsl:sort select="."/>
                        <input style="margin-left:7px;" type="checkbox" name="category" value="{current-grouping-key()}" checked="checked" onclick="showHideTier(this,'{current-grouping-key()}')">
                            <b>
                                <xsl:value-of select="current-grouping-key()"/>
                            </b>
                        </input>
                    </xsl:for-each-group>
                </div>
            </div>
        </div>
    </xsl:template>

    <xsl:template name="MAKE_DOWNLOAD_FILES_CONTROL">
        <div class="sidebarcontrol">
            <div class="collapse_box" id="tier_display">
                <div class="collapse_title">
				 	Files
				</div>
                <div class="collapse_content">
                    <img alt="EXB" class="collapse_icon" src="{$TOP_LEVEL_PATH}exb-icon.png"/>
                    <a href="{test}/EXB" style="text-decoration:none;font-weight:bold;font-family:sans-serif;font-size:10pt;"
                        >
                        EXMARaLDA Basic Transcription
                    </a>
                    <br/>
                    <img alt="EXS" class="collapse_icon" src="{$TOP_LEVEL_PATH}exs-icon.png"/>
                    <a href="{$RECORDING_PATH}/EXB" style="text-decoration:none;font-weight:bold;font-family:sans-serif;font-size:10pt;"
                        >
                        EXMARaLDA Segmented Transcription
                    </a>
                    <br/>
                </div>
            </div>
        </div>
    </xsl:template>

    <xsl:template name="MAKE_FOOTER">
        <div id="footer-new">
            <p>
                This visualization was generated on <xsl:value-of select="format-date(current-date(), '[D01].[M01].[Y0001]')"/> with Corpus Services.
                <!--with <xsl:value-of select="$WEBSERVICE_NAME"
                />.-->
                <!--Please contact HZSK for more information: <xsl:value-of select="$EMAIL_ADDRESS"/>--> </p>
        </div>
    </xsl:template>

    <xsl:template name="MAKE_WEB_SERVICE_INFO">
        <div class="sidebarcontrol">
            <div class="collapse_box" id="tier_display">
                <div class="collapse_title"> Web service information </div>
                <div class="collapse_content" style="width:310;">
                    <p>
                        Generated on <xsl:value-of select="format-date(current-date(), '[D01].[M01].[Y0001]')"/> with Corpus Services.
                        <!--with <xsl:value-of select="$WEBSERVICE_NAME"
                        />.-->
                    </p>
                    <!--p>Please contact the <a href="{$HZSK_WEBSITE}" title="Hamburger Zentrum für Sprachkorpora">HZSK</a> for more information.</p-->
                </div>
            </div>
        </div>
    </xsl:template>

    <xsl:function name="exmaralda:FORMAT_TIME">
        <xsl:param name="TIME"/>
        <xsl:variable name="totalseconds" select="0 + $TIME"/>
        <xsl:variable name="hours" select="0 + floor($totalseconds div 3600)"/>
        <xsl:variable name="minutes" select="0 + floor(($totalseconds - 3600 * $hours) div 60)"/>
        <xsl:variable name="seconds" select="0 + ($totalseconds - 3600 * $hours - 60 * $minutes)"/>
        <xsl:if test="$hours + 0 &lt; 10 and $hours &gt; 0">
            <xsl:text>0</xsl:text>
            <xsl:value-of select="$hours"/>
        </xsl:if>
        <xsl:if test="$hours + 0 = 0">
            <xsl:text>00</xsl:text>
        </xsl:if>
        <xsl:text>:</xsl:text>
        <xsl:if test="$minutes + 0 &lt; 10">
            <xsl:text>0</xsl:text>
        </xsl:if>
        <xsl:value-of select="$minutes"/>
        <xsl:text>:</xsl:text>
        <xsl:if test="$seconds + 0 &lt; 10">
            <xsl:text>0</xsl:text>
        </xsl:if>
        <xsl:value-of select="round($seconds * 100) div 100"/>
    </xsl:function>

    <xsl:function name="exmaralda:FORMAT_TIMELIGHT">
        <xsl:param name="TIME"/>
        <xsl:variable name="totalseconds" select="0 + $TIME"/>
        <xsl:value-of select="$totalseconds"/>
    </xsl:function>

    <xsl:function name="string:ID-conform" as="xs:string">
        <xsl:param name="string" as="xs:string"/>
        <xsl:value-of select="string:multi-replace($string, ('^\s+', '[\s\\/\(\)\[\]´`''\.:,;\?#=\+_]+$', '[\s\\/\(\)\[\]´`'':,;\?#=\+_]+', 'Ä', 'Ö', 'Ü', 'ä', 'ö', 'ü', 'ß', '[éèê]', '[íìî]', '[úùû]', '[áàâ]', '[óòô]'), ('', '', '_', 'Ae', 'Oe', 'Ue', 'ae', 'oe', 'ue', 'ss', 'e', 'i', 'u', 'a', 'o' ))"/>
    </xsl:function>
    
    <xsl:function name="string:multi-replace" as="xs:string">
        <xsl:param name="input" as="xs:string"/>
        <xsl:param name="replace" as="xs:string*"/>
        <xsl:param name="with" as="xs:string*"/>
        <xsl:choose>
            <xsl:when test="$replace[1]">
                <xsl:copy-of select="string:multi-replace(replace($input, $replace[1], $with[1]), subsequence($replace, 2), subsequence($with, 2))"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$input"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

</xsl:stylesheet>
