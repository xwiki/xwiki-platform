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

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.script.ScriptContext;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.properties.ConverterManager;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.async.internal.block.AbstractBlockAsyncRenderer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroExecutionFinishedEvent;
import org.xwiki.rendering.macro.wikibridge.WikiMacroExecutionStartsEvent;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameters;
import org.xwiki.rendering.macro.wikibridge.binding.WikiMacroBinding;
import org.xwiki.rendering.macro.wikibridge.binding.WikiMacroBindingInitializer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.script.ScriptContextManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

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
     * 
     * @deprecated since 11.6RC1, 11.3.2, 10.11.8. {@link WikiMacroBinding} should now be used.
     */
    @Deprecated
    private static final String MACRO_KEY = "macro";

    /**
     * The key under which {@link WikiMacroBinding} is available in the script context.
     */
    private static final String MACRO_BINDING = "wikimacro";

    /**
     * The key under which macro body will be available inside macro context.
     * 
     * @deprecated since 11.6RC1, 11.3.2, 10.11.8. {@link WikiMacroBinding} should now be used.
     */
    @Deprecated
    private static final String MACRO_CONTENT_KEY = "content";

    /**
     * The key under which macro parameters will be available inside macro context.
     * 
     * @deprecated since 11.6RC1, 11.3.2, 10.11.8. {@link WikiMacroBinding} should now be used.
     */
    @Deprecated
    private static final String MACRO_PARAMS_KEY = "params";

    /**
     * The key under which macro transformation context will be available inside macro context.
     * 
     * @deprecated since 11.6RC1, 11.3.2, 10.11.8. {@link WikiMacroBinding} should now be used.
     */
    @Deprecated
    private static final String MACRO_CONTEXT_KEY = "context";

    /**
     * The key under which macro descriptor will be available inside macro context.
     * 
     * @deprecated since 11.6RC1, 11.3.2, 10.11.8. {@link WikiMacroBinding} should now be used.
     */
    @Deprecated
    private static final String MACRO_DESCRIPTOR_KEY = "descriptor";

    /**
     * The key under which the old context binding is saved.
     */
    private static final String BACKUP_CONTEXTBINDING_KEY = "wikimacro.backup.contextbinding";

    /**
     * The key under which the new binding is saved.
     */
    private static final String BACKUP_BINDING_KEY = "wikimacro.backup.binding";

    /**
     * The key under which macro can access the document where it's defined. Same as CONTEXT_DOCUMENT_KEY (Check style
     * fix).
     */
    private static final String MACRO_DOC_KEY = "doc";

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

    // All the following matchers are used in cleanMacroMarkers. They are defined here for perf reasons.

    /**
     * Match all the macro marker blocks.
     */
    private static final BlockMatcher MACRO_MARKER_MATCHER = testedBlock -> (testedBlock instanceof MacroMarkerBlock);

    /**
     * Match all the metadata blocks that contains wikimacrocontent=true.
     */
    private static final BlockMatcher MACROCONTENT_METADATA_MATCHER =
        testedBlock -> (testedBlock instanceof MetaDataBlock)
            && "true".equals(((MetaDataBlock) testedBlock).getMetaData().getMetaData("wikimacrocontent"));

    /**
     * Match all the metadata blocks that contains a non-generated-content metadata.
     */
    private static final BlockMatcher NON_GENERATED_CONTENT_METADATA_MATCHER =
        testedBlock -> (testedBlock instanceof MetaDataBlock)
            && ((MetaDataBlock) testedBlock).getMetaData().getMetaData(MetaData.NON_GENERATED_CONTENT) != null;

    @Inject
    private AsyncContext asyncContext;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ObservationManager observation;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private Execution execution;

    @Inject
    private Logger logger;

    private DefaultWikiMacro wikimacro;

    private List<String> id;

    private WikiMacroParameters parameters;

    private WikiMacroParameters originalParameters;

    private String macroContent;

    private boolean inline;

    private Syntax targetSyntax;

    private MacroTransformationContext syncContext;

    void initialize(DefaultWikiMacro wikimacro, WikiMacroParameters parameters, String macroContent,
        MacroTransformationContext syncContext)
    {
        this.wikimacro = wikimacro;

        this.originalParameters = parameters;
        this.macroContent = macroContent;
        this.inline = syncContext.isInline();
        this.targetSyntax = syncContext.getTransformationContext().getTargetSyntax();

        this.syncContext = syncContext;

        // Find index of the macro in the XDOM
        long index = syncContext.getXDOM().indexOf(syncContext.getCurrentMacroBlock());

        this.id = createId("rendering", "wikimacro", wikimacro.getId(), index);
        try {
            this.parameters = convertParameters(parameters);
        } catch (ComponentLookupException e) {
            logger.error("Error while converting wikimacro parameters value.", e);
        }
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
        return this.wikimacro.isCacheAllowed();
    }

    @Override
    public boolean isInline()
    {
        return this.inline;
    }

    @Override
    public Syntax getTargetSyntax()
    {
        return this.targetSyntax;
    }

    private Block extractResult(Block block, WikiMacroBinding macroBinding, boolean async)
    {
        Object resultObject = macroBinding.getResult();

        if (resultObject == null) {
            resultObject = ((Map<String, Object>) this.xcontextProvider.get().get(MACRO_KEY)).get(MACRO_RESULT_KEY);
        }

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
    public Block execute(boolean async, boolean cached) throws RenderingException
    {
        // Register the known involved references and components
        this.asyncContext.useComponent(this.wikimacro.getRoleType(), this.wikimacro.getRoleHint());

        // Parse the wiki macro content.
        XDOM macroXDOM = prepareWikiMacroContent();

        ///////////////////////////////////////
        // Transform

        return transform(macroXDOM, async);
    }

    private WikiMacroBinding createBinding(boolean async)
    {
        DocumentReference macroDocumentReference = this.wikimacro.getDocumentReference();

        // old macro binding for $xcontext.macro
        Map<String, Object> contextMacroBinding = new HashMap<>();
        contextMacroBinding.put(MACRO_PARAMS_KEY, this.originalParameters);
        contextMacroBinding.put(MACRO_CONTENT_KEY, this.macroContent);
        contextMacroBinding.put(MACRO_DESCRIPTOR_KEY, this.wikimacro.getDescriptor());
        contextMacroBinding.put(MACRO_RESULT_KEY, null);

        if (!async) {
            contextMacroBinding.put(MACRO_CONTEXT_KEY, this.syncContext);
        }

        WikiMacroBinding macroBinding = null;
        try {
            ComponentManager currentComponentManager = this.componentManagerProvider.get();

            XWikiContext xWikiContext = this.xcontextProvider.get();
            Document document =
                new Document(xWikiContext.getWiki().getDocument(macroDocumentReference, xWikiContext), xWikiContext);

            contextMacroBinding.put(MACRO_DOC_KEY, document);

            // new macro binding for $wikimacro
            macroBinding = new WikiMacroBinding(this.wikimacro.getDescriptor(), this.parameters, this.macroContent,
                async ? null : this.syncContext);
            macroBinding.put(MACRO_DOC_KEY, document);

            // Extension point to add more wiki macro bindings
            List<WikiMacroBindingInitializer> bindingInitializers =
                currentComponentManager.getInstanceList(WikiMacroBindingInitializer.class);

            for (WikiMacroBindingInitializer bindingInitializer : bindingInitializers) {
                bindingInitializer.initialize(this.wikimacro, this.parameters, this.macroContent,
                    async ? null : this.syncContext, macroBinding);
            }

            List<org.xwiki.rendering.macro.wikibridge.WikiMacroBindingInitializer> oldBindingInitializers =
                currentComponentManager
                    .getInstanceList(org.xwiki.rendering.macro.wikibridge.WikiMacroBindingInitializer.class);

            for (org.xwiki.rendering.macro.wikibridge.WikiMacroBindingInitializer bindingInitializer : oldBindingInitializers) {
                bindingInitializer.initialize(this.wikimacro, this.parameters, this.macroContent,
                    async ? null : this.syncContext, contextMacroBinding);
            }

            xWikiContext.put(MACRO_KEY, contextMacroBinding);
            this.scriptContextManager.getCurrentScriptContext().setAttribute(MACRO_BINDING, macroBinding,
                ScriptContext.ENGINE_SCOPE);
            backupBindings(contextMacroBinding, macroBinding);
        } catch (ComponentLookupException | XWikiException e) {
            logger.error("Error while performing wikimacro binding.", e);
        }

        return macroBinding;
    }

    private void backupBindings(Map<String, Object> contextBinding, WikiMacroBinding wikiMacroBinding)
    {
        ExecutionContext econtext = this.execution.getContext();
        Deque<Map<String, Object>> backupContextBinding =
            (Deque<Map<String, Object>>) econtext.getProperty(BACKUP_CONTEXTBINDING_KEY);
        if (backupContextBinding == null) {
            backupContextBinding = new LinkedList<>();
            econtext.newProperty(BACKUP_CONTEXTBINDING_KEY).initial(backupContextBinding).makeFinal().inherited()
                .declare();
        }
        backupContextBinding.push(contextBinding);

        Deque<WikiMacroBinding> backupWikiMacroBinding =
            (Deque<WikiMacroBinding>) econtext.getProperty(BACKUP_BINDING_KEY);
        if (backupWikiMacroBinding == null) {
            backupWikiMacroBinding = new LinkedList<>();
            econtext.newProperty(BACKUP_BINDING_KEY).initial(backupWikiMacroBinding).makeFinal().inherited().declare();
        }
        backupWikiMacroBinding.push(wikiMacroBinding);
    }

    private void restoreBindingsOrClean()
    {
        ExecutionContext econtext = this.execution.getContext();
        Deque<Map<String, Object>> backupContextBinding =
            (Deque<Map<String, Object>>) econtext.getProperty(BACKUP_CONTEXTBINDING_KEY);

        backupContextBinding.pop();
        if (!backupContextBinding.isEmpty()) {
            this.xcontextProvider.get().put(MACRO_KEY, backupContextBinding.peek());
        } else {
            this.xcontextProvider.get().remove(MACRO_KEY);
        }

        Deque<WikiMacroBinding> backupWikiMacroBinding =
            (Deque<WikiMacroBinding>) econtext.getProperty(BACKUP_BINDING_KEY);
        backupWikiMacroBinding.pop();
        if (!backupWikiMacroBinding.isEmpty()) {
            // we cannot just replace the attribute in the current script context: it would not be taken into account
            // if we are already running a velocity script. Instead we rely on the already existing instance and
            // we update it.
            WikiMacroBinding newMacroBinding = backupWikiMacroBinding.peek();
            WikiMacroBinding oldBinding =
                (WikiMacroBinding) this.scriptContextManager.getCurrentScriptContext().getAttribute(MACRO_BINDING);
            oldBinding.replaceAll(newMacroBinding);
        } else {
            this.scriptContextManager.getCurrentScriptContext().removeAttribute(MACRO_BINDING,
                ScriptContext.ENGINE_SCOPE);
        }
    }

    private WikiMacroParameters convertParameters(WikiMacroParameters parameters) throws ComponentLookupException
    {
        ConverterManager converterManager = this.componentManagerProvider.get().getInstance(ConverterManager.class);
        Map<String, ParameterDescriptor> parameterDescriptorMap =
            this.wikimacro.getDescriptor().getParameterDescriptorMap();
        WikiMacroParameters result = new WikiMacroParameters();
        for (String parameterName : parameters.getParameterNames()) {
            Object value = parameters.get(parameterName);

            if (parameterDescriptorMap.containsKey(parameterName.toLowerCase())) {
                ParameterDescriptor parameterDescriptor = parameterDescriptorMap.get(parameterName.toLowerCase());
                Type parameterType = parameterDescriptor.getParameterType();

                value = converterManager.convert(parameterType, value);
            }

            result.set(parameterName, value);
        }

        return result;
    }

    private Block transform(XDOM macroXDOM, boolean async) throws RenderingException
    {
        // Prepare macro context.
        WikiMacroBinding macroBinding = createBinding(async);

        // Get XWiki context
        XWikiContext xwikiContext = this.xcontextProvider.get();

        Block block;
        XDOM xdom;

        try {
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
            restoreBindingsOrClean();
        }

        cleanMacroMarkers(block);
        return block;
    }

    /**
     * Clean the rendered macro to allow inline editing of wikimacro content. Specifically this method will remove the
     * macro markers that are inside the wikimacro, except those that are used inside the content of this wikimacro. It
     * will also remove all the non-generated-content metadata blocks that are defined in the wikimacro except those
     * also used inside the content of the wikimacro content. This method doesn't return anything but alterate the given
     * block parameter.
     * 
     * @param block the block to clean.
     * @since 11.4RC1
     */
    private void cleanMacroMarkers(Block block)
    {

        List<Block> allMacroMarkerBlocks = block.getBlocks(MACRO_MARKER_MATCHER, Block.Axes.DESCENDANT);
        List<Block> allWikiMacroContentMetadataBlocks =
            block.getBlocks(MACROCONTENT_METADATA_MATCHER, Block.Axes.DESCENDANT);
        List<Block> allNonGeneratedContentMetadataBlocks =
            block.getBlocks(NON_GENERATED_CONTENT_METADATA_MATCHER, Block.Axes.DESCENDANT);

        // Before removing the blocks, we need to remove from the lists all those that are inside a wikimacro content
        // metadata block, so we don't alterate the wikimacro content itself.
        for (Block allWikiMacroContentMarkerBlock : allWikiMacroContentMetadataBlocks) {
            allMacroMarkerBlocks
                .removeAll(allWikiMacroContentMarkerBlock.getBlocks(MACRO_MARKER_MATCHER, Block.Axes.DESCENDANT));
            allNonGeneratedContentMetadataBlocks.removeAll(allWikiMacroContentMarkerBlock
                .getBlocks(NON_GENERATED_CONTENT_METADATA_MATCHER, Block.Axes.DESCENDANT_OR_SELF));
        }

        // Remove all macro marker blocks that remains (outside the wikimacro content block).
        for (Block markerBlock : allMacroMarkerBlocks) {
            markerBlock.getParent().replaceChild(markerBlock.getChildren(), markerBlock);
        }

        // Remove all non generated content metadata block that remains (outisde the wikimacro content block).
        for (Block nonGeneratedContentMetadataBlock : allNonGeneratedContentMetadataBlocks) {
            nonGeneratedContentMetadataBlock.getParent().replaceChild(nonGeneratedContentMetadataBlock.getChildren(),
                nonGeneratedContentMetadataBlock);
        }
    }

    private Block transform(Block block, XDOM xdom, WikiMacroBinding macroBinding, boolean async)
        throws TransformationException
    {
        TransformationContext transformationContext = new TransformationContext(xdom, this.wikimacro.getSyntax());
        transformationContext.setTargetSyntax(this.targetSyntax);
        if (!async) {
            // In synchronized mode keep current transformation id
            transformationContext.setId(this.syncContext.getTransformationContext().getId());
        }

        transform(block, transformationContext);

        return extractResult(block, macroBinding, async);
    }
}
