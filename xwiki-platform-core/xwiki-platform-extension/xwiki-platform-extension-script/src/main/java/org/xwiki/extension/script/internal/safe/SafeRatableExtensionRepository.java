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
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.rating.ExtensionRating;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.rating.Ratable;
import org.xwiki.extension.version.Version;
import org.xwiki.script.safe.ScriptSafeProvider;

/**
 * Provide a public script access to a ratable extension repository.
 * 
 * @param <T> the extension type
 * @version $Id$
 * @since 6.4M3
 */
public class SafeRatableExtensionRepository<T extends ExtensionRepository> extends SafeExtensionRepository<T>
    implements Ratable
{
    /**
     * @param repository wrapped repository
     * @param safeProvider the provider of instances safe for public scripts
     * @param execution provide access to the current context
     * @param hasProgrammingRight does the caller script has programming right
     */
    public SafeRatableExtensionRepository(T repository, ScriptSafeProvider<?> safeProvider, Execution execution,
        boolean hasProgrammingRight)
    {
        super(repository, safeProvider, execution, hasProgrammingRight);
    }

    // Ratable

    @Override
    public ExtensionRating getRating(ExtensionId extensionId) throws ResolveException
    {
        return safe(((Ratable) getWrapped()).getRating(extensionId));
    }

    @Override
    public ExtensionRating getRating(String extensionId, Version extensionVersion) throws ResolveException
    {
        return safe(((Ratable) getWrapped()).getRating(extensionId, extensionVersion));
    }

    @Override
    public ExtensionRating getRating(String extensionId, String extensionVersion) throws ResolveException
    {
        return safe(((Ratable) getWrapped()).getRating(extensionId, extensionVersion));
    }
}
