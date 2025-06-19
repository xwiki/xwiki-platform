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
package org.xwiki.wysiwyg;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Configuration options for the Wysiwyg Module.
 *
 * @version $Id$
 * @since 17.5.0RC1
 */
@Role
@Unstable
public interface WysiwygConfiguration
{
    /**
     * Define if the macro parameters type should be inferred from their default value when no specific type is
     * indicated to build the UI. Default is {@code true} for backward compatibility reasons.
     *
     * @return {@code true} if the type should be computed from the default value, {@code false} if the type defined
     * in the parameter should always be used.
     */
    boolean inferMacroParameterTypeBasedOnDefaultValue();
}
