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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.TableBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.parser.Parser;

/**
 * A data source that allows building charts from {@link XDOM} sources.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named("xdom")
@Singleton
public class DocumentTableBlockDataSource extends AbstractTableBlockDataSource
{
    /**
     * Identifies which xdom to process.
     */
    private static final String DOCUMENT = "document";

    /**
     * Identifies the table on the xdom.
     */
    private static final String TABLE = "table";

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

    /**
     * {@link EntityReferenceSerializer} component.
     */
    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    protected TableBlock getTableBlock(String macroContent, Map<String, String> macroParameters)
        throws MacroExecutionException
    {
        // Determine the document name.
        String documentName = macroParameters.get(DOCUMENT);
        if (null == documentName) {
            documentName = this.entityReferenceSerializer.serialize(this.docBridge.getCurrentDocumentReference());
        }
        if (!docBridge.exists(documentName)) {
            throw new MacroExecutionException(String.format("Document [%s] does not exist.", documentName));
        }
        
        // Parse the document content into an XDOM.
        XDOM xdom;
        try {
            Parser parser = componentManager.getInstance(Parser.class, docBridge.getDocumentSyntaxId(documentName));
            xdom = parser.parse(new StringReader(docBridge.getDocumentContent(documentName)));
        } catch (Exception ex) {
            throw new MacroExecutionException(String.format("Error while parsing document: [%s].", documentName), ex);
        }
        
        // Find the correct table block.
        List<TableBlock> tableBlocks = xdom.getBlocks(new ClassBlockMatcher(TableBlock.class), Block.Axes.DESCENDANT);
        String tableId = macroParameters.get(TABLE);
        TableBlock result = null;
        if (null != tableId) {
            for (TableBlock tableBlock : tableBlocks) {
                String id = tableBlock.getParameter("id");
                if (null != id && id.equals(tableId)) {
                    result = tableBlock;
                    break;
                }
            }
        } else {
            result = (tableBlocks.size() > 0) ? tableBlocks.get(0) : null;
        }
        
        if (null == result) {
            throw new MacroExecutionException("Unable to find a macthing data table.");
        }
        
        return result;
    }
}
