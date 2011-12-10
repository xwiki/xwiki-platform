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
package org.xwiki.extension.unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.CoreExtensionFile;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionFile;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.LocalExtensionFile;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;

/**
 * Utility class that offers methods for wrapping extension handlers inside read-only wrappers which can be safely used
 * from public scripts.
 * 
 * @version $Id$
 */
public final class UnmodifiableUtils
{
    /**
     * Prevents instantiation of this utility class.
     */
    private UnmodifiableUtils()
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
    public static <K> Map<K, Collection<LocalExtension>> unmodifiableExtensions(
        Map<K, Collection<LocalExtension>> extensions)
    {
        Map<K, Collection<LocalExtension>> wrappedExtensions = new LinkedHashMap<K, Collection<LocalExtension>>();

        for (Map.Entry<K, Collection<LocalExtension>> entry : extensions.entrySet()) {
            wrappedExtensions.put(entry.getKey(),
                UnmodifiableUtils.<LocalExtension, LocalExtension> unmodifiableExtensions(entry.getValue()));
        }

        return wrappedExtensions;
    }

    /**
     * Wrap a collection of internal (read-write) extension handlers into safe read-only bridges.
     * 
     * @param <T> the expected output type, should be a generic extension type, like {@link Extension},
     *            {@link LocalExtension} or {@code CoreExtension}
     * @param <U> the input type, a read-write subtype of T
     * @param extensions the read-write extension handlers to wrap
     * @return an equivalent collection of read-only wrappers
     */
    public static <T extends Extension, U extends T> Collection<T> unmodifiableExtensions(Collection<U> extensions)
    {
        List<T> wrappedExtensions = new ArrayList<T>(extensions.size());

        for (U extension : extensions) {
            T wrapper = unmodifiableExtension(extension);
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
     *            {@link LocalExtension} or {@code CoreExtension}
     * @param <U> the input type, a subtype of T
     * @param extension the read-write extension handler to wrap
     * @return a read-only wrapper, or {@code null} if the provided instance is {@code null}
     */
    public static <T extends Extension, U extends T> T unmodifiableExtension(U extension)
    {
        T wrappedExtension;

        if (extension == null) {
            wrappedExtension = null;
        } else if (extension instanceof CoreExtension) {
            wrappedExtension = (T) new UnmodifiableCoreExtension<CoreExtension>((CoreExtension) extension);
        } else if (extension instanceof LocalExtension) {
            wrappedExtension = (T) new UnmodifiableLocalExtension<LocalExtension>((LocalExtension) extension);
        } else {
            wrappedExtension = (T) new UnmodifiableExtension<Extension>(extension);
        }

        return wrappedExtension;
    }

    /**
     * Wrap an internal (read-write) repository handler into a safe read-only bridge.
     * 
     * @param <T> the expected output type, should be a generic repository type, like {@link ExtensionRepository},
     *            {@link LocalExtensionRepository} or {@code CoreExtensionRepository}
     * @param <U> the input type, a subtype of T
     * @param extensionRepository the read-write repository handler to wrap
     * @return a read-only wrapper, or {@code null} if the provided instance is {@code null}
     */
    public static <T extends ExtensionRepository, U extends T> T unmodifiableExtensionRepository(U extensionRepository)
    {
        T wrappedExtensionRepository;

        if (extensionRepository == null) {
            wrappedExtensionRepository = null;
        } else if (extensionRepository instanceof CoreExtensionRepository) {
            wrappedExtensionRepository =
                (T) new UnmodifiableCoreExtensionRepository<CoreExtensionRepository>(
                    (CoreExtensionRepository) extensionRepository);
        } else if (extensionRepository instanceof LocalExtensionRepository) {
            wrappedExtensionRepository =
                (T) new UnmodifiableLocalExtensionRepository<LocalExtensionRepository>(
                    (LocalExtensionRepository) extensionRepository);
        } else {
            wrappedExtensionRepository =
                (T) new UnmodifiableExtensionRepository<ExtensionRepository>(extensionRepository);
        }

        return wrappedExtensionRepository;
    }

    /**
     * Wrap an internal (read-write) extension file handler into a safe read-only bridge.
     * 
     * @param <T> the expected output type, should be a generic extension file type, like {@link ExtensionFile},
     *            {@link LocalExtensionFile} or {@code CoreExtensionFile}
     * @param <U> the input type, a subtype of T
     * @param extensionFile the read-write extension file handler to wrap
     * @return a read-only wrapper, or {@code null} if the provided instance is {@code null}
     */
    public static <T extends ExtensionFile, U extends T> T unmodifiableExtensionFile(U extensionFile)
    {
        T wrappedExtensionFile;

        if (extensionFile == null) {
            wrappedExtensionFile = null;
        } else if (extensionFile instanceof CoreExtension) {
            wrappedExtensionFile =
                (T) new UnmodifiableCoreExtensionFile<CoreExtensionFile>((CoreExtensionFile) extensionFile);
        } else if (extensionFile instanceof LocalExtension) {
            wrappedExtensionFile =
                (T) new UnmodifiableLocalExtensionFile<LocalExtensionFile>((LocalExtensionFile) extensionFile);
        } else {
            wrappedExtensionFile = (T) new UnmodifiableExtensionFile<ExtensionFile>(extensionFile);
        }

        return wrappedExtensionFile;
    }
}
