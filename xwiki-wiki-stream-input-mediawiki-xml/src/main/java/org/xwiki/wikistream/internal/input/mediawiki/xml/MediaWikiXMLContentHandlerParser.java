/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.wikistream.internal.input.mediawiki.xml;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xwiki.component.annotation.Component;
import org.xwiki.wikistream.input.ContentHandlerParser;
import org.xwiki.wikistream.listener.Listener;
import org.xwiki.wikistream.type.WikiStreamType;

/**
 * 
 * @version $Id$
 */
@Component
@Named("mediawiki/xml/contenthandler")
@Singleton
public class MediaWikiXMLContentHandlerParser extends DefaultHandler implements ContentHandlerParser
{
    private Listener listener;


    public WikiStreamType getType()
    {
        return WikiStreamType.MEDIAWIKI_XML;
    }


    public void setListener(Listener listener)
    {
        this.listener=listener;
    }


    public void startDocument() throws SAXException
    {
        // TODO Auto-generated method stub
        System.out.println("Start document");
    }


    public void endDocument() throws SAXException
    {
        // TODO Auto-generated method stub
        System.out.println("end document");
    }


    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        // TODO Auto-generated method stub
        super.startElement(uri, localName, qName, attributes);
        System.out.println(qName);
        
        if(qName.equalsIgnoreCase("page")){
            this.listener.beginDocument(null);
        }
        
        if(qName.equalsIgnoreCase("title")){
            this.listener.onTitle(qName);
        }
    }


    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        // TODO Auto-generated method stub
        super.endElement(uri, localName, qName);
        System.out.println(qName);
        
        if(qName.equalsIgnoreCase("page")){
            this.listener.endDocument(null);
        }
    }

    

}
