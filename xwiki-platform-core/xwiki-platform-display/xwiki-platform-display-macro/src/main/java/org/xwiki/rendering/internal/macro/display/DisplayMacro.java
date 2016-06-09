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
package org.xwiki.rendering.internal.macro.display;

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
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.display.DisplayMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.text.StringUtils;

/**
 * @version $Id$
 * @since 3.4M1
 */
// TODO: add support for others entity types (not only document). Mainly require more generic displayer API.
@Component
@Named("display")
@Singleton
public class DisplayMacro extends AbstractMacro<DisplayMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Display other pages into the current page.";

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
    private ThreadLocal<Stack<Object>> displaysBeingExecuted = new ThreadLocal<Stack<Object>>();

    /**
     * Default constructor.
     */
    public DisplayMacro()
    {
        super("Display", DESCRIPTION, DisplayMacroParameters.class);

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

    @Override
    public List<Block> execute(DisplayMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Step 1: Perform checks.
        if (StringUtils.isBlank(parameters.getReference())) {
            throw new MacroExecutionException(
                "You must specify a 'reference' parameter pointing to the entity to display.");
        }

        DocumentReference includedReference = resolve(context.getCurrentMacroBlock(), parameters);

        checkRecursiveDisplay(context.getCurrentMacroBlock(), includedReference);

        if (!this.documentAccessBridge.isDocumentViewable(includedReference)) {
            throw new MacroExecutionException("Current user [" + this.documentAccessBridge.getCurrentUserReference()
                + "] doesn't have view rights on document ["
                + this.defaultEntityReferenceSerializer.serialize(includedReference) + "]");
        }

        // Step 2: Retrieve the included document.
        DocumentModelBridge documentBridge;
        try {
            documentBridge = this.documentAccessBridge.getDocument(includedReference);
        } catch (Exception e) {
            throw new MacroExecutionException(
                "Failed to load Document [" + this.defaultEntityReferenceSerializer.serialize(includedReference) + "]",
                e);
        }

        // Step 3: Display the content of the included document.
        // Display the content in an isolated execution and transformation context.
        DocumentDisplayerParameters displayParameters = new DocumentDisplayerParameters();
        displayParameters.setContentTransformed(true);
        displayParameters.setExecutionContextIsolated(displayParameters.isContentTransformed());
        displayParameters.setSectionId(parameters.getSection());
        displayParameters.setTransformationContextIsolated(displayParameters.isContentTransformed());
        displayParameters.setTargetSyntax(context.getTransformationContext().getTargetSyntax());

        Stack<Object> references = this.displaysBeingExecuted.get();
        if (references == null) {
            references = new Stack<Object>();
            this.displaysBeingExecuted.set(references);
        }
        references.push(includedReference);

        XDOM result;
        try {
            result = this.documentDisplayer.display(documentBridge, displayParameters);
        } catch (Exception e) {
            throw new MacroExecutionException(e.getMessage(), e);
        } finally {
            references.pop();
        }

        // Step 4: Wrap Blocks in a MetaDataBlock with the "source" meta data specified so that we know from where the
        // content comes and "base" meta data so that reference are properly resolved
        MetaDataBlock metadata = new MetaDataBlock(result.getChildren(), result.getMetaData());
        String source = this.defaultEntityReferenceSerializer.serialize(includedReference);
        metadata.getMetaData().addMetaData(MetaData.SOURCE, source);
        metadata.getMetaData().addMetaData(MetaData.BASE, source);

        return Arrays.<Block>asList(metadata);
    }

    /**
     * Protect form recursive display.
     * 
     * @param currrentBlock the child block to check
     * @param documentReference the reference of the document being included
     * @throws MacroExecutionException recursive inclusion has been found
     */
    private void checkRecursiveDisplay(Block currrentBlock, DocumentReference documentReference)
        throws MacroExecutionException
    {
        // Try to find recursion in the thread
        Stack<Object> references = this.displaysBeingExecuted.get();
        if (references != null && references.contains(documentReference)) {
            throw new MacroExecutionException("Found recursive display of document [" + documentReference + "]");
        }
    }

    private DocumentReference resolve(MacroBlock block, DisplayMacroParameters parameters)
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
