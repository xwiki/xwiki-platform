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
package org.xwiki.extension.wrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.LocalExtension;

/**
 * Utility class that offers methods for wrapping extension handlers inside read-only wrappers which can be safely used
 * from public scripts.
 * 
 * @version $Id$
 * @since 3.3M2
 */
public final class WrappingUtils
{
    /**
     * Prevents instantiation of this utility class.
     */
    private WrappingUtils()
    {
        // Empty
    }

    /**
     * Wrap a set of collections of internal (read-write) extension handlers into safe read-only bridges.
     * 
     * @param <K> the type of the map keys, can be anything
     * @param extensions the read-write extension handlers to wrap
     * @return an equivalent collection of read-only wrappers
     */
    public static <K> Map<K, Collection<LocalExtension>> wrapExtensions(Map<K, Collection<LocalExtension>> extensions)
    {
        Map<K, Collection<LocalExtension>> wrappedExtensions = new LinkedHashMap<K, Collection<LocalExtension>>();

        for (Map.Entry<K, Collection<LocalExtension>> entry : extensions.entrySet()) {
            wrappedExtensions.put(entry.getKey(),
                WrappingUtils.<LocalExtension, LocalExtension> wrapExtensions(entry.getValue()));
        }

        return wrappedExtensions;
    }

    /**
     * Wrap a collection of internal (read-write) extension handlers into safe read-only bridges.
     * 
     * @param <T> the expected output type, should be a generic extension type, like {@link Extension},
     *        {@link LocalExtension} or {@code CoreExtension}
     * @param <U> the input type, a read-write subtype of T
     * @param extensions the read-write extension handlers to wrap
     * @return an equivalent collection of read-only wrappers
     */
    public static <T extends Extension, U extends T> Collection<T> wrapExtensions(Collection<U> extensions)
    {
        List<T> wrappedExtensions = new ArrayList<T>(extensions.size());

        for (U extension : extensions) {
            T wrapper = wrapExtension(extension);
            if (wrapper != null) {
                wrappedExtensions.add(wrapper);
            }
        }

        return wrappedExtensions;
    }

    /**
     * Wrap an internal (read-write) extension handler into a safe read-only bridge.
     * 
     * @param <T> the expected output type, should be a generic extension type, like {@link Extension},
     *        {@link LocalExtension} or {@code CoreExtension}
     * @param <U> the input type, a subtype of T
     * @param extension the read-write extension handler to wrap
     * @return a read-only wrapper, or {@code null} if the provided instance is {@code null} or if there is no known
     *         wrapper for this type of internal extension handler
     */
    public static <T extends Extension, U extends T> T wrapExtension(U extension)
    {
        T wrappedExtension;

        if (extension == null) {
            wrappedExtension = null;
        } else if (extension instanceof CoreExtension) {
            wrappedExtension = (T) new WrappingCoreExtension<CoreExtension>((CoreExtension) extension);
        } else if (extension instanceof LocalExtension) {
            wrappedExtension = (T) new WrappingLocalExtension<LocalExtension>((LocalExtension) extension);
        } else {
            wrappedExtension = (T) new WrappingExtension<Extension>(extension);
        }

        return wrappedExtension;
    }
}
