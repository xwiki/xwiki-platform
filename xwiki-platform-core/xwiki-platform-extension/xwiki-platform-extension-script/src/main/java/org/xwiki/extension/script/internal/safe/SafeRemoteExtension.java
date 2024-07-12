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

import org.xwiki.extension.ExtensionFile;
import org.xwiki.extension.RemoteExtension;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.wrap.WrappingRemoteExtension;
import org.xwiki.script.safe.ScriptSafeProvider;

/**
 * Provide a public script access to a rating extension.
 * 
 * @param <T> the extension type
 * @version $Id$
 * @since 8.3RC1
 */
public class SafeRemoteExtension<T extends RemoteExtension> extends WrappingRemoteExtension<T>
    implements RemoteExtension
{
    /**
     * The provider of instances safe for public scripts.
     */
    private ScriptSafeProvider<Object> safeProvider;

    /**
     * @param remoteExtension the wrapped rating extension
     * @param safeProvider the provider of instances safe for public scripts
     */
    public SafeRemoteExtension(T remoteExtension, ScriptSafeProvider<Object> safeProvider)
    {
        super(remoteExtension);

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
    public ExtensionFile getFile()
    {
        return safe(super.getFile());
    }

    @Override
    public ExtensionRepository getRepository()
    {
        return safe(super.getRepository());
    }
}
