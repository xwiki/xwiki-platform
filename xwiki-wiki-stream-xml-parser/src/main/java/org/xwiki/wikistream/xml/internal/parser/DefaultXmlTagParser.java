package org.xwiki.wikistream.xml.internal.parser;

import java.lang.reflect.Method;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xwiki.wikistream.xml.parser.AbstractXmlTagParser;

public class DefaultXmlTagParser extends AbstractXmlTagParser
{
    public DefaultXmlTagParser(Object listener){
        this.setListener(listener);
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        String parentElement = !this.currentElement.isEmpty() ? this.currentElement.peek() : null;
        
        if(listenerClassMethod.containsKey("start"+qName)){
            String methodName=listenerClassMethod.get("start"+qName);
            try {
                Method method=listener.getClass().getDeclaredMethod(methodName, null);
                method.invoke(listener);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
    }
    

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        // TODO Auto-generated method stub
        super.endElement(uri, localName, qName);
    }


}
