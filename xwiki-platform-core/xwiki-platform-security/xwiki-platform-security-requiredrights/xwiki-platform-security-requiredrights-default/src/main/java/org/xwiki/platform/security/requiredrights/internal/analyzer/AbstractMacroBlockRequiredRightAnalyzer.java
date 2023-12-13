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
package org.xwiki.platform.security.requiredrights.internal.analyzer;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.provider.BlockSupplierProvider;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.MetadataBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.RenderingContext;

/**
 * Abstract macro block analyzer that provides helper methods for handling macro blocks.
 *
 * @version $Id$
 * @since 15.9RC1
 */
public abstract class AbstractMacroBlockRequiredRightAnalyzer
{
    @Inject
    protected RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @Inject
    protected MacroContentParser macroContentParser;

    @Inject
    protected BlockSupplierProvider<MacroBlock> macroBlockBlockSupplierProvider;

    @Inject
    @Named("translation")
    protected BlockSupplierProvider<String> translationBlockSupplierProvider;

    @Inject
    private RenderingContext renderingContext;

    @Inject
    private MacroManager macroManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * Returns a MacroTransformationContext object based on the given MacroBlock.
     * <p>
     * It sets some properties, but there won't be any transformation set, for example.
     *
     * @param macroBlock the MacroBlock to create the transformation context for
     * @return a macro transformation context
     */
    private MacroTransformationContext getTransformationContext(MacroBlock macroBlock)
    {
        MacroTransformationContext macroTransformationContext = new MacroTransformationContext();
        macroTransformationContext.setId("RequiredRightAnalyzer_" + macroBlock.getId());
        macroTransformationContext.setCurrentMacroBlock(macroBlock);
        // fallback syntax: macro content parser search by default for the XDOM syntax.
        macroTransformationContext.setSyntax(this.renderingContext.getDefaultSyntax());
        macroTransformationContext.setInline(macroBlock.isInline());
        return macroTransformationContext;
    }

    /**
     * Gets the Macro object based on the given MacroBlock and MacroTransformationContext.
     * <p>
     * It retrieves the macro based on the macro ID and rendering syntax from the MacroManager.
     * If the macro cannot be found, it returns null.
     *
     * @param macroBlock the MacroBlock to get the macro for
     * @return the Macro object, or null if the macro is not found
     */
    protected Macro<?> getMacro(MacroBlock macroBlock)
    {
        Syntax syntax = macroBlock.getSyntaxMetadata().orElse(this.renderingContext.getDefaultSyntax());
        MacroId macroId = new MacroId(macroBlock.getId(), syntax);
        Macro<?> macro = null;
        try {
            macro = this.macroManager.getMacro(macroId);
        } catch (MacroLookupException ex) {
            // Ignore, if the macro cannot be found, we cannot check its permissions, but it also won't be executed.
        }
        return macro;
    }

    /**
     * Analyzes the macro content to determine the required rights.
     * <p>
     * It parses the macro content using the macro content parser and analyzes the resulting XDOM object
     * using the XDOM required right analyzer to determine the required rights.
     *
     * @param macroBlock the MacroBlock containing the macro content to analyze
     * @param content the macro content to analyze
     * @return a list of RequiredRightAnalysisResult objects representing the required rights for the macro content
     * @throws RequiredRightsException if the required rights cannot be determined
     */
    protected List<RequiredRightAnalysisResult> analyzeMacroContent(MacroBlock macroBlock, String content)
        throws RequiredRightsException
    {
        return analyzeMacroContent(macroBlock, content, null);
    }

    /**
     * Analyzes the macro content to determine the required rights.
     * <p>
     * It parses the macro content using the macro content parser and analyzes the resulting XDOM object
     * using the XDOM required right analyzer to determine the required rights.
     *
     * @param macroBlock the MacroBlock containing the macro content to analyze
     * @param content the macro content to analyze
     * @param syntax the syntax of the macro content
     * @return a list of RequiredRightAnalysisResult objects representing the required rights for the macro content
     * @throws RequiredRightsException if the required rights cannot be determined
     */
    protected List<RequiredRightAnalysisResult> analyzeMacroContent(MacroBlock macroBlock, String content,
        Syntax syntax) throws RequiredRightsException
    {
        // Keep whatever metadata was present on the XDOM.
        MetaData metaData = null;
        if (macroBlock.getRoot() instanceof XDOM) {
            metaData = new MetaData(((XDOM) macroBlock.getRoot()).getMetaData().getMetaData());
        }
        MacroTransformationContext transformationContext = getTransformationContext(macroBlock);
        try {
            XDOM xdom = this.macroContentParser
                .parse(content, syntax, transformationContext, false, metaData, macroBlock.isInline());
            return this.xdomRequiredRightAnalyzer.analyze(xdom);
        } catch (Exception e) {
            throw new RequiredRightsException("Failed to analyze macro content", e);
        }
    }

    /**
     * Extracts the source reference from a given block.
     * <p>
     * It first tries to find the source reference in the entity reference metadata of the block.
     * If it doesn't find it, it tries to find the source reference in the source metadata of the block.
     * If both fail, it returns null.
     *
     * @param source the block from which to extract the source reference
     * @return the extracted entity reference representing the source reference, or null if not found
     */
    protected EntityReference extractSourceReference(Block source)
    {
        EntityReference result = null;
        // First, try the entity reference metadata.
        MetaDataBlock metaDataBlock =
            source.getFirstBlock(new MetadataBlockMatcher(XDOMRequiredRightAnalyzer.ENTITY_REFERENCE_METADATA),
                Block.Axes.ANCESTOR);

        if (metaDataBlock != null && metaDataBlock.getMetaData()
            .getMetaData(XDOMRequiredRightAnalyzer.ENTITY_REFERENCE_METADATA) instanceof EntityReference) {
            result = (EntityReference) metaDataBlock.getMetaData()
                .getMetaData(XDOMRequiredRightAnalyzer.ENTITY_REFERENCE_METADATA);
        } else {
            metaDataBlock = source.getFirstBlock(new MetadataBlockMatcher(MetaData.SOURCE), Block.Axes.ANCESTOR);
            if (metaDataBlock != null) {
                // FIXME: the locale is lost here as the metadata source does not keep the locale when serializing the
                // entity reference
                result =
                    this.documentReferenceResolver.resolve(
                        (String) metaDataBlock.getMetaData().getMetaData(MetaData.SOURCE));
            }
        }
        return result;
    }

    protected RequiredRightAnalysisResult reportAnalysisError(MacroBlock macroBlock, Exception e)
    {
        return new RequiredRightAnalysisResult(extractSourceReference(macroBlock),
            this.translationBlockSupplierProvider.get("security.requiredrights.macro.analyzer.error",
                macroBlock.getId(),
                ExceptionUtils.getRootCauseMessage(e)),
            this.macroBlockBlockSupplierProvider.get(macroBlock),
            List.of(RequiredRight.MAYBE_PROGRAM)
        );
    }
}
