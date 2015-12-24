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

import org.xwiki.rendering.macro.descriptor.ContentDescriptor;

/**
 * Base class for a signable macro which does not support any parameter.
 *
 * @version $Id$
 * @since 6.1M2
 */
public abstract class AbstractNoParameterSignableMacro extends AbstractSignableMacro<Object>
{
    /**
     * Create and initialize a descriptor with no parameters.
     *
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     */
    public AbstractNoParameterSignableMacro(String name)
    {
        super(name);
    }

    /**
     * Create and initialize a descriptor with no parameters.
     *
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description the description of the macro
     */
    public AbstractNoParameterSignableMacro(String name, String description)
    {
        super(name, description);
    }

    /**
     * Create and initialize a descriptor with no parameters.
     *
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description the description of the macro
     * @param contentDescriptor the {@link org.xwiki.rendering.macro.descriptor.ContentDescriptor} describing the content of this macro.
     */
    public AbstractNoParameterSignableMacro(String name, String description, ContentDescriptor contentDescriptor)
    {
        super(name, description, contentDescriptor);
    }
}
