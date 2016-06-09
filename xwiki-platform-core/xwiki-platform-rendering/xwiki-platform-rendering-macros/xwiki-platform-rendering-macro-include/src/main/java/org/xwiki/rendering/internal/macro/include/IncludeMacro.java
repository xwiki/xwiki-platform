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
import java.util.HashSet;
import java.util.List;
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
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
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
import org.xwiki.text.StringUtils;

/**
 * @version $Id$
 * @since 1.5M2
 */
// TODO: add support for others entity types (not only document). Mainly require more generic displayer API.
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

    /**
     * Used to transform the passed document reference macro parameter into a typed {@link DocumentReference} object.
     */
    @Inject
    @Named("macro")
    private DocumentReferenceResolver<String> macroDocumentReferenceResolver;

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

    /**
     * A stack of all currently executing include macros with context=new for catching recursive inclusion.
     */
    private ThreadLocal<Stack<Object>> inclusionsBeingExecuted = new ThreadLocal<Stack<Object>>();

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
        if (StringUtils.isBlank(parameters.getReference())) {
            throw new MacroExecutionException(
                "You must specify a 'reference' parameter pointing to the entity to include.");
        }

        DocumentReference includedReference = resolve(context.getCurrentMacroBlock(), parameters);

        checkRecursiveInclusion(context.getCurrentMacroBlock(), includedReference);

        if (!this.documentAccessBridge.isDocumentViewable(includedReference)) {
            throw new MacroExecutionException(String.format(
                "Current user [%s] doesn't have view rights on document [%s]",
                this.documentAccessBridge.getCurrentUserReference(),
                this.defaultEntityReferenceSerializer.serialize(includedReference)));
        }

        Context parametersContext = parameters.getContext();

        // Step 2: Retrieve the included document.
        DocumentModelBridge documentBridge;
        try {
            documentBridge = this.documentAccessBridge.getDocument(includedReference);
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to load Document ["
                + this.defaultEntityReferenceSerializer.serialize(includedReference) + "]", e);
        }

        // Step 3: Display the content of the included document.

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

        Stack<Object> references = this.inclusionsBeingExecuted.get();
        if (parametersContext == Context.NEW) {
            if (references == null) {
                references = new Stack<Object>();
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

        // Step 4: Wrap Blocks in a MetaDataBlock with the "source" meta data specified so that we know from where the
        // content comes and "base" meta data so that reference are properly resolved
        MetaDataBlock metadata = new MetaDataBlock(result.getChildren(), result.getMetaData());
        String source = this.defaultEntityReferenceSerializer.serialize(includedReference);
        metadata.getMetaData().addMetaData(MetaData.SOURCE, source);
        if (parametersContext == Context.NEW) {
            metadata.getMetaData().addMetaData(MetaData.BASE, source);
        }

        return Arrays.<Block> asList(metadata);
    }

    /**
     * Protect form recursive inclusion.
     * 
     * @param currrentBlock the child block to check
     * @param documentReference the reference of the document being included
     * @throws MacroExecutionException recursive inclusion has been found
     */
    private void checkRecursiveInclusion(Block currrentBlock, DocumentReference documentReference)
        throws MacroExecutionException
    {
        // Check for parent context=new macros
        Stack<Object> references = this.inclusionsBeingExecuted.get();
        if (references != null && references.contains(documentReference)) {
            throw new MacroExecutionException("Found recursive inclusion of document [" + documentReference + "]");
        }

        // Check for parent context=current macros
        Block parentBlock = currrentBlock.getParent();

        if (parentBlock != null) {
            if (parentBlock instanceof MacroMarkerBlock) {
                MacroMarkerBlock parentMacro = (MacroMarkerBlock) parentBlock;

                if (isRecursive(parentMacro, documentReference)) {
                    throw new MacroExecutionException("Found recursive inclusion of document [" + documentReference
                        + "]");
                }
            }

            checkRecursiveInclusion(parentBlock, documentReference);
        }
    }

    /**
     * Indicate if the provided macro is an include macro wit the provided included document.
     * 
     * @param parentMacro the macro block to check
     * @param documentReference the document reference to compare to
     * @return true if the documents are the same
     */
    // TODO: Add support for any kind of macro including content linked to a reference
    private boolean isRecursive(MacroMarkerBlock parentMacro, DocumentReference documentReference)
    {
        if (parentMacro.getId().equals("include")) {
            String reference = parentMacro.getParameter("reference");
            if (reference == null) {
                reference = parentMacro.getParameter("document");
            }

            return documentReference.equals(this.macroDocumentReferenceResolver.resolve(reference, parentMacro));
        }

        return false;
    }

    private DocumentReference resolve(MacroBlock block, IncludeMacroParameters parameters)
        throws MacroExecutionException
    {
        // We have already checked previously that the reference is not empty
        String reference = parameters.getReference();

        // Parse that reference to get the real document reference
        DocumentReference documentReference = macroDocumentReferenceResolver.resolve(reference, block);

        // But the document might have a redirect object
        DocumentReference redirectClass = new DocumentReference(documentReference.getWikiReference().getName(),
                "XWiki", "RedirectClass");
        Object redirectLocation = documentAccessBridge.getProperty(documentReference, redirectClass, "location");
        HashSet<String> visitedLocations = new HashSet<>();
        // While we end-up on a document that have a redirect object, follow that redirection
        while (redirectLocation != null) {
            reference = (String) redirectLocation;
            if (!visitedLocations.add(reference)) {
                throw new MacroExecutionException(String.format("Infinite redirection recursion: [%s].",
                        parameters.getReference()));
            }
            documentReference = macroDocumentReferenceResolver.resolve(reference, block);
            redirectLocation = documentAccessBridge.getProperty(documentReference, redirectClass, "location");
        }

        return documentReference;
    }
}
