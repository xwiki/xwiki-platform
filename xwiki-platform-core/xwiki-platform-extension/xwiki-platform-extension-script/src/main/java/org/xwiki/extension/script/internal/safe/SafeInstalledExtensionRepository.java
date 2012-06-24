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
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;
import org.xwiki.extension.repository.InstalledExtensionRepository;

/**
 * Provide a public script access to a local extension repository.
 * 
 * @param <T> the extension type
 * @version $Id$
 * @since 4.0M2
 */
public class SafeInstalledExtensionRepository<T extends InstalledExtensionRepository> extends
    SafeSearchableExtensionRepository<T> implements InstalledExtensionRepository
{
    /**
     * @param repository wrapped repository
     * @param safeProvider the provider of instances safe for public scripts
     * @param execution provide access to the current context
     */
    public SafeInstalledExtensionRepository(T repository, ScriptSafeProvider< ? > safeProvider, Execution execution)
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
    public InstalledExtension getInstalledExtension(String feature, String namespace)
    {
        return safe(getWrapped().getInstalledExtension(feature, namespace));
    }

    @Override
    public InstalledExtension installExtension(LocalExtension extension, String namespace, boolean dependency)
    {
        throw new UnsupportedOperationException("Calling installExtension is forbidden in script proxy");
    }

    @Override
    public void uninstallExtension(InstalledExtension extension, String namespace)
    {
        throw new UnsupportedOperationException("Calling uninstallExtension is forbidden in script proxy");
    }

    @Override
    public Collection<InstalledExtension> getBackwardDependencies(String feature, String namespace)
    {
        setError(null);

        try {
            return safe(getWrapped().getBackwardDependencies(feature, namespace));
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    @Override
    public Map<String, Collection<InstalledExtension>> getBackwardDependencies(ExtensionId extensionId)
    {
        setError(null);

        try {
            return safe(getWrapped().getBackwardDependencies(extensionId));
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    @Override
    public Collection<InstalledExtension> getInstalledExtensions()
    {
        return safe(getWrapped().getInstalledExtensions());
    }

    @Override
    public Collection<InstalledExtension> getInstalledExtensions(String namespace)
    {
        return safe(getWrapped().getInstalledExtensions(namespace));
    }

    @Override
    public InstalledExtension resolve(ExtensionDependency extensionDependency)
    {
        return (InstalledExtension) super.resolve(extensionDependency);
    }

    @Override
    public InstalledExtension resolve(ExtensionId extensionId)
    {
        return (InstalledExtension) super.resolve(extensionId);
    }
}
