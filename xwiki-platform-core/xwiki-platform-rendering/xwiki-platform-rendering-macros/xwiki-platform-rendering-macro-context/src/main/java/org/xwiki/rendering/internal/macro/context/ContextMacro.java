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
package org.xwiki.rendering.internal.macro.context;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.async.internal.AbstractExecutedContentMacro;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererConfiguration;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.MacroPreparationException;
import org.xwiki.rendering.macro.context.ContextMacroParameters;
import org.xwiki.rendering.macro.context.TransformationContextMode;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.source.MacroContentWikiSource;
import org.xwiki.rendering.macro.source.MacroContentWikiSourceFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;

/**
 * Execute the macro's content in the context of another document's reference.
 * 
 * @version $Id$
 * @since 3.0M1
 */
@Component
@Named("context")
@Singleton
public class ContextMacro extends AbstractExecutedContentMacro<ContextMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Executes content in the context of the passed document";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION = "The content to execute";

    @Inject
    private TransformationManager transformationManager;

    @Inject
    private MacroContentWikiSourceFactory contentFactory;

    @Inject
    private ContextMacroDocument documentHandler;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public ContextMacro()
    {
        super("Context", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION, false, Block.LIST_BLOCK_TYPE),
            ContextMacroParameters.class);

        setDefaultCategories(Set.of(DEFAULT_CATEGORY_DEVELOPMENT));
    }

    @Override
    public List<Block> execute(ContextMacroParameters parameters, String macroContent,
        MacroTransformationContext context) throws MacroExecutionException
    {
        MetaData metadata;
        if (parameters.getDocument() != null) {
            metadata = new MetaData();
            metadata.addMetaData(MetaData.SOURCE, parameters.getDocument());
            metadata.addMetaData(MetaData.BASE, parameters.getDocument());
        } else {
            metadata = null;
        }
        String content = macroContent;
        Syntax syntax = null;
        if (parameters.getSource() != null) {
            MacroContentWikiSource wikiSource = this.contentFactory.getContent(parameters.getSource(), context);
            syntax = wikiSource.getSyntax();
            content = wikiSource.getContent();
        }

        XDOM xdom = this.parser.parse(content, syntax, context, false, metadata, context.isInline());

        if (xdom.getChildren().isEmpty()) {
            return Collections.emptyList();
        }

        List<Block> blocks;
        if (parameters.isRestricted() || parameters.getTransformationContext() == TransformationContextMode.DOCUMENT
            || parameters.getTransformationContext() == TransformationContextMode.TRANSFORMATIONS) {
            // Execute the content in the context of the target document
            blocks = executeContext(xdom, parameters, context);
        } else {
            // The content will be executed in the current context
            blocks = xdom.getChildren();
        }

        // Keep metadata so that the result stay associated to context properties when inserted in the parent
        // XDOM
        return Arrays.asList(new MetaDataBlock(blocks, xdom.getMetaData()));
    }

    private List<Block> executeContext(XDOM xdom, ContextMacroParameters parameters, MacroTransformationContext context)
        throws MacroExecutionException
    {
        DocumentReference referencedDocReference = this.documentHandler.getDocumentReference(parameters, context);

        // Reuse the very generic async rendering framework (even if we don't do async and caching) since it's
        // taking
        // care of many other things
        BlockAsyncRendererConfiguration configuration = createBlockAsyncRendererConfiguration(null, xdom, context);
        configuration.setAsyncAllowed(false);
        configuration.setCacheAllowed(false);

        if (parameters.isRestricted()) {
            configuration.setResricted(true);
        }

        Map<String, Object> backupObjects = null;
        try {
            if (referencedDocReference != null) {
                backupObjects = new HashMap<>();

                // Switch the context document
                this.documentAccessBridge.pushDocumentInContext(backupObjects, referencedDocReference);

                // Apply the transformations but with a Transformation Context having the XDOM of the passed
                // document so that macros execute on the passed document's XDOM (e.g. the TOC macro will generate
                // the toc for the passed document instead of the current document).
                DocumentModelBridge referencedDoc =
                    this.documentAccessBridge.getTranslatedDocumentInstance(referencedDocReference);
                XDOM referencedXDOM = referencedDoc.getPreparedXDOM();

                if (parameters.getTransformationContext() == TransformationContextMode.TRANSFORMATIONS) {
                    // Get the XDOM from the referenced doc but with Transformations applied so that all macro are
                    // executed and contribute XDOM elements.
                    // IMPORTANT: This can be dangerous since it means executing macros, and thus also script macros
                    // defined in the referenced document. To be used with caution.
                    TransformationContext referencedTxContext =
                        new TransformationContext(referencedXDOM, referencedDoc.getSyntax(),
                            referencedDoc.isRestricted());
                    this.transformationManager.performTransformations(referencedXDOM, referencedTxContext);
                }

                // Configure the Transformation Context XDOM depending on the mode asked.
                configuration.setXDOM(referencedXDOM);
            }

            // Execute the content
            Block result = this.executor.execute(configuration);

            return result.getChildren();
        } catch (Exception e) {
            throw new MacroExecutionException("Failed start the execution of the macro", e);
        } finally {
            if (backupObjects != null) {
                // Restore the context document
                this.documentAccessBridge.popDocumentFromContext(backupObjects);
            }
        }
    }

    @Override
    public void prepare(MacroBlock macroBlock) throws MacroPreparationException
    {
        if (macroBlock.getParameter("source") == null) {
            this.parser.prepareContentWiki(macroBlock);
        }
    }
}
