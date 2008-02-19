package com.xpn.xwiki.tool.xar;

import java.io.File;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * Parse XWiki document from xml.
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
     * Parse xml file to extract documents informations.
     * 
     * @param file the xml file.
     * @throws DocumentException error when parsing xml file.
     */
    public void fromXML(File file) throws DocumentException
    {
        SAXReader reader = new SAXReader();
        Document domdoc = reader.read(file);

        Element docel = domdoc.getRootElement();

        Element name = docel.element("name");
        if (name != null) {
            this.name = name.getText();
        }

        Element space = docel.element("web");
        if (space != null) {
            this.space = space.getText();
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
     * @return the full name of the document.
     */
    public String getFullName()
    {
        return this.space == null ? this.name : this.space + "." + this.name;
    }
}
