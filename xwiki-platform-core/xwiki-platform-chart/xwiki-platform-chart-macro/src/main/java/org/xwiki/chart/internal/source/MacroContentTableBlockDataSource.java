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
package org.xwiki.chart.internal.source;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.TableBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.parser.Parser;

/**
 * A data source for building charts from macro content.
 *
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named("inline")
@Singleton
public class MacroContentTableBlockDataSource extends AbstractTableBlockDataSource
{
    /**
     * {@link ComponentManager} used to dynamically lookup for various {@link Parser} implementations.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * {@link DocumentAccessBridge} component.
     */
    @Inject
    private DocumentAccessBridge docBridge;

    @Override
    protected TableBlock getTableBlock(String macroContent, Map<String, String> macroParameters)
        throws MacroExecutionException
    {
        // Since we are using an inline source the macro content cannot be empty/null.
        if (StringUtils.isEmpty(macroContent)) {
            throw new MacroExecutionException("A Chart Macro using an inline source must have a data table defined in "
                + "its content.");
        }

        // Parse the macro content into an XDOM.
        XDOM xdom;
        try {
            Parser parser = componentManager.getInstance(Parser.class,
                docBridge.getDocument(this.docBridge.getCurrentDocumentReference()).getSyntax().toIdString());
            xdom = parser.parse(new StringReader(macroContent));
        } catch (Exception ex) {
            throw new MacroExecutionException("Error while parsing macro content.", ex);
        }
        
        // Take the first TableBlock found in the macro content.
        List<TableBlock> tableBlocks = xdom.getBlocks(new ClassBlockMatcher(TableBlock.class), Block.Axes.DESCENDANT);
        if (tableBlocks.size() == 0) {
            throw new MacroExecutionException("Unable to locate a suitable data table.");
        }
        
        return tableBlocks.get(0);
    }    
}
