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
package org.xwiki.gwt.wysiwyg.client.plugin.style;

/**
 * Describes a CSS style (class name).
 * 
 * @version $Id$
 */
public class StyleDescriptor
{
    /**
     * @see #getName()
     */
    private String name;

    /**
     * @see #getLabel()
     */
    private String label;

    /**
     * @see #isInline()
     */
    private boolean inline;

    /**
     * Creates a new style descriptor.
     * 
     * @param name the style name
     * @param label the style label
     * @param inline {@code true} if the style is in-line, {@code false} otherwise
     */
    public StyleDescriptor(String name, String label, boolean inline)
    {
        this.name = name;
        this.label = label;
        this.inline = inline;
    }

    /**
     * @return the style name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the style name.
     * 
     * @param name the new style name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the style label
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * Sets the style label.
     * 
     * @param label the new style label
     */
    public void setLabel(String label)
    {
        this.label = label;
    }

    /**
     * @return whether the style is in-line or not
     */
    public boolean isInline()
    {
        return inline;
    }

    /**
     * Sets whether the style is in-line or not.
     * 
     * @param inline {@code true} to make the style in-line, {@code false} otherwise
     */
    public void setInline(boolean inline)
    {
        this.inline = inline;
    }
}
