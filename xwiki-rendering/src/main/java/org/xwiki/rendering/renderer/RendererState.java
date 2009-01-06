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
package org.xwiki.rendering.renderer;

/**
 * Saves a renderer's state (ie for example if the renderer is in a link, in a section, etc).
 * 
 * @version $Id$
 * @since 1.7
 */
public class RendererState
{
    /**
     * True if the text being written starts a new line. By text we mean Space, Special Symbol and Words. This is useful
     * for some renderers which need to have this information. For example the XWiki Syntax renderer uses it to decide
     * whether to escape "*" characters starting new lines since otherwise they would be confused for list items.
     */
    private boolean isTextOnNewLine;

    private boolean isInParagraph;

    private boolean isInSection;

    private boolean isInLink;

    private boolean isInTable;

    public boolean isInParagraph()
    {
        return this.isInParagraph;
    }

    public void setInParagraph(boolean isInParagraph)
    {
        this.isInParagraph = isInParagraph;
    }

    public boolean isInSection()
    {
        return this.isInSection;
    }

    public void setInSection(boolean isInSection)
    {
        this.isInSection = isInSection;
    }

    public boolean isInLink()
    {
        return this.isInLink;
    }

    public void setInLink(boolean isInLink)
    {
        this.isInLink = isInLink;
    }

    public boolean isTextOnNewLine()
    {
        return this.isTextOnNewLine;
    }

    public void setTextOnNewLine(boolean isTextOnNewLine)
    {
        this.isTextOnNewLine = isTextOnNewLine;
    }

    public void setInTable(boolean isInTable)
    {
        this.isInTable = isInTable;
    }

    public boolean isInTable()
    {
        return this.isInTable;
    }
}
