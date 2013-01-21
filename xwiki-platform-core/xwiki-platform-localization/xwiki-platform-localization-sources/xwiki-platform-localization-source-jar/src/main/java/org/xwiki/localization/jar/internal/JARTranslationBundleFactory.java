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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionEvent;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUninstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.TranslationBundleDoesNotExistsException;
import org.xwiki.localization.TranslationBundleFactory;
import org.xwiki.localization.message.TranslationMessageParser;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

/**
 * Generate and manage resource based translations bundles.
 * 
 * @version $Id$
 * @since 4.5M1
 */
@Component
@Named(JARTranslationBundleFactory.ID)
@Singleton
public class JARTranslationBundleFactory implements TranslationBundleFactory, Initializable, Disposable
{
    /**
     * The identifier of this {@link TranslationBundleFactory}.
     */
    public final static String ID = "jar";

    /**
     * The events to listen.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new ExtensionInstalledEvent(),
        new ExtensionUninstalledEvent(), new ExtensionUpgradedEvent());

    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Inject
    private ComponentManagerManager componentManagerManager;

    @Inject
    private ObservationManager observation;

    @Inject
    @Named("messagetool/1.0")
    private TranslationMessageParser translationParser;

    @Inject
    private ComponentManagerManager cmManager;

    @Inject
    private InstalledExtensionRepository installedRepository;

    @Inject
    private Logger logger;

    private EventListener listener = new EventListener()
    {
        @Override
        public void onEvent(Event event, Object source, Object data)
        {
            ExtensionEvent extensionEvent = (ExtensionEvent) event;
            InstalledExtension extension = (InstalledExtension) source;

            if (extension.getType().equals("jar")) {
                if (event instanceof ExtensionInstalledEvent) {
                    extensionAdded(extension, extensionEvent.getNamespace());
                } else if (event instanceof ExtensionUninstalledEvent) {
                    extensionDeleted(extension, extensionEvent.getNamespace());
                } else {
                    extensionUpgraded(extension, (InstalledExtension) data, extensionEvent.getNamespace());
                }
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
    };

    @Override
    public void initialize() throws InitializationException
    {
        // Load installed extensions
        for (InstalledExtension extension : this.installedRepository.getInstalledExtensions()) {
            if (extension.isInstalled(null)) {
                extensionAdded(extension, null);
            } else {
                for (String namespace : extension.getNamespaces()) {
                    extensionAdded(extension, namespace);
                }
            }
        }
    }

    @Override
    public TranslationBundle getBundle(String bundleId) throws TranslationBundleDoesNotExistsException
    {
        String id = ID + ':' + bundleId;

        if (this.contextComponentManagerProvider.get().hasComponent(TranslationBundle.class, id)) {
            try {
                return this.contextComponentManagerProvider.get().getInstance(TranslationBundle.class, id);
            } catch (ComponentLookupException e) {
                this.logger.debug("Failed to lookup component [{}] with hint [{}].", TranslationBundle.class, bundleId,
                    e);
            }
        }

        throw new TranslationBundleDoesNotExistsException(String.format("Can't find any JAR resource for jar [%s]",
            bundleId));
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
        descriptor.setRoleHint(ID + ':' + jarURL);
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
        URL jarURL = JARUtils.toJARURL(new File(extension.getFile().getAbsolutePath()));

        return createComponentDescriptor(jarURL);
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.observation.removeListener(this.listener.getName());
    }

    private void extensionUpgraded(InstalledExtension newExtension, InstalledExtension previousExtension,
        String namespace)
    {
        extensionDeleted(previousExtension, namespace);
        extensionAdded(newExtension, namespace);
    }

    private void extensionDeleted(InstalledExtension extension, String namespace)
    {
        try {
            ComponentDescriptor<TranslationBundle> descriptor = createComponentDescriptor(extension);

            ComponentManager componentManager = this.componentManagerManager.getComponentManager(namespace, false);

            componentManager.unregisterComponent(descriptor);
        } catch (MalformedURLException e) {
            this.logger.error("Failed to create TranslationBundle descriptor for extension [{}]", extension, e);
        }
    }

    private void extensionAdded(InstalledExtension extension, String namespace)
    {
        try {
            URL jarURL = JARUtils.toJARURL(new File(extension.getFile().getAbsolutePath()));

            ComponentDescriptor<TranslationBundle> descriptor = createComponentDescriptor(jarURL);

            ComponentManager componentManager = this.componentManagerManager.getComponentManager(namespace, false);

            JARFileTranslationBundle bundle =
                new JARFileTranslationBundle(jarURL, componentManager, translationMessageParser);

            componentManager.registerComponent(descriptor);
        } catch (MalformedURLException e) {
            this.logger.error("Failed to create TranslationBundle descriptor for extension [{}]", extension, e);
        } catch (ComponentRepositoryException e) {
            this.logger.error("Failed to register a TranslationBundle component for extension [{}] on namespace [{}]",
                extension, namespace, e);
        }
    }
}
