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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.MacroContentParser;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.include.IncludeMacroParameters;
import org.xwiki.rendering.macro.include.IncludeMacroParameters.Context;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * @version $Id$
 * @since 1.5M2
 */
@Component("include")
public class IncludeMacro extends AbstractMacro<IncludeMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Include other pages into the current page.";

    /**
     * Used to find the parser from syntax identifier.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Used to get the current context that we clone if the users asks to execute the included page in its own context.
     */
    @Requirement
    private Execution execution;

    /**
     * Used in order to clone the execution context when the user asks to execute the included page in its own context.
     */
    @Requirement
    private ExecutionContextManager executionContextManager;

    /**
     * Used to access document content and check view access right.
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to transform the passed document reference macro parameter to a typed {@link DocumentReference} object.
     */
    @Requirement("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    /**
     * Used to transform relative document links into absolute references relative to the included document.
     */
    @Requirement("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    /**
     * Used to transform relative attachment links into absolute references relative to the included document.
     */
    @Requirement("explicit")
    private AttachmentReferenceResolver<String> explicitAttachmentReferenceResolver;

    /**
     * Used to serialize resolved document links into a string again since the Rendering API only manipulates Strings
     * (done voluntarily to be independent of any wiki engine and not draw XWiki-specific dependencies).
     */
    @Requirement
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * The parser used to parse included document content.
     */
    @Requirement
    private MacroContentParser contentParser;

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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
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
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(IncludeMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Step 1: Perform checks
        if (parameters.getDocument() == null) {
            throw new MacroExecutionException(
                "You must specify a 'document' parameter pointing to the document to include.");
        }

        DocumentReference includedReference = resolve(context.getCurrentMacroBlock(), parameters.getDocument());

        if (context.getCurrentMacroBlock() != null) {
            checkRecursiveInclusion(context.getCurrentMacroBlock(), includedReference);
        }

        if (!this.documentAccessBridge.isDocumentViewable(includedReference)) {
            throw new MacroExecutionException("Current user doesn't have view rights on document ["
                + this.defaultEntityReferenceSerializer.serialize(includedReference) + "]");
        }

        Context parametersContext = parameters.getContext();

        // Step 2: Extract included document's content and syntax.
        // TODO: use macro source information to resolve document reference based on the macro source instead
        // of the context
        DocumentModelBridge documentBridge;
        try {
            documentBridge = this.documentAccessBridge.getDocument(includedReference);
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to load Document ["
                + this.defaultEntityReferenceSerializer.serialize(includedReference) + "]", e);
        }
        String includedContent = documentBridge.getContent();
        Syntax includedSyntax = documentBridge.getSyntax();

        // Step 3: Parse and transform the included document's content.

        // Check the value of the "context" parameter.
        //
        // If CONTEXT_NEW then get the included page content, parse it, apply Transformations to it and return the
        // resulting AST.
        // Note that we need to push a new Container Request before doing this so that the Velocity, Groovy and any
        // other scripting languages have an isolated execution context.
        //
        // if CONTEXT_CURRENT, then simply get the included page's content, parse it and return the resulting AST
        // (i.e. don't apply any transformations since we don't want any Macro to be executed at this stage since they
        // should be executed by the currently running Macro Transformation.
        List<Block> result;
        MacroTransformationContext newContext = context.clone();
        newContext.setSyntax(includedSyntax);
        if (parametersContext == Context.NEW) {
            // Since the execution happens in a separate context use a different transformation id to ensure it's
            // isolated (for ex this will ensure that the velocity macros defined in the included document cannot
            // interfere with the macros in the including document).
            newContext.setId(this.defaultEntityReferenceSerializer.serialize(includedReference));
            result = executeWithNewContext(includedReference, includedContent, newContext);
        } else {
            result = executeWithCurrentContext(includedReference, includedContent, newContext);
        }

        // Step 4: Modify relative references.
        resolveRelativeReferences(result, includedReference);

        return result;
    }

    /**
     * We need to handle the case when there are relative links specified in the content of the included document.
     * These link references need to be resolved against the document being included and not the including document.
     * TODO: When http://jira.xwiki.org/jira/browse/XWIKI-4802 is implemented it should be possible remove this
     * code portion and instead perform the resolution at render time, using context information.
     *
     * @param includedReference reference to the included document
     */
    private void resolveRelativeReferences(List<Block> blocks, DocumentReference includedReference)
    {
        XDOM xdom = new XDOM(blocks);

        // Resolve links
        for (LinkBlock block : xdom.getChildrenByType(LinkBlock.class, true)) {
            ResourceReference resourceReference = block.getReference();
            // Make reference absolute for links to document and attachments.
            if (resourceReference.getType().equals(ResourceType.DOCUMENT)
                || resourceReference.getType().equals(ResourceType.ATTACHMENT))
            {
                EntityReference entityReference;
                if (resourceReference.getType().equals(ResourceType.DOCUMENT)) {
                    entityReference = this.explicitDocumentReferenceResolver.resolve(resourceReference.getReference(),
                        includedReference);
                } else {
                    entityReference = this.explicitAttachmentReferenceResolver.resolve(resourceReference.getReference(),
                        includedReference);
                }
                String resolvedReference = this.defaultEntityReferenceSerializer.serialize(entityReference);
                resourceReference.setReference(resolvedReference);
            }
        }

        // Resolve images
        for (ImageBlock block : xdom.getChildrenByType(ImageBlock.class, true)) {
            ResourceReference resourceReference = block.getReference();
            // Make reference absolute for images in documents
            if (resourceReference.getType().equals(ResourceType.ATTACHMENT)) {
                String resolvedReference = this.defaultEntityReferenceSerializer.serialize(
                    this.explicitAttachmentReferenceResolver.resolve(resourceReference.getReference(),
                        includedReference));
                resourceReference.setReference(resolvedReference);
            }
        }
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
    private boolean isRecursive(MacroMarkerBlock parentMacro, DocumentReference documentReference)
    {
        if (parentMacro.getId().equals("include")) {
            DocumentReference parentDocumentReference = resolve(parentMacro, parentMacro.getParameter("document"));

            return documentReference.equals(parentDocumentReference);
        }

        return false;
    }

    /**
     * Convert document name into proper {@link DocumentReference}.
     * 
     * @param block the block from which to resolve the document reference
     * @param documentName the document name
     * @return the document reference
     */
    private DocumentReference resolve(Block block, String documentName)
    {
        // TODO: use macro source informations to resolve document reference besed on the macro source instead of the
        // context

        return this.currentDocumentReferenceResolver.resolve(documentName);
    }

    /**
     * Parse and execute target document content in a new context.
     * 
     * @param includedDocumentReference the name of the document to include.
     * @param includedContent the content of the document to include.
     * @param macroContext the transformation context to use to parse/transform the content
     * @return the result of parsing and transformation of the document to include.
     * @throws MacroExecutionException error when parsing content.
     */
    private List<Block> executeWithNewContext(DocumentReference includedDocumentReference, String includedContent,
        MacroTransformationContext macroContext) throws MacroExecutionException
    {
        List<Block> result;

        try {
            // Push new Execution Context to isolate the contexts (Velocity, Groovy, etc).
            ExecutionContext clonedEc = this.executionContextManager.clone(this.execution.getContext());

            this.execution.pushContext(clonedEc);

            Map<String, Object> backupObjects = new HashMap<String, Object>();
            try {
                this.documentAccessBridge.pushDocumentInContext(backupObjects, includedDocumentReference);
                result = generateIncludedPageDOM(includedDocumentReference, includedContent, macroContext, true);
            } finally {
                this.documentAccessBridge.popDocumentFromContext(backupObjects);
            }

        } catch (Exception e) {
            throw new MacroExecutionException("Failed to render page [" + includedDocumentReference
                + "] in new context", e);
        } finally {
            // Reset the Execution Context as before
            this.execution.popContext();
        }

        return result;
    }

    /**
     * Parse and execute target document content in a the current context.
     * 
     * @param includedDocumentReference the name of the document to include.
     * @param includedContent the content of the document to include.
     * @param macroContext the transformation context to use to parse the content
     * @return the result of parsing and transformation of the document to include.
     * @throws MacroExecutionException error when parsing content.
     */
    private List<Block> executeWithCurrentContext(DocumentReference includedDocumentReference, String includedContent,
        MacroTransformationContext macroContext) throws MacroExecutionException
    {
        return generateIncludedPageDOM(includedDocumentReference, includedContent, macroContext, false);
    }

    /**
     * Parse and execute target document content.
     * 
     * @param includedDocumentReference the name of the document to include.
     * @param includedContent the content of the document to include.
     * @param macroContext the transformation context to use to parse/transform the content
     * @param transform if true then execute transformations
     * @return the result of parsing and transformation of the document to include.
     * @throws MacroExecutionException error when parsing content.
     */
    private List<Block> generateIncludedPageDOM(DocumentReference includedDocumentReference, String includedContent,
        MacroTransformationContext macroContext, boolean transform) throws MacroExecutionException
    {
        List<Block> result;
        try {

            // Only run Macro transformation when the context is a new one as otherwise we need the macros in the
            // included page to be added to the list of macros on the including page so that they're all sorted
            // and executed in the right order. Note that this works only because the Include macro has the highest
            // execution priority and is thus executed first.
            result = this.contentParser.parse(includedContent, macroContext, transform, false);
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to parse included page [" + includedDocumentReference + "]", e);
        }

        return result;
    }
}
