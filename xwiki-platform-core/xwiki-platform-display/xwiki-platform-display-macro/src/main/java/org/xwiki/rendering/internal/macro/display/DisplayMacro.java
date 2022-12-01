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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.include.AbstractIncludeMacro;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.display.DisplayMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.Right;

/**
 * @version $Id$
 * @since 3.4M1
 */
// TODO: add support for others entity types (not only document). Mainly require more generic displayer API.
@Component
@Named("display")
@Singleton
public class DisplayMacro extends AbstractIncludeMacro<DisplayMacroParameters>
{
    private static final String DISPLAY = "display";

    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Display other pages into the current page.";

    /**
     * Default constructor.
     */
    public DisplayMacro()
    {
        super("Display", DESCRIPTION, DisplayMacroParameters.class);

        setDefaultCategories(Set.of(DEFAULT_CATEGORY_CONTENT));
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public List<Block> execute(DisplayMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Step 1: Perform checks.
        EntityReference displayedReference = resolve(context.getCurrentMacroBlock(), parameters.getReference(),
            parameters.getType(), DISPLAY);
        checkRecursion(displayedReference, DISPLAY);

        // Step 2: Retrieve the document to display.
        DocumentModelBridge documentBridge;
        try {
            documentBridge = this.documentAccessBridge.getDocumentInstance(displayedReference);
        } catch (Exception e) {
            throw new MacroExecutionException(
                "Failed to load Document [" + this.defaultEntityReferenceSerializer.serialize(displayedReference) + "]",
                e);
        }

        // Step 3: Check right
        if (!this.contextualAuthorization.hasAccess(Right.VIEW, documentBridge.getDocumentReference())) {
            throw new MacroExecutionException(
                String.format("Current user [%s] doesn't have view rights on document [%s]",
                    this.documentAccessBridge.getCurrentUserReference(), documentBridge.getDocumentReference()));
        }

        // Step 4: Display the content of the displayed document.
        // Display the content in an isolated execution and transformation context.
        DocumentDisplayerParameters displayParameters = new DocumentDisplayerParameters();
        displayParameters.setContentTransformed(true);
        displayParameters.setExecutionContextIsolated(displayParameters.isContentTransformed());
        displayParameters.setSectionId(parameters.getSection());
        displayParameters.setTransformationContextIsolated(displayParameters.isContentTransformed());
        displayParameters.setTargetSyntax(context.getTransformationContext().getTargetSyntax());
        displayParameters.setContentTranslated(true);
        if (context.getXDOM() != null) {
            displayParameters.setIdGenerator(context.getXDOM().getIdGenerator());
        }

        Stack<Object> references = this.macrosBeingExecuted.get();
        if (references == null) {
            references = new Stack<>();
            this.macrosBeingExecuted.set(references);
        }
        references.push(documentBridge.getDocumentReference());

        XDOM result;
        try {
            result = this.documentDisplayer.display(documentBridge, displayParameters);
        } catch (Exception e) {
            throw new MacroExecutionException(e.getMessage(), e);
        } finally {
            references.pop();
            if (references.isEmpty()) {
                // Get rid of the current ThreadLocal if not needed anymore
                this.macrosBeingExecuted.remove();
            }
        }

        // Step 5: If the user has asked for it, remove both Section and Heading Blocks if the first displayed block is
        // a Section block with a Heading block inside.
        if (parameters.isExcludeFirstHeading()) {
            excludeFirstHeading(result);
        }

        // Step 6: Wrap Blocks in a MetaDataBlock with the "source" meta data specified so that we know from where the
        // content comes and "base" meta data so that reference are properly resolved
        MetaDataBlock metadata = new MetaDataBlock(result.getChildren(), result.getMetaData());
        // Serialize the document reference since that's what is expected in those properties
        // TODO: add support for more generic source and base reference (object property reference, etc.)
        String source = this.defaultEntityReferenceSerializer.serialize(documentBridge.getDocumentReference());
        metadata.getMetaData().addMetaData(MetaData.SOURCE, source);
        metadata.getMetaData().addMetaData(MetaData.BASE, source);

        return Collections.singletonList(metadata);
    }
}
