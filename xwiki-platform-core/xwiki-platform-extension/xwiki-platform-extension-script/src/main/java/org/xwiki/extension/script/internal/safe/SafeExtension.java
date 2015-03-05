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

import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionFile;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.wrap.WrappingExtension;

/**
 * Provide a public script access to an extension.
 * 
 * @param <T> the extension type
 * @version $Id$
 * @since 4.0M2
 */
public class SafeExtension<T extends Extension> extends WrappingExtension<T>
{
    /**
     * The provider of instances safe for public scripts.
     */
    private ScriptSafeProvider<Object> safeProvider;

    /**
     * @param extension the wrapped extension
     * @param safeProvider the provider of instances safe for public scripts
     */
    public SafeExtension(T extension, ScriptSafeProvider<Object> safeProvider)
    {
        super(extension);

        this.safeProvider = safeProvider;
    }

    /**
     * @param <S> the type of the object
     * @param unsafe the unsafe object
     * @return the safe version of the object
     */
    protected <S> S safe(Object unsafe)
    {
        return this.safeProvider.get(unsafe);
    }

    // Extension

    @Override
    public <F> F get(String fieldName)
    {
        return safe(super.get(fieldName));
    }

    @Override
    public <P> P getProperty(String key)
    {
        return safe(super.getProperty(key));
    }

    @Override
    public <P> P getProperty(String key, P def)
    {
        return safe(super.getProperty(key));
    }

    @Override
    public ExtensionFile getFile()
    {
        return safe(super.getRepository());
    }

    @Override
    public ExtensionRepository getRepository()
    {
        return safe(super.getRepository());
    }
}
