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
package org.xwiki.extension.script;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.rating.ExtensionRating;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.rating.Ratable;
import org.xwiki.extension.version.Version;

/**
 * Various script APIs related to ratable extensions.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Component
@Named(ExtensionManagerScriptService.ROLEHINT + '.' + ExtensionRatingScriptService.ID)
@Singleton
public class ExtensionRatingScriptService extends AbstractExtensionScriptService
{
    /**
     * The identifier of the sub extension {@link org.xwiki.script.service.ScriptService}.
     */
    public static final String ID = "rating";

    /**
     * Repository manager, needed for cross-repository operations.
     */
    @Inject
    private ExtensionRepositoryManager repositoryManager;

    /**
     * @return all the remote repositories
     */
    private Collection<ExtensionRepository> getRepositories()
    {
        return this.repositoryManager.getRepositories();
    }

    /**
     * @param extensionId the extension id
     * @return the rating of an extension
     */
    public ExtensionRating getRating(ExtensionId extensionId)
    {
        setError(null);

        try {
            return getRating(extensionId.getId(), extensionId.getVersion());
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * @param extensionId the extension id
     * @param extensionVersion the extension version
     * @return the rating of an extension
     */
    public ExtensionRating getRating(String extensionId, Version extensionVersion)
    {
        setError(null);

        try {
            return getRating(extensionId, extensionVersion.getValue());
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * @param extensionId the extension id
     * @param extensionVersion the extension version
     * @return the rating of an extension
     */
    public ExtensionRating getRating(String extensionId, String extensionVersion)
    {
        setError(null);

        Collection<ExtensionRepository> repositories = getRepositories();
        for (ExtensionRepository repository : repositories) {
            if (repository instanceof Ratable) {
                try {
                    setError(null);
                    return ((Ratable) repository).getRating(extensionId, extensionVersion);
                } catch (ResolveException e) {
                    setError(e);
                    // Keep looking. Maybe there's another repository with the same extension.
                    continue;
                }
            }
        }

        return null;
    }
}
