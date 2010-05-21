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
package org.xwiki.xml.internal;

import java.lang.reflect.Method;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.xml.XMLReaderFactory;

/**
 * Verifies if Xerces is on the classpath and if so configures it to cache DTD grammars instead of reparsing 
 * it for every documents, thus greatly improving performances since most XML content handled in XWiki is
 * XHTML using the XHTML DTD. 
 *  
 * @version $Id$
 * @since 1.7.1
 */
@Component
public class DefaultXMLReaderFactory implements XMLReaderFactory, Initializable
{
    /**
     * We cache the Xerces Grammar Pool (when Xerces has been discovered in the classpath) so that we can
     * reuse it across creation of XML Readers. Note that don't cache the XML Reader itself since it can
     * contain chained XML Filters which are usually not stateless. 
     */
    private Object xercesGrammarPool;

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */    
    public void initialize() throws InitializationException
    {
        try {
            this.xercesGrammarPool = Class.forName("org.apache.xerces.util.XMLGrammarPoolImpl").newInstance();
        } catch (Exception e) {
            // There's no Xerces JAR in the classpath, don't do grammar caching for Xerces.
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see XMLReaderFactory#createXMLReader()
     */    
    public XMLReader createXMLReader() throws SAXException, ParserConfigurationException
    {
        XMLReader xmlReader;
        
        // Try to optimize speed by caching the DTD parsing for Xerces
        // (i.e. if Xerces is available on the classpath).
        try {
            // See http://xerces.apache.org/xerces2-j/faq-grammars.html#faq-1
            Object xercesConfiguration = 
                Class.forName("org.apache.xerces.parsers.XML11NonValidatingConfiguration").newInstance();
            Method setPropertyMethod = xercesConfiguration.getClass().getMethod(
                "setProperty", String.class, Object.class);
            setPropertyMethod.invoke(xercesConfiguration, "http://apache.org/xml/properties/internal/grammar-pool", 
                this.xercesGrammarPool);
            xmlReader = (XMLReader) Class.forName("org.apache.xerces.parsers.SAXParser").getConstructor(
                Class.forName("org.apache.xerces.xni.parser.XMLParserConfiguration")).newInstance(xercesConfiguration);
        } catch (Exception e) {
            // There's no Xerces JAR in the classpath, don't do grammar caching for Xerces.
            // Default to standard SAX parsing which will be slower.
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            SAXParser parser = parserFactory.newSAXParser();
            xmlReader = parser.getXMLReader();
        }
        
        return xmlReader;
    }
}
