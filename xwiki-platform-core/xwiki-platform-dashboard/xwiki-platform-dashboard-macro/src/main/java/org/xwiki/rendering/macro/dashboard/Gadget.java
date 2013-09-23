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
package org.xwiki.rendering.macro.dashboard;

import java.util.List;

import org.xwiki.rendering.block.Block;

/**
 * Abstraction for a gadget to put on a dashboard, to contain its title, content and position.
 * 
 * @version $Id$
 * @since 3.0M3
 */
public class Gadget
{
    /**
     * The identifier of this gadget, to render in the content.
     */
    private String id;

    /**
     * The title of this gadget.
     */
    private List<Block> title;

    /**
     * The content of this gadget.
     */
    private List<Block> content;

    /**
     * The position of this gadget, to be interpreted by the layouter.
     */
    private String position;

    /**
     * The string source of the title of this gadget, to make it editable further. <br/>
     * FIXME: this should be passed in a different way, potentially in metadata block attached to the title block. FTM,
     * to make it backwards compatible, keep it like this.
     */
    private String titleSource;

    /**
     * Creates a gadget from a title, content and position.
     * 
     * @param id the id of this gadget
     * @param title the title of the gadget
     * @param content the content of the gadget
     * @param position the position of the gadget
     */
    public Gadget(String id, List<Block> title, List<Block> content, String position)
    {
        this.setId(id);
        this.setTitle(title);
        this.setContent(content);
        this.setPosition(position);
    }

    /**
     * @return the title
     */
    public List<Block> getTitle()
    {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(List<Block> title)
    {
        this.title = title;
    }

    /**
     * @return the content
     */
    public List<Block> getContent()
    {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(List<Block> content)
    {
        this.content = content;
    }

    /**
     * @return the position
     */
    public String getPosition()
    {
        return position;
    }

    /**
     * @param position the position to set
     */
    public void setPosition(String position)
    {
        this.position = position;
    }

    /**
     * @return the id of this gadget, to render in the content
     */
    public String getId()
    {
        return id;
    }

    /**
     * @param id the id to set to this gadget
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the titleSource
     * FIXME: this should be passed in a different way, potentially in metadata block attached to the title block. FTM,
     * to make it backwards compatible, keep it like this. 
     */
    public String getTitleSource()
    {
        return titleSource;
    }

    /**
     * @param titleSource the titleSource to set
     */
    public void setTitleSource(String titleSource)
    {
        this.titleSource = titleSource;
    }
}
