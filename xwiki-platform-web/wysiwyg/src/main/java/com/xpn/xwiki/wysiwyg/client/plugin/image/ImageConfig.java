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
package com.xpn.xwiki.wysiwyg.client.plugin.image;

import org.xwiki.gwt.dom.client.JavaScriptObject;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Stores the data about an image: information about the image file (filename, url, etc.) and also about its
 * representation in an edited document (position, size, etc.).
 * 
 * @version $Id$
 */
public class ImageConfig implements IsSerializable
{
    /**
     * Enumeration holding all possible values for the image alignment.
     */
    public enum ImageAlignment
    {
        /**
         * The possible alignments for the image: block floated to the left or right or centered, inline aligned on the
         * top, middle or bottom.
         */
        LEFT, CENTER, RIGHT, TOP, MIDDLE, BOTTOM
    }

    /**
     * The URL to the image. This can be either a relative or an absolute URL.
     */
    private String imageURL;

    /**
     * The name of the wiki where this image is located.
     */
    private String wiki;

    /**
     * The name of the space of the document to which this image is attached.
     */
    private String space;

    /**
     * The name of the page where this image is located.
     */
    private String page;

    /**
     * The reference of the image, in the {@code wikiname:spacename.pagename@filename} form. <br />
     * Note: this value should take priority over {@code wiki}, {@code space}, {@code page} and {@code imageFileName}
     * set in this config: if the reference is set, that this one should be used instead of generating another. Ideally,
     * this should be set with the URL so that no further computing is necessary for this image. If this and url are not
     * set, they are to be computed from the {@code wiki}, {@code space}, {@code page}, {@code imageFileName} values.
     */
    private String reference;

    /**
     * The altText (alt string) of this image.
     */
    private String altText;

    /**
     * The width of this image, as a string. It can contain measure unit (pixels, pts) or not.
     */
    private String width;

    /**
     * The height of this image, as a string. It can contain measure unit (pixels, pts) or not.
     */
    private String height;

    /**
     * The alignment of this image: horizontal floated or vertical.
     */
    private ImageAlignment alignment;

    /**
     * Default constructor.
     */
    public ImageConfig()
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasImage#getImageURL()
     */
    public String getImageURL()
    {
        return imageURL;
    }

    /**
     * @param imageURL the imageURL to set
     */
    public void setImageURL(String imageURL)
    {
        this.imageURL = imageURL;
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasImage#getWiki()
     */
    public String getWiki()
    {
        return wiki;
    }

    /**
     * @param wiki the wiki to set
     */
    public void setWiki(String wiki)
    {
        this.wiki = wiki;
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasImage#getSpace()
     */
    public String getSpace()
    {
        return space;
    }

    /**
     * @param space the space to set
     */
    public void setSpace(String space)
    {
        this.space = space;
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasImage#getPage()
     */
    public String getPage()
    {
        return page;
    }

    /**
     * @param page the page to set
     */
    public void setPage(String page)
    {
        this.page = page;
    }

    /**
     * @return the reference
     */
    public String getReference()
    {
        return reference;
    }

    /**
     * @param reference the reference to set
     */
    public void setReference(String reference)
    {
        this.reference = reference;
    }

    /**
     * @return the altText
     */
    public String getAltText()
    {
        return altText;
    }

    /**
     * @param altText the altText to set
     */
    public void setAltText(String altText)
    {
        this.altText = altText;
    }

    /**
     * @return the width
     */
    public String getWidth()
    {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(String width)
    {
        this.width = width;
    }

    /**
     * @return the height
     */
    public String getHeight()
    {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(String height)
    {
        this.height = height;
    }

    /**
     * @return the alignment
     */
    public ImageAlignment getAlignment()
    {
        return alignment;
    }

    /**
     * @param alignment the alignment to set
     */
    public void setAlignment(ImageAlignment alignment)
    {
        this.alignment = alignment;
    }

    /**
     * @return the JSON representation of this ImageConfig
     */
    public String toJSON()
    {
        String jsonString =
            "{ " + formatValue("reference", getReference()) + formatValue("url", getImageURL())
                + formatValue("width", getWidth()) + formatValue("height", getHeight())
                + formatValue("alttext", getAltText()) + formatValue("alignment", getAlignment());
        // Remove last comma
        if (jsonString.length() > 4) {
            jsonString = jsonString.substring(0, jsonString.length() - 2);
        }
        // close it and return it
        jsonString = jsonString + " }";
        return jsonString;
    }

    /**
     * Formats the passed value as a key: value JSON pair, if the key is not null. If it is, the void string is
     * returned.
     * 
     * @param key the key of the formatted pair
     * @param value the value of the formatted pair
     * @return the formatted key: value JSON pair
     */
    private String formatValue(String key, Object value)
    {
        return value != null ? key + ": '" + value.toString().replace("'", "\\'") + "', " : "";
    }

    /**
     * Fills this object with data from the passed JSON representation.
     * 
     * @param json the JSON representation of this image config object.
     */
    public void fromJSON(String json)
    {
        JavaScriptObject jsObj = JavaScriptObject.fromJson(json);
        setReference((String) jsObj.get("reference"));
        setImageURL((String) jsObj.get("url"));
        setWidth((String) jsObj.get("width"));
        setHeight((String) jsObj.get("height"));
        setAltText((String) jsObj.get("alttext"));
        String foundAlignment = (String) jsObj.get("alignment");
        if (foundAlignment != null) {
            setAlignment(ImageAlignment.valueOf(foundAlignment));
        }
    }
}
