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

import org.xwiki.context.Execution;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.script.safe.ScriptSafeProvider;

/**
 * Provide a public script access to a core extension repository.
 * 
 * @param <T>
 * @version $Id$
 * @since 4.0M2
 */
public class SafeCoreExtensionRepository<T extends CoreExtensionRepository> extends
    SafeAdvancedSearchableExtensionRepository<T> implements CoreExtensionRepository
{
    /**
     * @param repository wrapped repository
     * @param safeProvider the provider of instances safe for public scripts
     * @param execution provide access to the current context
     * @param hasProgrammingRight does the caller script has programming right
     */
    public SafeCoreExtensionRepository(T repository, ScriptSafeProvider<?> safeProvider, Execution execution,
        boolean hasProgrammingRight)
    {
        super(repository, safeProvider, execution, hasProgrammingRight);
    }

    // CoreExtensionRepository

    @Override
    public int countExtensions()
    {
        return getWrapped().countExtensions();
    }

    @Override
    public Collection<CoreExtension> getCoreExtensions()
    {
        return safe(getWrapped().getCoreExtensions());
    }

    @Override
    public CoreExtension getCoreExtension(String id)
    {
        return safe(getWrapped().getCoreExtension(id));
    }

    @Override
    public boolean exists(String id)
    {
        return getWrapped().exists(id);
    }

    @Override
    public CoreExtension resolve(ExtensionDependency extensionDependency)
    {
        return (CoreExtension) super.resolve(extensionDependency);
    }

    @Override
    public CoreExtension resolve(ExtensionId extensionId)
    {
        return (CoreExtension) super.resolve(extensionId);
    }

    @Override
    public CoreExtension getEnvironmentExtension()
    {
        return safe(getWrapped().getEnvironmentExtension());
    }
}
