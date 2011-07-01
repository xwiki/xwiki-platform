package org.xwiki.wikistream.xml.parser;

import org.xml.sax.helpers.DefaultHandler;
import org.xwiki.wikistream.xml.listener.XmlListener;

public class ContentHandlerStreamParser extends DefaultHandler
{
    public XmlListener listener;
    
    
    
    
    
    
    
    /**
     * 
     * @param listener
     */
    public void setXmlListener(XmlListener listener){
        this.listener=listener;
    }

}
