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

import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.CoreExtensionFile;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.wrap.WrappingCoreExtension;

/**
 * Provide a public script access to a core extension.
 * 
 * @param <T> the extension type
 * @version $Id$
 * @since 4.0M2
 */
public class SafeCoreExtension<T extends CoreExtension> extends WrappingCoreExtension<T> implements CoreExtension
{
    /**
     * The provider of instances safe for public scripts.
     */
    private ScriptSafeProvider<Object> safeProvider;

    /**
     * @param extension the wrapped core extension
     * @param safeProvider the provider of instances safe for public scripts
     */
    public SafeCoreExtension(T extension, ScriptSafeProvider<Object> safeProvider)
    {
        super(extension);

        this.safeProvider = safeProvider;
    }

    // Extension

    @Override
    public CoreExtensionFile getFile()
    {
        return this.safeProvider.get(super.getFile());
    }

    @Override
    public ExtensionRepository getRepository()
    {
        return this.safeProvider.get(super.getRepository());
    }
}
