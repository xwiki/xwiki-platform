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
package org.xwiki.rendering.internal.macro.container;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.macro.container.LayoutManager;

/**
 * Layout manager implementation to layout the group blocks inside a container as columns.
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component("columns")
public class ColumnsLayoutManager implements LayoutManager
{
    /**
     * The total width of the container, in the page.
     */
    private static final double TOTAL_WIDTH = 99.9;

    /**
     * The name of the parameter to convey style information to the HTML (html style attribute).
     */
    private static final String PARAMETER_STYLE = "style";

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.container.LayoutManager#layoutContainer(org.xwiki.rendering.block.Block)
     */
    public void layoutContainer(Block container)
    {
        List<GroupBlock> innerGroups = container.getChildrenByType(GroupBlock.class, false);
        // FIXME Should we cry and throw an exception if ever we meet something else than a group right under
        // the container macro, or automatically put it in a group maybe ?

        int count = innerGroups.size();

        if (innerGroups.size() == 0) {
            // nothing inside, nothing to layout
            return;
        }

        // default padding, maybe should be read as a parameter
        double columnRightPadding = 1.5;
        // width of each column
        double computedColumnWidth = ((TOTAL_WIDTH - columnRightPadding * (count - 1)) / count);

        // add styles to all columns inside
        Iterator<GroupBlock> it = innerGroups.iterator();
        while (it.hasNext()) {
            GroupBlock column = it.next();
            ColumnStyle style = new ColumnStyle();
            style.setWidthPercent(computedColumnWidth);
            if (it.hasNext()) {
                style.setPaddingRightPercent(columnRightPadding);
            }
            // FIXME: merge the HTML style attribute?
            column.setParameter(PARAMETER_STYLE, style.getStyleAsString());
        }

        // finally, clear the floats introduced by the columns
        Map<String, String> clearFloatsParams = new HashMap<String, String>();
        clearFloatsParams.put(PARAMETER_STYLE, "clear: both;");
        container.addChild(new GroupBlock(clearFloatsParams));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.container.LayoutManager#getParameter(java.lang.String)
     */
    public Object getParameter(String parameterName)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.container.LayoutManager#setParameter(java.lang.String, Object)
     */
    public void setParameter(String parameterName, Object parameterValue)
    {
    }
}
