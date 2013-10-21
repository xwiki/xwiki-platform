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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;

/**
 * Various script APIs related to installed extensions.
 * 
 * @version $Id$
 * @since 5.3M1
 */
@Component
@Named(ExtensionManagerScriptService.ROLEHINT + '.' + InstalledExtensionScriptService.ID)
@Singleton
public class InstalledExtensionScriptService extends AbstractExtensionScriptService
{
    /**
     * The identifier of the sub extension {@link org.xwiki.script.service.ScriptService}.
     */
    public static final String ID = "installed";

    /**
     * The repository containing installed extensions.
     */
    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    /**
     * @return the installed extensions repository
     */
    public InstalledExtensionRepository getRepository()
    {
        return safe(this.installedExtensionRepository);
    }

    /**
     * Get a list of all currently installed extensions. This doesn't include core extensions, only custom extensions
     * installed by the administrators.
     * 
     * @return a list of read-only handlers corresponding to the installed extensions, an empty list if nothing is
     *         installed
     */
    public Collection<InstalledExtension> getInstalledExtensions()
    {
        return safe(this.installedExtensionRepository.getInstalledExtensions());
    }

    /**
     * Return all the extensions available for the provide namespace. This also include root extension since namespaces
     * inherit from root.
     * <p>
     * This doesn't include core extensions, only extension installed through the API.
     * 
     * @param namespace the target namespace for which to retrieve the list of installed extensions
     * @return a list of read-only handlers corresponding to the installed extensions, an empty list if nothing is
     *         installed in the target namespace
     */
    public Collection<InstalledExtension> getInstalledExtensions(String namespace)
    {
        return safe(this.installedExtensionRepository.getInstalledExtensions(namespace));
    }

    /**
     * Get the extension handler corresponding to the given installed extension ID or feature (virtual ID) provided by
     * the extension and namespace.
     * <p>
     * The returned handler can be used to get more information about the extension, such as the authors, an extension
     * description, its license...
     * 
     * @param feature the extension id or provided feature (virtual extension) of the extension to resolve
     * @param namespace the optional namespace where the extension should be installed
     * @return the read-only handler corresponding to the requested extension, or {@code null} if the extension isn't
     *         installed in the target namespace
     */
    public InstalledExtension getInstalledExtension(String feature, String namespace)
    {
        return safe(this.installedExtensionRepository.getInstalledExtension(feature, namespace));
    }

    /**
     * Get all the installed extensions that depend on the specified root extension. The results are grouped by
     * namespace.
     * 
     * @param feature the extension id or provided feature (virtual extension) of the extension to resolve
     * @return a map namespace -&gt; list of dependent extensions, or {@code null} if any error occurs while computing
     *         the result, in which case {@link #getLastError()} contains the failure reason
     */
    public Map<String, Collection<InstalledExtension>> getBackwardDependencies(String feature)
    {
        InstalledExtension installedExtension = this.installedExtensionRepository.getInstalledExtension(feature, null);

        Map<String, Collection<InstalledExtension>> extensions;

        if (installedExtension != null) {
            setError(null);

            try {
                extensions =
                    safe(this.installedExtensionRepository.getBackwardDependencies(installedExtension.getId()));
            } catch (Exception e) {
                setError(e);

                extensions = null;
            }
        } else {
            extensions = null;
        }

        return extensions;
    }
}
