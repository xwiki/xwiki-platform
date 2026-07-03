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

import org.xwiki.component.namespace.Namespace;
import org.xwiki.context.Execution;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.index.ExtensionIndex;
import org.xwiki.extension.index.ExtensionIndexStatus;
import org.xwiki.extension.index.IndexedExtension;
import org.xwiki.job.JobException;
import org.xwiki.script.safe.ScriptSafeProvider;

/**
 * Provide a public script access to the extension index.
 * 
 * @param <T> the extension type
 * @version $Id$
 * @since 12.10
 */
public class SafeExtensionIndex<T extends ExtensionIndex> extends SafeAdvancedSearchableExtensionRepository<T>
    implements ExtensionIndex
{
    /**
     * @param repository wrapped repository
     * @param safeProvider the provider of instances safe for public scripts
     * @param execution provide access to the current context
     * @param hasProgrammingRight does the caller script has programming right
     */
    public SafeExtensionIndex(T repository, ScriptSafeProvider<?> safeProvider, Execution execution,
        boolean hasProgrammingRight)
    {
        super(repository, safeProvider, execution, hasProgrammingRight);
    }

    // ExtensionIndex

    @Override
    public ExtensionIndexStatus getStatus(Namespace namespace)
    {
        return getWrapped().getStatus(namespace);
    }

    @Override
    public ExtensionIndexStatus index(Namespace namespace) throws JobException
    {
        return getWrapped().index(namespace);
    }

    // ExtensionRepository

    @Override
    public IndexedExtension resolve(ExtensionDependency extensionDependency)
    {
        return (IndexedExtension) super.resolve(extensionDependency);
    }

    @Override
    public IndexedExtension resolve(ExtensionId extensionId)
    {
        return (IndexedExtension) super.resolve(extensionId);
    }
}
