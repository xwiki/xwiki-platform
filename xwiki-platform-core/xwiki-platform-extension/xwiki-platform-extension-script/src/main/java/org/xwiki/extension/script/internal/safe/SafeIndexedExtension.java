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
package org.xwiki.extension.script.internal.safe;

import org.xwiki.extension.index.IndexedExtension;
import org.xwiki.script.safe.ScriptSafeProvider;

/**
 * Provide a public script access to an indexed extension.
 * 
 * @param <T> the extension type
 * @version $Id$
 * @since 12.10
 */
public class SafeIndexedExtension<T extends IndexedExtension> extends SafeRatingExtension<T> implements IndexedExtension
{
    /**
     * @param extension the wrapped extension
     * @param safeProvider the provider of instances safe for public scripts
     */
    public SafeIndexedExtension(T extension, ScriptSafeProvider<Object> safeProvider)
    {
        super(extension, safeProvider);
    }

    // IndexedExtension

    @Override
    public Boolean isCompatible(String namespace)
    {
        return getWrapped().isCompatible(namespace);
    }
}
