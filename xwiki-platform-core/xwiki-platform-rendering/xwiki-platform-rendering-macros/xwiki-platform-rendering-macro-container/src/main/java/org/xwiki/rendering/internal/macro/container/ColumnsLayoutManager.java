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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.macro.container.LayoutManager;
import org.xwiki.skinx.SkinExtension;

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
     * The name of the parameter to convey style information to the HTML (html style attribute).
     */
    private static final String CLASS_ATTRIBUTE = "class";

    /**
     * The javascript file skin extension, to fetch the columns layout css.
     */
    @Inject
    @Named("ssfx")
    private SkinExtension ssfx;

    @Override
    public void layoutContainer(Block container)
    {
        List<GroupBlock> innerGroups =
            container.getBlocks(new ClassBlockMatcher(GroupBlock.class), Block.Axes.CHILD);
        // FIXME Should we cry and throw an exception if ever we meet something else than a group right under
        // the container macro, or automatically put it in a group maybe ?

        int count = innerGroups.size();

        if (innerGroups.size() == 0) {
            // nothing inside, nothing to layout
            return;
        }

        Map<String, Object> skinxParams = new HashMap<String, Object>();
        skinxParams.put("forceSkinAction", true);

        ssfx.use("uicomponents/container/columns.css", skinxParams);

        // add styles to all columns inside
        for (int i = 0; i < count; i++) {
            GroupBlock column = innerGroups.get(i);
            String classValue = "column";
            if (i == 0) {
                // we're at the first element in the list, put a marker. Don't need it to do standard columns layout,
                // but maybe somebody needs it for customization...
                classValue = classValue + " first-column";
            }
            if (i == count - 1) {
                // we're at the last element in the list, put a marker
                classValue = classValue + " last-column";
            }
            String oldClass = column.getParameter(CLASS_ATTRIBUTE);
            column.setParameter(CLASS_ATTRIBUTE, (StringUtils.isEmpty(oldClass) ? classValue : oldClass + " "
                + classValue));
        }

        // finally, clear the floats introduced by the columns
        Map<String, String> clearFloatsParams = new HashMap<String, String>();
        clearFloatsParams.put(CLASS_ATTRIBUTE, "clearfloats");
        String oldClass = container.getParameter(CLASS_ATTRIBUTE);
        String newClass = "container-columns container-columns-" + count;
        container.setParameter(CLASS_ATTRIBUTE, StringUtils.isEmpty(oldClass) ? newClass : oldClass + " " + newClass);
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
