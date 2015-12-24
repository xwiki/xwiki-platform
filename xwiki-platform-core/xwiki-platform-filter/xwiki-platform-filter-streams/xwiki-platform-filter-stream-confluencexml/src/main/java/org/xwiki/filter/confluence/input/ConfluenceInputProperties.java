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
package org.xwiki.filter.confluence.input;

import java.util.Locale;

import org.xwiki.filter.DefaultFilterStreamProperties;
import org.xwiki.filter.input.InputSource;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyMandatory;
import org.xwiki.properties.annotation.PropertyName;

/**
 * Confluence XMLL input properties.
 * 
 * @version $Id$
 * @since 6.2M1
 */
public class ConfluenceInputProperties extends DefaultFilterStreamProperties
{
    /**
     * @see #getSource()
     */
    private InputSource source;

    /**
     * @see #getDefaultLocale()
     */
    private Locale defaultLocale;

    /**
     * @see #isConvertToXWiki()
     */
    private boolean convertToXWiki = true;

    /**
     * @see #getSpacePageName()
     */
    private String spacePageName = "WebHome";

    /**
     * @return The source to load the wiki from
     */
    @PropertyName("The source")
    @PropertyDescription("The source to load the wiki from")
    @PropertyMandatory
    public InputSource getSource()
    {
        return this.source;
    }

    /**
     * @param source The source to load the wiki from
     */
    public void setSource(InputSource source)
    {
        this.source = source;
    }

    /**
     * @return The locale of the documents
     */
    @PropertyName("Default locale")
    @PropertyDescription("The locale of the documents")
    public Locale getDefaultLocale()
    {
        return defaultLocale;
    }

    /**
     * @param defaultLocale The locale of the documents
     */
    public void setDefaultLocale(Locale defaultLocale)
    {
        this.defaultLocale = defaultLocale;
    }

    /**
     * @return if true, convert various Confluence standards to XWiki standard (the name of the admin group, etc.)
     */
    @PropertyName("XWiki conversion")
    @PropertyDescription("Convert various Confluence standards to XWiki standard (the name of the admin group, etc.)")
    public boolean isConvertToXWiki()
    {
        return this.convertToXWiki;
    }

    /**
     * @param convertToXWiki if true, convert various Confluence standards to XWiki standard (the name of the admin
     *            group, etc.)
     */
    public void setConvertToXWiki(boolean convertToXWiki)
    {
        this.convertToXWiki = convertToXWiki;
    }

    /**
     * @return The name to use for space home page
     */
    @PropertyName("Space home page")
    @PropertyDescription("The name to use for space home page")
    public String getSpacePageName()
    {
        return this.spacePageName;
    }

    /**
     * @param spacePageName The name to use for space home page
     */
    public void setSpacePageName(String spacePageName)
    {
        this.spacePageName = spacePageName;
    }
}
