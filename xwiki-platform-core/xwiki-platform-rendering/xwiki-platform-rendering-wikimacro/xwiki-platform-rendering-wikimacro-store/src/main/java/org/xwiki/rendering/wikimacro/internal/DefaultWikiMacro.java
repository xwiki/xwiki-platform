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

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.AbstractAsyncContentBaseObjectWikiComponent;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.script.NestedScriptMacroEnabled;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameters;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of {@link WikiMacro}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class DefaultWikiMacro extends AbstractAsyncContentBaseObjectWikiComponent
    implements WikiMacro, NestedScriptMacroEnabled, WikiMacroConstants
{
    /**
     * The {@link MacroDescriptor} for this macro.
     */
    private final MacroDescriptor descriptor;

    /**
     * Whether this macro supports inline mode or not.
     */
    private boolean supportsInlineMode;

    /**
     * Constructs a new {@link DefaultWikiMacro}.
     * 
     * @param baseObject the object containing the component definition
     * @param supportsInlineMode says if macro support inline mode or not
     * @param descriptor the {@link MacroDescriptor} describing this macro.
     * @param componentManager {@link ComponentManager} component used to look up for other components.
     * @throws WikiComponentException when failing to parse the content
     * @throws ComponentLookupException when failing to looked required components
     * @since 10.10RC1
     */
    public DefaultWikiMacro(BaseObject baseObject, boolean supportsInlineMode, MacroDescriptor descriptor,
        ComponentManager componentManager) throws WikiComponentException, ComponentLookupException
    {
        super(baseObject, Macro.class, descriptor.getId().getId(), componentManager);

        this.supportsInlineMode = supportsInlineMode;
        this.descriptor = descriptor;
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
        validate(parameters, macroContent);

        // Create renderer
        DefaultWikiMacroRenderer renderer;
        try {
            renderer = this.componentManager.getInstance(DefaultWikiMacroRenderer.class);
        } catch (ComponentLookupException e) {
            throw new MacroExecutionException("Failed to create wiki macro rendeder", e);
        }

        // Initialize the renderer
        renderer.initialize(this, parameters, macroContent, context);

        // Execute the renderer
        Block result;
        try {
            result = this.executor.execute(renderer, this.contextEntries, null, null);
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to execute wiki macro", e);
        }

        return result instanceof CompositeBlock ? result.getChildren() : Arrays.asList(result);
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
        return 1000;
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
        return this.supportsInlineMode;
    }

    boolean isAsyncAllowed()
    {
        return this.async;
    }

    boolean isCachedAllowed()
    {
        return this.cached;
    }

    XDOM getContent()
    {
        return this.xdom;
    }

    Syntax getSyntax()
    {
        return this.syntax;
    }
}
