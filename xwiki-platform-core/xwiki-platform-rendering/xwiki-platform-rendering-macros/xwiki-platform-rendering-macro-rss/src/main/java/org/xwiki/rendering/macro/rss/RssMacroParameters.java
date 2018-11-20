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
package org.xwiki.rendering.macro.rss;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyMandatory;
import org.xwiki.rendering.macro.box.BoxMacroParameters;
import org.xwiki.rendering.macro.parameter.MacroParameterException;

/**
 * Parameters for the {@link org.xwiki.rendering.internal.macro.rss.RssMacro} Macro.
 * 
 * @version $Id$
 * @since 1.8RC1
 */
public class RssMacroParameters extends BoxMacroParameters
{
    /**
     * The URL of the RSS feed.
     */
    private String feed;

    /**
     * If "true" displays the content of each feed in addition to the feed item link.
     */
    private boolean content;

    /**
     * The number of feed items to display.
     */
    private int count = 10;

    /**
     * If "true" and if the feed has an image, display it.
     */
    private boolean image;

    /**
     * The width of the enclosing box containing the RSS macro output.
     */
    private String width = StringUtils.EMPTY;

    /**
     * @see #setDecoration(boolean)
     */
    private boolean decoration = true;

    /**
     * The RSS feed URL.
     */
    private URL feedURL;

    /**
     * @see #getEncoding()
     */
    private String encoding;

    /**
     * @return the RSS feed URL.
     */
    public String getFeed()
    {
        return feed;
    }

    /**
     * @param feed the RSS feed URL.
     * @throws MacroParameterException if the feed URL is malformed.
     */
    @PropertyMandatory
    @PropertyDescription("URL of the RSS feed")
    public void setFeed(String feed) throws MacroParameterException
    {
        this.feed = feed;
        try {
            this.feedURL = new java.net.URL(feed);
        } catch (MalformedURLException ex) {
            throw new MacroParameterException("Malformed feed URL", ex);
        }
    }

    /**
     * @param image whether to display the feed's image.
     */
    @PropertyDescription("If the feeds has an image associated, display it?")
    public void setImage(boolean image)
    {
        this.image = image;
    }

    /**
     * @return whether to display the feed's image.
     */
    public boolean isImage()
    {
        return image;
    }

    /**
     * @param width the width of the RSS box, that will dismiss potential CSS rules defining its default value.
     */
    @PropertyDescription("The width, in px or %, of the box containing the RSS output (default is 30%)")
    public void setWidth(String width)
    {
        this.width = width;
    }

    /**
     * @return the width of the RSS box, that will dismiss potential CSS rules defining its default value.
     */
    public String getWidth()
    {
        return this.width;
    }

    /**
     * @param count the number of feed items to display.
     */
    @PropertyDescription("The maximum number of feed items to display on the page.")
    public void setCount(int count)
    {
        this.count = count;
    }

    /**
     * @return the number of feed items to display.
     */
    public int getCount()
    {
        return count;
    }

    /**
     * @return the feed's URL
     */
    public URL getFeedURL()
    {
        return feedURL;
    }

    /**
     * @param content if "true" displays the content of each feed in addition to the feed item link
     */
    @PropertyDescription("Display content for feed entries")
    public void setContent(boolean content)
    {
        this.content = content;
    }

    /**
     * @return true if the content of each feed should be displayed
     */
    public boolean isContent()
    {
        return this.content;
    }

    /**
     * @param decoration if "true" displays UI decorations around feed and feed entries (RSS feed icon, feed items in
     *        boxes, etc).
     */
    @PropertyDescription("Display UI decorations around feed and feed entries")
    public void setDecoration(boolean decoration)
    {
        this.decoration = decoration;
    }

    /**
     * @return true if UI decorations should be displayed
     */
    public boolean isDecoration()
    {
        return this.decoration;
    }

    /**
     * @param encoding the encoding to use when reading the RSS Feed
     */
    @PropertyDescription("The encoding to use when reading the RSS Feed (guessed by default).")
    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    /**
     * @return the encoding to use when reading the RSS Feed. If not specified then it's guessed from a variety
     *         of places (XML header, BOM, XML Prolog, etc). In general this parameter shouldn't
     *         be used.
     */
    public String getEncoding()
    {
        return this.encoding;
    }
}
