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
package org.xwiki.rendering.internal.macro.wikibridge;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.script.NestedScriptMacroEnabled;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroExecutionFinishedEvent;
import org.xwiki.rendering.macro.wikibridge.WikiMacroExecutionStartsEvent;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameters;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;

/**
 * Default implementation of {@link WikiMacro}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class DefaultWikiMacro implements WikiMacro, NestedScriptMacroEnabled
{
    /**
     * The key under which macro context will be available in the XWikiContext for scripts.
     */
    private static final String MACRO_KEY = "macro";

    /**
     * Macro hint for {@link Transformation} component. Same as MACRO_KEY (Check style fix).
     */
    private static final String MACRO_HINT = MACRO_KEY;

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
     * The key under which macro can directly return the resulting {@link List} of {@link Block}.
     */
    private static final String MACRO_RESULT_KEY = "result";

    /**
     * Event sent before wiki macro execution.
     */
    private static final WikiMacroExecutionStartsEvent STARTEXECUTION_EVENT = new WikiMacroExecutionStartsEvent();

    /**
     * Event sent after wiki macro execution.
     */
    private static final WikiMacroExecutionFinishedEvent ENDEXECUTION_EVENT = new WikiMacroExecutionFinishedEvent();

    /**
     * The {@link MacroDescriptor} for this macro.
     */
    private MacroDescriptor descriptor;

    /**
     * Document which contains the definition of this macro.
     */
    private DocumentReference macroDocumentReference;

    /**
     * User to be used to check rights for the macro.
     */
    private DocumentReference macroAuthor;

    /**
     * Whether this macro supports inline mode or not.
     */
    private boolean supportsInlineMode;

    /**
     * Macro content.
     */
    private XDOM content;

    /**
     * Syntax id.
     */
    private Syntax syntax;

    /**
     * The component manager used to lookup other components.
     */
    private ComponentManager componentManager;

    /**
     * Constructs a new {@link DefaultWikiMacro}.
     * 
     * @param macroDocumentReference the name of the document which contains the definition of this macro
     * @param supportsInlineMode says if macro support inline mode or not
     * @param descriptor the {@link MacroDescriptor} describing this macro.
     * @param macroContent macro content to be evaluated.
     * @param syntax syntax of the macroContent source.
     * @param componentManager {@link ComponentManager} component used to look up for other components.
     * @since 2.3M1
     */
    public DefaultWikiMacro(DocumentReference macroDocumentReference, DocumentReference macroAuthor,
        boolean supportsInlineMode, MacroDescriptor descriptor, XDOM macroContent, Syntax syntax,
        ComponentManager componentManager)
    {
        this.macroDocumentReference = macroDocumentReference;
        this.macroAuthor = macroAuthor;
        this.supportsInlineMode = supportsInlineMode;
        this.descriptor = descriptor;
        this.content = macroContent;
        this.syntax = syntax;
        this.componentManager = componentManager;
    }

    @Override
    public List<Block> execute(WikiMacroParameters parameters, String macroContent, MacroTransformationContext context)
        throws MacroExecutionException
    {
        validate(parameters, macroContent);

        // Parse the wiki macro content.
        XDOM xdom = prepareWikiMacroContent(context);

        // Prepare macro context.
        Map<String, Object> macroBinding = new HashMap<String, Object>();
        macroBinding.put(MACRO_PARAMS_KEY, parameters);
        macroBinding.put(MACRO_CONTENT_KEY, macroContent);
        macroBinding.put(MACRO_CONTEXT_KEY, context);
        macroBinding.put(MACRO_RESULT_KEY, null);

        // Extension point to add more wiki macro bindings
        try {
            List<WikiMacroBindingInitializer> bindingInitializers =
                this.componentManager.getInstanceList(WikiMacroBindingInitializer.class);

            for (WikiMacroBindingInitializer bindingInitializer : bindingInitializers) {
                bindingInitializer.initialize(this.macroDocumentReference, parameters, macroContent, context,
                    macroBinding);
            }
        } catch (ComponentLookupException e) {
            // TODO: we should probably log something but that should never happen
        }

        // Execute the macro
        ObservationManager observation = null;
        try {
            observation = this.componentManager.getInstance(ObservationManager.class);
        } catch (ComponentLookupException e) {
            // TODO: maybe log something
        }

        // Get XWiki context
        Map<String, Object> xwikiContext = null;
        try {
            Execution execution = this.componentManager.getInstance(Execution.class);
            ExecutionContext econtext = execution.getContext();
            if (econtext != null) {
                xwikiContext = (Map<String, Object>) execution.getContext().getProperty("xwikicontext");
            }
        } catch (ComponentLookupException e) {
            // TODO: maybe log something
        }

        try {
            Transformation macroTransformation = this.componentManager.getInstance(Transformation.class, MACRO_HINT);

            if (xwikiContext != null) {
                // Place macro context inside xwiki context ($xcontext.macro).
                xwikiContext.put(MACRO_KEY, macroBinding);
            }

            MacroBlock wikiMacroBlock = context.getCurrentMacroBlock();

            MacroMarkerBlock wikiMacroMarker =
                new MacroMarkerBlock(wikiMacroBlock.getId(), wikiMacroBlock.getParameters(),
                    wikiMacroBlock.getContent(), xdom.getChildren(), wikiMacroBlock.isInline());

            // make sure to use provided metadatas
            MetaDataBlock metaDataBlock =
                new MetaDataBlock(Collections.<Block> singletonList(wikiMacroMarker), xdom.getMetaData());

            // Make sure the context XDOM contains the html macro content
            wikiMacroBlock.getParent().replaceChild(metaDataBlock, wikiMacroBlock);

            try {
                if (observation != null) {
                    observation.notify(STARTEXECUTION_EVENT, this, macroBinding);
                }

                // Perform internal macro transformations.
                TransformationContext txContext = new TransformationContext(context.getXDOM(), this.syntax);
                txContext.setId(context.getId());

                RenderingContext renderingContext = componentManager.getInstance(RenderingContext.class);
                ((MutableRenderingContext) renderingContext).transformInContext(macroTransformation, txContext,
                    wikiMacroMarker);
            } finally {
                // Restore context XDOM to its previous state
                metaDataBlock.getParent().replaceChild(wikiMacroBlock, metaDataBlock);
            }

            return extractResult(wikiMacroMarker.getChildren(), macroBinding, context);
        } catch (Exception ex) {
            throw new MacroExecutionException("Error while performing internal macro transformations", ex);
        } finally {
            if (xwikiContext != null) {
                xwikiContext.remove(MACRO_KEY);
            }

            if (observation != null) {
                observation.notify(ENDEXECUTION_EVENT, this);
            }
        }
    }

    /**
     * Extract result of the wiki macro execution.
     * 
     * @param blocks the wiki macro content
     * @param macroContext the wiki macro context
     * @param context the macro execution context
     * @return the result
     */
    private List<Block> extractResult(List<Block> blocks, Map<String, Object> macroContext,
        MacroTransformationContext context)
    {
        Object resultObject = macroContext.get(MACRO_RESULT_KEY);

        List<Block> result;
        if (resultObject != null && resultObject instanceof List) {
            result = (List<Block>) macroContext.get(MACRO_RESULT_KEY);
        } else {
            result = blocks;
            // If in inline mode remove any top level paragraph.
            if (context.isInline()) {
                removeTopLevelParagraph(result);
            }
        }

        return result;
    }

    /**
     * Removes any top level paragraph since for example for the following use case we don't want an extra paragraph
     * block: <code>= hello {{velocity}}world{{/velocity}}</code>.
     * 
     * @param blocks the blocks to check and convert
     */
    private void removeTopLevelParagraph(List<Block> blocks)
    {
        // Remove any top level paragraph so that the result of a macro can be used inline for example.
        // We only remove the paragraph if there's only one top level element and if it's a paragraph.
        if ((blocks.size() == 1) && blocks.get(0) instanceof ParagraphBlock) {
            Block paragraphBlock = blocks.remove(0);
            blocks.addAll(0, paragraphBlock.getChildren());
        }
    }

    /**
     * Clone and filter wiki macro content depending of the context.
     * 
     * @param context the macro execution context
     * @return the cleaned wiki macro content
     */
    private XDOM prepareWikiMacroContent(MacroTransformationContext context)
    {
        XDOM xdom = this.content.clone();

        // Macro code segment is always parsed into a separate xdom document. Now if this code segment starts with
        // another macro block, it will always be interpreted as a block macro regardless of the current wiki macro's
        // context (because as far as the nested macro is concerned, it starts on a new line). This will introduce
        // unnecessary paragraph elements when the wiki macro is used inline, so we need to force such opening macro
        // blocks to behave as inline macros if the wiki macro is used inline.
        if (context.isInline()) {
            List<Block> children = xdom.getChildren();
            if (children.size() > 0 && children.get(0) instanceof MacroBlock) {
                MacroBlock old = (MacroBlock) children.get(0);
                MacroBlock replacement = new MacroBlock(old.getId(), old.getParameters(), old.getContent(), true);
                xdom.replaceChild(replacement, old);
            }
        }

        return xdom;
    }

    /**
     * Check validity of the given macro parameters and content.
     * 
     * @param parameters the macro parameters
     * @param macroContent the macro content
     * @throws MacroExecutionException given parameters of content is invalid
     */
    private void validate(WikiMacroParameters parameters, String macroContent) throws MacroExecutionException
    {
        // First verify that all mandatory parameters are provided.
        // Note that we currently verify automatically mandatory parameters in Macro Transformation but for the moment
        // this is only checked for Java-based macros. Hence why we need to check here too.
        Map<String, ParameterDescriptor> parameterDescriptors = getDescriptor().getParameterDescriptorMap();
        for (String parameterName : parameterDescriptors.keySet()) {
            ParameterDescriptor parameterDescriptor = parameterDescriptors.get(parameterName);
            Object parameterValue = parameters.get(parameterName);
            if (parameterDescriptor.isMandatory() && (null == parameterValue)) {
                throw new MacroParameterException(String.format("Parameter [%s] is mandatory", parameterName));
            }

            // Set default parameter value if applicable.
            Object parameterDefaultValue = parameterDescriptor.getDefaultValue();
            if (parameterValue == null && parameterDefaultValue != null) {
                parameters.set(parameterName, parameterDefaultValue);
            }
        }

        // Verify the a macro content is not empty if it was declared mandatory.
        if (getDescriptor().getContentDescriptor() != null && getDescriptor().getContentDescriptor().isMandatory()) {
            if (macroContent == null || macroContent.length() == 0) {
                throw new MacroExecutionException("Missing macro content: this macro requires content (a body)");
            }
        }
    }

    @Override
    public MacroDescriptor getDescriptor()
    {
        return this.descriptor;
    }

    @Override
    public int getPriority()
    {
        return 1000;
    }

    @Override
    public String getId()
    {
        return this.descriptor.getId().getId();
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.macroDocumentReference;
    }

    @Override
    public DocumentReference getAuthorReference()
    {
        return this.macroAuthor;
    }

    @Override
    public int compareTo(Macro< ? > macro)
    {
        return getPriority() - macro.getPriority();
    }

    @Override
    public boolean supportsInlineMode()
    {
        return this.supportsInlineMode;
    }
}
