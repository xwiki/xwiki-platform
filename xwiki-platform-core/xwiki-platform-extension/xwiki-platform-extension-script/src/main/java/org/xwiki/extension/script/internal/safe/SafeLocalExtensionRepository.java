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

import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepositoryException;

/**
 * Provide a readonly access to a local extension repository.
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
     */
    public SafeLocalExtensionRepository(T repository, ScriptSafeProvider<Object> safeProvider)
    {
        super(repository, safeProvider);
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
    public Collection<LocalExtension> getInstalledExtensions()
    {
        return safe(getWrapped().getInstalledExtensions());
    }

    @Override
    public Collection<LocalExtension> getInstalledExtensions(String namespace)
    {
        return safe(getWrapped().getInstalledExtensions(namespace));
    }

    @Override
    public LocalExtension getInstalledExtension(String feature, String namespace)
    {
        return safe(getWrapped().getInstalledExtension(feature, namespace));
    }

    @Override
    public LocalExtension storeExtension(Extension extension) throws LocalExtensionRepositoryException
    {
        throw new UnsupportedOperationException("Calling storeExtension is forbidden in readonly proxy");
    }

    @Override
    public void removeExtension(LocalExtension extension) throws ResolveException
    {
        throw new UnsupportedOperationException("Calling removeExtension is forbidden in readonly proxy");
    }

    @Override
    public void installExtension(LocalExtension extension, String namespace, boolean dependency)
        throws InstallException
    {
        throw new UnsupportedOperationException("Calling installExtension is forbidden in readonly proxy");
    }

    @Override
    public void uninstallExtension(LocalExtension extension, String namespace) throws UninstallException
    {
        throw new UnsupportedOperationException("Calling uninstallExtension is forbidden in readonly proxy");
    }

    @Override
    public Collection<LocalExtension> getBackwardDependencies(String feature, String namespace) throws ResolveException
    {
        return safe(getWrapped().getBackwardDependencies(feature, namespace));
    }

    @Override
    public Map<String, Collection<LocalExtension>> getBackwardDependencies(ExtensionId extensionId)
        throws ResolveException
    {
        return safe(getWrapped().getBackwardDependencies(extensionId));
    }
}
