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

import java.util.Collections;
import java.util.List;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.listener.HeaderLevel;

/**
 * @version $Id$
 */
public abstract class AbstractDashboardRenderer implements DashboardRenderer
{
    /**
     * The HTML class attribute name.
     */
    protected static final String CLASS = "class";

    /**
     * Decorates the passed gadget and renders it as a list of XDOM blocks.
     * 
     * @param gadget the gadget to render
     * @return the list of blocks resulted from rendering the passed gadget and decorating it (with containers, etc)
     */
    public List<Block> decorateGadget(Gadget gadget)
    {
        // prepare the title of the gadget, in a heading 2
        HeaderBlock titleBlock = new HeaderBlock(gadget.getTitle(), HeaderLevel.LEVEL2);
        titleBlock.setParameter(CLASS, "gadget-title");

        // And then the content wrapped in a group block with class, to style it
        GroupBlock contentGroup = new GroupBlock();
        contentGroup.setParameter(CLASS, "gadget-content");
        contentGroup.addChildren(gadget.getContent());

        // and wrap everything in a container, to give it a class
        GroupBlock gadgetBlock = new GroupBlock();
        gadgetBlock.setParameter(CLASS, "gadget");
        gadgetBlock.addChild(titleBlock);
        gadgetBlock.addChild(contentGroup);

        return Collections.<Block> singletonList(gadgetBlock);
    }
}
