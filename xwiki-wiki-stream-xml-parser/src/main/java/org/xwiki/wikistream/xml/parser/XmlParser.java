package org.xwiki.wikistream.xml.parser;

import java.io.InputStream;
import java.util.Map;

import org.xwiki.component.annotation.ComponentRole;

@ComponentRole
public interface XmlParser
{
    void parse(InputStream inputStream,Object listener) throws XmlParserException; 
    
    void parse(InputStream inputStream,Map<String,String> parameterMap,Object listener) throws XmlParserException; 
    
}
