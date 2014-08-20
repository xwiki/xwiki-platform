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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.search.Searchable;

/**
 * Provide safe Extension.
 * 
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class ExtensionRepositoryScriptSafeProvider extends AbstractScriptSafeProvider<ExtensionRepository>
{
    /**
     * The provider of instances safe for public scripts.
     */
    @Inject
    @SuppressWarnings("rawtypes")
    private ScriptSafeProvider defaultSafeProvider;

    /**
     * Provide access to the current context.
     */
    @Inject
    private Execution execution;

    @Override
    public <S> S get(ExtensionRepository unsafe)
    {
        ExtensionRepository safe;

        if (unsafe instanceof CoreExtensionRepository) {
            safe =
                new SafeCoreExtensionRepository<CoreExtensionRepository>((CoreExtensionRepository) unsafe,
                    this.defaultSafeProvider, this.execution, hasProgrammingRights());
        } else if (unsafe instanceof InstalledExtensionRepository) {
            safe =
                new SafeInstalledExtensionRepository<InstalledExtensionRepository>(
                    (InstalledExtensionRepository) unsafe, this.defaultSafeProvider, this.execution,
                    hasProgrammingRights());
        } else if (unsafe instanceof LocalExtensionRepository) {
            safe =
                new SafeLocalExtensionRepository<LocalExtensionRepository>((LocalExtensionRepository) unsafe,
                    this.defaultSafeProvider, this.execution, hasProgrammingRights());
        } else if (unsafe instanceof Searchable) {
            safe =
                new SafeSearchableExtensionRepository<ExtensionRepository>(unsafe, this.defaultSafeProvider,
                    this.execution, hasProgrammingRights());
        } else {
            safe =
                new SafeExtensionRepository<ExtensionRepository>(unsafe, this.defaultSafeProvider, this.execution,
                    hasProgrammingRights());
        }

        return (S) safe;
    }
}
