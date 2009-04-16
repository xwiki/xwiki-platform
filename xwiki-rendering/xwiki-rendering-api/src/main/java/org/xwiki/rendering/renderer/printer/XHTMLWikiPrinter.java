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

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;

/**
 * Base toolkit class for all XHTML-based renderers. This printer handles whitespaces so that it prints "&nbsp;" when
 * needed (i.e. when the spaces are at the beginning or at the end of an element's content or when there are more than 1
 * contiguous spaces, except for CDATA sections and inside PRE elements. It also knows how to handle XHTML comments).
 * 
 * @version $Id$
 * @since 1.7M1
 */
public class XHTMLWikiPrinter extends XMLWikiPrinter
{
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
        super(printer);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.printer.XMLWikiPrinter#printXML(java.lang.String)
     */
    @Override
    public void printXML(String str)
    {
        handleSpaceWhenInText();
        super.printXML(str);
        this.hasTextBeenPrinted = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.printer.XMLWikiPrinter#printXMLElement(java.lang.String)
     */
    @Override
    public void printXMLElement(String name)
    {
        handleSpaceWhenStartElement();
        super.printXMLElement(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.printer.XMLWikiPrinter#printXMLElement(java.lang.String, java.lang.String[][])
     */
    @Override
    public void printXMLElement(String name, String[][] attributes)
    {
        handleSpaceWhenStartElement();
        super.printXMLElement(name, attributes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.printer.XMLWikiPrinter#printXMLElement(java.lang.String, java.util.Map)
     */
    @Override
    public void printXMLElement(String name, Map<String, String> attributes)
    {
        handleSpaceWhenStartElement();
        super.printXMLElement(name, attributes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.printer.XMLWikiPrinter#printXMLStartElement(java.lang.String)
     */
    @Override
    public void printXMLStartElement(String name)
    {
        handleSpaceWhenStartElement();
        super.printXMLStartElement(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.printer.XMLWikiPrinter#printXMLStartElement(java.lang.String,
     *      java.lang.String[][])
     */
    @Override
    public void printXMLStartElement(String name, String[][] attributes)
    {
        handleSpaceWhenStartElement();
        super.printXMLStartElement(name, attributes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.printer.XMLWikiPrinter#printXMLStartElement(java.lang.String, java.util.Map)
     */
    @Override
    public void printXMLStartElement(String name, Map<String, String> attributes)
    {
        handleSpaceWhenStartElement();
        super.printXMLStartElement(name, attributes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.printer.XMLWikiPrinter#printXMLStartElement(java.lang.String,
     *      org.xml.sax.Attributes)
     */
    @Override
    public void printXMLStartElement(String name, Attributes attributes)
    {
        handleSpaceWhenStartElement();
        super.printXMLStartElement(name, attributes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.printer.XMLWikiPrinter#printXMLEndElement(java.lang.String)
     */
    @Override
    public void printXMLEndElement(String name)
    {
        handleSpaceWhenEndlement();
        super.printXMLEndElement(name);
        this.elementEnded = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.printer.XMLWikiPrinter#printXMLComment(java.lang.String)
     */
    @Override
    public void printXMLComment(String content)
    {
        printXMLComment(content, false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.printer.XMLWikiPrinter#printXMLComment(java.lang.String, boolean)
     */
    @Override
    public void printXMLComment(String content, boolean escape)
    {
        handleSpaceWhenStartElement();
        super.printXMLComment(content, escape);
        this.elementEnded = true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.printer.XMLWikiPrinter#printXMLStartCData()
     */
    @Override
    public void printXMLStartCData()
    {
        handleSpaceWhenStartElement();
        super.printXMLStartCData();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.renderer.printer.XMLWikiPrinter#printXMLEndCData()
     */
    @Override
    public void printXMLEndCData()
    {
        handleSpaceWhenEndlement();
        super.printXMLEndCData();
    }

    /**
     * This method should be used to print a space rather than calling <code>printXML(" ")</code>.
     */
    public void printSpace()
    {
        this.spaceCount++;
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
        // Use case: <tag1>something <tag2>...
        // Use case: <tag1>something <!--...
        if (this.spaceCount > 0) {
            if (!this.isInCData && !this.isInPreserveElement) {
                // The first space is a normal space
                super.printXML(" ");
                for (int i = 0; i < this.spaceCount - 1; i++) {
                    printEntity("&nbsp;");
                }
            } else {
                super.printXML(StringUtils.repeat(" ", this.spaceCount));
            }
        }
        this.spaceCount = 0;
        this.elementEnded = false;
        this.hasTextBeenPrinted = false;
    }

    private void handleSpaceWhenEndlement()
    {
        // Use case: <tag1>something </tag1>...
        // All spaces are &nbsp; spaces since otherwise they'll be all stripped by browsers
        if (!this.isInCData && !this.isInPreserveElement) {
            for (int i = 0; i < this.spaceCount; i++) {
                printEntity("&nbsp;");
            }
        } else {
            super.printXML(StringUtils.repeat(" ", this.spaceCount));
        }
        this.spaceCount = 0;
        this.elementEnded = false;
        this.hasTextBeenPrinted = false;
    }
}
