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

/**
 * Describe a Macro (macro description and macro parameters description).
 * 
 * @version $Id$
 * @since 1.6M1
 */
public interface MacroDescriptor
{
    /**
     * @return the description of the macro.
     */
    String getDescription();

    /**
     * @return the class of the JAVA bean containing macro parameters.
     */
    Class< ? > getParametersBeanClass();

    /**
     * @return a {@link Map} containing the {@link ParameterDescriptor} for each parameter.
     * @since 1.7M2
     */
    Map<String, ParameterDescriptor> getParameterDescriptorMap();
}
