package de.uni_hamburg.corpora.validation.quest

import de.uni_hamburg.corpora.validation.Checker
import de.uni_hamburg.corpora.{Corpus, CorpusData, CorpusFunction, EXMARaLDATranscriptionData, Report}

import java.{lang, util}
import java.util.{Collections, Properties}

class ScalaCorpusFunctionTest(properties: Properties) extends Checker (false,properties) with CorpusFunction {

  override def getDescription: String = "Test corpus function written in Scala"

  override def getIsUsableFor: util.Collection[Class[? <: CorpusData]] = {
    Collections.singleton(classOf[EXMARaLDATranscriptionData])
  }

  override def function(c: Corpus, fix: lang.Boolean): Report = {
    var report = new Report
    return report
  }

  override def function(cd: CorpusData, fix: lang.Boolean): Report = {
    var report = new Report
    return report
  }
}