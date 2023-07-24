<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:saxon="http://saxon.sf.net/" xmlns:string="http://www.corpora.uni-hamburg.de/ns/string" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" exclude-result-prefixes="#all" version="2.0">
  <xsl:import href="https://corpora.uni-hamburg.de:8443/fedora/get/hzsk%3Aconfig/general-functions-variables.xsl"></xsl:import>
  <xsl:output method="xhtml" omit-xml-declaration="yes"></xsl:output>
  <xsl:key match="*:Communication" name="com-by-id" use="@Id"></xsl:key>
  <xsl:key match="lang/@name" name="language-name-by-code" use="../@code"></xsl:key>
  <xsl:key match="*:Speaker" name="speaker-by-id" use="@Id"></xsl:key>
  <xsl:key match="@type" name="mime-type-by-file-extension" use="../@extension"></xsl:key>
  <xsl:param as="xs:string" name="identifier" required="no"></xsl:param>
  <xsl:variable name="COMA-ROOT" select="/"></xsl:variable>
  <xsl:variable name="CONFIG-PARAMS" select="document(&apos;https://corpora.uni-hamburg.de:8443/fedora/get/hzsk:config/config-params.xml&apos;)"></xsl:variable>
  <xsl:variable as="xs:string" name="corpus-identifier" select="(/*:results/@for, $identifier)[1]"></xsl:variable>
  <xsl:variable as="xs:string" name="ORIG_CORPUS_NAMESPACE" select="substring-after($corpus-identifier, &apos;spoken-corpus:&apos;)"></xsl:variable>
  <xsl:variable as="xs:string" name="CORPUS_NAMESPACE" select="if(starts-with($ORIG_CORPUS_NAMESPACE, &apos;fadac&apos;)) then &apos;fadac&apos; else $ORIG_CORPUS_NAMESPACE"></xsl:variable>
  <xsl:variable name="CONFIG-THIS-CORPUS" select="$CONFIG-PARAMS//*:resource[@fedora-identifier = $corpus-identifier]"></xsl:variable>
  <xsl:template match="/">
    <xsl:call-template name="coma2html"></xsl:call-template>
  </xsl:template>
  <xsl:template name="coma2html">
    <xsl:choose>
      <xsl:when test="exists($CONFIG-THIS-CORPUS//*:coma2htmlDisplayFacet)">
        <xsl:choose>
          <xsl:when test="$CONFIG-THIS-CORPUS//*:coma2htmlDisplayFacet = &apos;languages&apos;">
            <xsl:call-template name="facet-languages-coma2html"></xsl:call-template>
          </xsl:when>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="com-boxes">
          <xsl:with-param name="communications" select="descendant::*:Communication"></xsl:with-param>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="com-boxes">
    <xsl:param as="element()*" name="communications"></xsl:param>
    <xsl:for-each select="$communications">
      <xsl:sort select="@Name"></xsl:sort>
      <xsl:variable as="element()*" name="SPEAKERS" select="key(&apos;speaker-by-id&apos;, *:Setting/*:Person)"></xsl:variable>
      <xsl:variable as="element()*" name="RECORDINGS" select="*:Recording"></xsl:variable>
      <xsl:variable as="element()*" name="TRANSCRIPTIONS" select="*:Transcription[ends-with(*:Filename, &apos;.exs&apos;)]"></xsl:variable>
      <xsl:variable as="element()*" name="ADDITIONAL-FILES" select="*:AsocFile | *:File"></xsl:variable>
      <xsl:variable as="element()*" name="LANGUAGES" select="*:Language[empty(@Type) or @Type=&apos;Communication&apos;]"></xsl:variable>
      <xsl:variable as="element()?" name="LOCATION" select="*:Location[exists(@Type) and @Type=&apos;Communication&apos;]"></xsl:variable>
      <xsl:variable name="LANGUAGE-NAMES">
        <xsl:for-each select="distinct-values($LANGUAGES//*:LanguageCode)">
          <lang code="{.}" name="{unparsed-text(concat(&apos;https://corpora.uni-hamburg.de:8443/lang-utils/name?iso-639-3=&apos;, ., &apos;&amp;lang=eng&apos;))}"></lang>
        </xsl:for-each>
      </xsl:variable>
      <xsl:variable as="xs:string" name="THIS_COMMUNICATION_NAME" select="concat(                         @Name,                         (: if there are more than one communication with this name, then a suffix is returned :)                         concat(&apos; &apos;, (key(&apos;com-by-id&apos;, @Id, $COMA-ROOT)/count(preceding-sibling::*:Communication[lower-case(@Name) = lower-case(current()/@Name)]) + 1))[exists(($COMA-ROOT//*:Communication[lower-case(@Name) = lower-case(current()/@Name)])[2])]                         )"></xsl:variable>
      <xsl:variable as="xs:string" name="PID_SUFFIX" select="string:ID-conform($THIS_COMMUNICATION_NAME)"></xsl:variable>
      <div class="previewBox communication" style="font-size:0.9em">
        <h5>
          <a class="showmore" href="#com-{$PID_SUFFIX}">
            <xsl:value-of select="@Name"></xsl:value-of>
          </a> 
                    (<xsl:value-of select="concat(count($SPEAKERS), &apos; Speaker&apos;, &apos;s&apos;[$SPEAKERS[2]])[$SPEAKERS[1]]"></xsl:value-of>
          <xsl:value-of select="concat(&apos;, &apos;, count($RECORDINGS), &apos; Recording&apos;, &apos;s&apos;[$RECORDINGS[2]])[$RECORDINGS[1]]"></xsl:value-of>
          <xsl:value-of select="concat(&apos;, &apos;, count($TRANSCRIPTIONS), &apos; Transcription&apos;, &apos;s&apos;[$TRANSCRIPTIONS[2]])[$TRANSCRIPTIONS[1]]"></xsl:value-of>
          <xsl:value-of select="concat(&apos;, &apos;, count($ADDITIONAL-FILES), &apos; Additional file&apos;, &apos;s&apos;[$ADDITIONAL-FILES[2]])[$ADDITIONAL-FILES[1]]"></xsl:value-of>)
                </h5>
        <xsl:choose>
          <xsl:when test="exists(*:Description/*:Key[@Name=&apos;Background information&apos;][not(matches(., &apos;^\s*$&apos;))])">
            <p>
              <xsl:value-of select="*:Description/*:Key[@Name=&apos;Background information&apos;]"></xsl:value-of>
            </p>
          </xsl:when>
          <xsl:when test="starts-with($CORPUS_NAMESPACE, &apos;fadac&apos;)">
            <p>
              <xsl:value-of select="concat(*:Description/*:Key[@Name=&apos;Informant&apos;], &apos; is interviewed in &apos;, *:Description/*:Key[@Name=&apos;Language in interview&apos;], &apos; by &apos;, *:Description/*:Key[@Name=&apos;Interviewer&apos;],                              &apos; (&apos;, *:Description/*:Key[@Name=&apos;Time&apos;], &apos;)&apos;)"></xsl:value-of>
            </p>
          </xsl:when>
        </xsl:choose>
        <div class="com-details" id="com-{$PID_SUFFIX}" style="display:none;">
          <xsl:for-each-group group-by="true()" select="*:Description/*:Key[not(@Name=&apos;Background information&apos;)]">
            <table class="corpus-overview">
              <xsl:for-each select="current-group()[exists(text()) and not(matches(text(), &apos;^\s*$&apos;))]">
                <tr>
                  <td>
                    <xsl:value-of select="@Name"></xsl:value-of>
                  </td>
                  <td>
                    <xsl:value-of select="text()"></xsl:value-of>
                  </td>
                </tr>
              </xsl:for-each>
              <xsl:if test="exists($SPEAKERS[1])">
                <tr>
                  <td>Speakers</td>
                  <td>
                    <xsl:for-each select="$SPEAKERS">
                      <xsl:value-of select="*:Sigle"></xsl:value-of>
                      <xsl:if test="not(position()=last())">, </xsl:if>
                    </xsl:for-each>
                  </td>
                </tr>
              </xsl:if>
              <xsl:if test="exists($LANGUAGES[1])">
                <tr>
                  <td>Language<xsl:value-of select="&apos;s&apos;[$LANGUAGES[2]]"></xsl:value-of>
                  </td>
                  <td>
                    <xsl:for-each select="$LANGUAGES">
                      <xsl:sort select="key(&apos;language-name-by-code&apos;, *:LanguageCode, $LANGUAGE-NAMES)"></xsl:sort>
                      <xsl:variable as="xs:boolean" name="isNotLast" select="position() != last()"></xsl:variable>
                      <xsl:variable as="xs:string?" name="language-code" select="if(exists(*:LanguageCode[not(matches(., &apos;^\s*$&apos;))])) then lower-case(*:LanguageCode) else ()"></xsl:variable>
                      <xsl:value-of select="concat(key(&apos;language-name-by-code&apos;, $language-code, $LANGUAGE-NAMES), &apos; (&apos;, $language-code, &apos;)&apos;, &apos;, &apos;[$isNotLast])"></xsl:value-of>
                      <a href="https://www.ethnologue.com/language/{$language-code}" target="_blank" title="Information about {key(&apos;language-name-by-code&apos;, $language-code, $LANGUAGE-NAMES)} at www.ethnologue.com">
                        <img alt="Icon of ethnologue.com" src="https://corpora.uni-hamburg.de/hzsk/sites/default/files/images/icons/ethnologue-icon-small.png" width="14"></img>
                      </a>
                    </xsl:for-each>
                  </td>
                </tr>
              </xsl:if>
              <xsl:for-each select="$LOCATION/substring-before(*:Period/*:PeriodStart, &apos;T&apos;)">
                <tr>
                  <td>Date</td>
                  <td>
                    <xsl:value-of select="."></xsl:value-of>
                  </td>
                </tr>
              </xsl:for-each>
              <xsl:for-each select="$LOCATION/string-join((*:Street, string-join((*:PostalCode, *:City), &apos; &apos;)[not(.=&apos;&apos;)], *:Country), &apos;, &apos;)[not(.=&apos;&apos;)]">
                <tr>
                  <td>Location</td>        
          <td>
                    <xsl:value-of select="."></xsl:value-of>
                    <a href="https://www.openstreetmap.org/search?query={.}" target="_blank" title="Search for this place on https://www.openstreetmap.com">
                      <img alt="Icon of Open Street Map" src="https://corpora.uni-hamburg.de/hzsk/sites/default/files/images/icons/openstreetmaps-icon-small.png" width="14"></img>
                    </a>
                    <a href="https://www.google.de/maps/?q={.}" target="_blank" title="Search for this place on https://www.google.de/maps/">
                      <img alt="Icon of Google Maps" src="https://corpora.uni-hamburg.de/hzsk/sites/default/files/images/icons/googlemaps-icon-small.png" width="14"></img>
                    </a>
                  </td>
                </tr>
              </xsl:for-each>
              <xsl:if test="exists($TRANSCRIPTIONS[1])">
                <tr>
                  <td>Views</td>
                  <td>
                    <xsl:for-each select="$TRANSCRIPTIONS">
                      <xsl:variable as="xs:integer" name="COUNT_PRECEDING_TRANSCRIPTIONS_WITH_SAME_NAME" select="count(preceding::*:Transcription[ends-with(*:Filename, &apos;.exs&apos;) and (lower-case(*:Name) = lower-case(current()/*:Name))])"></xsl:variable>
                      <xsl:variable as="xs:string" name="TRANSCRIPTION_NAME" select="concat(                                                 (substring-before(*:Name, &apos;_s.exs&apos;)[.!=&apos;&apos;], substring-before(*:Name, &apos;.exs&apos;)[.!=&apos;&apos;], *:Name)[1],                                                 concat(&apos; &apos;, ($COUNT_PRECEDING_TRANSCRIPTIONS_WITH_SAME_NAME + 1))[$COUNT_PRECEDING_TRANSCRIPTIONS_WITH_SAME_NAME &gt; 0]                                                   )"></xsl:variable>
                      <xsl:variable as="xs:string" name="PID_SUFFIX" select="string:ID-conform($TRANSCRIPTION_NAME)"></xsl:variable>
                      <xsl:variable as="xs:string" name="TRANSCRIPTION_IDENTIFIER" select="concat($CORPUS_NAMESPACE, &apos;_&apos;, $PID_SUFFIX)"></xsl:variable>
                      <xsl:if test="$TRANSCRIPTIONS[2]">
                        <xsl:value-of select="concat(*:Name, &apos;: &apos;)"></xsl:value-of>
                      </xsl:if>
                      <xsl:if test="$CORPUS_NAMESPACE=(&apos;demo&apos;, &apos;hamatac&apos;, &apos;eurowiss-0.1&apos;, &apos;cosi&apos;, &apos;alcebla&apos;, &apos;dik&apos;, &apos;hamcopolig&apos;, &apos;nslc-0.1&apos;, &apos;comindat&apos;, &apos;skandsemikoradio-0.7&apos;, &apos;wd-0.1&apos;)">
                        <a class="overlayLink" data-overlay-left-right-margin="5%" data-overlay-top-bottom-margin="10%" data-role="{$CORPUS_NAMESPACE}-transcript-{$PID_SUFFIX}" href="https://corpora.uni-hamburg.de/hzsk/de/islandora/object/transcript:{$TRANSCRIPTION_IDENTIFIER}/datastream/SCORE">Score</a>,
                                                <a class="overlayLink" data-overlay-left-right-margin="5%" data-overlay-top-bottom-margin="10%" data-role="{$CORPUS_NAMESPACE}-transcript-{$PID_SUFFIX}" href="https://corpora.uni-hamburg.de/hzsk/de/islandora/object/transcript:{$TRANSCRIPTION_IDENTIFIER}/datastream/LIST">List</a>,
                                            </xsl:if>
                      <xsl:for-each select="&apos;BT2ColumnHTML&apos;">
                        <a class="convertExb" data-role="{$CORPUS_NAMESPACE}-transcript-{$PID_SUFFIX}" href="https://corpora.uni-hamburg.de/hzsk/de/islandora/object/transcript:{$TRANSCRIPTION_IDENTIFIER}/datastream/EXB?to={.}">
                          <xsl:value-of select="substring-after(., &apos;BT2&apos;)"></xsl:value-of>
                        </a>
                        <xsl:if test="position() != last()">, </xsl:if>
                      </xsl:for-each>
                      <xsl:if test="position()!=last()">
                        <br></br>
                      </xsl:if>
                    </xsl:for-each>
                  </td>
                </tr>
                <tr>
                  <td>Transcription formats</td>
                  <td>
                    <xsl:for-each select="$TRANSCRIPTIONS">
                      <xsl:variable as="xs:integer" name="COUNT_PRECEDING_TRANSCRIPTIONS_WITH_SAME_NAME" select="count(preceding::*:Transcription[ends-with(*:Filename, &apos;.exs&apos;) and (lower-case(*:Name) = lower-case(current()/*:Name))])"></xsl:variable>
                      <xsl:variable as="xs:string" name="TRANSCRIPTION_NAME" select="concat(                                                 (substring-before(*:Name, &apos;_s.exs&apos;)[.!=&apos;&apos;], substring-before(*:Name, &apos;.exs&apos;)[.!=&apos;&apos;], *:Name)[1],                                                 concat(&apos; &apos;, ($COUNT_PRECEDING_TRANSCRIPTIONS_WITH_SAME_NAME + 1))[$COUNT_PRECEDING_TRANSCRIPTIONS_WITH_SAME_NAME &gt; 0]                                                   )"></xsl:variable>
                      <xsl:variable as="xs:string" name="PID_SUFFIX" select="string:ID-conform($TRANSCRIPTION_NAME)"></xsl:variable>
                      <xsl:variable as="xs:string" name="TRANSCRIPTION_IDENTIFIER" select="concat($CORPUS_NAMESPACE, &apos;_&apos;, $PID_SUFFIX)"></xsl:variable>
                      <xsl:if test="$TRANSCRIPTIONS[2]">
                        <xsl:value-of select="concat(*:Name, &apos;: &apos;)"></xsl:value-of>
                      </xsl:if>
                      <a data-role="{$CORPUS_NAMESPACE}-transcript-{$PID_SUFFIX}" download="{$TRANSCRIPTION_IDENTIFIER}.exb" href="https://corpora.uni-hamburg.de/hzsk/de/islandora/object/transcript:{$TRANSCRIPTION_IDENTIFIER}/datastream/EXB/{$TRANSCRIPTION_IDENTIFIER}.exb" title="EXMARaLDA Basic Transcription">EXB</a>, 
                                            <a data-role="{$CORPUS_NAMESPACE}-transcript-{$PID_SUFFIX}" download="{$TRANSCRIPTION_IDENTIFIER}.exs" href="https://corpora.uni-hamburg.de/hzsk/de/islandora/object/transcript:{$TRANSCRIPTION_IDENTIFIER}/datastream/EXS/{$TRANSCRIPTION_IDENTIFIER}.exs" title="EXMARaLDA Segmented Transcription">EXS</a>, 
                                            <xsl:for-each select="&apos;EAF&apos;, &apos;FOLKER&apos;, &apos;PRAAT&apos;, &apos;TEI&apos;">
                        <a class="convertExb promptDownload" data-role="{$CORPUS_NAMESPACE}-transcript-{$PID_SUFFIX}" href="https://corpora.uni-hamburg.de/hzsk/de/islandora/object/transcript:{$TRANSCRIPTION_IDENTIFIER}/datastream/EXB?to={.}" title="{(&apos;ELAN annotation tool format&apos;[current()=&apos;EAF&apos;], &apos;FOLKER transcription tool format&apos;[current()=&apos;FOLKER&apos;], &apos;Praat format&apos;[current()=&apos;PRAAT&apos;], &apos;Text Encoding Initiative format&apos;[current()=&apos;TEI&apos;])[1]}">
                          <xsl:value-of select="."></xsl:value-of>
                        </a>
                        <xsl:if test="position() != last()">, </xsl:if>
                      </xsl:for-each>
                      <xsl:if test="position()!=last()">
                        <br></br>
                      </xsl:if>
                    </xsl:for-each>
                  </td>
                </tr>
              </xsl:if>
              <xsl:if test="exists($RECORDINGS[1])">
                <tr>
                  <td>Recordings</td>
                  <td>
                    <xsl:for-each select="$RECORDINGS">
                      <xsl:variable as="xs:integer" name="COUNT_PRECEDING_RECORDINGS_SAME_NAME" select="count(preceding::*:Recording[lower-case(*:Name) = lower-case(current()/*:Name)])"></xsl:variable>
                      <xsl:variable as="xs:string" name="THIS_RECORDING_NAME" select="concat(*:Name, concat(&apos; &apos;, $COUNT_PRECEDING_RECORDINGS_SAME_NAME)[$COUNT_PRECEDING_RECORDINGS_SAME_NAME!=0])"></xsl:variable>
                      <xsl:variable as="xs:string" name="PID_SUFFIX" select="string:ID-conform($THIS_RECORDING_NAME)"></xsl:variable>
                      <xsl:variable as="xs:string" name="RECORDING_IDENTIFIER" select="concat($CORPUS_NAMESPACE, &apos;_&apos;, $PID_SUFFIX)"></xsl:variable>
                      <xsl:variable as="xs:string*" name="MEDIA_TYPES" select="distinct-values(for $mime in key(&apos;mime-type-by-file-extension&apos;, *:Media/*:NSLink/lower-case(tokenize(., &apos;\.&apos;)[last()]), $MIMETYPES) return tokenize($mime, &apos;/&apos;)[1])"></xsl:variable>
                      <xsl:if test="$RECORDINGS[2]">
                        <xsl:value-of select="concat(*:Name, &apos;: &apos;[empty($MEDIA_TYPES)])"></xsl:value-of>   
                     <xsl:if test="exists($MEDIA_TYPES[1])">
                          <xsl:value-of select="concat(&apos; (&apos; , string-join($MEDIA_TYPES, &apos;, &apos;), &apos;): &apos;)"></xsl:value-of>
                        </xsl:if>
                      </xsl:if>
                      <xsl:for-each select="*:Media/*:NSLink">
                        <xsl:variable as="xs:string" name="DS_NAME" select="upper-case(tokenize(., &apos;\.&apos;)[last()])"></xsl:variable>
                        <a data-role="{$CORPUS_NAMESPACE}-recording-{$PID_SUFFIX}" href="https://corpora.uni-hamburg.de/hzsk/de/islandora/object/recording:{$RECORDING_IDENTIFIER}/datastream/{$DS_NAME}/{$PID_SUFFIX}.{lower-case($DS_NAME)}">
                          <xsl:if test="not($DS_NAME = (&apos;MP3&apos;,&apos;AVI&apos;))">
                            <xsl:attribute name="class" select="&apos;overlayLink&apos;"></xsl:attribute>
                          </xsl:if>
                          <xsl:value-of select="$DS_NAME"></xsl:value-of>
                        </a>
                        <xsl:if test="position() != last()">, </xsl:if>
                      </xsl:for-each>
                      <xsl:if test="position()!=last()">
                                                |
                                            </xsl:if>
                    </xsl:for-each>
                  </td>
                </tr>
              </xsl:if>
              <xsl:if test="exists($ADDITIONAL-FILES[1])">
                <tr>
                  <td>Additional files</td>
                  <td>
                    <xsl:for-each select="$ADDITIONAL-FILES/(self::*:File|*:File)">
                      <xsl:variable as="xs:integer" name="COUNT_FOLLOWING_FILES_WITH_SAME_NAME" select="count(following::*:File[string:strip-file-extension(lower-case((../self::*:AsocFile/*:Name, *:filename)[1])) = string:strip-file-extension(lower-case(current()/(../self::*:AsocFile/*:Name, *:filename)[1]))]) "></xsl:variable>
                      <xsl:variable as="xs:integer" name="COUNT_PRECEDING_FILES_WITH_SAME_NAME" select="count(preceding::*:File[string:strip-file-extension(lower-case((../self::*:AsocFile/*:Name, *:filename)[1])) = string:strip-file-extension(lower-case(current()/(../self::*:AsocFile/*:Name, *:filename)[1]))]) "></xsl:variable>
                      <xsl:variable as="xs:integer?" name="FILE_NUMBER" select="($COUNT_PRECEDING_FILES_WITH_SAME_NAME + 1)[($COUNT_PRECEDING_FILES_WITH_SAME_NAME + $COUNT_FOLLOWING_FILES_WITH_SAME_NAME) &gt; 0]"></xsl:variable>
                      <xsl:variable as="xs:string" name="FILE_NAME" select="concat(string:strip-file-extension((../self::*:AsocFile/*:Name, *:filename)[1]), concat(&apos; &apos;, $FILE_NUMBER)[exists($FILE_NUMBER)])"></xsl:variable>
                      <xsl:variable as="xs:string" name="PID_SUFFIX" select="string:ID-conform($FILE_NAME)"></xsl:variable>
                      <xsl:variable as="xs:string" name="FILE_IDENTIFIER" select="concat(&apos;file:&apos;, $CORPUS_NAMESPACE, &apos;_&apos;, $PID_SUFFIX)"></xsl:variable>
                      <xsl:variable as="xs:string" name="DS_NAME" select="upper-case(tokenize((../self::*:AsocFile/*:Name, *:filename)[1], &apos;\.&apos;)[last()])"></xsl:variable>
                      <a data-role="{$CORPUS_NAMESPACE}-file-{$PID_SUFFIX}" href="/hzsk/de/islandora/object/{$FILE_IDENTIFIER}/datastream/{$DS_NAME}/{$PID_SUFFIX}.{lower-case($DS_NAME)}">
                        <xsl:if test="not($DS_NAME = (&apos;MP3&apos;,&apos;AVI&apos;))">
                          <xsl:attribute name="class" select="&apos;overlayLink&apos;"></xsl:attribute>
                        </xsl:if>
                        <xsl:value-of select="(../self::*:AsocFile/*:Name, *:filename)[1]"></xsl:value-of>
                        <xsl:for-each select="(*:mimetype[not(matches(., &apos;\s*(unknown)?\s*&apos;))], lower-case($DS_NAME)[.!=&apos;&apos;])[1]"> (type: <xsl:value-of select="."></xsl:value-of>)</xsl:for-each>
                      </a>
                      <xsl:if test="position()!=last()">
                                                |
                                            </xsl:if>
                    </xsl:for-each>
                  </td>
                </tr>
              </xsl:if>
              <tr>
                <td>Metadata</td>
                <td>
                  <a class="overlayLink" href="https://corpora.uni-hamburg.de/repository/communication:{$CORPUS_NAMESPACE}_{$PID_SUFFIX}/CMDI/">CMDI</a>
                  <a download="communication-{$CORPUS_NAMESPACE}-{$PID_SUFFIX}.cmdi" href="https://corpora.uni-hamburg.de/hzsk/de/islandora/object/communication:{$CORPUS_NAMESPACE}_{$PID_SUFFIX}/datastream/CMDI/communication-{$CORPUS_NAMESPACE}-{$PID_SUFFIX}.cmdi">CMDI</a>
                </td>
              </tr>
            </table>
          </xsl:for-each-group>
        </div>
      </div>
    </xsl:for-each>
  </xsl:template>
  <xsl:template name="facet-languages-coma2html">
    <xsl:for-each-group group-by="string-join(string:az-sort(*:Language[empty(@Type) or @Type=&apos;Communication&apos;]/*:LanguageCode), &apos;#&apos;)" select="descendant::*:Communication">
      <xsl:sort order="descending" select="count(current-group())"></xsl:sort>
      <xsl:sort order="descending" select="count(tokenize(current-grouping-key(), &apos;#&apos;))"></xsl:sort>
      <xsl:sort select="current-grouping-key()"></xsl:sort>
      <h4>
        <xsl:for-each select="tokenize(current-grouping-key(), &apos;#&apos;)">
          <img height="20" src="/hzsk/sites/default/files/images/flags/{.}-flag-small.png" style="margin-right:5px;" width="20"></img>
        </xsl:for-each>
        <xsl:value-of select="string-join((for $l in tokenize(current-grouping-key(), &apos;#&apos;) return unparsed-text(concat(&apos;https://corpora.uni-hamburg.de:8443/lang-utils/name?iso-639-3=&apos;, $l, &apos;&amp;lang=eng&apos;))), &apos; / &apos;)"></xsl:value-of>
      </h4>
      <xsl:comment select="$CORPUS_NAMESPACE, tokenize(current-grouping-key(), &apos;#&apos;)"></xsl:comment>
      <xsl:call-template name="com-boxes">
        <xsl:with-param name="communications" select="current-group()"></xsl:with-param>
      </xsl:call-template>
    </xsl:for-each-group>
  </xsl:template>
  <xsl:template match="text()"></xsl:template>
</xsl:stylesheet>
