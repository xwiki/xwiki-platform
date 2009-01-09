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
package org.xwiki.rendering.renderer.printer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.codehaus.plexus.util.StringUtils;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.DefaultComment;
import org.dom4j.tree.DefaultElement;
import org.dom4j.tree.DefaultEntity;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xwiki.rendering.internal.renderer.printer.WikiWriter;
import org.xwiki.rendering.internal.renderer.printer.XHTMLWriter;

/**
 * Base toolkit class for all XHTML-based renderers. This printer handles whitespaces so that it prints "&nbsp;"
 * when needed (i.e. when the spaces are at the beginning or at the end of an element's content or when there are
 * more than 1 contiguous spaces, except for CDATA sections and inside PRE elements. It also knows how to handle
 * XHTML comments).
 * 
 * @version $Id$
 * @since 1.7M1
 */
public class XHTMLWikiPrinter
{
    protected WikiWriter wikiWriter;

    protected XMLWriter xmlWriter;

    private int spaceCount = 0;
    
    private boolean isInCData;
    
    private boolean isInPreserveElement;
    
    private boolean elementEnded;
    
    private boolean hasTextBeenPrinted;

    /**
     * @param printer the object to which to write the XHTML output to
     */
    public XHTMLWikiPrinter(WikiPrinter printer)
    {
        this.wikiWriter = new WikiWriter(printer);

        try {
            this.xmlWriter = new XHTMLWriter(this.wikiWriter);
        } catch (UnsupportedEncodingException e) {
            // TODO: add error log "should not append"
        }
    }

    public XMLWriter getXMLWriter()
    {
        return this.xmlWriter;
    }

    public void setWikiPrinter(WikiPrinter printer)
    {
        this.wikiWriter.setWikiPrinter(printer);
    }

    /**
     * Print provided text. Takes care of xml escaping.
     */
    public void printXML(String str)
    {
        handleSpaceWhenInText();
        printXMLInternal(str);
        this.hasTextBeenPrinted = true;
    }

    /**
     * Print the xml element. In the form <name/>.
     */
    public void printXMLElement(String name)
    {
        handleSpaceWhenStartElement();
        printXMLElement(name, (String[][]) null);
    }

    /**
     * Print the xml element. In the form <name att1="value1" att2="value2"/>.
     */
    public void printXMLElement(String name, String[][] attributes)
    {
        handleSpaceWhenStartElement();
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
    public void printXMLElement(String name, Map<String, String> attributes)
    {
        handleSpaceWhenStartElement();
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
     * Print the start tag of xml element. In the form &lt;name&gt;.
     */
    public void printXMLStartElement(String name)
    {
        handleSpaceWhenStartElement();
        printXMLStartElement(name, new AttributesImpl());
    }

    /**
     * Print the start tag of xml element. In the form &lt;name att1="value1" att2="value2"&gt;.
     */
    public void printXMLStartElement(String name, String[][] attributes)
    {
        handleSpaceWhenStartElement();
        printXMLStartElement(name, createAttributes(attributes));
    }

    /**
     * Print the start tag of xml element. In the form &lt;name att1="value1" att2="value2"&gt;.
     */
    public void printXMLStartElement(String name, Map<String, String> attributes)
    {
        handleSpaceWhenStartElement();
        printXMLStartElement(name, createAttributes(attributes));
    }

    /**
     * Print the start tag of xml element. In the form &lt;name att1="value1" att2="value2"&gt;.
     */
    public void printXMLStartElement(String name, Attributes attributes)
    {
        handleSpaceWhenStartElement();
        try {
            this.xmlWriter.startElement("", name, name, attributes);
        } catch (SAXException e) {
            // TODO: add error log here
        }
    }

    /**
     * Print the end tag of xml element. In the form &lt;/name&gt;.
     */
    public void printXMLEndElement(String name)
    {
        handleSpaceWhenEndlement();
        try {
            this.xmlWriter.endElement("", name, name);
        } catch (SAXException e) {
            // TODO: add error log here
        }
        this.elementEnded = true;
    }

    /**
     * Print a XML comment.
     * 
     * @param content the comment content
     */
    public void printXMLComment(String content)
    {
        handleSpaceWhenStartElement();
        try {
            DefaultComment commentElement = new DefaultComment(content);
            this.xmlWriter.write(commentElement);
        } catch (IOException e) {
            // TODO: add error log here
        }
        this.elementEnded = true;
    }

    /**
     * Start a CDATA section.
     */
    public void printXMLStartCData()
    {
        handleSpaceWhenStartElement();
        try {
            this.xmlWriter.startCDATA();
            // Ensure that characters inside CDATA sections are not escaped
            this.xmlWriter.setEscapeText(false);
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
    
    /**
     * End a CDATA section.
     */
    public void printXMLEndCData()
    {
        handleSpaceWhenEndlement();
        try {
            this.xmlWriter.setEscapeText(true);
            this.xmlWriter.endCDATA();
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    /**
     * This method should be used to print a space rather than calling <code>printXML(" ")</code>. 
     */
    public void printSpace()
    {
        this.spaceCount++;
    }
    
    public void printEntity(String entity)
    {
        try {
            this.xmlWriter.write(new DefaultEntity(entity, entity));
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
    
    /**
     * Convert provided table into {@link Attributes} to use in xml writer.
     */
    private Attributes createAttributes(String[][] parameters)
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
    private Attributes createAttributes(Map<String, String> parameters)
    {
        AttributesImpl attributes = new AttributesImpl();

        if (parameters != null && !parameters.isEmpty()) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                attributes.addAttribute(null, null, entry.getKey(), null, entry.getValue());
            }
        }

        return attributes;
    }
    
    private void printXMLInternal(String str)
    {
        try {
            this.xmlWriter.write(str);
        } catch (IOException e) {
            // TODO: add error log here
        }
    }

    private void handleSpaceWhenInText()
    {
        if (this.elementEnded || this.hasTextBeenPrinted) {
            handleSpaceWhenStartElement();
        } else {
            handleSpaceWhenEndlement();
        }
    }

    private void handleSpaceWhenStartElement()
    {
        // Use case: <tag1>something   <tag2>...
        // Use case: <tag1>something   <!--...
        if (this.spaceCount > 0) {
            if (!this.isInCData && !this.isInPreserveElement) {
                // The first space is a normal space
                printXMLInternal(" ");
                for (int i = 0; i < this.spaceCount - 1; i++) {
                    printEntity("&nbsp;");
                }
            } else {
                printXMLInternal(StringUtils.repeat(" ", this.spaceCount));
            }
        }
        this.spaceCount = 0;
        this.elementEnded = false;
        this.hasTextBeenPrinted = false;
    }
    
    private void handleSpaceWhenEndlement()
    {
        // Use case: <tag1>something   </tag1>...
        // All spaces are &nbsp; spaces since otherwise they'll be all stripped by browsers
        if (!this.isInCData && !this.isInPreserveElement) {
            for (int i = 0; i < this.spaceCount ; i++) {
                printEntity("&nbsp;");
            }
        } else {
            printXMLInternal(StringUtils.repeat(" ", this.spaceCount));
        }
        this.spaceCount = 0;
        this.elementEnded = false;
        this.hasTextBeenPrinted = false;
    }
}
