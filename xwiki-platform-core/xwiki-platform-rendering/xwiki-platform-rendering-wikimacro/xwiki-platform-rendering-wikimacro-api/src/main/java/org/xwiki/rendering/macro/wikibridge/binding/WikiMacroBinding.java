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
package org.xwiki.rendering.macro.wikibridge.binding;

import java.util.HashMap;

import javax.script.Bindings;

import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.stability.Unstable;

/**
 * Binding representation for the wikimacros.
 *
 * @version $Id$
 * @since 11.6RC1
 * @since 11.3.2
 * @since 10.11.9
 */
@Unstable
public class WikiMacroBinding extends HashMap<String, Object> implements Bindings
{
    private static final String PARAMETERS = "parameters";

    private static final String CONTENT = "content";

    private static final String DESCRIPTOR = "descriptor";

    private static final String RESULT = "result";

    private static final String DOCUMENT = "doc";

    private static final String CONTEXT = "context";

    /**
     * Default constructor.
     * @param descriptor the macro descriptor to set for the binding.
     * @param parameters the parameters to set for the binding.
     * @param content the input macro content to set for the binding.
     * @param transformationContext the transformation context used for that wikimacro, is null if the
     *          transformation is done asynchronously.
     */
    public WikiMacroBinding(MacroDescriptor descriptor, WikiMacroParameters parameters, String content,
        MacroTransformationContext transformationContext)
    {
        super();
        setParameters(parameters);
        setDescriptor(descriptor);
        setContent(content);
        setContext(transformationContext);
    }

    /**
     * @return the parameters of the wikimacro.
     */
    public WikiMacroParameters getParameters()
    {
        return (WikiMacroParameters) this.get(PARAMETERS);
    }

    protected void setParameters(WikiMacroParameters parameters)
    {
        this.put(PARAMETERS, parameters);
    }

    /**
     * @return the input content of the wikimacro.
     */
    public String getContent()
    {
        return (String) this.get(CONTENT);
    }

    protected void setContent(String content)
    {
        this.put(CONTENT, content);
    }

    /**
     * @return the macro descriptor of the wikimacro.
     */
    public MacroDescriptor getDescriptor()
    {
        return (MacroDescriptor) this.get(DESCRIPTOR);
    }

    protected void setDescriptor(MacroDescriptor descriptor)
    {
        this.put(DESCRIPTOR, descriptor);
    }

    /**
     * @return the result of the wikimacro.
     */
    public Object getResult()
    {
        return this.get(RESULT);
    }

    /**
     * Allows to set a result during the execution of the macro, which can be retrieved with {@link #getResult()}.
     * @param result the result to set.
     */
    public void setResult(Object result)
    {
        this.put(RESULT, result);
    }

    /**
     * @return the transformation context used for this macro, or null in case of asynchronous transformation.
     */
    public MacroTransformationContext getContext()
    {
        return (MacroTransformationContext) this.get(CONTEXT);
    }

    protected void setContext(MacroTransformationContext context)
    {
        this.put(CONTEXT, context);
    }

    /**
     * Replace all the content of the current binding with the given binding.
     * @param wikiMacroBinding the macro binding values to use as replacement.
     * @since 11.6.1
     * @since 11.7RC1
     */
    public void replaceAll(WikiMacroBinding wikiMacroBinding)
    {
        this.clear();
        this.putAll(wikiMacroBinding);
    }
}
