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

import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;

/**
 * Stores data about a link: reference (wiki, space, page), URL, label, tooltip.
 * 
 * @version $Id$
 */
public class LinkConfig extends EntityConfig
{
    /**
     * Link types.
     */
    public static enum LinkType
    {
        /** External link (default for any unrecognized link). */
        EXTERNAL("wikiexternallink"),

        /** Internal link targeting a new page. */
        NEW_WIKIPAGE("wikicreatelink"),

        /** Internal link targeting an existent page. */
        WIKIPAGE("wikilink"),

        /** Link targeting an attached file. */
        ATTACHMENT("wikiattachmentlink"),

        /** External link to an email address. Currently it doesn't have a dedicated CSS class name. */
        EMAIL(EXTERNAL.className);

        /**
         * The CSS class name associated with a link type.
         */
        private final String className;

        /**
         * Creates a new link type that has the given CSS class name associated.
         * 
         * @param className the CSS class name associated with the new link type
         */
        LinkType(String className)
        {
            this.className = className;
        }

        /**
         * @return the CSS class name associated with this link type
         */
        public String getClassName()
        {
            return className;
        }

        /**
         * @param className a CSS class name used to mark a specific type of link
         * @return the {@link LinkType} corresponding to the given class name, {@code null} if none of the link types
         *         match the given class name
         */
        public static LinkType getByClassName(String className)
        {
            for (LinkType linkType : values()) {
                if (linkType.className.equals(className)) {
                    return linkType;
                }
            }
            return null;
        }
    };

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
