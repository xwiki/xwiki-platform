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
package org.xwiki.platform.security.requiredrights.display;

import java.util.function.Supplier;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.Block;
import org.xwiki.stability.Unstable;

/**
 * Provides a block supplier for a given object and (optionally) parameters. This is used to get a {@link Block} for
 * displaying a message, a code block, a macro or an XObject in a result of a required rights analysis.
 * <p>XWiki provides the following implementations of this role:</p>
 * <ul>
 *     <li>{@code BlockSupplierProvider<BaseObject>}: displays all properties of an XObject</li>
 *     <li>{@code BlockSupplierProvider<MacroBlock>}: displays a macro including all parameters and content.</li>
 *     <li>{@code BlockSupplierProvider<String>} with name {@code stringCode}: displays a string as a code block.</li>
 *     <li>{@code BlockSupplierProvider<String>} with name {@code translation}: displays a translation message with
 *     the given parameters.</li>
 * </ul>
 *
 * @param <T> the type of the object
 * @version $Id$
 * @since 16.3.0RC1
 */
@Role
@Unstable
public interface BlockSupplierProvider<T>
{
    /**
     * Get a block supplier for the given object and (optionally) parameters.
     *
     * @param object the object to get a block supplier for
     * @param parameters optional parameters for the object
     * @return a block supplier
     */
    Supplier<Block> get(T object, Object... parameters);
}
