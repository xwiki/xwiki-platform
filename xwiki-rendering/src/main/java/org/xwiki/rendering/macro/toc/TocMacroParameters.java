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
package org.xwiki.rendering.macro.toc;

import java.util.Map;

import org.xwiki.rendering.macro.parameter.DefaultMacroParameters;
import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.toc.TocMacroDescriptor.Scope;

public class TocMacroParameters extends DefaultMacroParameters
{
    public TocMacroParameters(Map<String, String> parameters, TocMacroDescriptor macroDescriptor)
    {
        super(parameters, macroDescriptor);
    }

    /**
     * @return the minimum section level. For example if 2 then level 1 sections will not be listed.
     * @exception MacroParameterException error when converting value.
     */
    public int getStart() throws MacroParameterException
    {
        return this.<Integer> getParameterValue(TocMacroDescriptor.PARAM_START);
    }

    /**
     * @return the maximum section level. For example if 3 then all section levels from 4 will not be listed.
     * @exception MacroParameterException error when converting value.
     */
    public int getDepth() throws MacroParameterException
    {
        return this.<Integer> getParameterValue(TocMacroDescriptor.PARAM_DEPTH);
    }

    /**
     * @return local or page. If local only section in the current scope will be listed. For example if the macro is
     *         written in a section, only subsections of this section will be listed.
     * @exception MacroParameterException error when converting value.
     */
    public Scope getScope() throws MacroParameterException
    {
        return getParameterValue(TocMacroDescriptor.PARAM_SCOPE);
    }

    /**
     * @return true or false. If true the section title number is printed.
     * @exception MacroParameterException error when converting value.
     */
    public boolean numbered() throws MacroParameterException
    {
        return this.<Boolean> getParameterValue(TocMacroDescriptor.PARAM_NUMBERED);
    }
}
