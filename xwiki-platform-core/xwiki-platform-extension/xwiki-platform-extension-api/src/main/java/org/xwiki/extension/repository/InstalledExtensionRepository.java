package org.xwiki.extension.repository;

import java.util.Collection;
import java.util.Map;

import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;

public interface InstalledExtensionRepository
{
    /**
     * Put provided extension (generally a remote extension) in the local repository or increment the namespaces in
     * which the extension is installed if already in the local repository.
     * 
     * @param extension the extension to install
     * @param dependency indicate of the extension is installed as a dependency of another one
     * @param namespace the namespace where the extension is being installed
     * @return the new local extension
     * @throws InstallException error when trying install provided extension in the local repository
     */
    void installExtension(LocalExtension extension, boolean dependency, String namespace) throws InstallException;

    /**
     * Indicate that the provided extension is uninstalled from provided namespace.
     * <p>
     * Extension is never removed form the local repository. It's just namespace related informations.
     * 
     * @param extension the extension to uninstall
     * @param namespace the namespace from which the extension is uninstalled
     * @throws UninstallException error when trying to uninstall provided extension
     */
    void uninstallExtension(LocalExtension extension, String namespace) throws UninstallException;

    /**
     * @return all installed local extensions, an empty collection if none could be found
     */
    Collection<LocalExtension> getInstalledExtensions();

    /**
     * @param namespace the namespace where to search for installed extensions, null mean installed in all namespaces.
     * @return all the local extensions installed in the provided namespace, an empty collection if none could be found
     */
    Collection<LocalExtension> getInstalledExtensions(String namespace);

    /**
     * @param extensionId the extension unique identifier, version is not needed since a namespace can contain only one
     *            version of an extension
     * @param namespace the namespace where the extension is installed
     * @return the extension, null if none could be found
     */
    LocalExtension getInstalledExtension(String extensionId, String namespace);

    /**
     * Get provided installed extension backward dependencies in the provided namespace.
     * <p>
     * Only look at the backward dependencies in the provided namespace. To get dependencies of a root extension
     * (namespace=null) use {@link #getBackwardDependencies(ExtensionId)} instead.
     * 
     * @param extensionId the extension unique identifier
     * @param namespace the namespace where to search for backward dependencies
     * @return the backward dependencies, an empty collection of none could be found
     * @throws ResolveException error when searching for backward dependencies
     */
    Collection<LocalExtension> getBackwardDependencies(String extensionId, String namespace) throws ResolveException;

    /**
     * Get all backward dependencies by namespace for the provided installed extension.
     * 
     * @param extensionId the extension identifier
     * @return the extension backward dependencies in all namespaces
     * @throws ResolveException error when searching for extension backward dependencies
     */
    Map<String, Collection<LocalExtension>> getBackwardDependencies(ExtensionId extensionId) throws ResolveException;
}
