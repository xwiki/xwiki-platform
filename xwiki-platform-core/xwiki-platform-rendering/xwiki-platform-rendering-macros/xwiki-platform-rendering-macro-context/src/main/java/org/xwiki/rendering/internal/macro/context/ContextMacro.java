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
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.async.internal.AbstractExecutedContentMacro;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererConfiguration;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.context.ContextMacroParameters;
import org.xwiki.rendering.macro.context.TransformationContextMode;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

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
    private AuthorizationManager authorizationManager;

    @Inject
    private TransformationManager transformationManager;

    @Inject
    @Named("macro")
    private DocumentReferenceResolver<String> macroReferenceResolver;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public ContextMacro()
    {
        super("Context", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION, true, Block.LIST_BLOCK_TYPE),
            ContextMacroParameters.class);

        // The Context macro must execute early since it can contain include macros which can bring stuff like headings
        // for other macros (TOC macro, etc). Make it the same priority as the Include macro.
        setPriority(10);
        setDefaultCategories(Set.of(DEFAULT_CATEGORY_DEVELOPMENT));
    }

    private void checkAccess(DocumentReference currentAuthor, DocumentReference referencedDocReference)
        throws MacroExecutionException
    {
        // Current author must have view right on the target document to use it as context document
        try {
            this.authorizationManager.checkAccess(Right.VIEW, currentAuthor, referencedDocReference);
        } catch (AccessDeniedException e) {
            throw new MacroExecutionException("Author [" + currentAuthor
                + "] is not allowed to access target document [" + referencedDocReference + "]", e);
        }
    }

    private XDOM getXDOM(DocumentReference referencedDocReference, ContextMacroParameters parameters)
        throws MacroExecutionException
    {
        try {
            if (parameters.getTransformationContext() == TransformationContextMode.DOCUMENT
                || parameters.getTransformationContext() == TransformationContextMode.TRANSFORMATIONS) {
                // Apply the transformations but with a Transformation Context having the XDOM of the passed
                // document so that macros execute on the passed document's XDOM (e.g. the TOC macro will generate
                // the toc for the passed document instead of the current document).
                DocumentModelBridge referencedDoc =
                    this.documentAccessBridge.getTranslatedDocumentInstance(referencedDocReference);
                XDOM referencedXDOM = referencedDoc.getXDOM();

                if (parameters.getTransformationContext() == TransformationContextMode.TRANSFORMATIONS) {
                    // Get the XDOM from the referenced doc but with Transformations applied so that all macro are
                    // executed and contribute XDOM elements.
                    // IMPORTANT: This can be dangerous since it means executing macros, and thus also script macros
                    // defined in the referenced document. To be used with caution.
                    TransformationContext referencedTxContext =
                        new TransformationContext(referencedXDOM, referencedDoc.getSyntax());
                    this.transformationManager.performTransformations(referencedXDOM, referencedTxContext);
                }

                return referencedXDOM;
            }
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to resolve the XDOM to use in the transformation", e);
        }

        return null;
    }

    @Override
    public List<Block> execute(ContextMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        if (parameters.getDocument() == null) {
            throw new MacroExecutionException("You must specify a 'document' parameter pointing to the document to "
                + "set in the context as the current document.");
        }

        DocumentReference currentAuthor = this.documentAccessBridge.getCurrentAuthorReference();
        DocumentReference referencedDocReference =
            this.macroReferenceResolver.resolve(parameters.getDocument(), context.getCurrentMacroBlock());

        // Make sure the author is allowed to use the target document
        checkAccess(currentAuthor, referencedDocReference);

        MetaData metadata = new MetaData();
        metadata.addMetaData(MetaData.SOURCE, parameters.getDocument());
        metadata.addMetaData(MetaData.BASE, parameters.getDocument());

        XDOM xdom = this.parser.parse(content, context, false, metadata, context.isInline());

        if (xdom.getChildren().isEmpty()) {
            return Collections.emptyList();
        }

        // Reuse the very generic async rendering framework (even if we don't do async and caching) since it's taking
        // care of many other things
        BlockAsyncRendererConfiguration configuration = createBlockAsyncRendererConfiguration(null, xdom, context);

        Map<String, Object> backupObjects = new HashMap<>();
        try {
            // Switch the context document
            this.documentAccessBridge.pushDocumentInContext(backupObjects, referencedDocReference);

            // Configure the Transformation Context XDOM depending on the mode asked.
            configuration.setXDOM(getXDOM(referencedDocReference, parameters));

            // Execute the content
            Block result = this.executor.execute(configuration);

            // Keep metadata so that the result stay associated to context properties when inserted in the parent XDOM
            return Arrays.asList((Block) new MetaDataBlock(result.getChildren(), xdom.getMetaData()));
        } catch (Exception e) {
            throw new MacroExecutionException("Failed start the execution of the macro", e);
        } finally {
            // Restore the context document
            this.documentAccessBridge.popDocumentFromContext(backupObjects);
        }
    }
}
