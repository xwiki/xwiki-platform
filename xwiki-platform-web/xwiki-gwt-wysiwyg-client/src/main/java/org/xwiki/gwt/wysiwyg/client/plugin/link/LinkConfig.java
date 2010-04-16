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
package org.xwiki.gwt.wysiwyg.client.plugin.link;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Stores data about a link: reference (wiki, space, page), URL, label, tooltip.
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
         * internal link targeting a new page, link targeting an attached file, external link to an email address.
         */
        EXTERNAL, NEW_WIKIPAGE, WIKIPAGE, ATTACHMENT, EMAIL
    };

    /**
     * The URL of this link which can be either a relative or an absolute URL.
     * 
     * @see #reference
     */
    private String url;

    /**
     * The name of the wiki where the target document of this link is located.
     * 
     * @see #reference
     */
    private String wiki;

    /**
     * The name of the space of the target page of this link.
     * 
     * @see #reference
     */
    private String space;

    /**
     * The name of the target page of this link.
     * 
     * @see #reference
     */
    private String page;

    /**
     * The reference of the link, in the {@code wikiname:spacename.pagename} form. <br />
     * Note: this value should take priority over wiki, space, page set in this config: if the reference is set, that
     * this one should be used instead of generating another. Ideally, this should be set with the URL so that no
     * further computing is necessary for this link. If this and url are not set, they are to be computed from the wiki,
     * space, page values.
     */
    private String reference;

    /**
     * The label of this link, in original (HTML) form.
     */
    private String label;

    /**
     * The label of this link, in text form (editable form: the one which we present to the user and allow her to edit).
     */
    private String labelText;

    /**
     * Specifies if the editable form of the label of this link is readonly or not.
     */
    private boolean readOnlyLabel;

    /**
     * The type of this link.
     */
    private LinkType type;

    /**
     * An explanatory text for the link.
     */
    private String tooltip;

    /**
     * Flag indicating if the link should be opened in a new window.
     */
    private boolean openInNewWindow;

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
     * @return {@code true} if this link is configured to open in a new window, {@code false} otherwise
     */
    public boolean isOpenInNewWindow()
    {
        return openInNewWindow;
    }

    /**
     * @param openInNewWindow whether this link should be opened in a new window or not
     */
    public void setOpenInNewWindow(boolean openInNewWindow)
    {
        this.openInNewWindow = openInNewWindow;
    }

    /**
     * @return the tooltip of this link
     */
    public String getTooltip()
    {
        return tooltip;
    }

    /**
     * @param tooltip the tooltip to set for this link
     */
    public void setTooltip(String tooltip)
    {
        this.tooltip = tooltip;
    }
}
