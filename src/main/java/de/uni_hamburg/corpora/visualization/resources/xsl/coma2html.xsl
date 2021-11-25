<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes" encoding="UTF-8"/>
	<xsl:template match="/">
		<html>
			<head>
				<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
				<script type="text/javascript">
					<![CDATA[										
						function switchMenu(div,span,forcevisible) {
							var el = document.getElementById(div);
							var spel = document.getElementById(span);
							if (forcevisible == "true") {
									el.style.display = '';
									spel.className = 'tabhead';
							} else {
								if ( el.style.display != "none" ) {
									el.style.display = 'none';
									spel.className = 'notab';
								} else {
									el.style.display = '';
									spel.className = 'tabhead';
								}
							}
						}
						
						
						]]>
				</script>
				<link rel="stylesheet" type="text/css" href="stylesheets/coma2html.css"/>
				<title>
					<xsl:value-of select="Corpus/@Name"/>
				</title>
			</head>
			<body>
				<xsl:apply-templates/>
			</body>
		</html>
	</xsl:template>
	<!--CORPUS-->
	<xsl:template match="Corpus">
		<xsl:call-template name="MAKE_TITLE"/>
		<div id="main">
			<div>[<a><xsl:attribute name="onclick">switchMenu('DIV<xsl:value-of select="@Id"
						/>','null','false');</xsl:attribute>DC/OLAC metadata</a>]</div>
			<div>
				<xsl:attribute name="id">DIV<xsl:value-of select="@Id"/></xsl:attribute>
				<xsl:attribute name="style">display:none</xsl:attribute>
				<xsl:apply-templates select="Description"/>

			</div>
			<div>[<a><xsl:attribute name="onclick">switchMenu('DIVfile<xsl:value-of select="@Id"
			/>','null','false');</xsl:attribute>Corpus materials and files</a>]</div>
			<div>
				<xsl:attribute name="id">DIVfile<xsl:value-of select="@Id"/></xsl:attribute>
				<xsl:attribute name="style">display:none</xsl:attribute>
				<xsl:apply-templates select="AsocFile">
					<xsl:sort select="Name"/>
				</xsl:apply-templates>
			</div>
			<br/>
			<xsl:apply-templates select="CorpusData"/>
			<br/>
		</div>
	</xsl:template>
	<!--CORPUSDATA-->
	<xsl:template match="CorpusData">
		<div class="communications">
			<h4><xsl:value-of select="count(Communication)"/> Communications</h4>
			<xsl:apply-templates
				select="Communication">
				<xsl:sort select="@Name"/>
			</xsl:apply-templates>
		</div>
		<div class="speakers">
			<h4><xsl:value-of select="count(Speaker)"/> Speakers</h4>
			<xsl:apply-templates select="Speaker">
				<xsl:sort select="Sigle"/>
			</xsl:apply-templates>
		</div>
	</xsl:template>
	<!--COMMUNICATION-->
	<xsl:template match="Communication">
		<xsl:param name="code" select="Transcription/Description/Key[@Name='Code']/text()"/>

		<xsl:variable name="transcount">
			<xsl:value-of
				select="count(Transcription[Description/Key[@Name='segmented']/text()='true'])"/>
		</xsl:variable>
		<div>
			<xsl:attribute name="class">notab</xsl:attribute>
			<xsl:attribute name="id">SPAN<xsl:value-of select="@Id"/></xsl:attribute>
			<a>
				<xsl:attribute name="onclick">switchMenu('DIV<xsl:value-of select="@Id"
						/>','SPAN<xsl:value-of select="@Id"/>','false');</xsl:attribute>
				<xsl:attribute name="name">
					<xsl:value-of select="@Id"/>
				</xsl:attribute>
				<b>
					<xsl:value-of select="@Name"/>
				</b>
			</a> (<xsl:value-of select="count(Setting/Person)"/> Speaker<xsl:if
				test="not(count(Setting/Person)=1)">s</xsl:if>, <xsl:value-of
				select="count(Recording)"/> Recording<xsl:if test="not(count(Recording)=1)">s</xsl:if><xsl:choose>

				<xsl:when test="$transcount=1">, 1 Transcription</xsl:when>
				<xsl:when test="$transcount>1">, <xsl:value-of select="$transcount"/>
				Transcriptions</xsl:when>
			</xsl:choose>) </div>
		<div class="tabbody">
			<xsl:attribute name="id">DIV<xsl:value-of select="@Id"/></xsl:attribute>
			<xsl:attribute name="style">display:none;</xsl:attribute>
			<xsl:apply-templates select="Description">
				<xsl:with-param name="code">
					<xsl:value-of select="$code"/>
				</xsl:with-param>
			</xsl:apply-templates>
			<p>
				<xsl:apply-templates select="Setting"/>
				<xsl:if test="string-length(Language[1]/LanguageCode) > 1">
					<b>Language<xsl:if test="count(Language) > 1">s</xsl:if>: </b>
					<xsl:for-each select="Language">
						<a>
							<xsl:attribute name="href"
									>http://www.ethnologue.com/show_language.asp?code=<xsl:value-of
									select="LanguageCode"/></xsl:attribute>
							<xsl:value-of select="LanguageCode"/>
							<xsl:if test="position() != last()">
								<xsl:text>; </xsl:text>
							</xsl:if>
						</a>
					</xsl:for-each>
					<xsl:apply-templates select="Language/Description"/>
				</xsl:if>
			</p>
			<xsl:apply-templates select="Location"/>
			<xsl:apply-templates select="Recording">
				<xsl:with-param name="code">
					<xsl:value-of select="$code"/>
				</xsl:with-param>
			</xsl:apply-templates>
			<xsl:for-each select="Transcription[Description/Key[@Name='segmented']/text()='true']">
				<xsl:sort select="Description/Key[@Name='code']"/>
				<xsl:apply-templates select=".">
					<xsl:with-param name="code">
						<xsl:value-of select="$code"/>
					</xsl:with-param>
				</xsl:apply-templates>
			</xsl:for-each>
			<p><b>Files linked to this communication:</b>
				<xsl:for-each select="File">
					<a>
						<xsl:attribute name="href"><xsl:value-of select="relPath"/></xsl:attribute>
                                                <xsl:choose>
                                                    <xsl:when test="upper-case(substring-after(filename/text(),'.')) != 'XML'">
                                                        <xsl:value-of select="upper-case(substring-after(filename/text(),'.'))"/>
                                                    </xsl:when> 
                                                    <xsl:otherwise>
                                                        <xsl:value-of>TEI</xsl:value-of>
                                                    </xsl:otherwise>
                                                </xsl:choose>
					</a>
					<xsl:if test="position() != last()">
						<xsl:text>; </xsl:text>
					</xsl:if>
				</xsl:for-each>
			</p>
			<xsl:apply-templates select="AsocFile"/>
		</div>
	</xsl:template>
	<!--SPEAKER-->
	<xsl:template match="Speaker">
		<xsl:variable name="spid">
			<xsl:value-of select="@Id"/>
		</xsl:variable>
		<div>
			<xsl:attribute name="class">notab</xsl:attribute>
			<xsl:attribute name="id">SPAN<xsl:value-of select="@Id"/></xsl:attribute>
			<a>
				<xsl:attribute name="onclick">switchMenu('DIV<xsl:value-of select="@Id"
						/>','SPAN<xsl:value-of select="@Id"/>','false');</xsl:attribute>
				<xsl:attribute name="name">
					<xsl:value-of select="@Id"/>
				</xsl:attribute>
				<b><xsl:value-of select="Sigle"/></b><xsl:text> - </xsl:text><xsl:value-of select="Description/Key[@Name='Name']"/> 
			</a>
		</div>
		<div class="tabbody">
			<xsl:attribute name="id">DIV<xsl:value-of select="@Id"/></xsl:attribute>
			<xsl:attribute name="style">display:none;</xsl:attribute>
			<xsl:apply-templates select="Description"/>
			<br/>
			<xsl:apply-templates select="Location[@Type='Birth']"/>
			<xsl:apply-templates select="Location[@Type='Education']">
				<xsl:sort select="Period/PeriodStart"/>
			</xsl:apply-templates>
			<xsl:apply-templates select="Location[@Type='Occupation']">
				<xsl:sort select="Period/PeriodStart"/>
			</xsl:apply-templates>
			<xsl:apply-templates select="Location[@Type='Residence']">
				<xsl:sort select="Period/PeriodStart"/>
			</xsl:apply-templates>
			<xsl:apply-templates select="Location[@Type='Stay']">
				<xsl:sort select="Period/PeriodStart"/>
			</xsl:apply-templates>
			<xsl:apply-templates select="Language">
				<xsl:sort select="string-length(@Type)" data-type="number" order="descending"/>
				<xsl:sort select="number(substring-after(@Type, 'L'))" data-type="number"/>
			</xsl:apply-templates>
			<xsl:apply-templates select="AsocFile">
				<xsl:sort select="Name"/>
			</xsl:apply-templates>
			<p>
				<b>In Communications: </b>
				<xsl:for-each select="//Communication[Setting/Person/text()=$spid]">
					<a>
						<xsl:attribute name="onclick">switchMenu('DIV<xsl:value-of select="@Id"
								/>','SPAN<xsl:value-of select="@Id"/>','true');</xsl:attribute>
						<xsl:attribute name="href">#<xsl:value-of select="@Id"/></xsl:attribute>
						<xsl:value-of select="@Name"/>
					</a>
					<xsl:if test="position() != last()">
						<xsl:text>; </xsl:text>
					</xsl:if>
				</xsl:for-each>
			</p>
		</div>
	</xsl:template>


	<!--LANGUAGE-->
	<xsl:template match="Language">
		<xsl:if test="string-length(LanguageCode)>0"/>
		<div class="lang">
			<xsl:choose>
				<xsl:when test="@Type and not(@Type='Communication')">
					<b>
						<xsl:value-of select="@Type"/>
						<xsl:text> : </xsl:text>
						<a>
							<xsl:attribute name="href"
									>http://www.ethnologue.com/show_language.asp?code=<xsl:value-of
									select="LanguageCode"/></xsl:attribute>
							<xsl:value-of select="LanguageCode"/>
						</a>
					</b>
				</xsl:when>
				<xsl:otherwise>
					<b>Language : <a>
							<xsl:attribute name="href"
									>http://www.ethnologue.com/show_language.asp?code=<xsl:value-of
									select="LanguageCode"/></xsl:attribute>
							<xsl:value-of select="LanguageCode"/>
						</a></b>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:apply-templates select="Description"/>
		</div>
	</xsl:template>
	<!--SETTING-->
	<xsl:template match="Setting">

		<b>
			<xsl:text>Speaker</xsl:text>
			<xsl:if test="count(Person) > 1">
				<xsl:text>s</xsl:text>
			</xsl:if>
			<xsl:text>: </xsl:text>
		</b>
		<xsl:for-each select="Person">
			<a>
				<xsl:attribute name="onclick">switchMenu('DIV<xsl:value-of select="text()"
						/>','SPAN<xsl:value-of select="text()"/>','true');</xsl:attribute>
				<xsl:variable name="pid">
					<xsl:value-of select="text()"/>
				</xsl:variable>
				<xsl:variable name="spk">
					<xsl:value-of select="//Speaker[@Id=$pid]/Sigle/text()"/>
				</xsl:variable>
				<xsl:variable name="info">
					<xsl:value-of select="//Speaker[@Id=$pid]/Sigle/text()"/> (<xsl:for-each
						select="//Speaker[@Id=$pid]/Language">
						<xsl:value-of select="LanguageCode/text()"/>
						<xsl:if test="position() != last()">
							<xsl:text>, </xsl:text>
						</xsl:if>
					</xsl:for-each>) </xsl:variable>
				<xsl:attribute name="title">
					<xsl:value-of select="$info"/>
				</xsl:attribute>
				<xsl:attribute name="href">#<xsl:value-of select="text()"/></xsl:attribute>
				<xsl:value-of select="$spk"/>
			</a>
			<xsl:if test="position() != last()">
				<xsl:text>; </xsl:text>
			</xsl:if>
		</xsl:for-each>

		<br/>
	</xsl:template>
	<!--TRANSCRIPTION-->
	<xsl:template match="Transcription">
		<xsl:variable name="trCode">
			<xsl:value-of select="substring-before(NSLink, concat('/',Filename))"/>
		</xsl:variable>
		<xsl:variable name="trCode0">
			<xsl:value-of select="substring-before(Filename, '_s')"/>
		</xsl:variable>
		<div class="trans">
			<b>
				<xsl:text>Transcription: </xsl:text>
			</b>
			<br/>
			<!--<xsl:apply-templates select="Description"/>-->
			<xsl:text>EXMARaLDA: [</xsl:text>
			<a>
				<xsl:attribute name="href">
					<xsl:value-of select="$trCode"/>
					<xsl:text>/</xsl:text>
					<xsl:value-of select="$trCode0"/>
					<xsl:text>.exb</xsl:text>
				</xsl:attribute>
				<xsl:attribute name="target">_self</xsl:attribute>
				<xsl:attribute name="title">EXMARaLDA Basic-Transcription</xsl:attribute>
				<xsl:text>Transcription</xsl:text>
			</a>
			<xsl:text>] [</xsl:text>
			<a>
				<xsl:attribute name="href">
					<xsl:value-of select="NSLink"/>
				</xsl:attribute>
				<xsl:attribute name="target">_self</xsl:attribute>
				<xsl:attribute name="title">EXMARaLDA Segmented-Transcription</xsl:attribute>
				<xsl:text>Segmented</xsl:text>
			</a>
			<xsl:text>] </xsl:text>
		</div>
	</xsl:template>
	<!-- RECORDING -->
	<xsl:template match="Recording">
		<xsl:variable name="durms">
			<xsl:value-of select="RecordingDuration[1]"/>
		</xsl:variable>
		<div class="rec">
			<b>Recording </b>
			<table>
				<tr>
					<td align="left">
						<xsl:text>Recording name</xsl:text>
					</td>
					<td style="background:#ffffff; opacity:0.7">
						<xsl:value-of select="Name"/>
					</td>
				</tr>
				<tr>
					<td align="left">
						<xsl:text>Duration</xsl:text>
					</td>
					<td style="background:#ffffff; opacity:0.7">
						<xsl:value-of select="format-number(($durms div 60) div 1000, '#.#')"/>
						<xsl:text> minutes</xsl:text>
					</td>
				</tr>
				<xsl:apply-templates select="Description"/>
			</table>
			<xsl:apply-templates select="Media"/>
		</div>
	</xsl:template>
	<!-- CORPUS ASOC FILE -->
	<xsl:template match="Corpus/AsocFile">
		<div class="corpusfile">
			<a>
				<xsl:attribute name="href">
					<xsl:value-of select="File/relPath"/>
				</xsl:attribute>
				<xsl:attribute name="target">_self</xsl:attribute>
				<xsl:attribute name="title">
					<xsl:value-of select="File/mimetype"/>
				</xsl:attribute>
				<xsl:value-of select="Name"/>
			</a>
			<xsl:apply-templates select="Description"/>
		</div>
	</xsl:template>
	<!-- ASOC FILE -->
	<xsl:template match="AsocFile">
		<div class="file">
			<a>
				<xsl:attribute name="href">
					<xsl:value-of select="File/relPath"/>
				</xsl:attribute>
				<xsl:attribute name="target">_self</xsl:attribute>
				<xsl:attribute name="title">
					<xsl:value-of select="File/mimetype"/>
				</xsl:attribute>
				<xsl:value-of select="Name"/>
			</a>
			<xsl:apply-templates select="Description"/>
		</div>
	</xsl:template>
	<!--MEDIA-->
	<xsl:template match="Media">
		<xsl:value-of select="substring-after(NSLink, '.')"/> file: [<a>
			<xsl:attribute name="href">
				<xsl:value-of select="NSLink"/>
			</xsl:attribute>
			<xsl:value-of select="../Name"/>
			<xsl:if test="not(contains(../Name, '.'))">
				<xsl:text>.</xsl:text>
				<xsl:value-of select="substring-after(NSLink, '.')"/>
			</xsl:if>
		</a>]<xsl:if test="position() != last()">
			<br/>
		</xsl:if>
	</xsl:template>

	<!--COMMUNICATION LOCATION-->
	<xsl:template match="Communication/Location">
		<div class="loc">
			<b>Location</b>
			<xsl:if test="Description/Key[@Name='Name']/text()">
				<b>
					<xsl:text>: </xsl:text>
					<xsl:value-of select="Description/Key[@Name='Name']/text()"/>
				</b>
			</xsl:if>
			<table>
				<xsl:if test="Period/PeriodStart">
					<xsl:apply-templates select="Period"/>
				</xsl:if>
				<xsl:if test="string-length(Country) > 0">
					<tr>
						<td align="left">
							<xsl:text>Country</xsl:text>
						</td>
						<td style="background:#ffffff; opacity:0.7">
							<xsl:value-of select="Country"/>
						</td>
					</tr>
					<xsl:if test="string-length(City) > 0">
						<tr>
							<td align="left">
								<xsl:text>City</xsl:text>
							</td>
							<td style="background:#ffffff; opacity:0.7">
								<xsl:value-of select="City"/>
								<xsl:if test="string-length(Street) > 0">
									<xsl:text> (</xsl:text>
									<xsl:value-of select="Street"/>
									<xsl:text>)</xsl:text>
								</xsl:if>
							</td>
						</tr>
					</xsl:if>
				</xsl:if>
				<xsl:apply-templates select="Description"/>
			</table>
		</div>
	</xsl:template>


	<!--BIRTH LOCATION-->
	<xsl:template match="Location[@Type='Birth']">
		<div class="bloc">
			<b>
				<xsl:value-of select="@Type"/>
			</b>
			<xsl:choose>
				<xsl:when test="string-length(Country) > 0 or string-length(Period/PeriodStart) > 0">
					<table>
						<xsl:if test="Period/PeriodStart">
							<xsl:apply-templates select="Period"/>
						</xsl:if>
						<xsl:if test="string-length(Country) > 0">
							<tr>
								<td align="left">
									<xsl:text>Country</xsl:text>
								</td>
								<td style="background:#ffffff; opacity:0.7">
									<xsl:value-of select="Country"/>
								</td>
							</tr>
							<xsl:if test="string-length(City) > 0">
								<tr>
									<td align="left">
										<xsl:text>City</xsl:text>
									</td>
									<td style="background:#ffffff; opacity:0.7">
										<xsl:value-of select="City"/>
										<xsl:if test="string-length(Street) > 0">
											<xsl:text> (</xsl:text>
											<xsl:value-of select="Street"/>
											<xsl:text>)</xsl:text>
										</xsl:if>
									</td>
								</tr>
							</xsl:if>
						</xsl:if>
						<xsl:apply-templates select="Description"/>

					</table>
				</xsl:when>
				<xsl:when test="Description/Key[@Name='Precision']">
					<table>
						<tr>
							<td align="left">
								<xsl:text>Date</xsl:text>
							</td>
							<td style="background:#ffffff; opacity:0.7">
								<xsl:text> (</xsl:text>
								<xsl:value-of select="Description/Key[@Name='Precision']/text()"/>
								<xsl:text>)</xsl:text>
							</td>
						</tr>
					</table>
				</xsl:when>
			</xsl:choose>
		</div>
	</xsl:template>


	<!-- EDUCATION/OCCUPATION/RESIDENCE/STAY LOCATION-->
	<xsl:template
		match="Location[@Type='Education' or @Type='Occupation' or @Type='Residence' or @Type='Stay']">
		<div>
			<xsl:choose>
				<xsl:when test="@Type='Education'">
					<xsl:attribute name="class">eduloc</xsl:attribute>
				</xsl:when>
				<xsl:when test="@Type='Occupation'">
					<xsl:attribute name="class">occloc</xsl:attribute>
				</xsl:when>
				<xsl:when test="@Type='Residence'">
					<xsl:attribute name="class">resloc</xsl:attribute>
				</xsl:when>
				<xsl:when test="@Type='Stay'">
					<xsl:attribute name="class">staloc</xsl:attribute>
				</xsl:when>
			</xsl:choose>
			<b>
				<xsl:value-of select="@Type"/>
			</b>
			<xsl:if test="Description/Key[@Name='Name']/text()">
				<b>
					<xsl:text>: </xsl:text>
					<xsl:value-of select="Description/Key[@Name='Name']/text()"/>
				</b>
			</xsl:if>
			<xsl:if
				test="string-length(Country) > 0 or string-length(Period/PeriodStart) > 0 or (Description/Key[not(@Name='Name')] and Description/Key[not(@Name='Precision')])">
				<table>
					<xsl:if test="string-length(Country) > 0">
						<tr>
							<td align="left">
								<xsl:text>Country</xsl:text>
							</td>
							<td style="background:#ffffff; opacity:0.7">
								<xsl:value-of select="Country"/>
							</td>
						</tr>
						<xsl:if test="string-length(City) > 0">
							<tr>
								<td align="left">
									<xsl:text>City</xsl:text>
								</td>
								<td style="background:#ffffff; opacity:0.7">
									<xsl:value-of select="City"/>
									<xsl:if test="string-length(Street) > 0">
										<xsl:text> (</xsl:text>
										<xsl:value-of select="Street"/>
										<xsl:text>)</xsl:text>
									</xsl:if>
								</td>
							</tr>
						</xsl:if>
					</xsl:if>
					<xsl:if test="string-length(Period/PeriodStart) > 0">
						<xsl:apply-templates select="Period"/>
					</xsl:if>
					<xsl:apply-templates select="Description"/>
				</table>
			</xsl:if>
		</div>
	</xsl:template>


	<!--PERIOD-->
	<xsl:template match="Period">
		<tr>
			<td align="left">
				<xsl:choose>
					<xsl:when test="ancestor::Location[@Type='Birth' or @Type='Communication'] ">
						<xsl:text>Date</xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:text>From</xsl:text>
					</xsl:otherwise>
				</xsl:choose>
			</td>
			<td style="background:#ffffff; opacity:0.7">
				<xsl:value-of select="substring-before(PeriodStart/text(), 'T')"/>
				<xsl:if test="ancestor::Location/Description/Key[@Name='Precision']">

					<xsl:text> (</xsl:text>
					<xsl:value-of
						select="ancestor::Location/Description/Key[@Name='Precision']/text()"/>
					<xsl:text>)</xsl:text>

				</xsl:if>
			</td>
		</tr>
		<xsl:if test="PeriodDuration/text() and not(PeriodDuration/text()='0')">
			<xsl:variable name="dur" select="PeriodDuration"/>
			<xsl:variable name="years" select="floor($dur div 31536000000)"/>
			<tr>
				<td align="left">
					<xsl:text>Duration</xsl:text>
				</td>
				<td style="background:#ffffff; opacity:0.7">
					<xsl:if test="$years > 0">
						<xsl:value-of select="$years"/>
						<xsl:text> years</xsl:text>
					</xsl:if>
					<xsl:if test="$dur mod 31536000000 > 0">
						<xsl:variable name="days"
							select="floor(($dur mod 31536000000) div 86400000)"/>
						<xsl:if test="$days > 0">
							<xsl:if test="$years > 0">
								<xsl:text>, </xsl:text>
							</xsl:if>
							<xsl:value-of select="$days"/>
							<xsl:text> days</xsl:text>
						</xsl:if>
					</xsl:if>
				</td>
			</tr>
		</xsl:if>
	</xsl:template>


	<!--RECORDING DESCRIPTION-->
	<xsl:template match="Recording/Description">
		<xsl:if test="Key">
			<xsl:if test="not(normalize-space(Key[@Name='comment']/text())='')">
				<xsl:for-each select="Key">
					<xsl:sort select="@Name"/>
					<tr>
						<td align="left">
							<xsl:value-of select="@Name"/>
						</td>
						<td style="background:#ffffff; opacity:0.7">
							<xsl:value-of select="text()"/>
						</td>
					</tr>
				</xsl:for-each>
			</xsl:if>
		</xsl:if>
	</xsl:template>
	<!--TRANSCRIPTION DESCRIPTION-->
	<xsl:template match="Transcription/Description">
		<xsl:if test="Key">
			<table>
				<tr>
					<td align="left">
						<xsl:text>Transcription name</xsl:text>
					</td>
					<td style="background:#ffffff; opacity:0.7">
						<xsl:value-of select="Key[@Name='Transcription name']/text()"/>
					</td>
				</tr>
				<xsl:for-each select="Key">
					<xsl:sort select="@Name"/>
					<xsl:if
						test="(not(normalize-space(text())='')) and (not(contains(@Name,'#'))) and (not(contains(@Name,'segmented'))) and (not(contains(@Name,'-name')) and (not(contains(@Name,'Transcription name'))))">
						<tr>
							<td align="left">
								<xsl:value-of select="@Name"/>
							</td>
							<td style="background:#ffffff; opacity:0.7">
								<xsl:value-of select="text()"/>
							</td>
						</tr>
					</xsl:if>
				</xsl:for-each>
			</table>
		</xsl:if>
	</xsl:template>
	<!--LOCATION DESCRIPTION-->
	<xsl:template match="Location/Description">
		<xsl:if test=" Key[not(@Name='Precision') and not(@Name='Name')]">
			<xsl:for-each select="Key[not(@Name='Precision') and not(@Name='Name')]">
				<xsl:sort select="@Name"/>
				<tr>
					<td align="left">
						<xsl:value-of select="@Name"/>
					</td>
					<td style="background:#ffffff; opacity:0.7">
						<xsl:value-of select="text()"/>
					</td>
				</tr>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>
	<!--SPEAKER DESCRIPTION-->
	<xsl:template match="Speaker/Description">
		<xsl:if test="Key">
			<table>
				<tr>
					<td align="left">Sex</td>
					<td style="background:#ffffff; opacity:0.7">
						<xsl:value-of select="../Sex"/>
					</td>
				</tr>
				<xsl:for-each select="Key">
					<xsl:sort select="@Name"/>
					<tr>
						<td align="left">
							<xsl:value-of select="@Name"/>
						</td>
						<td style="background:#ffffff; opacity:0.7">
							<xsl:value-of select="text()"/>
						</td>
					</tr>
				</xsl:for-each>
			</table>
		</xsl:if>
	</xsl:template>
	<!--DEFAULT DESCRIPTION-->
	<xsl:template match="Description">
		<xsl:if test="Key">
			<table>
				<xsl:for-each select="Key">
					<xsl:sort select="@Name"/>
					<tr>
						<td align="left">
							<xsl:value-of select="@Name"/>
						</td>
						<td style="background:#ffffff; opacity:0.7">
							<xsl:value-of select="text()"/>
						</td>
					</tr>
				</xsl:for-each>
			</table>
		</xsl:if>
	</xsl:template>
	<xsl:template name="MAKE_TITLE">
		<div id="head">
			<a href="http://www.uni-hamburg.de/" title="Universität Hamburg">
				<img src="resources/uhh.png" alt="Uni Hamburg" border="0px" height="25px"
					width="25px" style="margin-right:10px;"/>
			</a>
			<span id="corpus-title" title="Corpus overview"> EXMARaLDA Demo Korpus </span>
			<a href="http://www.uni-hamburg.de/fachbereiche-einrichtungen/sfb538/"
				title="SFB 538 &apos;Mehrsprachigkeit&apos;" style="margin-left:10px">
				<img src="resources/sfb538.png" alt="SFB 538 'Mehrsprachigkeit" border="0px"
					height="25px" width="25px"/>
			</a>
		</div>
	</xsl:template>

	<xsl:template name="MAKE_FOOTER">
		<div id="footer"> Demo Korpus by <a href="http://www.exmaralda.org/" title="Projekt Z2"
				>Projekt Z2</a> , powered by <a href="http://www.exmaralda.org/" title="EXMARaLDA"
				>EXMARaLDA</a> . </div>
	</xsl:template>
</xsl:stylesheet>
