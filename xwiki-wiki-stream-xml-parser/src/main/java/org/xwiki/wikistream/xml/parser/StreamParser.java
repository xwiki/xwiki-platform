package org.xwiki.wikistream.xml.parser;

import java.util.Map;

import org.xwiki.wikistream.xml.listener.XmlListener;
import org.xwiki.wikistream.xml.param.XmlParameter;

public interface StreamParser
{

    void parse(Map<String,XmlParameter> paramMap,XmlListener listener);
   
}
