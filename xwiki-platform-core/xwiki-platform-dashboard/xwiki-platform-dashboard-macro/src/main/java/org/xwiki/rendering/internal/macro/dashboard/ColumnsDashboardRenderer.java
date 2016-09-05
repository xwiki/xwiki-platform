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
package org.xwiki.rendering.internal.macro.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.container.ContainerMacroParameters;
import org.xwiki.rendering.macro.dashboard.DashboardRenderer;
import org.xwiki.rendering.macro.dashboard.Gadget;
import org.xwiki.rendering.macro.dashboard.GadgetRenderer;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Specialized gadget that interprets its position as a pair of numbers, the first being the index of the column, and
 * the second being the index of the gadget inside the column.
 * 
 * @version $Id$
 * @since 3.0M3
 */
class ColumnGadget extends Gadget
{
    /**
     * The index of the column of this gadget.
     */
    private Integer column;

    /**
     * The index of this gadget inside its column.
     */
    private Integer index;

    /**
     * Creates a column gadget which is a copy of the passed gadget.
     * 
     * @param gadget the gadget to copy into a column gadget.
     */
    ColumnGadget(Gadget gadget)
    {
        super(gadget.getId(), gadget.getTitle(), gadget.getContent(), gadget.getPosition());
        this.setTitleSource(gadget.getTitleSource());
    }

    /**
     * @return the column
     */
    public Integer getColumn()
    {
        return this.column;
    }

    /**
     * @return the index
     */
    public Integer getIndex()
    {
        return this.index;
    }

    @Override
    public void setPosition(String position)
    {
        super.setPosition(position);

        // parse the position as a "container, index" pair and store the container number and index. <br>
        // TODO: move this code in an API class since the comma separated format is more generic than the columns layout
        // split by comma, first position is column, second position is index
        String[] split = position.split(",");
        try {
            this.column = Integer.valueOf(split[0].trim());
            this.index = Integer.valueOf(split[1].trim());
        } catch (ArrayIndexOutOfBoundsException e) {
            // nothing, just leave column and index null. Not layoutable in columns
        } catch (NumberFormatException e) {
            // same, nothing, just leave column and index null. Not layoutable in columns
        }
    }
}

/**
 * The columns dashboard renderer, that renders the list of passed gadgets in columns, and interprets the positions as
 * pairs of column number and gadget index.
 * 
 * @version $Id$
 * @since 3.0M3
 */
@Component
@Named("columns")
@Singleton
public class ColumnsDashboardRenderer implements DashboardRenderer
{
    /**
     * The HTML class attribute name.
     */
    protected static final String CLASS = "class";

    /**
     * The HTML id attribute name.
     */
    protected static final String ID = "id";

    /**
     * The component manager, to inject to the {@link BlocksContainerMacro}.
     */
    @Inject
    private ComponentManager componentManager;

    @Override
    public List<Block> renderGadgets(List<Gadget> gadgets, GadgetRenderer gadgetsRenderer,
        MacroTransformationContext context) throws MacroExecutionException
    {
        // transform the passed gagdets in a list of column gadgets
        List<ColumnGadget> columnGadgets = new ArrayList<ColumnGadget>();
        List<Gadget> invalidGadgets = new ArrayList<Gadget>();
        for (Gadget gadget : gadgets) {
            ColumnGadget cGadget = new ColumnGadget(gadget);
            if (cGadget.getColumn() != null && cGadget.getIndex() != null) {
                columnGadgets.add(cGadget);
            } else {
                invalidGadgets.add(gadget);
            }
        }

        // sort the column gadgets by first the column number then by index in column, for all those which have a valid
        // position
        Collections.sort(columnGadgets, new Comparator<ColumnGadget>()
        {
            public int compare(ColumnGadget g1, ColumnGadget g2)
            {
                return g1.getColumn().equals(g2.getColumn()) ? g1.getIndex() - g2.getIndex() : g1.getColumn()
                    - g2.getColumn();
            }
        });

        // get the maximmum column number in the gadgets list and create that number of columns. This is the column
        // number of the last gadget in the ordered list. Default is 1 column, the empty dashboard is made of one empty
        // column
        int columns = 1;
        if (!columnGadgets.isEmpty()) {
            // prevent bad configurations to mess up the dashboard layout
            int lastGadgetsColumn = columnGadgets.get(columnGadgets.size() - 1).getColumn();
            if (lastGadgetsColumn > 1) {
                columns = lastGadgetsColumn;
            }
        }

        // create the list of gadget containers
        List<Block> gadgetContainers = new ArrayList<Block>();
        for (int i = 0; i < columns; i++) {
            GroupBlock gContainer = new GroupBlock();
            gContainer.setParameter(CLASS, DashboardMacro.GADGET_CONTAINER);
            // and generate the ids of the gadget containers, which are column numbers, 1 based
            gContainer.setParameter(ID, DashboardMacro.GADGET_CONTAINER_PREFIX + (i + 1));
            gadgetContainers.add(gContainer);
        }

        // render them as columns using the container macro and appropriate parameters
        ContainerMacroParameters containerParams = new ContainerMacroParameters();
        containerParams.setLayoutStyle("columns");
        BlocksContainerMacro containerMacro = new BlocksContainerMacro();
        containerMacro.setComponentManager(this.componentManager);
        containerMacro.setContent(gadgetContainers);
        List<Block> layoutedResult = containerMacro.execute(containerParams, null, context);

        for (ColumnGadget gadget : columnGadgets) {
            int columnIndex = gadget.getColumn() - 1;
            gadgetContainers.get(columnIndex).addChildren(gadgetsRenderer.decorateGadget(gadget));
        }

        // and return the result
        return layoutedResult;
    }
}
