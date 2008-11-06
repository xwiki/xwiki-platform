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
package com.xpn.xwiki.tool.xar;

import java.io.File;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * Parse XWiki document from XML.
 * 
 * @version $Id: $
 */
public class XWikiDocument
{
    /**
     * The name of the document.
     */
    private String name;

    /**
     * The space of the document.
     */
    private String space;

    /**
     * The language of the document.
     */
    private String language;

    /**
     * The default language of the document.
     */
    private String defaultLanguage;

    /**
     * Parse xml file to extract documents informations.
     * 
     * @param file the xml file.
     * @throws DocumentException error when parsing XML file.
     */
    public void fromXML(File file) throws DocumentException
    {
        SAXReader reader = new SAXReader();
        Document domdoc = reader.read(file);

        Element docel = domdoc.getRootElement();

        Element elementName = docel.element("name");
        if (elementName != null) {
            this.name = elementName.getText();
        }

        Element elementSpace = docel.element("web");
        if (elementSpace != null) {
            this.space = elementSpace.getText();
        }

        Element elementLanguage = docel.element("language");
        if (elementLanguage != null) {
            this.language = elementLanguage.getText();
        }

        Element elementDefaultLanguage = docel.element("defaultLanguage");
        if (elementDefaultLanguage != null) {
            this.defaultLanguage = elementDefaultLanguage.getText();
        }
    }

    /**
     * @return the name of the document.
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @param name the name of the document.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the space of the document.
     */
    public String getSpace()
    {
        return this.space;
    }

    /**
     * @param space the space of the document.
     */
    public void setSpace(String space)
    {
        this.space = space;
    }

    /**
     * @return the language of the document.
     */
    public String getLanguage()
    {
        return this.language;
    }

    /**
     * @param language the language of the document.
     */
    public void setLanguage(String language)
    {
        this.language = language;
    }

    /**
     * @return the default language of the document.
     */
    public String getDefaultLanguage()
    {
        return this.defaultLanguage;
    }

    /**
     * @param defaultLanguage the default language of the document.
     */
    public void setDefaultLanguage(String defaultLanguage)
    {
        this.defaultLanguage = defaultLanguage;
    }

    /**
     * @return the full name of the document.
     */
    public String getFullName()
    {
        return this.space == null ? this.name : this.space + "." + this.name;
    }

    /**
     * @param file the file containing the document.
     * @return the full name of the document or null, if the document is invalid
     */
    public static String getFullName(File file)
    {
        XWikiDocument doc = null;
        try {
            doc = new XWikiDocument();
            doc.fromXML(file);
        } catch (Exception e) {
            return null;
        }

        return doc.getFullName();
    }
}
