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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.AbstractAsyncContentBaseObjectWikiComponent;
import org.xwiki.rendering.async.internal.AsyncRendererConfiguration;
import org.xwiki.rendering.block.AbstractBlock;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.internal.macro.script.NestedScriptMacroEnabled;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.MacroPreparationException;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of {@link WikiMacro}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component(roles = DefaultWikiMacro.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultWikiMacro extends AbstractAsyncContentBaseObjectWikiComponent
    implements WikiMacro, NestedScriptMacroEnabled, WikiMacroConstants
{
    /**
     * The name of the macro block attribute holding the id of the block.
     * 
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    public static final String ATTRIBUTE_PREPARE_BLOCK_ID =
        "org.xwiki.rendering.wikimacro.internal.DefaultWikiMacro#BLOCK_ID";

    @Inject
    private MacroContentParser contentParser;

    /**
     * The {@link MacroDescriptor} for this macro.
     */
    private MacroDescriptor descriptor;

    private int macroPriority;

    private boolean executionIsolated;

    /**
     * Constructs a new {@link DefaultWikiMacro}.
     * 
     * @param baseObject the object containing the component definition
     * @param descriptor the {@link MacroDescriptor} describing this macro.
     * @throws WikiComponentException when failing to parse the content
     * @since 15.9RC1
     */
    void initialize(BaseObject baseObject, MacroDescriptor descriptor) throws WikiComponentException
    {
        super.initialize(baseObject, Macro.class, descriptor.getId().getId());

        this.descriptor = descriptor;
        this.macroPriority = baseObject.getIntValue(MACRO_PRIORITY_PROPERTY, 1000);
        this.executionIsolated = baseObject.getIntValue(MACRO_EXECUTION_ISOLATED_PROPERTY, 0) == 1;
    }

    @Override
    protected String getContentPropertyName()
    {
        return MACRO_CODE_PROPERTY;
    }

    @Override
    public List<Block> execute(WikiMacroParameters parameters, String macroContent, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Create renderer
        DefaultWikiMacroRenderer renderer;
        try {
            renderer = this.componentManager.getInstance(DefaultWikiMacroRenderer.class);
        } catch (ComponentLookupException e) {
            throw new MacroExecutionException("Failed to create wiki macro rendeder", e);
        }
        validate(parameters, macroContent);
        // Initialize the renderer
        renderer.initialize(this, parameters, macroContent, context);

        AsyncRendererConfiguration configuration = new AsyncRendererConfiguration();
        configuration.setSecureReference(getDocumentReference(), getAuthorReference());
        configuration.setContextEntries(this.contextEntries);

        // Execute the renderer
        Block result;
        try {
            result = this.executor.execute(renderer, configuration);
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to execute wiki macro", e);
        }

        return result instanceof CompositeBlock ? result.getChildren() : Arrays.asList(result);
    }

    @Override
    public void prepare(MacroBlock macroBlock) throws MacroPreparationException
    {
        // Prepare the content
        prepareContent(macroBlock);

        // Prepare the block id
        prepareBlockId(macroBlock);
    }

    private void prepareContent(MacroBlock macroBlock) throws MacroPreparationException
    {
        // Prepare the content
        if (macroBlock.getContent() != null) {
            ContentDescriptor contentDescriptor = getDescriptor().getContentDescriptor();
            if (contentDescriptor != null && contentDescriptor.getType().equals(Block.LIST_BLOCK_TYPE)) {
                this.contentParser.prepareContentWiki(macroBlock);
            }
        }
    }

    private void prepareBlockId(MacroBlock macroBlock)
    {
        if (!isAsyncAllowed() && !isCacheAllowed()) {
            // Use a less expensive way to generate the id when the macro is not async or cacheable
            // We can also prepare it since it does not depend on the context
            macroBlock.setAttribute(ATTRIBUTE_PREPARE_BLOCK_ID, getFastBlockId(macroBlock));
        }
    }

    public Object getBlockId(MacroBlock macroBlock)
    {
        Object cachedIndex = macroBlock.getAttribute(ATTRIBUTE_PREPARE_BLOCK_ID);
        if (cachedIndex != null) {
            return cachedIndex;
        }

        Object index = null;
        if (isAsyncAllowed() || isCacheAllowed()) {
            // We need a stable id when the macro is async or cacheable
            // Find index of the macro in the XDOM
            // TODO: find a faster and safer way to identify a given macro block (indexOf can be very slow of the XDOM
            // is huge, and yet it's theoretically possible for it to not be 100% stable depending on what previous
            // dynamic macro may be doing or if other macros modify the XDOM)
            Block rootBlock = macroBlock.getRoot();
            if (rootBlock instanceof AbstractBlock abstractblock) {
                index = abstractblock.indexOf(macroBlock);
            }
        }

        if (index == null) {
            // Use a less expensive way to generate the id when the macro is not async or cacheable
            index = getFastBlockId(macroBlock);
        }

        macroBlock.setAttribute(ATTRIBUTE_PREPARE_BLOCK_ID, index);

        return index;
    }

    public Object getFastBlockId(MacroBlock macroBlock)
    {
        return UUID.randomUUID().toString();
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
        for (ParameterDescriptor parameterDescriptor : parameterDescriptors.values()) {
            Object parameterValue = parameters.get(parameterDescriptor.getId());
            if (parameterDescriptor.isMandatory() && (null == parameterValue)) {
                throw new MacroParameterException(
                    String.format("Parameter [%s] is mandatory", parameterDescriptor.getId()));
            }

            // Set default parameter value if applicable.
            Object parameterDefaultValue = parameterDescriptor.getDefaultValue();
            if (parameterValue == null && parameterDefaultValue != null) {
                parameters.set(parameterDescriptor.getId(), parameterDefaultValue);
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
        return this.macroPriority;
    }

    @Override
    public String getId()
    {
        return getRoleHint();
    }

    @Override
    public int compareTo(Macro<?> macro)
    {
        return getPriority() - macro.getPriority();
    }

    @Override
    public boolean supportsInlineMode()
    {
        return this.descriptor.supportsInlineMode();
    }

    boolean isAsyncAllowed()
    {
        return this.asyncAllowed;
    }

    boolean isCacheAllowed()
    {
        return this.cacheAllowed;
    }

    @Override
    public boolean isExecutionIsolated(WikiMacroParameters parameters, String content)
    {
        return this.executionIsolated;
    }
}
