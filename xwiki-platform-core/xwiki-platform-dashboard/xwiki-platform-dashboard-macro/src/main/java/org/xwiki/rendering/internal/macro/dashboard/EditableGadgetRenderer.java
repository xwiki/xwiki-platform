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
package org.xwiki.rendering.internal.macro.dashboard;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.macro.dashboard.Gadget;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;

/**
 * Editable gadget renderer, that renders the gadget as in view mode, but with additional metadata that allows editing
 * the gadget parameters.
 * 
 * @version $Id$
 * @since 3.0rc1
 */
@Component
@Named("edit")
@Singleton
public class EditableGadgetRenderer extends DefaultGadgetRenderer
{
    /**
     * The identifier of the metadata block for this dashboard (class parameter of the generated XDOM container that
     * holds the rest of the metadata).
     */
    protected static final String METADATA = "metadata";
    
    private static final String ANNOTATED_SYNTAXES_PREFIX = "annotated";

    /**
     * The macro content renderer, to render the macro as annotated XHTML to be editable.
     */
    @Inject
    @Named("annotatedxhtml/1.0")
    protected BlockRenderer defaultGadgetContentRenderer;
    
    @Inject
    protected ComponentManager componentManager;
    
    @Inject
    protected RenderingContext renderingContext;
    
    @Inject
    protected Logger logger;

    /** 
     * @return the renderer corresponding to the "annotated" version of the current target syntax
     */
    protected BlockRenderer getGadgetContentRenderer()
    {
        // Get the current syntax
        Syntax currentTargetSyntax = renderingContext.getTargetSyntax();
        if (currentTargetSyntax == null) {
            // (it should never happen actually)
            return defaultGadgetContentRenderer;
        }

        // Get the annotated syntax corresponding to the current target syntax
        String annotatedTargetSyntax = currentTargetSyntax.toIdString();
        if (!Strings.CS.startsWith(annotatedTargetSyntax, ANNOTATED_SYNTAXES_PREFIX)) {
            annotatedTargetSyntax = ANNOTATED_SYNTAXES_PREFIX + annotatedTargetSyntax;
        }
        
        try {
            return componentManager.getInstance(BlockRenderer.class, annotatedTargetSyntax);
        } catch (ComponentLookupException e) {
            logger.warn("Failed to load the syntax [{}].", annotatedTargetSyntax);
            // Failback to the default renderer
            return defaultGadgetContentRenderer;
        }
    }

    /** 
     * @param gadgetContentRenderer the renderer used to render the gadget
     * @return the syntax to set to the raw block according to the given renderer
     */
    protected Syntax getRawBlockSyntax(BlockRenderer gadgetContentRenderer)
    {
        // If there is a BlockRenderer corresponding to the "annotated" version of the current 
        // RenderingContext#getTargetSyntax(), then we return the current target syntax.
        //
        // On the opposite, if we have fallbacked to the defaultGadgetContentRenderer (see getGadgetContentRenderer()),
        // we have to use the syntax corresponding to that renderer: XHTML 1.0.
        //
        return gadgetContentRenderer != defaultGadgetContentRenderer
                ? renderingContext.getTargetSyntax() : Syntax.XHTML_1_0;
    }

    /**
     * @param gadget the gadget to decorate
     * @return the block containing the metadata that will allow clients to edit this gadget
     */
    protected Block getGadgetEditMetadata(Gadget gadget)
    {
        GroupBlock metadataBlock = new GroupBlock();
        metadataBlock.setParameter(CLASS, METADATA);

        // look at the content of the gadget and store whether it's a macro or not
        boolean isMacro = gadget.getContent().size() == 1 && gadget.getContent().get(0) instanceof MacroMarkerBlock;
        GroupBlock isMacroBlock = new GroupBlock();
        isMacroBlock.setParameter(CLASS, "isMacro");
        isMacroBlock.addChild(new WordBlock(Boolean.toString(isMacro)));

        metadataBlock.addChild(isMacroBlock);

        if (isMacro) {
            // render the annotated macro call in the page, to be able to edit it. Only the macro call comments will be
            // rendered, since transformations are not ran, so there is no content in the macro. But annotation is
            // enough.
            GroupBlock renderedContentBlock = new GroupBlock();
            renderedContentBlock.setParameter(CLASS, "content");
            WikiPrinter printer = new DefaultWikiPrinter();
            BlockRenderer gadgetContentRenderer = getGadgetContentRenderer();            
            gadgetContentRenderer.render(gadget.getContent(), printer);
            RawBlock rawBlock = new RawBlock(printer.toString(), getRawBlockSyntax(gadgetContentRenderer));
            renderedContentBlock.addChild(rawBlock);

            // render the title in the page as well, to be edited as source
            GroupBlock gadgetTitleBlock = new GroupBlock();
            gadgetTitleBlock.setParameter(CLASS, "title");
            // even if it's not a word, it's fine since it will be rendered in one piece
            gadgetTitleBlock.addChild(new WordBlock(gadget.getTitleSource()));

            metadataBlock.addChild(renderedContentBlock);
            metadataBlock.addChild(gadgetTitleBlock);
        }

        return metadataBlock;
    }

    @Override
    public List<Block> decorateGadget(Gadget gadget)
    {
        List<Block> viewBlock = super.decorateGadget(gadget);

        if (viewBlock.size() > 0) {
            viewBlock.get(0).addChild(getGadgetEditMetadata(gadget));
        }

        return viewBlock;
    }
}
