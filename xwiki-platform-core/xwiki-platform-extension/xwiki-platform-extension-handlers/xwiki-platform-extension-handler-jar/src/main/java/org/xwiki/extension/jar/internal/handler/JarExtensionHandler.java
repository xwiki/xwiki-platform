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
package org.xwiki.extension.jar.internal.handler;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.annotation.ComponentDeclaration;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.LocalExtensionFile;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.internal.AbstractExtensionHandler;

@Component("jar")
public class JarExtensionHandler extends AbstractExtensionHandler implements Initializable
{
    @Inject
    private ComponentManagerManager componentManagerManager;

    @Inject
    private JarExtensionClassLoader jarExtensionClassLoader;

    @Inject
    private Logger logger;

    private ComponentAnnotationLoader jarLoader;

    @Override
    public void initialize() throws InitializationException
    {
        this.jarLoader = new ComponentAnnotationLoader();
    }

    @Override
    public void initialize(LocalExtension localExtension, String namespace) throws ExtensionException
    {
        install(localExtension, namespace);
    }

    private static URL getExtensionURL(LocalExtension localExtension) throws MalformedURLException
    {
        return new File(localExtension.getFile().getAbsolutePath()).toURI().toURL();
    }

    @Override
    public void install(LocalExtension localExtension, String namespace) throws InstallException
    {
        ExtensionURLClassLoader classLoader = this.jarExtensionClassLoader.getURLClassLoader(namespace, true);

        // 1) load jar into classloader
        try {
            classLoader.addURL(getExtensionURL(localExtension));
        } catch (MalformedURLException e) {
            throw new InstallException("Failed to load jar file", e);
        }

        // 2) load and register components
        loadComponents(localExtension.getFile(), classLoader, namespace);
    }

    @Override
    public void uninstall(LocalExtension localExtension, String namespace) throws UninstallException
    {
        ExtensionURLClassLoader classLoader = this.jarExtensionClassLoader.getURLClassLoader(namespace, false);

        if (namespace == null || classLoader.getWiki().equals(namespace)) {
            // unregister components
            unloadComponents(localExtension.getFile(), classLoader, namespace);

            // The ClassLoader(s) will be replaced and reloaded at the end of the job
            // @see org.xwiki.extension.jar.internal.handler.JarExtensionJobFinishedListener
        }
    }

    private void loadComponents(LocalExtensionFile jarFile, ExtensionURLClassLoader classLoader, String namespace)
        throws InstallException
    {
        try {
            List<ComponentDeclaration> componentDeclarations = getDeclaredComponents(jarFile);

            if (componentDeclarations == null) {
                this.logger.debug("[{}] does not contain any component", jarFile.getName());
                return;
            }

            this.jarLoader.initialize(this.componentManagerManager.getComponentManager(namespace, true), classLoader,
                componentDeclarations);
        } catch (Exception e) {
            throw new InstallException("Failed to load jar file components", e);
        }
    }

    private List<ComponentDeclaration> getDeclaredComponents(LocalExtensionFile jarFile) throws IOException
    {
        ZipInputStream zis = new ZipInputStream(jarFile.openStream());

        List<ComponentDeclaration> componentDeclarations = null;
        List<ComponentDeclaration> componentOverrideDeclarations = null;

        try {
            for (ZipEntry entry = zis.getNextEntry(); entry != null
                && (componentDeclarations == null || componentOverrideDeclarations == null); entry = zis.getNextEntry()) {
                if (entry.getName().equals(ComponentAnnotationLoader.COMPONENT_LIST)) {
                    componentDeclarations = this.jarLoader.getDeclaredComponents(zis);
                } else if (entry.getName().equals(ComponentAnnotationLoader.COMPONENT_OVERRIDE_LIST)) {
                    componentOverrideDeclarations = this.jarLoader.getDeclaredComponents(zis);
                }
            }
        } finally {
            zis.close();
        }

        // Merge all overrides found with a priority of 0. This is purely for backward compatibility since the
        // override files is now deprecated.
        if (componentOverrideDeclarations != null) {
            if (componentDeclarations == null) {
                componentDeclarations = new ArrayList<ComponentDeclaration>();
            }
            for (ComponentDeclaration componentOverrideDeclaration : componentOverrideDeclarations) {
                componentDeclarations.add(new ComponentDeclaration(componentOverrideDeclaration
                    .getImplementationClassName(), 0));
            }
        }

        return componentDeclarations;
    }

    private void unloadComponents(LocalExtensionFile jarFile, ExtensionURLClassLoader classLoader, String namespace)
        throws UninstallException
    {
        try {
            List<ComponentDeclaration> componentDeclarations = getDeclaredComponents(jarFile);

            if (componentDeclarations == null) {
                this.logger.debug("[{}] does not contain any component", jarFile.getName());
                return;
            }

            for (ComponentDeclaration componentDeclaration : componentDeclarations) {
                try {
                    for (ComponentDescriptor componentDescriptor : this.jarLoader.getComponentsDescriptors(classLoader
                        .loadClass(componentDeclaration.getImplementationClassName()))) {
                        this.componentManagerManager.getComponentManager(namespace, false).unregisterComponent(
                            componentDescriptor.getRole(), componentDescriptor.getRoleHint());
                    }
                } catch (ClassNotFoundException e) {
                    this.logger
                        .error("Failed to load class [{}]", componentDeclaration.getImplementationClassName(), e);
                }
            }
        } catch (Exception e) {
            throw new UninstallException("Failed to load jar file components", e);
        }
    }
}
