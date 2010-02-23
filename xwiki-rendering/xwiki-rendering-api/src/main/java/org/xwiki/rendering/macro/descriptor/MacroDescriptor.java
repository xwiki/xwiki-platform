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

import java.util.Map;

import org.xwiki.rendering.macro.MacroId;

/**
 * Describe a Macro (macro description and macro parameters description).
 * 
 * @version $Id$
 * @since 1.6M1
 */
public interface MacroDescriptor
{
    /**
     * @return the id of the macro
     * @since 2.3M1
     */
    MacroId getId();
    
    /**
     * @return the human-readable name of the macro (eg "Table Of Contents" for the TOC macro).
     * @since 2.0M3
     */
    String getName();
    
    /**
     * @return the description of the macro.
     */
    String getDescription();

    /**
     * @return the class of the JAVA bean containing macro parameters.
     */
    Class< ? > getParametersBeanClass();

    /**
     * @return describe the macro content. If null the macro does not support content.
     * @since 1.9M1
     */
    ContentDescriptor getContentDescriptor();

    /**
     * @return a {@link Map} containing the {@link ParameterDescriptor} for each parameter.
     * @since 1.7M2
     */
    Map<String, ParameterDescriptor> getParameterDescriptorMap();
    
    /**
     * A macro can define a default classification category under which it falls. For an example, the "skype" macro
     * would fall under the "Communication" category of macros. However, a wiki administrator has the ability to
     * override the default category for a given macro in order to organize categories as he sees fit. Thus this
     * default category is only an indication from the macro author about what category the macro should fall.
     * 
     * @return the default category under which this macro should be listed or null if the macro doesn't have a
     *         default category defined
     * @since 2.0M3
     */
    String getDefaultCategory();
}
