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

import org.xwiki.context.Execution;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.version.Version;
import org.xwiki.script.safe.ScriptSafeProvider;

/**
 * Provide a public script access to a repository.
 * 
 * @param <T>
 * @version $Id$
 * @since 4.0M2
 */
public class SafeExtensionRepository<T extends ExtensionRepository> extends AbstractNoExceptionSafeObject<T> implements
    ExtensionRepository
{
    /**
     * @param repository the wrapped repository
     * @param safeProvider the provider of instances safe for public scripts
     * @param execution provide access to the current context
     * @param hasProgrammingRight does the caller script has programming right
     */
    public SafeExtensionRepository(T repository, ScriptSafeProvider< ? > safeProvider, Execution execution,
        boolean hasProgrammingRight)
    {
        super(repository, safeProvider, execution, hasProgrammingRight);
    }

    // ExtensionRepository

    @Override
    public Extension resolve(ExtensionId extensionId)
    {
        setError(null);

        try {
            return safe(getWrapped().resolve(extensionId));
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    @Override
    public Extension resolve(ExtensionDependency extensionDependency)
    {
        setError(null);

        try {
            return safe(getWrapped().resolve(extensionDependency));
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    @Override
    public ExtensionRepositoryId getId()
    {
        return getWrapped().getId();
    }

    @Override
    public ExtensionRepositoryDescriptor getDescriptor()
    {
        return getWrapped().getDescriptor();
    }

    @Override
    public boolean exists(ExtensionId extensionId)
    {
        return getWrapped().exists(extensionId);
    }

    @Override
    public IterableResult<Version> resolveVersions(String id, int offset, int nb)
    {
        setError(null);

        try {
            return getWrapped().resolveVersions(id, offset, nb);
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }
}
