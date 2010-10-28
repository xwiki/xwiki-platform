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
package org.xwiki.rendering.transformation.macro;

import java.util.Properties;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Configuration properties for the Macro Transformation module.
 * <p>
 * You can override the default values for each of the configuration properties below by defining them in XWiki's
 * global configuration file using a prefix of "rendering.transformation.macro." followed by the property name.
 * For example: <code>rendering.transformation.macro.categories = toc:my Category</code>
 *
 * @version $Id$
 * @since 2.6RC1
 */
@ComponentRole
public interface MacroTransformationConfiguration
{
    /**
     * @return the category names to use for the macros. These are the categories under which they'll be listed
     *         in UIs for example. If a category for a macro is null then the category defined in the macro
     *         descriptor (ie defined by the macro author) will be used
     */
    Properties getCategories();
}
