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
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.wrap.WrappingExtensionRepository;

/**
 * Provide a readonly access to a repository.
 * 
 * @param <T>
 * @version $Id$
 * @since 4.0M2
 */
public class SafeExtensionRepository<T extends ExtensionRepository> extends WrappingExtensionRepository<T>
{
    /**
     * The provider of instances safe for public scripts.
     */
    protected ScriptSafeProvider<Object> safeProvider;

    /**
     * @param repository the wrapped repository
     * @param safeProvider the provider of instances safe for public scripts
     */
    public SafeExtensionRepository(T repository, ScriptSafeProvider<Object> safeProvider)
    {
        super(repository);

        this.safeProvider = safeProvider;
    }

    /**
     * @param <S> the type of the object
     * @param unsafe the unsafe object
     * @return the safe version of the object
     */
    protected <S> S safe(S unsafe)
    {
        return this.safeProvider.get(unsafe);
    }

    // ExtensionRepository

    @Override
    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        return safe(super.resolve(extensionId));
    }

    @Override
    public Extension resolve(ExtensionDependency extensionDependency) throws ResolveException
    {
        return safe(super.resolve(extensionDependency));
    }
}
