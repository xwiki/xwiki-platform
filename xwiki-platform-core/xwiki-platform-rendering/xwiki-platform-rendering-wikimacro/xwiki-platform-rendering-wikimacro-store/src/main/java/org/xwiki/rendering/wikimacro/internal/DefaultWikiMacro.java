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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.AbstractAsyncContentBaseObjectWikiComponent;
import org.xwiki.rendering.async.internal.AsyncRendererConfiguration;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.internal.macro.script.NestedScriptMacroEnabled;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
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
     * The {@link MacroDescriptor} for this macro.
     */
    private MacroDescriptor descriptor;

    private int macroPriority;

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
}
