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
package org.xwiki.rendering.internal.macro.chart.source.table;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.TableBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import static org.xwiki.component.descriptor.ComponentInstantiationStrategy.PER_LOOKUP;

/**
 * A data source for building charts from a wiki table defined in the macro content.
 *
 * @version $Id$
 * @since 4.2M1
 */
@Component
@Named("inline")
@InstantiationStrategy(PER_LOOKUP)
public class MacroContentTableBlockDataSource extends AbstractTableBlockDataSource
{
    /**
     * Used to parse macro content containing wiki syntax.
     */
    @Inject
    private MacroContentParser macroContentParser;

    @Override
    protected TableBlock getTableBlock(String macroContent, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Since we are using an inline source the macro content cannot be empty/null.
        if (StringUtils.isEmpty(macroContent)) {
            throw new MacroExecutionException("A Chart Macro using an inline source must have a data table defined in "
                + "its content.");
        }

        // Parse the macro content into an XDOM.
        XDOM xdom = this.macroContentParser.parse(macroContent, context, true, false);

        // Take the first TableBlock found in the macro content.
        List<TableBlock> tableBlocks = xdom.getBlocks(new ClassBlockMatcher(TableBlock.class), Block.Axes.DESCENDANT);
        if (tableBlocks.size() == 0) {
            throw new MacroExecutionException("Unable to locate a suitable data table.");
        }

        return tableBlocks.get(0);
    }
}
