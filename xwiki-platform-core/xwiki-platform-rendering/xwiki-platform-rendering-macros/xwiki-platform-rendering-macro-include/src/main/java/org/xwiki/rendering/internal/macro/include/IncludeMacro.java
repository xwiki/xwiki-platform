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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.BeanManager;
import org.xwiki.properties.PropertyException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.include.IncludeMacroParameters;
import org.xwiki.rendering.macro.include.IncludeMacroParameters.Context;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * @version $Id$
 * @since 1.5M2
 */
// TODO: add support for others entity types (not only document and page). Mainly require more generic displayer API.
@Component
@Named("include")
@Singleton
public class IncludeMacro extends AbstractMacro<IncludeMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Include other pages into the current page.";

    /**
     * Used to access document content and check view access right.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private ContextualAuthorizationManager authorization;

    /**
     * Used to transform the passed reference macro parameter into a complete {@link DocumentReference} one.
     */
    @Inject
    @Named("macro")
    private EntityReferenceResolver<String> macroEntityReferenceResolver;

    /**
     * Used to serialize resolved document links into a string again since the Rendering API only manipulates Strings
     * (done voluntarily to be independent of any wiki engine and not draw XWiki-specific dependencies).
     */
    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * Used to display the content of the included document.
     */
    @Inject
    @Named("configured")
    private DocumentDisplayer documentDisplayer;

    @Inject
    private BeanManager beans;

    /**
     * A stack of all currently executing include macros with context=new for catching recursive inclusion.
     */
    private ThreadLocal<Stack<Object>> inclusionsBeingExecuted = new ThreadLocal<>();

    /**
     * Default constructor.
     */
    public IncludeMacro()
    {
        super("Include", DESCRIPTION, IncludeMacroParameters.class);

        // The include macro must execute first since if it runs with the current context it needs to bring
        // all the macros from the included page before the other macros are executed.
        setPriority(10);
        setDefaultCategory(DEFAULT_CATEGORY_CONTENT);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    /**
     * Allows overriding the Document Access Bridge used (useful for unit tests).
     * 
     * @param documentAccessBridge the new Document Access Bridge to use
     */
    public void setDocumentAccessBridge(DocumentAccessBridge documentAccessBridge)
    {
        this.documentAccessBridge = documentAccessBridge;
    }

    /**
     * Allows overriding the Document Displayer used (useful for unit tests).
     * 
     * @param documentDisplayer the new Document Displayer to use
     */
    public void setDocumentDisplayer(DocumentDisplayer documentDisplayer)
    {
        this.documentDisplayer = documentDisplayer;
    }

    @Override
    public List<Block> execute(IncludeMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Step 1: Perform checks.
        if (parameters.getReference() == null) {
            throw new MacroExecutionException(
                "You must specify a 'reference' parameter pointing to the entity to include.");
        }

        EntityReference includedReference = resolve(context.getCurrentMacroBlock(), parameters);

        checkRecursiveInclusion(context.getCurrentMacroBlock(), includedReference);

        Context parametersContext = parameters.getContext();

        // Step 2: Retrieve the included document.
        DocumentModelBridge documentBridge;
        try {
            documentBridge = this.documentAccessBridge.getDocumentInstance(includedReference);
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to load Document [" + includedReference + "]", e);
        }

        // Step 3: Check right
        if (!this.authorization.hasAccess(Right.VIEW, documentBridge.getDocumentReference())) {
            throw new MacroExecutionException(
                String.format("Current user [%s] doesn't have view rights on document [%s]",
                    this.documentAccessBridge.getCurrentUserReference(), includedReference));
        }

        // Step 4: Display the content of the included document.

        // Check the value of the "context" parameter.
        //
        // If CONTEXT_NEW then display the content in an isolated execution and transformation context.
        //
        // if CONTEXT_CURRENT then display the content without performing any transformations (we don't want any Macro
        // to be executed at this stage since they should be executed by the currently running Macro Transformation.
        DocumentDisplayerParameters displayParameters = new DocumentDisplayerParameters();
        displayParameters.setContentTransformed(parametersContext == Context.NEW);
        displayParameters.setExecutionContextIsolated(displayParameters.isContentTransformed());
        displayParameters.setSectionId(parameters.getSection());
        displayParameters.setTransformationContextIsolated(displayParameters.isContentTransformed());
        displayParameters.setTransformationContextRestricted(context.getTransformationContext().isRestricted());
        displayParameters.setTargetSyntax(context.getTransformationContext().getTargetSyntax());
        displayParameters.setContentTranslated(true);

        Stack<Object> references = this.inclusionsBeingExecuted.get();
        if (parametersContext == Context.NEW) {
            if (references == null) {
                references = new Stack<>();
                this.inclusionsBeingExecuted.set(references);
            }
            references.push(includedReference);
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

        // Step 5: Wrap Blocks in a MetaDataBlock with the "source" meta data specified so that we know from where the
        // content comes and "base" meta data so that reference are properly resolved
        MetaDataBlock metadata = new MetaDataBlock(result.getChildren(), result.getMetaData());
        String source = this.defaultEntityReferenceSerializer.serialize(includedReference);
        metadata.getMetaData().addMetaData(MetaData.SOURCE, source);
        if (parametersContext == Context.NEW) {
            metadata.getMetaData().addMetaData(MetaData.BASE, source);
        }

        return Arrays.asList(metadata);
    }

    /**
     * Protect form recursive inclusion.
     * 
     * @param currrentBlock the child block to check
     * @param reference the reference of the document being included
     * @throws MacroExecutionException recursive inclusion has been found
     */
    private void checkRecursiveInclusion(Block currrentBlock, EntityReference reference) throws MacroExecutionException
    {
        // Check for parent context=new macros
        Stack<Object> references = this.inclusionsBeingExecuted.get();
        if (references != null && references.contains(reference)) {
            throw new MacroExecutionException("Found recursive inclusion of document [" + reference + "]");
        }

        // Check for parent context=current macros
        Block parentBlock = currrentBlock.getParent();

        if (parentBlock != null) {
            if (parentBlock instanceof MacroMarkerBlock) {
                MacroMarkerBlock parentMacro = (MacroMarkerBlock) parentBlock;

                if (isRecursive(parentMacro, reference)) {
                    throw new MacroExecutionException("Found recursive inclusion of document [" + reference + "]");
                }
            }

            checkRecursiveInclusion(parentBlock, reference);
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

    private EntityReference resolve(MacroBlock block, IncludeMacroParameters parameters) throws MacroExecutionException
    {
        String reference = parameters.getReference();

        if (reference == null) {
            throw new MacroExecutionException(
                "You must specify a 'reference' parameter pointing to the entity to include.");
        }

        return this.macroEntityReferenceResolver.resolve(reference, parameters.getType(), block);
    }
}
