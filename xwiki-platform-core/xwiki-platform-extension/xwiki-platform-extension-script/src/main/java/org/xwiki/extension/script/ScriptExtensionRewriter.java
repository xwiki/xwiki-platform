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
package org.xwiki.extension.script;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionRewriter;
import org.xwiki.extension.internal.ExtensionUtils;
import org.xwiki.extension.wrap.WrappingExtension;

/**
 * Script oriented helper with various ways of rewriting extensions.
 * 
 * @version $Id$
 * @since 8.4.2
 * @since 9.0RC1
 */
public class ScriptExtensionRewriter implements ExtensionRewriter
{
    private static final long serialVersionUID = 1L;

    private static final List<String> ROOT_NAMESPACES = Arrays.asList((String) null);

    private Set<String> installOnRootNamespace = new HashSet<>();

    /**
     * @param type the type of extensions that should be installed on root namespace
     */
    public void installExtensionTypeOnRootNamespace(String type)
    {
        this.installOnRootNamespace.add(type);
    }

    // ExtensionRewriter

    @Override
    public Extension rewrite(Extension extension)
    {
        if (this.installOnRootNamespace.contains(extension.getType())) {
            WrappingExtension<?> wrapper = ExtensionUtils.wrap(extension);

            // Overwrite

            wrapper.setOverwrite(Extension.FIELD_ALLOWEDNAMESPACES, ROOT_NAMESPACES);

            return wrapper;
        }

        return extension;
    }
}
