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
package org.xwiki.rendering.wikimacro.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.async.internal.block.AbstractBlockAsyncRenderer;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererResult;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.wikibridge.WikiMacroBindingInitializer;
import org.xwiki.rendering.macro.wikibridge.WikiMacroExecutionFinishedEvent;
import org.xwiki.rendering.macro.wikibridge.WikiMacroExecutionStartsEvent;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameters;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;

import com.xpn.xwiki.XWikiContext;

/**
 * Actually execute the wiki macro.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Component(roles = DefaultWikiMacroRenderer.class)
public class DefaultWikiMacroRenderer extends AbstractBlockAsyncRenderer
{
    /**
     * The key under which macro context will be available in the XWikiContext for scripts.
     */
    private static final String MACRO_KEY = "macro";

    /**
     * The key under which macro body will be available inside macro context.
     */
    private static final String MACRO_CONTENT_KEY = "content";

    /**
     * The key under which macro parameters will be available inside macro context.
     */
    private static final String MACRO_PARAMS_KEY = "params";

    /**
     * The key under which macro transformation context will be available inside macro context.
     */
    private static final String MACRO_CONTEXT_KEY = "context";

    /**
     * The key under which macro descriptor will be available inside macro context.
     */
    private static final String MACRO_DESCRIPTOR_KEY = "descriptor";

    /**
     * Event sent before wiki macro execution.
     */
    private static final WikiMacroExecutionStartsEvent STARTEXECUTION_EVENT = new WikiMacroExecutionStartsEvent();

    /**
     * Event sent after wiki macro execution.
     */
    private static final WikiMacroExecutionFinishedEvent ENDEXECUTION_EVENT = new WikiMacroExecutionFinishedEvent();

    /**
     * The key under which macro can directly return the resulting {@link List} of {@link Block}.
     */
    private static final String MACRO_RESULT_KEY = "result";

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Inject
    private AsyncContext asyncContext;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ObservationManager observation;

    private DefaultWikiMacro wikimacro;

    private List<String> id;

    private WikiMacroParameters parameters;

    private String macroContent;

    private boolean inline;

    private Syntax targetSyntax;

    private MacroTransformationContext syncContext;

    void initialize(DefaultWikiMacro wikimacro, WikiMacroParameters parameters, String macroContent,
        MacroTransformationContext syncContext)
    {
        this.wikimacro = wikimacro;

        this.parameters = parameters;
        this.macroContent = macroContent;
        this.inline = syncContext.isInline();
        this.targetSyntax = syncContext.getTransformationContext().getTargetSyntax();

        this.syncContext = syncContext;

        // Find index of the macro in the XDOM
        long index = syncContext.getXDOM().indexOf(syncContext.getCurrentMacroBlock());

        this.id = Arrays.asList("rendering", "wikimacro", wikimacro.getId(), String.valueOf(index));
    }

    @Override
    public List<String> getId()
    {
        return this.id;
    }

    @Override
    public boolean isAsyncAllowed()
    {
        return this.wikimacro.isAsyncAllowed();
    }

    @Override
    public boolean isCacheAllowed()
    {
        return this.wikimacro.isCachedAllowed();
    }

    @Override
    public boolean isInline()
    {
        return this.inline;
    }

    /**
     * Removes any top level paragraph since for example for the following use case we don't want an extra paragraph
     * block: <code>= hello {{velocity}}world{{/velocity}}</code>.
     * 
     * @param blocks the blocks to check and convert
     */
    private Block removeTopLevelParagraph(Block block)
    {
        List<Block> blocks = block.getChildren();

        // Remove any top level paragraph so that the result of a macro can be used inline for example.
        // We only remove the paragraph if there's only one top level element and if it's a paragraph.
        if ((block.getChildren().size() == 1) && block.getChildren().get(0) instanceof ParagraphBlock) {
            Block paragraphBlock = blocks.remove(0);
            blocks.addAll(0, paragraphBlock.getChildren());

            return new CompositeBlock(blocks);
        }

        return block;
    }

    private Block extractResult(Block block, Map<String, Object> macroContext, boolean async)
    {
        Object resultObject = macroContext.get(MACRO_RESULT_KEY);

        Block result;
        if (resultObject instanceof List) {
            result = new CompositeBlock((List<Block>) resultObject);
        } else if (resultObject instanceof Block) {
            result = (Block) resultObject;
        } else {
            if (!async) {
                // If synchronized the top level block is a temporary macro marker so we need to get rid of it
                result = new CompositeBlock(block.getChildren());
            } else {
                result = block;
            }

            // If in inline mode remove any top level paragraph.
            if (this.inline) {
                result = removeTopLevelParagraph(result);
            }
        }

        return result;
    }

    /**
     * Clone and filter wiki macro content depending of the context.
     * 
     * @param context the macro execution context
     * @return the cleaned wiki macro content
     */
    private XDOM prepareWikiMacroContent()
    {
        XDOM xdom = this.wikimacro.getContent().clone();

        // Macro code segment is always parsed into a separate xdom document. Now if this code segment starts with
        // another macro block, it will always be interpreted as a block macro regardless of the current wiki macro's
        // context (because as far as the nested macro is concerned, it starts on a new line). This will introduce
        // unnecessary paragraph elements when the wiki macro is used inline, so we need to force such opening macro
        // blocks to behave as inline macros if the wiki macro is used inline.
        if (this.inline) {
            List<Block> children = xdom.getChildren();
            if (!children.isEmpty() && children.get(0) instanceof MacroBlock) {
                MacroBlock old = (MacroBlock) children.get(0);
                MacroBlock replacement = new MacroBlock(old.getId(), old.getParameters(), old.getContent(), true);
                xdom.replaceChild(replacement, old);
            }
        }

        return xdom;
    }

    @Override
    public BlockAsyncRendererResult render(boolean async, boolean cached) throws RenderingException
    {
        // Register the known involved references and components
        this.asyncContext.useComponent(this.wikimacro.getRoleType(), this.wikimacro.getRoleHint());

        // Parse the wiki macro content.
        XDOM macroXDOM = prepareWikiMacroContent();

        ///////////////////////////////////////
        // Transform

        Block blockResult = transform(macroXDOM, async);

        ///////////////////////////////////////
        // Rendering

        String resultString = null;

        if (async || cached) {
            BlockRenderer renderer;
            try {
                renderer = this.componentManager.get().getInstance(BlockRenderer.class, this.targetSyntax.toIdString());
            } catch (ComponentLookupException e) {
                throw new RenderingException("Failed to lookup renderer for syntax [" + this.targetSyntax + "]", e);
            }

            WikiPrinter printer = new DefaultWikiPrinter();
            renderer.render(blockResult, printer);

            resultString = printer.toString();
        }

        return new BlockAsyncRendererResult(resultString, blockResult);
    }

    private Map<String, Object> createBinding(boolean async)
    {
        Map<String, Object> macroBinding = new HashMap<>();
        macroBinding.put(MACRO_PARAMS_KEY, this.parameters);
        macroBinding.put(MACRO_CONTENT_KEY, this.macroContent);
        macroBinding.put(MACRO_DESCRIPTOR_KEY, this.wikimacro.getDescriptor());
        macroBinding.put(MACRO_RESULT_KEY, null);

        if (!async) {
            macroBinding.put(MACRO_CONTEXT_KEY, this.syncContext);
        }

        ComponentManager currentComponentManager = this.componentManager.get();

        // Extension point to add more wiki macro bindings
        try {
            List<WikiMacroBindingInitializer> bindingInitializers =
                currentComponentManager.getInstanceList(WikiMacroBindingInitializer.class);

            for (WikiMacroBindingInitializer bindingInitializer : bindingInitializers) {
                bindingInitializer.initialize(this.wikimacro, this.parameters, this.macroContent,
                    async ? null : this.syncContext, macroBinding);
            }
        } catch (ComponentLookupException e) {
            // TODO: we should probably log something but that should never happen
        }

        return macroBinding;
    }

    private Block transform(XDOM macroXDOM, boolean async) throws RenderingException
    {
        // Prepare macro context.
        Map<String, Object> macroBinding = createBinding(async);

        // Get XWiki context
        XWikiContext xwikiContext = this.xcontextProvider.get();

        Block block;
        XDOM xdom;

        try {
            if (xwikiContext != null) {
                // Place macro context inside xwiki context ($xcontext.macro).
                xwikiContext.put(MACRO_KEY, macroBinding);
            }

            Block syncMetaDataBlock;

            if (async) {
                block = macroXDOM;
                xdom = macroXDOM;

                syncMetaDataBlock = null;
            } else {
                MacroBlock wikiMacroBlock = this.syncContext.getCurrentMacroBlock();

                MacroMarkerBlock wikiMacroMarker =
                    new MacroMarkerBlock(wikiMacroBlock.getId(), wikiMacroBlock.getParameters(),
                        wikiMacroBlock.getContent(), macroXDOM.getChildren(), wikiMacroBlock.isInline());

                // Make sure to use provided metadatas
                syncMetaDataBlock =
                    new MetaDataBlock(Collections.<Block>singletonList(wikiMacroMarker), macroXDOM.getMetaData());

                // Make sure the context XDOM contains the wiki macro content
                wikiMacroBlock.getParent().replaceChild(syncMetaDataBlock, wikiMacroBlock);

                // "Emulate" the fact that wiki macro block is still part of the XDOM (what is in the XDOM is a
                // MacroMarkerBlock and MacroTransformationContext current macro block only support MacroBlock so we
                // can't switch it without breaking some APIs)
                wikiMacroBlock.setParent(syncMetaDataBlock.getParent());
                wikiMacroBlock.setNextSiblingBlock(syncMetaDataBlock.getNextSibling());
                wikiMacroBlock.setPreviousSiblingBlock(syncMetaDataBlock.getPreviousSibling());

                block = wikiMacroMarker;
                xdom = this.syncContext.getXDOM();
            }

            try {
                this.observation.notify(STARTEXECUTION_EVENT, this.wikimacro, macroBinding);

                block = transform(block, xdom, macroBinding, async);
            } finally {
                if (syncMetaDataBlock != null) {
                    // Restore context XDOM to its previous state
                    syncMetaDataBlock.getParent().replaceChild(this.syncContext.getCurrentMacroBlock(),
                        syncMetaDataBlock);
                }

                this.observation.notify(ENDEXECUTION_EVENT, this.wikimacro);
            }
        } catch (Exception ex) {
            throw new RenderingException("Error while performing internal macro transformations", ex);
        } finally {
            if (xwikiContext != null) {
                xwikiContext.remove(MACRO_KEY);
            }
        }

        return block;
    }

    private Block transform(Block block, XDOM xdom, Map<String, Object> macroBinding, boolean async)
        throws TransformationException
    {

        TransformationContext transformationContext = new TransformationContext(xdom, this.wikimacro.getSyntax());
        transformationContext.setTargetSyntax(this.targetSyntax);
        if (!async) {
            // In synchronized mode keep current transformation id
            transformationContext.setId(this.syncContext.getTransformationContext().getId());
        }

        transform(block, transformationContext, this.wikimacro.getAuthorReference());

        return extractResult(block, macroBinding, async);
    }
}
