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
     * Creates a gadget from a title, content and position.
     * 
     * @param title the title of the gadget
     * @param content the content of the gadget
     * @param position the position of the gadget
     */
    public Gadget(List<Block> title, List<Block> content, String position)
    {
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
}
