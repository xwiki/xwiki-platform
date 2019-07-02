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

    public WikiMacroBinding(MacroDescriptor descriptor, WikiMacroParameters parameters, String content)
    {
        super();
        setParameters(parameters);
        setDescriptor(descriptor);
        setContent(content);
    }

    public WikiMacroParameters getParameters()
    {
        return (WikiMacroParameters) this.get(PARAMETERS);
    }

    protected void setParameters(WikiMacroParameters parameters)
    {
        this.put(PARAMETERS, parameters);
    }

    public String getContent()
    {
        return (String) this.get(CONTENT);
    }

    protected void setContent(String content)
    {
        this.put(CONTENT, content);
    }

    public MacroDescriptor getDescriptor()
    {
        return (MacroDescriptor) this.get(DESCRIPTOR);
    }

    protected void setDescriptor(MacroDescriptor descriptor)
    {
        this.put(DESCRIPTOR, descriptor);
    }
}
