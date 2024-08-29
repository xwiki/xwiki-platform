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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.ReferenceRenamer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.LinkParser;
import org.xwiki.rendering.listener.reference.ResourceReference;
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
import org.xwiki.rendering.transformation.RenderingContext;

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
    private Provider<LinkParser> linkParserProvider;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private RenderingContext renderingContext;

    @Inject
    private Logger logger;

    private boolean shouldMacroContentBeParsed(Macro<?> macro)
    {
        ContentDescriptor contentDescriptor = macro.getDescriptor().getContentDescriptor();
        return contentDescriptor != null && Block.LIST_BLOCK_TYPE.equals(contentDescriptor.getType());
    }

    private Macro<?> getMacro(MacroBlock macroBlock, Syntax syntax)
    {
        MacroId macroId = new MacroId(macroBlock.getId(), syntax);
        try {
            return this.macroManager.getMacro(macroId);
        } catch (MacroLookupException e) {
            // if the macro cannot be found or instantiated we shouldn't raise an exception, just ignore that macro.
            this.logger.debug("Cannot get macro with id [{}]: [{}]", macroId, ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    private MacroTransformationContext getTransformationContext(MacroBlock macroBlock)
    {
        MacroTransformationContext macroTransformationContext = new MacroTransformationContext();
        macroTransformationContext.setId("refactoring_" + macroBlock.getId());
        macroTransformationContext.setCurrentMacroBlock(macroBlock);
        // fallback syntax: macro content parser search by default for the XDOM syntax.
        macroTransformationContext.setSyntax(this.renderingContext.getDefaultSyntax());
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

    private XDOM parseMacro(MacroBlock macroBlock) throws MacroRefactoringException
    {
        MacroTransformationContext transformationContext = this.getTransformationContext(macroBlock);
        Syntax renderingSyntax = this.macroContentParser.getCurrentSyntax(transformationContext);
        Macro<?> macro = this.getMacro(macroBlock, renderingSyntax);
        // if the macro cannot be found or if it cannot be parsed, we don't consider it.
        if (macro != null && this.shouldMacroContentBeParsed(macro)) {
            try {
                return this.macroContentParser
                    .parse(macroBlock.getContent(), transformationContext, true, macroBlock.isInline());
            } catch (MacroExecutionException e) {
                throw new MacroRefactoringException("Error while parsing macro content for reference replacement", e);
            }
        }
        return null;
    }

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        DocumentReference sourceReference, DocumentReference targetReference, boolean relative)
        throws MacroRefactoringException
    {
        return innerReplaceReference(macroBlock,
            xdom -> this.referenceRenamerProvider.get().renameReferences(xdom, currentDocumentReference,
            sourceReference, targetReference, relative));
    }

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        AttachmentReference sourceReference, AttachmentReference targetReference, boolean relative)
        throws MacroRefactoringException
    {
        return innerReplaceReference(macroBlock,
            xdom -> this.referenceRenamerProvider.get().renameReferences(xdom, currentDocumentReference,
            sourceReference, targetReference, relative));
    }

    private Optional<MacroBlock> innerReplaceReference(MacroBlock macroBlock, Predicate<Block> lambda)
        throws MacroRefactoringException
    {
        XDOM xdom = this.parseMacro(macroBlock);
        MacroTransformationContext transformationContext = this.getTransformationContext(macroBlock);
        Syntax renderingSyntax = this.macroContentParser.getCurrentSyntax(transformationContext);
        if (xdom != null) {
            try {
                boolean updated = lambda.test(xdom);
                if (updated) {
                    return Optional.of(this.renderMacroBlock(macroBlock, xdom, renderingSyntax));
                }
            } catch (ComponentLookupException e) {
                throw new MacroRefactoringException("Error while rendering macro content after reference replacement ",
                    e);
            }
        }
        return Optional.empty();
    }

    @Override
    public Set<ResourceReference> extractReferences(MacroBlock macroBlock) throws MacroRefactoringException
    {
        XDOM xdom = this.parseMacro(macroBlock);
        if (xdom != null) {
            return this.linkParserProvider.get().extractReferences(xdom);
        } else {
            return Collections.emptySet();
        }
    }
}
