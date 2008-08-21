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
package org.xwiki.rendering.macro.descriptor;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.rendering.macro.parameter.DefaultMacroParameters;
import org.xwiki.rendering.macro.parameter.MacroParameters;
import org.xwiki.rendering.macro.parameter.descriptor.MacroParameterDescriptor;

/**
 * Describe the macro.
 * 
 * @param <P>
 * @version $Id$
 * @since 1.6M1
 */
public abstract class AbstractMacroDescriptor<P extends MacroParameters> implements MacroDescriptor<P>
{
    /**
     * The list of parameters descriptors of the macro.
     */
    private Map<String, MacroParameterDescriptor< ? >> parameterDescriptorMap =
        new HashMap<String, MacroParameterDescriptor< ? >>();

    public AbstractMacroDescriptor()
    {
    }

    /**
     * @param parameterDescriptor add parameter descriptor to the macro parameters manager.
     */
    protected void registerParameterDescriptor(MacroParameterDescriptor< ? > parameterDescriptor)
    {
        this.parameterDescriptorMap.put(parameterDescriptor.getName().toLowerCase(), parameterDescriptor);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.MacroDescriptor#getParameterDescriptor(java.lang.String)
     */
    public <D extends MacroParameterDescriptor< ? >> D getParameterDescriptor(String name)
    {
        return (D) this.parameterDescriptorMap.get(name.toLowerCase());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.MacroDescriptor#createMacroParameters(java.util.Map)
     */
    public P createMacroParameters(Map<String, String> parameters)
    {
        return (P) new DefaultMacroParameters(parameters, this);
    }
}
