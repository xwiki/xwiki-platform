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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.macro.container.LayoutManager;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.xml.XMLAttributeValue;

/**
 * Layout manager implementation to layout the group blocks inside a container as columns.
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component
@Named("columns")
@Singleton
public class ColumnsLayoutManager implements LayoutManager
{
    /**
     * The CSS class attribute name (used to style the content in CSS).
     */
    private static final String CLASS_ATTRIBUTE_NAME = "class";

    /**
     * The CSS class attribute value to identify a column.
     */
    private static final String COLUMN_CLASS_VALUE = "column";

    /**
     * The CSS class attribute value to identify the first column.
     */
    private static final String FIRST_COLUMN_CLASS_VALUE = "first-column";

    /**
     * The CSS class attribute value to identify the last column.
     */
    private static final String LAST_COLUMN_CLASS_VALUE = "last-column";

    /**
     * The javascript file skin extension, to fetch the columns layout css.
     */
    @Inject
    @Named("ssfx")
    private SkinExtension ssfx;

    @Override
    public void layoutContainer(Block container)
    {
        // The container macro works by having GroupBlock inside the macro content, each group representing a column.
        // Any top level block that is not a GroupBlock is ignored.
        // Find all GroupBlocks and thus all column contents to layout.
        List<GroupBlock> innerGroups = container.getBlocks(new ClassBlockMatcher(GroupBlock.class), Block.Axes.CHILD);

        int count = innerGroups.size();
        if (count == 0) {
            // No columns. Thus, nothing to layout...
            return;
        }

        // Add CSS for the columns.
        Map<String, Object> skinxParams = new HashMap<>();
        skinxParams.put("forceSkinAction", true);
        this.ssfx.use("uicomponents/container/columns.css", skinxParams);

        // Add style metadata to all columns
        for (int i = 0; i < count; i++) {
            GroupBlock column = innerGroups.get(i);
            XMLAttributeValue classValue = new XMLAttributeValue(column.getParameter(CLASS_ATTRIBUTE_NAME));
            classValue.addValue(COLUMN_CLASS_VALUE);
            if (i == 0) {
                // We're at the first column in the list, put a marker. Don't need it to do standard columns layout,
                // but maybe somebody needs it for customization...
                classValue.addValue(FIRST_COLUMN_CLASS_VALUE);
            }
            if (i == count - 1) {
                // We're at the last element in the list, put a marker
                classValue.addValue(LAST_COLUMN_CLASS_VALUE);
            }
            column.setParameter(CLASS_ATTRIBUTE_NAME, classValue.toString());
        }

        // Add metadata to the container
        XMLAttributeValue classValue = new XMLAttributeValue(container.getParameter(CLASS_ATTRIBUTE_NAME));
        classValue.addValues("container-columns", "container-columns-" + count);
        container.setParameter(CLASS_ATTRIBUTE_NAME, classValue.toString());

        // Finally, clear the floats introduced by the columns
        Map<String, String> clearFloatsParams = new HashMap<>();
        clearFloatsParams.put(CLASS_ATTRIBUTE_NAME, "clearfloats");
        container.addChild(new GroupBlock(clearFloatsParams));
    }

    @Override
    public Object getParameter(String parameterName)
    {
        return null;
    }

    @Override
    public void setParameter(String parameterName, Object parameterValue)
    {
    }
}
