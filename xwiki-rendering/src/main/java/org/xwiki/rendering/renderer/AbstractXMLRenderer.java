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
package org.xwiki.rendering.renderer;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.dom4j.Element;
import org.dom4j.io.HTMLWriter;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.DefaultElement;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xwiki.rendering.renderer.AbstractPrintRenderer;
import org.xwiki.rendering.renderer.WikiPrinter;

/**
 * Base toolkit class for all xml based renderers.
 * 
 * @version $Id$
 */
public abstract class AbstractXMLRenderer extends AbstractPrintRenderer
{
    protected WikiWriter wikiWriter;

    protected XMLWriter xmlWriter;

    /**
     * @param printer the object to which to write the XHTML output to
     * @param documentAccessBridge see {@link #documentAccessBridge}
     */
    public AbstractXMLRenderer(WikiPrinter printer)
    {
        super(printer);

        this.wikiWriter = new WikiWriter(printer);

        try {
            this.xmlWriter = createNewXMLWriter(this.wikiWriter);
        } catch (Exception e) {
            this.xmlWriter = createDefaultXMLWriter(this.wikiWriter);
        }
    }

    /**
     * Override to be able to use a different {@link XMLWriter} (like {@link HTMLWriter}, etc.).
     */
    protected XMLWriter createNewXMLWriter(Writer writer) throws Exception
    {
        return createDefaultXMLWriter(writer);
    }

    private XMLWriter createDefaultXMLWriter(Writer writer)
    {
        return new XMLWriter(writer);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.AbstractPrintRenderer#setPrinter(org.xwiki.rendering.renderer.WikiPrinter)
     */
    @Override
    protected void setPrinter(WikiPrinter printer)
    {
        super.setPrinter(printer);

        this.wikiWriter.setWikiPrinter(printer);
    }

    /**
     * Print provided text. Takes care of xml escaping.
     */
    protected void printXML(String str)
    {
        try {
            this.xmlWriter.write(str);
        } catch (IOException e) {
            // TODO: add error log here
        }
    }

    /**
     * Print the xml element. In the form <name/>.
     */
    protected void printXMLElement(String name)
    {
        printXMLElement(name, (String[][]) null);
    }

    /**
     * Print the xml element. In the form <name att1="value1" att2="value2"/>.
     */
    protected void printXMLElement(String name, String[][] attributes)
    {
        Element element = new DefaultElement(name);

        if (attributes != null && attributes.length > 0) {
            for (String[] entry : attributes) {
                element.addAttribute(entry[0], entry[1]);
            }
        }

        try {
            this.xmlWriter.write(element);
        } catch (IOException e) {
            // TODO: add error log here
        }
    }

    /**
     * Print the xml element. In the form <name att1="value1" att2="value2"/>.
     */
    protected void printXMLElement(String name, Map<String, String> attributes)
    {
        Element element = new DefaultElement(name);

        if (attributes != null && !attributes.isEmpty()) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                element.addAttribute(entry.getKey(), entry.getValue());
            }
        }

        try {
            this.xmlWriter.write(element);
        } catch (IOException e) {
            // TODO: add error log here
        }
    }

    /**
     * Print the start tag of xml element. In the form <name>.
     */
    protected void printXMLStartElement(String name)
    {
        printXMLStartElement(name, new AttributesImpl());
    }

    /**
     * Print the start tag of xml element. In the form <name att1="value1" att2="value2">.
     */
    protected void printXMLStartElement(String name, String[][] attributes)
    {
        printXMLStartElement(name, createAttributes(attributes));
    }

    /**
     * Print the start tag of xml element. In the form <name att1="value1" att2="value2">.
     */
    protected void printXMLStartElement(String name, Map<String, String> attributes)
    {
        printXMLStartElement(name, createAttributes(attributes));
    }

    /**
     * Print the start tag of xml element. In the form <name att1="value1" att2="value2">.
     */
    protected void printXMLStartElement(String name, Attributes attributes)
    {
        try {
            this.xmlWriter.startElement("", name, name, attributes);
        } catch (SAXException e) {
            // TODO: add error log here
        }
    }

    /**
     * Print the end tag of xml element. In the form </name>.
     */
    protected void printXMLEndElement(String name)
    {
        try {
            this.xmlWriter.endElement("", name, name);
        } catch (SAXException e) {
            // TODO: add error log here
        }
    }

    /**
     * Convert provided table into {@link Attributes} to use in xml writer.
     */
    protected Attributes createAttributes(String[][] parameters)
    {
        AttributesImpl attributes = new AttributesImpl();

        if (parameters != null && parameters.length > 0) {
            for (String[] entry : parameters) {
                attributes.addAttribute(null, null, entry[0], null, entry[1]);
            }
        }

        return attributes;
    }

    /**
     * Convert provided map into {@link Attributes} to use in xml writer.
     */
    protected Attributes createAttributes(Map<String, String> parameters)
    {
        AttributesImpl attributes = new AttributesImpl();

        if (parameters != null && !parameters.isEmpty()) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                attributes.addAttribute(null, null, entry.getKey(), null, entry.getValue());
            }
        }

        return attributes;
    }
}
