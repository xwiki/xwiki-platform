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

import java.util.Collection;
import java.util.Map;

import org.xwiki.context.Execution;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepositoryException;

/**
 * Provide a public script access to a local extension repository.
 * 
 * @param <T> the extension type
 * @version $Id$
 * @since 4.0M2
 */
public class SafeLocalExtensionRepository<T extends LocalExtensionRepository> extends
    SafeSearchableExtensionRepository<T> implements LocalExtensionRepository
{
    /**
     * @param repository wrapped repository
     * @param safeProvider the provider of instances safe for public scripts
     * @param execution provide access to the current context
     */
    public SafeLocalExtensionRepository(T repository, ScriptSafeProvider< ? > safeProvider, Execution execution)
    {
        super(repository, safeProvider, execution);
    }

    // LocalExtensionRepository

    @Override
    public int countExtensions()
    {
        return getWrapped().countExtensions();
    }

    @Override
    public Collection<LocalExtension> getLocalExtensions()
    {
        return safe(getWrapped().getLocalExtensions());
    }

    @Override
    public LocalExtension storeExtension(Extension extension)
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }

    @Override
    public void removeExtension(LocalExtension extension)
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }

    @Override
    public LocalExtension resolve(ExtensionDependency extensionDependency)
    {
        return (LocalExtension) super.resolve(extensionDependency);
    }

    @Override
    public LocalExtension resolve(ExtensionId extensionId)
    {
        return (LocalExtension) super.resolve(extensionId);
    }

    @Override
    public Collection<LocalExtension> getLocalExtensionVersions(String id)
    {
        return safe(getWrapped().getLocalExtensionVersions(id));
    }

    @Override
    public void setProperties(LocalExtension localExtension, Map<String, Object> properties)
        throws LocalExtensionRepositoryException
    {
        throw new UnsupportedOperationException("Operation forbidden in script proxy");
    }
}
