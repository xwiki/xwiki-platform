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
import java.util.Collections;
import java.util.Map;

import org.xwiki.component.namespace.Namespace;
import org.xwiki.context.Execution;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.tree.ExtensionNode;
import org.xwiki.script.safe.ScriptSafeProvider;

/**
 * Provide a public script access to a local extension repository.
 * 
 * @param <T> the extension type
 * @version $Id$
 * @since 4.0M2
 */
public class SafeInstalledExtensionRepository<T extends InstalledExtensionRepository>
    extends SafeAdvancedSearchableExtensionRepository<T> implements InstalledExtensionRepository
{
    /**
     * @param repository wrapped repository
     * @param safeProvider the provider of instances safe for public scripts
     * @param execution provide access to the current context
     * @param hasProgrammingRight does the caller script has programming right
     */
    public SafeInstalledExtensionRepository(T repository, ScriptSafeProvider<?> safeProvider, Execution execution,
        boolean hasProgrammingRight)
    {
        super(repository, safeProvider, execution, hasProgrammingRight);
    }

    // LocalExtensionRepository

    @Override
    public int countExtensions()
    {
        return getWrapped().countExtensions();
    }

    @Override
    public InstalledExtension getInstalledExtension(ExtensionId extensionId)
    {
        return safe(getWrapped().getInstalledExtension(extensionId));
    }

    @Override
    public InstalledExtension getInstalledExtension(String feature, String namespace)
    {
        return safe(getWrapped().getInstalledExtension(feature, namespace));
    }

    @Override
    public InstalledExtension installExtension(LocalExtension extension, String namespace, boolean dependency)
    {
        return installExtension(extension, namespace, dependency, Collections.<String, Object>emptyMap());
    }

    @Override
    public InstalledExtension installExtension(LocalExtension extension, String namespace, boolean dependency,
        Map<String, Object> properties)
    {
        if (!this.hasProgrammingRight) {
            setError(new UnsupportedOperationException(FORBIDDEN));

            return null;
        }

        setError(null);

        try {
            return safe(getWrapped().installExtension(extension, namespace, dependency, properties));
        } catch (InstallException e) {
            setError(e);
        }

        return null;
    }

    @Override
    public void uninstallExtension(InstalledExtension extension, String namespace)
    {
        if (!this.hasProgrammingRight) {
            setError(new UnsupportedOperationException(FORBIDDEN));

            return;
        }

        setError(null);

        try {
            getWrapped().uninstallExtension(extension, namespace);
        } catch (UninstallException e) {
            setError(e);
        }
    }

    @Override
    public Collection<InstalledExtension> getBackwardDependencies(String feature, String namespace)
    {
        return safeWrapError(() -> getWrapped().getBackwardDependencies(feature, namespace));
    }

    @Override
    public Collection<InstalledExtension> getBackwardDependencies(String feature, String namespace,
        boolean withOptionals) throws ResolveException
    {
        return safeWrapError(() -> getWrapped().getBackwardDependencies(feature, namespace, withOptionals));
    }

    @Override
    public Map<String, Collection<InstalledExtension>> getBackwardDependencies(ExtensionId extensionId)
    {
        return safeWrapError(() -> getWrapped().getBackwardDependencies(extensionId));
    }

    @Override
    public Map<String, Collection<InstalledExtension>> getBackwardDependencies(ExtensionId extensionId,
        boolean withOptionals) throws ResolveException
    {
        return safeWrapError(() -> getWrapped().getBackwardDependencies(extensionId, withOptionals));
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

    @Override
    public IterableResult<InstalledExtension> searchInstalledExtensions(String pattern, String namespace, int offset,
        int nb) throws SearchException
    {
        return safe(getWrapped().searchInstalledExtensions(pattern, namespace, offset, nb));
    }

    @Override
    public IterableResult<InstalledExtension> searchInstalledExtensions(String namespace, ExtensionQuery query)
        throws SearchException
    {
        return safe(getWrapped().searchInstalledExtensions(namespace, query));
    }

    @Override
    public IterableResult<InstalledExtension> searchInstalledExtensions(ExtensionQuery query) throws SearchException
    {
        return safe(getWrapped().searchInstalledExtensions(query));
    }

    @Override
    public IterableResult<InstalledExtension> searchInstalledExtensions(Collection<String> namespaces,
        ExtensionQuery query) throws SearchException
    {
        return safe(getWrapped().searchInstalledExtensions(namespaces, query));
    }

    @Override
    public ExtensionNode<InstalledExtension> getOrphanedDependencies(InstalledExtension extension, Namespace namespace)
    {
        return safe(getWrapped().getOrphanedDependencies(extension, namespace));
    }
}
