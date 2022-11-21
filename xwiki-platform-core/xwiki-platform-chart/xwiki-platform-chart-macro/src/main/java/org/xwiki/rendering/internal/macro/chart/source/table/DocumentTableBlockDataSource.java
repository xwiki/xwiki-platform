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
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.TableBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.block.match.MetadataBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import static org.xwiki.component.descriptor.ComponentInstantiationStrategy.PER_LOOKUP;

/**
 * A data source that allows building charts from {@link XDOM} sources.
 *
 * @version $Id$
 * @since 4.2M1
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
     * The document name of the document holding the table. If null then the data source is located in the current
     * document.
     */
    private DocumentReference documentReference;

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
    protected TableBlock getTableBlock(String macroContent, MacroTransformationContext context)
        throws MacroExecutionException
    {
        XDOM xdom = computeXDOM(context);

        // Find the correct table block.
        List<TableBlock> tableBlocks = xdom.getBlocks(new ClassBlockMatcher(TableBlock.class), Block.Axes.DESCENDANT);
        TableBlock result = null;
        this.logger.debug("Table id is [{}], there are [{}] tables in the document [{}]",
            new Object[]{this.tableId, tableBlocks.size(), this.documentReference});
        if (null != tableId) {
            for (TableBlock tableBlock : tableBlocks) {
                String id = tableBlock.getParameter("id");
                if (null != id && id.equals(this.tableId)) {
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

    /**
     * Get the XDOM for the data source.
     *
     * @param context the Macro context from which we can get the XDOM if the source is in the current content
     * @return the XDOM in which the data source is located
     * @throws MacroExecutionException in case of an error getting the XDOM
     */
    private XDOM computeXDOM(MacroTransformationContext context) throws MacroExecutionException
    {
        XDOM xdom;

        // Parse the document content into an XDOM. If the reference is to the current document then we should not
        // Parse the content again since 1) that's unnecessary since we can hold of the XDOM from the Transformation
        // Context and 2) it's going to cause a cycle...
        if (isDefinedChartSourceTheCurrentDocument(context.getCurrentMacroBlock())) {
            xdom = context.getXDOM();
        } else {
            try {
                DocumentModelBridge document = this.docBridge.getDocumentInstance(this.documentReference);
                DocumentDisplayerParameters parameters = new DocumentDisplayerParameters();
                parameters.setContentTranslated(true);
                parameters.setTargetSyntax(context.getTransformationContext().getTargetSyntax());
                parameters.setContentTranslated(true);

                xdom = this.documentDisplayer.display(document, parameters);
            } catch (Exception e) {
                throw new MacroExecutionException(String.format("Error getting Chart table from document [%s]",
                    this.documentReference, e));
            }
        }
        return xdom;
    }

    /**
     * @param currentMacroBlock the current macro block being rendered
     * @return true if the chart macro takes its source in the current document or false otherwise
     */
    protected boolean isDefinedChartSourceTheCurrentDocument(MacroBlock currentMacroBlock)
    {
        boolean result;
        if (this.documentReference == null) {
            result = true;
        } else {
            String sourceReference = extractSourceContentReference(currentMacroBlock);
            if (this.documentReferenceResolver.resolve(sourceReference,
                this.docBridge.getCurrentDocumentReference()).equals(this.documentReference))
            {
                result = true;
            } else {
                result = false;
            }
        }

        return result;
    }

    @Override
    protected boolean setParameter(String key, String value) throws MacroExecutionException
    {
        if (DOCUMENT_PARAM.equals(key)) {
            this.documentReference
                = this.documentReferenceResolver.resolve(value, docBridge.getCurrentDocumentReference());
            return true;
        }

        if (TABLE_PARAM.equals(key)) {
            this.tableId = value;
            return true;
        }

        return super.setParameter(key, value);
    }

    @Override
    protected void validateParameters() throws MacroExecutionException
    {
        super.validateParameters();

        if (this.documentReference != null) {
            if (!authorizationManager.hasAccess(Right.VIEW, this.docBridge.getCurrentUserReference(),
                this.documentReference))
            {
                throw new MacroExecutionException("You do not have permission to view the document.");
            }

            if (!exists()) {
                throw new MacroExecutionException(
                    String.format("Document [%s] does not exist.", this.documentReference));
            }
        }
    }

    private boolean exists() throws MacroExecutionException
    {
        try {
            return this.docBridge.exists(this.documentReference);
        } catch (Exception e) {
            throw new MacroExecutionException(
                "Failed to check the existence of the document with reference [" + this.documentReference + "]", e);
        }
    }

    /**
     * @param source the blocks from where to try to extract the source content
     * @return the source content reference or null if none is found
     */
    private String extractSourceContentReference(Block source)
    {
        String contentSource = null;
        MetaDataBlock metaDataBlock =
            source.getFirstBlock(new MetadataBlockMatcher(MetaData.SOURCE), Block.Axes.ANCESTOR);
        if (metaDataBlock != null) {
            contentSource = (String) metaDataBlock.getMetaData().getMetaData(MetaData.SOURCE);
        }
        return contentSource;
    }
}
