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
package org.xwiki.localization.jar.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionEvent;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUninstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.message.TranslationMessageParser;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * Generate and manage resource based translations bundles.
 * 
 * @version $Id$
 * @since 4.5M1
 */
@Component
@Named(JARTranslationBundleFactoryListener.NAME)
@Singleton
public class JARTranslationBundleFactoryListener implements EventListener, Initializable
{
    /**
     * The name of the event listener.
     */
    protected static final String NAME = "localization.bundle.JARTranslationBundleFactoryListener";

    /**
     * The type of extension supported by this translation bundle.
     */
    private static final String EXTENSION_TYPE = "jar";

    /**
     * The events to listen.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new ExtensionInstalledEvent(),
        new ExtensionUninstalledEvent(), new ExtensionUpgradedEvent());

    /**
     * Used to create or access right component manager depending of a namespace.
     */
    @Inject
    private ComponentManagerManager componentManagerManager;

    /**
     * The root component manager to fallback on.
     */
    @Inject
    private ComponentManager rootComponentManager;

    /**
     * Used to parse translation messages.
     */
    @Inject
    @Named("messagetool/1.0")
    private TranslationMessageParser translationParser;

    /**
     * The virtual repository containing installed extensions.
     */
    @Inject
    private InstalledExtensionRepository installedRepository;

    /**
     * USed to log.
     */
    @Inject
    private Logger logger;

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        ExtensionEvent extensionEvent = (ExtensionEvent) event;
        InstalledExtension extension = (InstalledExtension) source;

        if (event instanceof ExtensionInstalledEvent) {
            if (EXTENSION_TYPE.equals(extension.getType())) {
                extensionAdded(extension, extensionEvent.getNamespace());
            }
        } else if (event instanceof ExtensionUninstalledEvent) {
            if (EXTENSION_TYPE.equals(extension.getType())) {
                extensionDeleted(extension, extensionEvent.getNamespace());
            }
        } else {
            extensionUpgraded(extension, (Collection<InstalledExtension>) data, extensionEvent.getNamespace());
        }
    }

    @Override
    public String getName()
    {
        return "localization.bundle.JARTranslationBundleFactory";
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Load installed extensions
        for (InstalledExtension extension : this.installedRepository.getInstalledExtensions()) {
            if (EXTENSION_TYPE.equals(extension.getType())) {
                if (extension.isInstalled(null)) {
                    extensionAdded(extension, null);
                } else {
                    for (String namespace : extension.getNamespaces()) {
                        extensionAdded(extension, namespace);
                    }
                }
            }
        }
    }

    /**
     * @param jarURL the jar URL
     * @return the component descriptor to use to register/unregister the translation bundle
     */
    private ComponentDescriptor<TranslationBundle> createComponentDescriptor(URL jarURL)
    {
        DefaultComponentDescriptor<TranslationBundle> descriptor = new DefaultComponentDescriptor<TranslationBundle>();

        descriptor.setImplementation(JARFileTranslationBundle.class);
        descriptor.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);
        descriptor.setRoleHint(JARTranslationBundleFactory.ID + ':' + jarURL);
        descriptor.setRoleType(TranslationBundle.class);

        return descriptor;
    }

    /**
     * @param extension the jar extension
     * @return the component descriptor to use to register/unregister the translation bundle
     * @throws MalformedURLException failed to create URL for the extension file
     */
    private ComponentDescriptor<TranslationBundle> createComponentDescriptor(InstalledExtension extension)
        throws MalformedURLException
    {
        File jarFile = new File(extension.getFile().getAbsolutePath());

        return createComponentDescriptor(jarFile.toURI().toURL());
    }

    /**
     * @param newExtension the installed extension
     * @param previousExtensions the previous version of the extensions
     * @param namespace the namespace where this upgrade took place
     */
    private void extensionUpgraded(InstalledExtension newExtension, Collection<InstalledExtension> previousExtensions,
        String namespace)
    {
        for (InstalledExtension previousExtension : previousExtensions) {
            if (EXTENSION_TYPE.equals(previousExtension.getType())) {
                extensionDeleted(previousExtension, namespace);
            }
        }
        if (EXTENSION_TYPE.equals(newExtension.getType())) {
            extensionAdded(newExtension, namespace);
        }
    }

    /**
     * @param extension the installed extension
     * @param namespace the namespace where the extension has been installed
     */
    private void extensionDeleted(InstalledExtension extension, String namespace)

    {
        try {
            ComponentDescriptor<TranslationBundle> descriptor = createComponentDescriptor(extension);

            ComponentManager componentManager = this.componentManagerManager.getComponentManager(namespace, false);

            componentManager.unregisterComponent(descriptor);
        } catch (Exception e) {
            this.logger.error("Failed to create TranslationBundle descriptor for extension [{}]", extension, e);
        }
    }

    /**
     * @param extension the uninstalled extension
     * @param namespace the namespace from where the extension has been uninstalled
     */
    private void extensionAdded(InstalledExtension extension, String namespace)
    {
        try {
            File jarFile = new File(extension.getFile().getAbsolutePath());

            ComponentManager componentManager = this.componentManagerManager.getComponentManager(namespace, false);

            if (componentManager == null) {
                componentManager = this.rootComponentManager;
            }

            JARFileTranslationBundle bundle =
                new JARFileTranslationBundle(jarFile, componentManager, this.translationParser);

            ComponentDescriptor<TranslationBundle> descriptor = createComponentDescriptor(jarFile.toURI().toURL());
            componentManager.registerComponent(descriptor, bundle);
        } catch (Exception e) {
            this.logger.error("Failed to register a TranslationBundle component for extension [{}] on namespace [{}]",
                extension, namespace, e);
        }
    }
}
