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
package org.xwiki.rendering.macro;

import org.xwiki.rendering.macro.parameter.AbstractMacroParameterManager;
import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.parameter.descriptor.BooleanMacroParameterDescriptor;
import org.xwiki.rendering.macro.parameter.descriptor.EnumMacroParameterDescriptor;
import org.xwiki.rendering.macro.parameter.descriptor.IntegerMacroParameterDescriptor;

/**
 * Parse and convert TOC macro parameters values into more readable java values (like boolean, int etc.).
 * 
 * @version $Id: $
 * @since 1.6M1
 */
// TODO: Use an I8N service to translate the descriptions in several languages
public class TocMacroParameterManager extends AbstractMacroParameterManager
{
    /**
     * @version $Id: $
     */
    public enum Scope
    {
        /**
         * List section starting where macro block is located in the XDOM.
         */
        LOCAL,

        /**
         * List the sections of the whole page.
         */
        PAGE;
    };

    /**
     * The name of the macro parameter "start".
     */
    private static final String PARAM_START = "start";

    /**
     * The description of the macro parameter "start".
     */
    private static final String PARAM_START_DESC =
        "The minimum section level. For example if 2 then level 1 sections will not be listed.";

    /**
     * The default value of the macro parameter "start".
     */
    private static final int PARAM_START_DEF = 2;

    /**
     * The name of the macro parameter "depth".
     */
    private static final String PARAM_DEPTH = "depth";

    /**
     * The description of the macro parameter "depth".
     */
    private static final String PARAM_DEPTH_DESC =
        "The maximum section level. For example if 3 then all section levels from 4 will not be listed.";

    /**
     * The default value of the macro parameter "depth".
     */
    private static final int PARAM_DEPTH_DEF = 3;

    /**
     * The name of the macro parameter "scope".
     */
    private static final String PARAM_SCOPE = "scope";

    /**
     * The description of the macro parameter "scope".
     */
    private static final String PARAM_SCOPE_DESC =
        "local or page. If local only section in the current scope will be listed."
            + " For example if the macro is written in a section, only subsections of this section will be listed.";

    /**
     * The default value of the macro parameter "scope".
     */
    private static final Scope PARAM_SCOPE_DEF = Scope.PAGE;

    /**
     * The name of the macro parameter "numbered".
     */
    private static final String PARAM_NUMBERED = "numbered";

    /**
     * The description of the macro parameter "numbered".
     */
    private static final String PARAM_NUMBERED_DESC = "true or false. If true the section title number is printed.";

    /**
     * The default value of the macro parameter "numbered".
     */
    private static final boolean PARAM_NUMBERED_DEF = false;

    /**
     * Set the macro parameters class list.
     */
    public TocMacroParameterManager()
    {
        IntegerMacroParameterDescriptor startParamClass =
            new IntegerMacroParameterDescriptor(PARAM_START, PARAM_START_DESC, PARAM_START_DEF);
        startParamClass.setMinValue(1);
        register(startParamClass);

        IntegerMacroParameterDescriptor depthParamClass =
            new IntegerMacroParameterDescriptor(PARAM_DEPTH, PARAM_DEPTH_DESC, PARAM_DEPTH_DEF);
        depthParamClass.setMinValue(1);
        register(depthParamClass);

        EnumMacroParameterDescriptor<Scope> scopeParamClass =
            new EnumMacroParameterDescriptor<Scope>(PARAM_SCOPE, PARAM_SCOPE_DESC, PARAM_SCOPE_DEF);
        register(scopeParamClass);

        BooleanMacroParameterDescriptor numberedParamClass =
            new BooleanMacroParameterDescriptor(PARAM_NUMBERED, PARAM_NUMBERED_DESC, PARAM_NUMBERED_DEF);
        register(numberedParamClass);
    }

    /**
     * @return the minimum section level. For example if 2 then level 1 sections will not be listed.
     * @exception MacroParameterException error when converting value.
     */
    public int getStart() throws MacroParameterException
    {
        return this.<Integer> getParameterValue(PARAM_START);
    }

    /**
     * @return the maximum section level. For example if 3 then all section levels from 4 will not be listed.
     * @exception MacroParameterException error when converting value.
     */
    public int getDepth() throws MacroParameterException
    {
        return this.<Integer> getParameterValue(PARAM_DEPTH);
    }

    /**
     * @return local or page. If local only section in the current scope will be listed. For example if the macro is
     *         written in a section, only subsections of this section will be listed.
     * @exception MacroParameterException error when converting value.
     */
    public Scope getScope() throws MacroParameterException
    {
        return getParameterValue(PARAM_SCOPE);
    }

    /**
     * @return true or false. If true the section title number is printed.
     * @exception MacroParameterException error when converting value.
     */
    public boolean numbered() throws MacroParameterException
    {
        return this.<Boolean> getParameterValue(PARAM_NUMBERED);
    }
}
