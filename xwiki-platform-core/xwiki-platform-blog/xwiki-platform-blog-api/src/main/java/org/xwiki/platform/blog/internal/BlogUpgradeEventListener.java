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
package org.xwiki.platform.blog.internal;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.VersionConstraint;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.platform.blog.BlogVisibilityMigration;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * React to the upgrade of the blog application by starting the blog post visibility migration.
 *
 * @version $Id$
 *
 * @since 9.0RC1
 * @since 8.4.3
 * @since 7.4.6
 */
@Component
@Singleton
@Named(BlogUpgradeEventListener.NAME)
public class BlogUpgradeEventListener extends AbstractEventListener
{
    /**
     * Name of the listener.
     */
    public static final String NAME = "Blog Upgrade Listener";

    /**
     * ID of the Blog Application.
     */
    private static final String EXTENSION_ID = "org.xwiki.platform:xwiki-platform-blog-ui";

    /**
     * The visibility is synchronized since 7.4.6, 8.4.2 and 9.0RC1, so we do the migration only if the previous
     * version was anterior, ie matches the following constraint.
     */
    private static final VersionConstraint VERSION_CONSTRAINT = new DefaultVersionConstraint("(,7.4.6),[8.0,8.4.2)");

    @Inject
    private BlogVisibilityMigration blogVisibilityMigration;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    /**
     * Construct a BlogUpgradeEventListener.
     */
    public BlogUpgradeEventListener()
    {
        super(NAME, new ExtensionUpgradedEvent(EXTENSION_ID));
    }

    @Override
    public void onEvent(Event event, Object installedExtension, Object previousExtensions)
    {
        ExtensionUpgradedEvent extensionUpgradedEvent = (ExtensionUpgradedEvent) event;

        Version previousVersion = getPreviousVersion((Collection<InstalledExtension>) previousExtensions);

        if (previousVersion != null && VERSION_CONSTRAINT.containsVersion(previousVersion)) {
            String namespace = extensionUpgradedEvent.getNamespace();
            if (namespace == null) {
                // When the namespace is null, it means the application is installed on the root namespace, ie. on
                // the farm.
                migrateAllWikis();
            } else if (namespace.startsWith("wiki:")) {
                migrateWiki(new WikiReference(namespace.substring(5)));
            }
        }
    }

    private void migrateAllWikis()
    {
        try {
            for (String wikiId : wikiDescriptorManager.getAllIds()) {
                migrateWiki(new WikiReference(wikiId));
            }
        } catch (WikiManagerException e) {
            logger.warn("Failed to migrate the visibility of non published blog posts.", e);
        }
    }

    private void migrateWiki(WikiReference wikiReference)
    {
        try {
            blogVisibilityMigration.execute(wikiReference);
        } catch (Exception e) {
            logger.warn("Failed to migrate the visibility of non published blog posts on the wiki [{}].",
                    wikiReference.getName(), e);
        }
    }

    private Version getPreviousVersion(Collection<InstalledExtension> previousExtensions)
    {
        for (InstalledExtension extension : previousExtensions) {
            if (extension.getId().getId().equals(EXTENSION_ID)) {
                return extension.getId().getVersion();
            }
        }
        // Should never happen
        return null;
    }
}
