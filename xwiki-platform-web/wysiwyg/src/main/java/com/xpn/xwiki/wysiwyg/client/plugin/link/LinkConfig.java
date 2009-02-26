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
package com.xpn.xwiki.wysiwyg.client.plugin.link;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.xpn.xwiki.wysiwyg.client.dom.JavaScriptObject;

/**
 * Stores the data about a link: information about the link: reference (wiki, space, page), URL and also about its
 * representation in a document (parameters, etc).
 * 
 * @version $Id$
 */
public class LinkConfig implements IsSerializable
{
    /**
     * Enumeration type to store the type of link: wiki external link, link to an existing page, link to a new page,
     * etc.
     */
    public enum LinkType
    {
        /**
         * Link types: external link (default for any unrecognized link), internal link targeting an existent page,
         * internal link targeting a new page, external link to an email address.
         */
        EXTERNAL, NEW_PAGE, EXISTING_PAGE, EMAIL
    };

    /**
     * The URL of this link. This can be either a relative or an absolute URL.
     */
    private String url;

    /**
     * The name of the wiki where the target document of this link is located.
     */
    private String wiki;

    /**
     * The name of the space of the target page of this link.
     */
    private String space;

    /**
     * The name of the target page of this link.
     */
    private String page;

    /**
     * The label of this link, in text form (editable form: the one which we present to the user and allow her to edit).
     */
    private String labelText;

    /**
     * Specifies if the editable form of the label of this link is readonly or not.
     */
    private boolean readOnlyLabel;

    /**
     * The label of this link, in original (HTML) form.
     */
    private String label;

    /**
     * The type of this link.
     */
    private LinkType type;

    /**
     * @return the url
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * @return the wiki
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
     * @return the space
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
     * @return the page
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
     * @return the label
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label)
    {
        this.label = label;
    }

    /**
     * @return the type
     */
    public LinkType getType()
    {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(LinkType type)
    {
        this.type = type;
    }

    /**
     * @return the labelText
     */
    public String getLabelText()
    {
        return labelText;
    }

    /**
     * @param labelText the labelText to set
     */
    public void setLabelText(String labelText)
    {
        this.labelText = labelText;
    }

    /**
     * @return the readOnlyLabel
     */
    public boolean isReadOnlyLabel()
    {
        return readOnlyLabel;
    }

    /**
     * @param readOnlyLabel the readOnlyLabel to set
     */
    public void setReadOnlyLabel(boolean readOnlyLabel)
    {
        this.readOnlyLabel = readOnlyLabel;
    }

    /**
     * @return the JSON representation of this ImageConfig
     */
    public String toJSON()
    {
        String jsonString =
            "{ " + formatValue("wiki", getWiki()) + formatValue("space", getSpace()) + formatValue("page", getPage())
                + formatValue("url", getUrl()) + formatValue("label", getLabel())
                + formatValue("labeltext", getLabelText())
                + formatValue("readonlylabel", isReadOnlyLabel() ? null : isReadOnlyLabel())
                + formatValue("type", getType());
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
        return value != null ? key + ": '" + value + "', " : "";
    }

    /**
     * Fills this object with data from the passed JSON representation.
     * 
     * @param json the JSON representation of this image config object.
     */
    public void fromJSON(String json)
    {
        JavaScriptObject jsObj = JavaScriptObject.fromJson(json);
        setWiki((String) jsObj.get("wiki"));
        setSpace((String) jsObj.get("space"));
        setPage((String) jsObj.get("page"));
        setUrl((String) jsObj.get("url"));
        setLabel((String) jsObj.get("label"));
        setLabelText((String) jsObj.get("labeltext"));
        setReadOnlyLabel(jsObj.get("readonlylabel") != null ? Boolean.parseBoolean((String) jsObj.get("readonlylabel"))
            : false);
        setType(jsObj.get("type") != null ? LinkType.valueOf((String) jsObj.get("type")) : null);
    }
}
