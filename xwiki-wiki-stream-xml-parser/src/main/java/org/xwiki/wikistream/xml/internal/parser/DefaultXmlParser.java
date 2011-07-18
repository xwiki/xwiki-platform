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
package org.xwiki.wikistream.xml.internal.parser;

import java.io.InputStream;
import java.util.Map;

import javax.inject.Singleton;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.DefaultHandler;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.wikistream.xml.parser.XmlParser;
import org.xwiki.wikistream.xml.parser.XmlParserException;

/**
 * 
 * @version $Id: 5c213c4c836ba7a506c7fae073a3c2eee28e20be $
 */
@Component
@Singleton
public class DefaultXmlParser implements XmlParser, Initializable
{

    @Override
    public void parse(InputStream inputStream, Object listener) throws XmlParserException
    {
        this.parse(inputStream,null,listener);
        
    }

    @Override
    public void parse(InputStream inputStream, Map<String, String> parameterMap, Object listener)
        throws XmlParserException
    {
        try {
            SAXParser parser=SAXParserFactory.newInstance().newSAXParser();
            DefaultHandler handler=null;

            parser.parse(inputStream, handler);
        } catch (Exception e) {
            throw new XmlParserException("Failed to parse the input source",e);
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        // TODO Auto-generated method stub
        
    }

}
