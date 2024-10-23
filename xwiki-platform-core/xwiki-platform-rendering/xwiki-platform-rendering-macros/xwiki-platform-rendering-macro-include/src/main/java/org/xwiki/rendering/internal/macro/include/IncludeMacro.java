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
package org.xwiki.rendering.internal.macro.include;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.properties.BeanManager;
import org.xwiki.properties.PropertyException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.include.IncludeMacroParameters;
import org.xwiki.rendering.macro.include.IncludeMacroParameters.Author;
import org.xwiki.rendering.macro.include.IncludeMacroParameters.Context;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.rendering.util.ParserUtils;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * @version $Id$
 * @since 1.5M2
 */
// TODO: add support for others entity types (not only document and page). Mainly require more generic displayer API.
@Component
@Named("include")
@Singleton
public class IncludeMacro extends AbstractIncludeMacro<IncludeMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Include other pages into the current page.";

    @Inject
    private BeanManager beans;

    @Inject
    private TransformationManager transformationManager;

    @Inject
    private AuthorExecutor authorExecutor;

    @Inject
    protected AuthorizationManager authorization;

    /**
     * Default constructor.
     */
    public IncludeMacro()
    {
        super("Include", DESCRIPTION, IncludeMacroParameters.class);

        setDefaultCategories(Set.of(DEFAULT_CATEGORY_CONTENT));
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public List<Block> execute(IncludeMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Step 1: Perform checks.
        EntityReference includedReference =
            resolve(context.getCurrentMacroBlock(), parameters.getReference(), parameters.getType(), "include");
        checkRecursion(context.getCurrentMacroBlock(), includedReference);

        // Step 2: Retrieve the included document.
        DocumentModelBridge documentBridge;
        try {
            documentBridge = this.documentAccessBridge.getDocumentInstance(includedReference);
        } catch (Exception e) {
            throw new MacroExecutionException(
                "Failed to load Document [" + this.defaultEntityReferenceSerializer.serialize(includedReference) + "]",
                e);
        }

        // Step 3: Check right
        if (!this.contextualAuthorization.hasAccess(Right.VIEW, documentBridge.getDocumentReference())) {
            throw new MacroExecutionException(
                String.format("Current user [%s] doesn't have view rights on document [%s]",
                    this.documentAccessBridge.getCurrentUserReference(), documentBridge.getDocumentReference()));
        }

        // Step 4: Display the content of the included document.

        // Check the value of the "context" parameter.
        //
        // If CONTEXT_NEW then display the content in an isolated execution and transformation context.
        //
        // if CONTEXT_CURRENT then display the content without performing any transformations (we don't want any Macro
        // to be executed at this stage since they should be executed by the currently running Macro Transformation.
        Context parametersContext = parameters.getContext();
        DocumentDisplayerParameters displayParameters = new DocumentDisplayerParameters();
        displayParameters.setContentTransformed(parametersContext == Context.NEW);
        displayParameters.setExecutionContextIsolated(displayParameters.isContentTransformed());
        displayParameters.setSectionId(parameters.getSection());
        displayParameters.setTransformationContextIsolated(displayParameters.isContentTransformed());
        displayParameters.setTransformationContextRestricted(context.getTransformationContext().isRestricted());
        displayParameters.setTargetSyntax(context.getTransformationContext().getTargetSyntax());
        displayParameters.setContentTranslated(true);
        if (context.getXDOM() != null) {
            displayParameters.setIdGenerator(context.getXDOM().getIdGenerator());
        }

        Stack<Object> references = this.macrosBeingExecuted.get();
        if (parametersContext == Context.NEW) {
            if (references == null) {
                references = new Stack<>();
                this.macrosBeingExecuted.set(references);
            }
            references.push(documentBridge.getDocumentReference());
        }

        XDOM result;
        try {
            result = this.documentDisplayer.display(documentBridge, displayParameters);
        } catch (Exception e) {
            throw new MacroExecutionException(e.getMessage(), e);
        } finally {
            if (parametersContext == Context.NEW) {
                references.pop();
            }
        }

        // Step 5: If the user has asked for it, remove both Section and Heading Blocks if the first included block is
        // a Section block with a Heading block inside.
        if (parameters.isExcludeFirstHeading()) {
            excludeFirstHeading(result);
        }

        // Step 5a: if the macro is in an inline context, try converting the included content to inline.
        if (context.isInline()) {
            result = (XDOM) (new ParserUtils()).convertToInline(result, true);
        }

        // Step 6: Wrap Blocks in a MetaDataBlock with the "source" meta data specified so that we know from where the
        // content comes and "base" meta data so that reference are properly resolved
        MetaDataBlock metadata = new MetaDataBlock(result.getChildren(), result.getMetaData());
        // Serialize the document reference since that's what is expected in those properties
        // TODO: add support for more generic source and base reference (object property reference, etc.)
        String source = this.defaultEntityReferenceSerializer.serialize(documentBridge.getDocumentReference());
        metadata.getMetaData().addMetaData(MetaData.SOURCE, source);
        if (parametersContext == Context.NEW) {
            metadata.getMetaData().addMetaData(MetaData.BASE, source);
        }

        if (parametersContext == Context.CURRENT) {
            // Step 7: If the include macro is explicitly configured to be executed with the included document content
            // author or if that author does not have programming right, execute it right away
            // Get the translated version of the document to get the content author
            DocumentModelBridge translatedDocumentBridge;
            try {
                translatedDocumentBridge = this.documentAccessBridge.getTranslatedDocumentInstance(documentBridge);
            } catch (Exception e) {
                throw new MacroExecutionException("Failed to retrieve the translated version of the document", e);
            }
            if (parameters.getAuthor() == Author.TARGET || parameters.getAuthor() == Author.AUTO && !this.authorization
                .hasAccess(Right.PROGRAM, translatedDocumentBridge.getContentAuthorReference(), null)) {
                // Merge the two XDOM before executing the included content so that it's as close as possible to the
                // expect execution conditions
                MacroBlock includeMacro = context.getCurrentMacroBlock();
                MacroMarkerBlock includeMacroMarker = new MacroMarkerBlock(includeMacro.getId(),
                    includeMacro.getParameters(), Collections.singletonList(metadata), includeMacro.isInline());
                includeMacro.getParent().replaceChild(includeMacroMarker, includeMacro);

                try {
                    // Execute the content with the right author
                    // Keep the same transformation context
                    this.authorExecutor.call(() -> {
                        this.transformationManager.performTransformations(metadata, context.getTransformationContext());
                        return null;
                    }, translatedDocumentBridge.getContentAuthorReference(), documentBridge.getDocumentReference());
                } catch (Exception e) {
                    throw new MacroExecutionException("Failed to execute tranformations for document ["
                        + translatedDocumentBridge.getDocumentReference() + "]");
                } finally {
                    // Put back the macro in the main XDOM (it will be replaced that the current macro transformation)
                    includeMacroMarker.getParent().replaceChild(includeMacro, includeMacroMarker);
                }
            }
        }

        return Collections.singletonList(metadata);
    }

    /**
     * Protect form recursive inclusion.
     * 
     * @param currentBlock the child block to check
     * @param reference the reference of the document being included
     * @throws MacroExecutionException recursive inclusion has been found
     */
    protected void checkRecursion(Block currentBlock, EntityReference reference) throws MacroExecutionException
    {
        super.checkRecursion(reference, "inclusion");

        // Check for parent context=current macros
        Block parentBlock = currentBlock.getParent();

        if (parentBlock != null) {
            if (parentBlock instanceof MacroMarkerBlock) {
                MacroMarkerBlock parentMacro = (MacroMarkerBlock) parentBlock;

                if (isRecursive(parentMacro, reference)) {
                    throw new MacroExecutionException("Found recursive inclusion of document [" + reference + "]");
                }
            }

            checkRecursion(parentBlock, reference);
        }
    }

    /**
     * Indicate if the provided macro is an include macro with the provided included document.
     * 
     * @param parentMacro the macro block to check
     * @param completeReference the document reference to compare to
     * @return true if the documents are the same
     */
    // TODO: Add support for any kind of macro including content linked to a reference
    private boolean isRecursive(MacroMarkerBlock parentMacro, EntityReference completeReference)
    {
        if (parentMacro.getId().equals("include")) {
            IncludeMacroParameters macroParameters = getParameters(parentMacro.getParameters());

            return completeReference.equals(this.macroEntityReferenceResolver.resolve(macroParameters.getReference(),
                macroParameters.getType(), parentMacro));
        }

        return false;
    }

    private IncludeMacroParameters getParameters(Map<String, String> values)
    {
        IncludeMacroParameters parameters = new IncludeMacroParameters();

        try {
            this.beans.populate(parameters, values);
        } catch (PropertyException e) {
            // It should not not happen since it's a macro that was already executed
        }

        return parameters;
    }
}
