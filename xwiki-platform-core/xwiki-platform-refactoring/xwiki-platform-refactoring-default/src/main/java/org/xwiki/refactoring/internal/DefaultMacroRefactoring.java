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
package org.xwiki.refactoring.internal;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.util.ReferenceRenamer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.MacroRefactoring;
import org.xwiki.rendering.macro.MacroRefactoringException;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Default implementation of {@link MacroRefactoring}.
 * This implementation tries to parse the macro content if it's of type
 * {@link org.xwiki.rendering.block.Block#LIST_BLOCK_TYPE} and replace the link and image references.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@Component
@Singleton
public class DefaultMacroRefactoring implements MacroRefactoring
{
    @Inject
    private MacroManager macroManager;

    @Inject
    private MacroContentParser macroContentParser;

    @Inject
    private Provider<ReferenceRenamer> referenceRenamerProvider;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    private boolean shouldMacroContentBeParsed(Macro<?> macro)
    {
        ContentDescriptor contentDescriptor = macro.getDescriptor().getContentDescriptor();
        return Block.LIST_BLOCK_TYPE.equals(contentDescriptor.getType());
    }

    private Macro<?> getMacro(MacroBlock macroBlock, Syntax syntax) throws MacroRefactoringException
    {
        MacroId macroId = new MacroId(macroBlock.getId(), syntax);
        try {
            return this.macroManager.getMacro(macroId);
        } catch (MacroLookupException e) {
            throw new MacroRefactoringException(String.format("Error while getting macro [%s]", macroId), e);
        }
    }

    private MacroTransformationContext getTransformationContext(MacroBlock macroBlock, Syntax syntax)
    {
        MacroTransformationContext macroTransformationContext = new MacroTransformationContext();
        macroTransformationContext.setId("refactoring_" + macroBlock.getId());
        macroTransformationContext.setCurrentMacroBlock(macroBlock);
        // fallback syntax: macro content parser search by default for the XDOM syntax.
        macroTransformationContext.setSyntax(syntax);
        macroTransformationContext.setInline(macroBlock.isInline());
        return macroTransformationContext;
    }

    private MacroBlock renderMacroBlock(MacroBlock originalBlock, XDOM newXDOMContent, Syntax syntax)
        throws ComponentLookupException
    {
        BlockRenderer renderer = this.componentManager.getInstance(BlockRenderer.class, syntax.toIdString());
        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        renderer.render(newXDOMContent, printer);
        String content = printer.toString();
        return new MacroBlock(originalBlock.getId(), originalBlock.getParameters(), content, originalBlock.isInline());
    }

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference sourceReference,
        DocumentReference targetReference, DocumentReference currentDocumentReference, Syntax syntax, boolean relative)
        throws MacroRefactoringException
    {
        Macro<?> macro = this.getMacro(macroBlock, syntax);
        if (this.shouldMacroContentBeParsed(macro)) {
            try {
                MacroTransformationContext transformationContext = this.getTransformationContext(macroBlock, syntax);
                XDOM xdom = this.macroContentParser
                    .parse(macroBlock.getContent(), transformationContext, true, macroBlock.isInline());
                boolean updated = this.referenceRenamerProvider.get().renameReferences(xdom, currentDocumentReference,
                    sourceReference, targetReference, relative);
                if (updated) {
                    Syntax renderingSyntax = this.macroContentParser.getCurrentSyntax(transformationContext);
                    return Optional.of(this.renderMacroBlock(macroBlock, xdom, renderingSyntax));
                }
            } catch (MacroExecutionException e) {
                throw new MacroRefactoringException("Error while parsing macro content for reference replacement", e);
            } catch (ComponentLookupException e) {
                throw new MacroRefactoringException("Error while rendering macro content after reference replacement ",
                    e);
            }
        }
        return Optional.empty();
    }
}
