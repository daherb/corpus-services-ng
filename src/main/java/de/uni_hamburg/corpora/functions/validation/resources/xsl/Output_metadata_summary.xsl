<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
    <xsl:decimal-format name="european" decimal-separator="."/>

    <!-- param for INEL -->
    <xsl:param name="mode" select="'normal'"/>

    <xsl:template match="/">
        <html>
            <head>
                <xsl:call-template name="GENERATE_STYLES"/>
                <link rel="stylesheet" type="text/css"
                    href="https://cdn.datatables.net/1.10.16/css/jquery.dataTables.css"/>
                <script type="text/javascript" charset="utf8" src="https://code.jquery.com/jquery-3.2.1.min.js"/>
                <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.10.16/js/jquery.dataTables.js"/>
                <script type="text/javascript" class="init">
                    
                    $(document).ready(function() {
                    $('table.compact').DataTable();
                    } );           
                </script>
            </head>
        <body> 
        <div id='timestamp'>Generated: <xsl:value-of select="format-dateTime(current-dateTime(),'[Y0001]-[M01]-[D01] [H01]:[m01]:[f]')"/></div>
        <xsl:call-template name="GET_STATISTICAL_INFO_ON_WHOLE_CORPUS"/>
        <xsl:if test="$mode = 'inel'">
            <xsl:call-template name="GET_INEL_SHORT_OVERVIEW"/>
        </xsl:if>
        <xsl:call-template name="GET_DESCRIPTIONS_WITH_ADDITIONAL_INFO"/>
        <!--        <xsl:call-template name="GET_DESCRIPTIONS">
            <xsl:with-param name="PARENT">Communication</xsl:with-param>
        </xsl:call-template>-->
        <xsl:call-template name="GET_DESCRIPTIONS">
            <xsl:with-param name="PARENT">Setting</xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="GET_DESCRIPTIONS">
            <xsl:with-param name="PARENT">Speaker</xsl:with-param>
        </xsl:call-template>
        <xsl:if test="$mode = 'normal'">
            <xsl:call-template name="GET_DESCRIPTIONS">
                <xsl:with-param name="PARENT">Transcription</xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="GET_DESCRIPTIONS">
                <xsl:with-param name="PARENT">Recording</xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="GET_DESCRIPTIONS">
                <xsl:with-param name="PARENT">AsocFile</xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        <xsl:call-template name="GET_KEYS_LOC">
            <xsl:with-param name="PARENT">Communication</xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="GET_KEYS_LOC">
            <xsl:with-param name="PARENT">Speaker</xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="GET_KEYS_LANG">
            <xsl:with-param name="PARENT">Communication</xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="GET_KEYS_LANG">
            <xsl:with-param name="PARENT">Speaker</xsl:with-param>
        </xsl:call-template>
      </body>
        </html>  
    </xsl:template>



    <xsl:template name="GET_STATISTICAL_INFO_ON_WHOLE_CORPUS">
        <h1> Statistical Info on Whole Corpus</h1>
        <table id="" class="info">
            <thead>
                <tr>
                    <th class="info">Number of Sentences Whole Corpus</th>
                    <th class="info">Number of Words Whole Corpus</th>
                    <th class="info">Duration of Audio (hh:mm:ss) Whole Corpus</th>
                    <th class="info">Number of Communi-cations Whole Corpus</th>
                    <th class="info">Number Exb Whole Corpus</th>
                    <th class="info">Number Exs Whole Corpus</th>
                    <th class="info">Number of Speakers Whole Corpus</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td class="info">
                        <xsl:value-of
                            select="sum(//Transcription/Description/Key[@Name = '# HIAT:u'])"/>
                    </td>
                    <td class="info">
                        <xsl:value-of
                            select="sum(//Transcription/Description/Key[@Name = '# HIAT:w'])"/>
                    </td>
                    <td class="info">
                        <xsl:variable name="milliseconds">
                            <xsl:value-of
                                select="sum(//Communication/Recording[1]/RecordingDuration)"/>
                        </xsl:variable>
                        <xsl:variable name="hours" select="floor($milliseconds div (1000 * 3600))"/>
                        <xsl:variable name="minutes"
                            select="($milliseconds mod (1000 * 3600)) div (1000 * 60)"/>
                        <xsl:variable name="seconds"
                            select="(($milliseconds mod (1000 * 60)) div 1000)"/>
                        <xsl:value-of
                            select="concat(format-number($hours, '#00'), ':', format-number($minutes, '00'), ':')"/>
                        <xsl:value-of select="format-number($seconds, '00', 'european')"/>
                    </td>
                    <td class="info">
                        <xsl:value-of
                            select="count(//Communication)"
                        />
                    </td>
                    <td class="info">
                        <xsl:value-of
                            select="count(//Transcription/Description/Key[@Name = 'segmented' and text() = 'false'])"
                        />
                    </td>
                    <td class="info">
                        <xsl:value-of
                            select="count(//Transcription/Description/Key[@Name = 'segmented' and text() = 'true'])"
                        />
                    </td>
                    <td class="info">
                        <xsl:value-of
                            select="count(//Speaker)"
                        />
                    </td>
                </tr>
            </tbody>
        </table>
    </xsl:template>

    <xsl:template name="GET_INEL_SHORT_OVERVIEW">
        <h1> Short Overview</h1>
        <table id="" class="compact">
            <thead>
                <tr>
                    <th>Communication Name</th>
                    <th>Duration of Audio (hh:mm:ss)</th>
                    <th>Number of Sentences</th>
                    <th>Number of Words</th>
                    <th>Transcribed by</th>
                    <th>Glossed by</th>
                    <th>Has EXB</th>
                    <th>Translation completed</th>
                    <th>Annotation completed</th>
                </tr>
            </thead>
            <tbody>
                <xsl:for-each-group select="//Communication" group-by="@Name">
                    <xsl:sort select="current-grouping-key()"/>
                    <xsl:variable name="commElement" select="."/>
                    <tr>
                        <td class="firstcolumn">
                            <xsl:value-of select="current-grouping-key()"/>
                        </td>
                        <td>
                            <xsl:choose>
                                <xsl:when
                                    test="not($commElement//Recording/Media/NSLink[ends-with(lower-case(text()), '.wav')])"
                                    > no wav </xsl:when>
                                <xsl:when test="$commElement/Recording[1]/RecordingDuration/text()">
                                    <xsl:variable name="milliseconds">
                                        <xsl:value-of
                                            select="$commElement/Recording[1]/RecordingDuration[1]/text()"
                                        />
                                    </xsl:variable>
                                    <xsl:variable name="hours"
                                        select="floor($milliseconds div (1000 * 3600))"/>
                                    <xsl:variable name="minutes"
                                        select="($milliseconds mod (1000 * 3600)) div (1000 * 60)"/>
                                    <xsl:variable name="seconds"
                                        select="(($milliseconds mod (1000 * 60)) div 1000)"/>
                                    <xsl:value-of
                                        select="concat(format-number($hours, '#00'), ':', format-number($minutes, '00'), ':')"/>
                                    <xsl:value-of select="format-number($seconds, '00', 'european')"
                                    />
                                </xsl:when>
                                <xsl:otherwise> no duration added </xsl:otherwise>
                            </xsl:choose>
                        </td>
                        <td>
                            <xsl:choose>
                                <xsl:when
                                    test="$commElement/Transcription/Description/Key[@Name = 'segmented']/text() = 'true'">
                                    <!--     and it need to be a wav file too-->
                                    <xsl:attribute name="data-order">
                                        <xsl:value-of
                                            select="$commElement/Transcription/Description/Key[@Name = '# HIAT:u']/text()"
                                        />
                                    </xsl:attribute>
                                    <xsl:value-of
                                        select="$commElement/Transcription/Description/Key[@Name = '# HIAT:u']/text()"
                                    />
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:attribute name="data-order">
                                        <xsl:value-of select="0"/>
                                    </xsl:attribute> no exs </xsl:otherwise>
                            </xsl:choose>
                        </td>
                        <td>
                            <xsl:choose>
                                <xsl:when
                                    test="$commElement/Transcription/Description/Key[@Name = 'segmented']/text() = 'true'">
                                    <!--     and it need to be a wav file too-->
                                    <xsl:attribute name="data-order">
                                        <xsl:value-of
                                            select="$commElement/Transcription/Description/Key[@Name = '# HIAT:w']/text()"
                                        />
                                    </xsl:attribute>
                                    <xsl:value-of
                                        select="$commElement/Transcription/Description/Key[@Name = '# HIAT:w']/text()"
                                    />
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:attribute name="data-order">
                                        <xsl:value-of select="0"/>
                                    </xsl:attribute> no exs </xsl:otherwise>
                            </xsl:choose>
                        </td>
                        <td>
                            <xsl:value-of
                                select="$commElement/Description/Key[contains(lower-case(@Name), 'transcribed')]/text()"
                            />
                        </td>
                        <td>
                            <xsl:value-of
                                select="$commElement/Description/Key[contains(lower-case(@Name), 'glossed')]/text()"
                            />
                        </td>
                        <td>
                            <xsl:choose>
                                <xsl:when
                                    test="$commElement/Transcription/Description/Key[@Name = 'segmented']/text() = 'false'"
                                    > yes </xsl:when>
                                <xsl:otherwise> no </xsl:otherwise>
                            </xsl:choose>
                        </td>
                        <td>
                            <xsl:choose>
                                <!-- TO DO -->
                                <xsl:when
                                    test="$commElement/Description/Key[contains(lower-case(@Name), 'translation') and (contains(text(), '.') or contains(text(), '?') or not(string(text())))]"
                                    > no(<xsl:value-of
                                        select="$commElement/Description/Key[contains(lower-case(@Name), 'translation')]"
                                        separator="|"/>) </xsl:when>
                                <xsl:otherwise> yes(<xsl:value-of
                                        select="$commElement/Description/Key[contains(lower-case(@Name), 'translation')]"
                                        separator="|"/>) </xsl:otherwise>
                            </xsl:choose>
                        </td>
                        <td>
                            <xsl:choose>
                                <xsl:when
                                    test="$commElement/Description/Key[contains(lower-case(@Name), 'annotation') and (contains(text(), '.') or contains(text(), '?') or not(string(text())))]"
                                    > no(<xsl:value-of
                                        select="$commElement/Description/Key[contains(lower-case(@Name), 'annotation')]"
                                        separator="|"/>) </xsl:when>
                                <xsl:otherwise> yes(<xsl:value-of
                                        select="$commElement/Description/Key[contains(lower-case(@Name), 'annotation')]"
                                        separator="|"/>) </xsl:otherwise>
                            </xsl:choose>
                        </td>
                    </tr>
                </xsl:for-each-group>
            </tbody>
        </table>
    </xsl:template>

    <xsl:template name="GET_DESCRIPTIONS_WITH_ADDITIONAL_INFO">
        <xsl:if test="//Communication/Description/Key[not(starts-with(@Name, '#'))]">
            <h1> Communication Descriptions</h1>
            <table id="" class="compact">
                <xsl:variable name="temp">
                    <xsl:for-each-group
                        select="//Communication/Description/Key[not(starts-with(@Name, '#'))]"
                        group-by="@Name">
                        <xsl:sort select="current-grouping-key()"/>
                        <group>
                            <xsl:value-of select="current-grouping-key()"/>
                        </group>
                    </xsl:for-each-group>
                </xsl:variable>

                <xsl:variable name="columnheaders" as="text()">
                    <xsl:value-of select="$temp/group/text()"/>
                </xsl:variable>


                <thead>
                    <tr>
                        <th>Communication Name</th>
                        <th>Number of Utterances</th>
                        <th>Number of Words</th>
                        <th>Duration of Audio (hh:mm:ss)</th>
                        <th>Has Exb</th>
                        <xsl:for-each-group
                            select="//Communication/Description/Key[not(starts-with(@Name, '#'))]"
                            group-by="@Name">
                            <xsl:sort select="current-grouping-key()"/>
                            <th>
                                <xsl:value-of select="current-grouping-key()"/>
                            </th>
                        </xsl:for-each-group>
                    </tr>
                </thead>
                <tbody>
                    <xsl:for-each-group select="//Communication" group-by="@Name">
                        <xsl:sort select="current-grouping-key()"/>
                        <xsl:variable name="commElement" select="."/>
                        <tr>
                            <td class="firstcolumn">
                                <xsl:value-of select="current-grouping-key()"/>
                            </td>
                            <td>
                                <xsl:choose>
                                    <xsl:when
                                        test="$commElement/Transcription/Description/Key[@Name = 'segmented']/text() = 'true'">
                                        <!--     and it need to be a wav file too-->
                                        <xsl:attribute name="data-order">
                                            <xsl:value-of
                                                select="$commElement/Transcription/Description/Key[@Name = '# HIAT:u']/text()"
                                            />
                                        </xsl:attribute>
                                        <xsl:value-of
                                            select="$commElement/Transcription/Description/Key[@Name = '# HIAT:u']/text()"
                                        />
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:attribute name="data-order">
                                            <xsl:value-of select="0"/>
                                        </xsl:attribute> no exs </xsl:otherwise>
                                </xsl:choose>
                            </td>
                            <td>
                                <xsl:choose>
                                    <xsl:when
                                        test="$commElement/Transcription/Description/Key[@Name = 'segmented']/text() = 'true'">
                                        <!--     and it need to be a wav file too-->
                                        <xsl:attribute name="data-order">
                                            <xsl:value-of
                                                select="$commElement/Transcription/Description/Key[@Name = '# HIAT:w']/text()"
                                            />
                                        </xsl:attribute>
                                        <xsl:value-of
                                            select="$commElement/Transcription/Description/Key[@Name = '# HIAT:w']/text()"
                                        />
                                    </xsl:when>
                                    <xsl:otherwise><xsl:attribute name="data-order">
                                            <xsl:value-of select="0"/>
                                        </xsl:attribute> no exs </xsl:otherwise>
                                </xsl:choose>
                            </td>
                            <td>
                                <xsl:choose>
                                    <xsl:when
                                        test="not($commElement//Recording/Media/NSLink[ends-with(lower-case(text()), '.wav')])"
                                        > no wav </xsl:when>
                                    <xsl:when
                                        test="$commElement/Recording[1]/RecordingDuration/text()">
                                        <xsl:variable name="milliseconds">
                                            <xsl:value-of
                                                select="$commElement/Recording[1]/RecordingDuration[1]/text()"
                                            />
                                        </xsl:variable>
                                        <xsl:variable name="hours"
                                            select="floor($milliseconds div (1000 * 3600))"/>
                                        <xsl:variable name="minutes"
                                            select="($milliseconds mod (1000 * 3600)) div (1000 * 60)"/>
                                        <xsl:variable name="seconds"
                                            select="(($milliseconds mod (1000 * 60)) div 1000)"/>
                                        <xsl:value-of
                                            select="concat(format-number($hours, '#00'), ':', format-number($minutes, '00'), ':')"/>
                                        <xsl:value-of
                                            select="format-number($seconds, '00', 'european')"/>
                                    </xsl:when>
                                    <xsl:otherwise> no duration added </xsl:otherwise>
                                </xsl:choose>

                            </td>
                            <td>
                                <xsl:choose>
                                    <xsl:when
                                        test="$commElement/Transcription/Description/Key[@Name = 'segmented']/text() = 'false'"
                                        > yes </xsl:when>
                                    <xsl:otherwise> no </xsl:otherwise>
                                </xsl:choose>
                            </td>
                            <xsl:for-each select="1 to count($temp/group)">
                                <xsl:variable name="number" select="."/>
                                <xsl:choose>
                                    <xsl:when
                                        test="$commElement/Description/Key[@Name = $temp/group[$number]]/text()">
                                        <td>
                                            <xsl:value-of
                                                select="$commElement/Description/Key[@Name = $temp/group[$number]]/text()"
                                            />
                                        </td>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <td/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:for-each>
                            <!-- <td>
                                    <xsl:variable name="DISTINCT_VALUES" select="count(distinct-values(current-group()/text()))"/>
                                    <i> <xsl:value-of select="$DISTINCT_VALUES"/> distinct values</i>
                                    <br/>
                                    <xsl:for-each-group select="current-group()" group-by="text()">
                                        <xsl:sort select="count(current-group())" order="descending"/>
                                        <xsl:if test="$DISTINCT_VALUES &lt; 8 or count(current-group()) > 3">
                                            <xsl:value-of select="substring(current-grouping-key(), 0, 50)"/>
                                            <xsl:if test="string-length(current-grouping-key()) > 50">
                                                <i> [...]</i>
                                            </xsl:if>
                                            <xsl:text> (</xsl:text>
                                            <xsl:value-of select="count(current-group())"/>
                                            <xsl:text>)</xsl:text>
                                            <br/>
                                        </xsl:if>
                                    </xsl:for-each-group>
                                </td>-->


                        </tr>
                    </xsl:for-each-group>
                </tbody>
            </table>
        </xsl:if>
    </xsl:template>

    <xsl:template name="GET_KEYS_LOC">
        <xsl:param name="PARENT"/>
        <xsl:for-each-group select="//*[name() = $PARENT]/Location" group-by="@Type">

            <xsl:if test="//*[name() = $PARENT]/Location[@Type = current-grouping-key()]">
                <h1><xsl:value-of select="$PARENT"/> (<xsl:value-of select="current-grouping-key()"
                    />) Locations</h1>
                <table id="" class="compact">
                    <xsl:variable name="temp">
                        <xsl:for-each-group
                            select="//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/Description/Key[not(starts-with(@Name, '#'))]"
                            group-by="@Name">
                            <xsl:sort select="current-grouping-key()"/>
                            <group>
                                <xsl:value-of select="current-grouping-key()"/>
                            </group>
                        </xsl:for-each-group>
                    </xsl:variable>


                    <xsl:variable name="columnheaders" as="text()">
                        <xsl:value-of select="$temp/group/text()"/>
                    </xsl:variable>
                    <thead>
                        <tr>
                            <th><xsl:value-of select="$PARENT"/> Name</th>
                            <th>Type</th>
                            <xsl:if
                                test="//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/Street">
                                <th>Street</th>
                            </xsl:if>
                            <xsl:if
                                test="//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/City">
                                <th>City</th>
                            </xsl:if>
                            <xsl:if
                                test="//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/PostalCode">
                                <th>PostalCode</th>
                            </xsl:if>
                            <xsl:if
                                test="//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/Country">
                                <th>Country</th>
                            </xsl:if>
                            <xsl:if
                                test="(//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/Period/PeriodStart) or (//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/Period/PeriodExact) or (//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/Period/PeriodDuration)">
                                <th>PeriodStart</th>
                                <th>PeriodExcact</th>
                                <th>PeriodDuration</th>
                            </xsl:if>
                            <xsl:for-each-group
                                select="//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/Description/Key[not(starts-with(@Name, '#'))]"
                                group-by="@Name">
                                <xsl:sort select="current-grouping-key()"/>
                                <th>
                                    <xsl:value-of select="current-grouping-key()"/>
                                </th>
                            </xsl:for-each-group>
                        </tr>
                    </thead>
                    <tbody>
                        <xsl:for-each
                            select="//*[name() = $PARENT]/Location[@Type = current-grouping-key()]">
                            <xsl:variable name="commElement" select="."/>
                            <tr>
                                <td class="firstcolumn">
                                    <xsl:choose>
                                        <xsl:when test="./../Sigle">
                                            <xsl:value-of select="./../Sigle"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="./../@Name"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </td>
                                <td>
                                    <xsl:value-of select="./@Type"/>
                                </td>
                                <xsl:if
                                    test="//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/Street">
                                    <td>
                                        <xsl:value-of select="./Street"/>
                                    </td>
                                </xsl:if>
                                <xsl:if
                                    test="//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/City">
                                    <td>
                                        <xsl:value-of select="./City"/>
                                    </td>
                                </xsl:if>
                                <xsl:if
                                    test="//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/PostalCode">
                                    <td>
                                        <xsl:value-of select="./PostalCode"/>
                                    </td>
                                </xsl:if>
                                <xsl:if
                                    test="//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/Country">
                                    <td>
                                        <xsl:value-of select="./Country"/>
                                    </td>
                                </xsl:if>
                                <xsl:if
                                    test="(//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/Period/PeriodStart) or (//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/Period/PeriodExact) or (//*[name() = $PARENT]/Location[@Type = current-grouping-key()]/Period/PeriodDuration)">
                                    <td>
                                        <xsl:value-of select="./Period/PeriodStart"/>
                                    </td>
                                    <td>
                                        <xsl:value-of select="./Period/PeriodExcact"/>
                                    </td>
                                    <td>
                                        <xsl:value-of select="./Period/PeriodDuration"/>
                                    </td>
                                </xsl:if>
                                <xsl:for-each select="1 to count($temp/group)">
                                    <xsl:variable name="number" select="."/>
                                    <xsl:choose>
                                        <xsl:when
                                            test="$commElement/Description/Key[@Name = $temp/group[$number]]/text()">
                                            <td>
                                                <xsl:value-of
                                                  select="$commElement/Description/Key[@Name = $temp/group[$number]]/text()"
                                                />
                                            </td>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <td/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:for-each>
                            </tr>
                        </xsl:for-each>
                    </tbody>
                </table>
            </xsl:if>
        </xsl:for-each-group>
    </xsl:template>

    <xsl:template name="GET_KEYS_LANG">
        <xsl:param name="PARENT"/>
        <xsl:for-each-group select="//*[name() = $PARENT]/Language" group-by="@Type">
            <xsl:if test="//*[name() = $PARENT]/Language[@Type = current-grouping-key()]">
                <h1><xsl:value-of select="$PARENT"/> (<xsl:value-of select="current-grouping-key()"
                    />) Languages</h1>
                <table id="" class="compact">
                    <xsl:variable name="temp">
                        <xsl:for-each-group
                            select="//*[name() = $PARENT]/Language[@Type = current-grouping-key()]/Description/Key[not(starts-with(@Name, '#'))]"
                            group-by="@Name">
                            <xsl:sort select="current-grouping-key()"/>
                            <group>
                                <xsl:value-of select="current-grouping-key()"/>
                            </group>
                        </xsl:for-each-group>
                    </xsl:variable>


                    <xsl:variable name="columnheaders" as="text()">
                        <xsl:value-of select="$temp/group/text()"/>
                    </xsl:variable>
                    <thead>
                        <tr>
                            <th><xsl:value-of select="$PARENT"/> Name</th>
                            <th>Type</th>
                            <th>LanguageCode</th>
                            <xsl:for-each-group
                                select="//*[name() = $PARENT]/Language[@Type = current-grouping-key()]/Description/Key[not(starts-with(@Name, '#'))]"
                                group-by="@Name">
                                <xsl:sort select="current-grouping-key()"/>
                                <th>
                                    <xsl:value-of select="current-grouping-key()"/>
                                </th>
                            </xsl:for-each-group>
                        </tr>
                    </thead>
                    <tbody>
                        <xsl:for-each
                            select="//*[name() = $PARENT]/Language[@Type = current-grouping-key()]">
                            <xsl:variable name="commElement" select="."/>
                            <tr>
                                <td class="firstcolumn">
                                    <xsl:choose>
                                        <xsl:when test="./../Sigle">
                                            <xsl:value-of select="./../Sigle"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="./../@Name"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </td>
                                <td>
                                    <xsl:value-of select="./@Type"/>
                                </td>
                                <td>
                                    <xsl:value-of select="./LanguageCode/text()"/>
                                </td>
                                <xsl:for-each select="1 to count($temp/group)">
                                    <xsl:variable name="number" select="."/>
                                    <xsl:choose>
                                        <xsl:when
                                            test="$commElement/Description/Key[@Name = $temp/group[$number]]/text()">
                                            <td>
                                                <xsl:value-of
                                                  select="$commElement/Description/Key[@Name = $temp/group[$number]]/text()"
                                                />
                                            </td>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <td/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:for-each>
                            </tr>
                        </xsl:for-each>
                    </tbody>
                </table>
            </xsl:if>
        </xsl:for-each-group>
    </xsl:template>

    <xsl:template name="GET_DESCRIPTIONS">
        <xsl:param name="PARENT"/>
        <xsl:if test="//*[name() = $PARENT]/Description/Key[not(starts-with(@Name, '#'))]">
            <h1><xsl:value-of select="$PARENT"/> Descriptions</h1>
            <table id="" class="compact">
                <xsl:variable name="temp">
                    <xsl:for-each-group
                        select="//*[name() = $PARENT]/Description/Key[not(starts-with(@Name, '#'))]"
                        group-by="@Name">
                        <xsl:sort select="current-grouping-key()"/>
                        <group>
                            <xsl:value-of select="current-grouping-key()"/>
                        </group>
                    </xsl:for-each-group>
                </xsl:variable>


                <xsl:variable name="columnheaders" as="text()">
                    <xsl:value-of select="$temp/group/text()"/>
                </xsl:variable>
                <thead>
                    <tr>
                        <th>
                            <xsl:choose>
                                <xsl:when test="$PARENT = 'Speaker' or $PARENT = 'Communication'">
                                    <xsl:value-of select="$PARENT"/></xsl:when>
                            </xsl:choose> Name</th>
                        <xsl:for-each-group
                            select="//*[name() = $PARENT]/Description/Key[not(starts-with(@Name, '#'))]"
                            group-by="@Name">
                            <xsl:sort select="current-grouping-key()"/>
                            <th>
                                <xsl:value-of select="current-grouping-key()"/>
                            </th>
                        </xsl:for-each-group>
                        <xsl:if test="$PARENT = 'Speaker'">
                            <th>Pseudo</th>
                            <th>Sex</th>
                        </xsl:if>
                    </tr>
                </thead>
                <tbody>
                    <xsl:for-each select="//*[name() = $PARENT]">
                        <xsl:variable name="commElement" select="."/>
                        <tr>
                            <td class="firstcolumn">
                                <xsl:choose>
                                    <xsl:when test="./Sigle">
                                        <xsl:value-of select="./Sigle"/>
                                    </xsl:when>
                                    <xsl:when test="./../Sigle">
                                        <xsl:value-of select="./../Sigle"/>
                                    </xsl:when>
                                    <xsl:when test="./@Name">
                                        <xsl:value-of select="./@Name"/>
                                    </xsl:when>
                                    <xsl:when test="./../@Name">
                                        <xsl:value-of select="./../@Name"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="./@Id"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </td>
                            <xsl:for-each select="1 to count($temp/group)">
                                <xsl:variable name="number" select="."/>
                                <xsl:choose>
                                    <xsl:when
                                        test="$commElement/Description/Key[@Name = $temp/group[$number]]/text()">
                                        <td>
                                            <xsl:value-of
                                                select="$commElement/Description/Key[@Name = $temp/group[$number]]/text()"
                                            />
                                        </td>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <td/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:for-each>
                            <xsl:if test="$PARENT = 'Speaker'">
                                <td>
                                    <xsl:value-of select="$commElement/Pseudo/text()"/>
                                </td>
                                <td>
                                    <xsl:value-of select="$commElement/Sex/text()"/>
                                </td>
                            </xsl:if>
                        </tr>
                    </xsl:for-each>
                </tbody>
            </table>
        </xsl:if>
    </xsl:template>

    <xsl:template name="GENERATE_STYLES">
        <style type="text/css">
            body{
                font-family: calibri, helvetica, sans-serif;
                font-size: 10pt;
            }
            th{
                border: 1px solid silver;
                font-size: 9pt;
                background-color: silver;
            }
            th.info{
                border: 1px solid silver;
                font-size: 9pt;
                background-color: #ADD8E6;
                max-width: 75px;
            }
            td{
                border: 1px solid silver;
                font-size: 9pt;
                vertical-align: top;
            }
            td.firstcolumn{
                border: 1px solid silver;
                font-size: 9pt;
                vertical-align: top;
                background-color: #e6e6e6;
            }
            td.info{
                border: 1px solid silver;
                font-size: 9pt;
                vertical-align: top;
                background-color: #ebf5f9;
            }
            h1{
                font-size: 11pt;
                padding: 3px;
                max-width: 500px;
            }</style>
    </xsl:template>

</xsl:stylesheet>
