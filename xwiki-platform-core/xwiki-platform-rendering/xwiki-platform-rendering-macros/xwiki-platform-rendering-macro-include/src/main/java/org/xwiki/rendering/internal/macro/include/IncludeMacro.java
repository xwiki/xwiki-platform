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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.block.match.CompositeBlockMatcher;
import org.xwiki.rendering.block.match.MetadataBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.include.IncludeMacroParameters;
import org.xwiki.rendering.macro.include.IncludeMacroParameters.Context;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.TransformationContext;

/**
 * @version $Id$
 * @since 1.5M2
 */
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
     * Used to get the current context that we clone if the users asks to execute the included page in its own context.
     */
    @Inject
    private Execution execution;

    /**
     * Used in order to clone the execution context when the user asks to execute the included page in its own context.
     */
    @Inject
    private ExecutionContextManager executionContextManager;

    /**
     * Used to access document content and check view access right.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to transform the passed document reference macro parameter to a typed {@link DocumentReference} object.
     */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    /**
     * Used to serialize resolved document links into a string again since the Rendering API only manipulates Strings
     * (done voluntarily to be independent of any wiki engine and not draw XWiki-specific dependencies).
     */
    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

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
    public List<Block> execute(IncludeMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        // Step 1: Perform checks
        if (parameters.getDocument() == null) {
            throw new MacroExecutionException(
                "You must specify a 'document' parameter pointing to the document to include.");
        }

        DocumentReference includedReference = resolve(context.getCurrentMacroBlock(), parameters.getDocument());

        checkRecursiveInclusion(context.getCurrentMacroBlock(), includedReference);

        if (!this.documentAccessBridge.isDocumentViewable(includedReference)) {
            throw new MacroExecutionException("Current user [" + this.documentAccessBridge.getCurrentUser()
                + "] doesn't have view rights on document ["
                + this.defaultEntityReferenceSerializer.serialize(includedReference) + "]");
        }

        Context parametersContext = parameters.getContext();

        // Step 2: Extract included document's content and syntax.
        DocumentModelBridge documentBridge;
        try {
            documentBridge = this.documentAccessBridge.getDocument(includedReference);
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to load Document ["
                + this.defaultEntityReferenceSerializer.serialize(includedReference) + "]", e);
        }
        Syntax includedSyntax = documentBridge.getSyntax();
        XDOM includedContent = getContent(documentBridge, parameters.getSection(), includedReference);

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
        XDOM result;
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

        // Step 4: Wrap Blocks in a MetaDataBlock with the "source" metadata specified so that potential relative
        // links/images are resolved correctly at render time.
        MetaDataBlock metadata = new MetaDataBlock(result.getChildren(), result.getMetaData());
        metadata.getMetaData().addMetaData(MetaData.SOURCE, parameters.getDocument());

        return Arrays.<Block>asList(metadata);
    }

    /**
     * Get the content to include (either full target document or a specific section's content).
     *
     * @param document the reference to the document from which to get the content
     * @param section the id of the section from which to get the content in that document or null to take the whole
     *        content
     * @param includedReference the resolved absolute reference of the included document
     * @return the content as an XDOM tree
     * @throws MacroExecutionException if no section of the passed if exists in the included document
     */
    private XDOM getContent(DocumentModelBridge document, final String section, DocumentReference includedReference)
        throws MacroExecutionException
    {
        XDOM includedContent = document.getXDOM();

        if (section != null) {
            HeaderBlock headerBlock = (HeaderBlock) includedContent.getFirstBlock(
                new CompositeBlockMatcher(new ClassBlockMatcher(HeaderBlock.class), new BlockMatcher() {
                    public boolean match(Block block)
                    {
                        return ((HeaderBlock) block).getId().equals(section);
                    }
                }),Block.Axes.DESCENDANT);
            if (headerBlock == null) {
                throw new MacroExecutionException("Cannot find section [" + section
                    + "] in document [" + this.defaultEntityReferenceSerializer.serialize(includedReference) + "]");
            } else {
                includedContent = new XDOM(headerBlock.getSection().getChildren(), includedContent.getMetaData());
            }
        }

        return includedContent;
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
     * @param block the block from which to look for a MetaData Block containing the Source
     * @param documentName the document reference passed by the user to the macro
     * @return the resolved absolute document reference
     */
    private DocumentReference resolve(Block block, String documentName)
    {
        DocumentReference result;

        MetaDataBlock metaDataBlock =
            (MetaDataBlock) block.getFirstBlock(new MetadataBlockMatcher(MetaData.SOURCE), Block.Axes.ANCESTOR);

        // If no Source MetaData was found resolve against the current document as a failsafe solution.
        if (metaDataBlock == null) {
            result = this.currentDocumentReferenceResolver.resolve(documentName);
        } else {
            String sourceMetaData = (String) metaDataBlock.getMetaData().getMetaData(MetaData.SOURCE);
            result =
                this.currentDocumentReferenceResolver.resolve(documentName,
                    this.currentDocumentReferenceResolver.resolve(sourceMetaData));
        }

        return result;
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
    private XDOM executeWithNewContext(DocumentReference includedDocumentReference, XDOM includedContent,
        MacroTransformationContext macroContext) throws MacroExecutionException
    {
        XDOM result;

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
    private XDOM executeWithCurrentContext(DocumentReference includedDocumentReference, XDOM includedContent,
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
    private XDOM generateIncludedPageDOM(DocumentReference includedDocumentReference, XDOM includedContent,
        MacroTransformationContext macroContext, boolean transform) throws MacroExecutionException
    {
        XDOM result;

        if (transform && macroContext.getTransformation() != null) {
            // Make sure we clone the XDOM since the transformation is going to modify it and we don't want the
            // original XDOM to carry away the changes.
            XDOM clonedContent = includedContent.clone();
            TransformationContext txContext = new TransformationContext(clonedContent, macroContext.getSyntax());
            txContext.setId(macroContext.getId());
            try {
                macroContext.getTransformation().transform(clonedContent, txContext);
            } catch (Exception e) {
                throw new MacroExecutionException("Failed to include page [" + includedDocumentReference + "]", e);
            }
            result = clonedContent;
        } else {
            result = includedContent;
        }

        return result;
    }
}
