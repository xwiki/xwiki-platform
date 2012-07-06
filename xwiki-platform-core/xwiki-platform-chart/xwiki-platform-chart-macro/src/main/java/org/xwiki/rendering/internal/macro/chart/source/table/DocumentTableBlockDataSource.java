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

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.TableBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import static org.xwiki.component.descriptor.ComponentInstantiationStrategy.PER_LOOKUP;

/**
 * A data source that allows building charts from {@link XDOM} sources.
 *
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named("xdom")
@InstantiationStrategy(PER_LOOKUP)
public class DocumentTableBlockDataSource extends AbstractTableBlockDataSource
{
    /**
     * Identifies which xdom to process.
     */
    private static final String DOCUMENT_PARAM = "document";

    /**
     * Identifies the table on the xdom.
     */
    private static final String TABLE_PARAM = "table";

    /**
     * The document name of the document holding the table.
     */
    private DocumentReference documentRef;

    /**
     * The id of the table holding the data.
     */
    private String tableId;

    /**
     * A logger.
     */
    @Inject
    private Logger logger;

    /**
     * {@link DocumentDisplayer} used for rendering the document contents.
     */
    @Inject
    private DocumentDisplayer documentDisplayer;

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

    /**
     * A document reference resolver.
     */
    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * The authorization manager.
     */
    @Inject
    private AuthorizationManager authorizationManager;

    @Override
    protected TableBlock getTableBlock(String macroContent)
        throws MacroExecutionException
    {
        // Parse the document content into an XDOM.
        XDOM xdom;
        try {
            String language = docBridge.getDocument(docBridge.getCurrentDocumentReference()).getRealLanguage();
            DocumentModelBridge document = docBridge.getDocument(documentRef);
            DocumentDisplayerParameters parameters = new DocumentDisplayerParameters();
            parameters.setContentTranslated(true);
            xdom = documentDisplayer.display(document, parameters);
        } catch (Exception ex) {
            throw new MacroExecutionException(String.format("Error while parsing document: [%s].",
                entityReferenceSerializer.serialize(documentRef)), ex);
        }

        // Find the correct table block.
        List<TableBlock> tableBlocks = xdom.getBlocks(new ClassBlockMatcher(TableBlock.class), Block.Axes.DESCENDANT);
        TableBlock result = null;
        logger.debug("Table id is [{}], there are [{}] tables in the document [{}]",
            new Object[]{tableId, tableBlocks.size(), documentRef});
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
            throw new MacroExecutionException("Unable to find a matching data table.");
        }

        return result;
    }

    @Override
    protected boolean setParameter(String key, String value) throws MacroExecutionException
    {
        if (DOCUMENT_PARAM.equals(key)) {
            documentRef = documentReferenceResolver.resolve(value);
            return true;
        }

        if (TABLE_PARAM.equals(key)) {
            tableId = value;
            return true;
        }

        return super.setParameter(key, value);
    }

    @Override
    protected void validateParameters() throws MacroExecutionException
    {
        super.validateParameters();

        if (null == documentRef) {
            documentRef = this.docBridge.getCurrentDocumentReference();
        } else if (!authorizationManager.hasAccess(Right.VIEW,
            docBridge.getCurrentUserReference(),
            documentRef))
        {
            throw new MacroExecutionException("You do not have permission to view the document.");
        }

        if (!docBridge.exists(documentRef)) {
            throw new MacroExecutionException(String.format("Document [%s] does not exist.",
                entityReferenceSerializer.serialize(documentRef)));
        }
    }
}
