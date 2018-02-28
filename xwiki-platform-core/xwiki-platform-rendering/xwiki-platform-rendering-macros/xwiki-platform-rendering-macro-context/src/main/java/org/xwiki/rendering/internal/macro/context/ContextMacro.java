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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.context.ContextMacroParameters;
import org.xwiki.rendering.macro.context.TransformationContextMode;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
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
public class ContextMacro extends AbstractMacro<ContextMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Executes content in the context of the passed document";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION = "The content to execute";

    /**
     * Used to set the current document in the context (old way) and check rights.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The parser used to parse macro content.
     */
    @Inject
    private MacroContentParser contentParser;

    /**
     * Used to transform document links into absolute references.
     */
    @Inject
    @Named("macro")
    private DocumentReferenceResolver<String> macroDocumentReferenceResolver;

    @Inject
    private TransformationManager transformationManager;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public ContextMacro()
    {
        super("Context", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION), ContextMacroParameters.class);

        // The Context macro must execute early since it can contain include macros which can bring stuff like headings
        // for other macros (TOC macro, etc). Make it the same priority as the Include macro.
        setPriority(10);
        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public List<Block> execute(ContextMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        if (parameters.getDocument() == null) {
            throw new MacroExecutionException("You must specify a 'document' parameter pointing to the document to "
                + "set in the context as the current document.");
        }

        DocumentReference referencedDocReference =
            this.macroDocumentReferenceResolver.resolve(parameters.getDocument(), context.getCurrentMacroBlock());

        boolean currentContextHasProgrammingRights = this.documentAccessBridge.hasProgrammingRights();

        List<Block> result;
        try {
            Map<String, Object> backupObjects = new HashMap<>();
            try {
                this.documentAccessBridge.pushDocumentInContext(backupObjects, referencedDocReference);

                // The current document is now the passed document. Check for programming rights for it. If it has
                // programming rights then the initial current document also needs programming right, else throw an
                // error since it would be a security breach otherwise.
                if (this.documentAccessBridge.hasProgrammingRights() && !currentContextHasProgrammingRights) {
                    throw new MacroExecutionException("Current document must have programming rights since the "
                        + "context document provided [" + parameters.getDocument() + "] has programming rights.");
                }

                MetaData metadata = new MetaData();
                metadata.addMetaData(MetaData.SOURCE, parameters.getDocument());
                metadata.addMetaData(MetaData.BASE, parameters.getDocument());

                XDOM xdom = this.contentParser.parse(content, context, false, metadata, false);

                // Configure the  Transformation Context depending on the mode asked.
                if (parameters.getTransformationContext() == TransformationContextMode.DOCUMENT
                    || parameters.getTransformationContext() == TransformationContextMode.TRANSFORMATIONS)
                {
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

                    // Now execute transformation on the context macro content but with the referenced XDOM in the
                    // Transformation context!
                    TransformationContext txContext =
                        new TransformationContext(referencedXDOM, referencedDoc.getSyntax());
                    this.transformationManager.performTransformations(xdom, txContext);
                }

                // Keep metadata so that the result stay associated to context properties when inserted in the parent
                // XDOM
                result = Arrays.asList((Block) new MetaDataBlock(xdom.getChildren(), xdom.getMetaData()));

            } finally {
                this.documentAccessBridge.popDocumentFromContext(backupObjects);
            }
        } catch (Exception e) {
            if (e instanceof MacroExecutionException) {
                throw (MacroExecutionException) e;
            } else {
                throw new MacroExecutionException(
                    String.format("Failed to render page in the context of [%s]", referencedDocReference), e);
            }
        }

        return result;
    }
}
