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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.internal.AbstractExtensionHandler;

@Component("jar")
public class JarExtensionHandler extends AbstractExtensionHandler implements Initializable
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private JarExtensionClassLoader jarExtensionClassLoader;

    @Inject
    private Logger logger;

    private ComponentAnnotationLoader jarLoader;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.jarLoader = new ComponentAnnotationLoader();
    }

    public void install(LocalExtension localExtension, String namespace) throws InstallException
    {
        ExtensionURLClassLoader classLoader = this.jarExtensionClassLoader.getURLClassLoader(namespace, true);

        // 1) load jar into classloader
        try {
            classLoader.addURL(localExtension.getFile().toURI().toURL());
        } catch (MalformedURLException e) {
            throw new InstallException("Failed to load jar file", e);
        }

        // 2) load and register components
        loadComponents(localExtension.getFile(), classLoader);
    }

    public void uninstall(LocalExtension localExtension, String namespace) throws UninstallException
    {
        ExtensionURLClassLoader classLoader = this.jarExtensionClassLoader.getURLClassLoader(namespace, false);

        if (namespace == null || classLoader.getWiki().equals(namespace)) {
            // unregister components
            unloadComponents(localExtension.getFile(), classLoader);

            // TODO: find a way to unload the jar from the classloader
        }
    }

    private void loadComponents(File jarFile, ExtensionURLClassLoader classLoader) throws InstallException
    {
        try {
            List<String>[] components = getDeclaredComponents(jarFile);

            if (components[0] == null) {
                this.logger.debug(jarFile + " does not contains any component");
                return;
            }

            this.jarLoader.initialize(this.componentManager, classLoader, components[0], components[1] == null
                ? Collections.<String> emptyList() : components[1]);
        } catch (Exception e) {
            throw new InstallException("Failed to load jar file components", e);
        }
    }

    private List<String>[] getDeclaredComponents(File jarFile) throws IOException
    {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(jarFile));

        List<String> componentClassNames = null;
        List<String> componentOverrideClassNames = null;

        try {
            for (ZipEntry entry = zis.getNextEntry(); entry != null && componentClassNames == null
                && componentOverrideClassNames == null; entry = zis.getNextEntry()) {
                if (entry.getName().equals(ComponentAnnotationLoader.COMPONENT_LIST)) {
                    componentClassNames = this.jarLoader.getDeclaredComponents(zis);
                } else if (entry.getName().equals(ComponentAnnotationLoader.COMPONENT_OVERRIDE_LIST)) {
                    componentOverrideClassNames = this.jarLoader.getDeclaredComponents(zis);
                }
            }
        } finally {
            zis.close();
        }

        return new List[] {componentClassNames, componentOverrideClassNames};
    }

    private void unloadComponents(File jarFile, ExtensionURLClassLoader classLoader) throws UninstallException
    {
        try {
            List<String>[] components = getDeclaredComponents(jarFile);

            if (components[0] == null) {
                this.logger.debug(jarFile + " does not contains any component");
                return;
            }

            for (String componentImplementation : components[0]) {
                try {
                    for (ComponentDescriptor componentDescriptor : this.jarLoader.getComponentsDescriptors(classLoader
                        .loadClass(componentImplementation))) {
                        this.componentManager.unregisterComponent(componentDescriptor.getRole(),
                            componentDescriptor.getRoleHint());
                    }
                } catch (ClassNotFoundException e) {
                    this.logger.error("Failed to load class [" + componentImplementation + "]", e);
                }
            }
        } catch (Exception e) {
            throw new UninstallException("Failed to load jar file components", e);
        }
    }
}
