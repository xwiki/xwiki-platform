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

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.platform.blog.BlogVisibilityMigration;

/**
 * React to the upgrade of the blog application by starting the blog post visibility migration.
 *
 * @version $Id$
 * @since 9.0RC1
 * @since 8.4.2
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

    @Inject
    private BlogVisibilityMigration blogVisibilityMigration;

    @Inject
    private Logger logger;

    /**
     * Construct a BlogUpgradeEventListener.
     */
    public BlogUpgradeEventListener()
    {
        super(NAME, Arrays.asList(new ExtensionUpgradedEvent()));
    }


    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        ExtensionUpgradedEvent extensionUpgradedEvent = (ExtensionUpgradedEvent) event;
        if (!extensionUpgradedEvent.getExtensionId().getId().equals("org.xwiki.platform:xwiki-platform-blog-ui")) {
            return;
        }

        WikiReference wikiReference = namespaceToWikiReference(extensionUpgradedEvent.getNamespace());
        if (wikiReference != null) {
            try {
                blogVisibilityMigration.execute(wikiReference);
            } catch (Exception e) {
                logger.warn("Failed to migrate the visibility of non published blog posts.");
            }
        }
    }

    private WikiReference namespaceToWikiReference(String namespace)
    {
        if (namespace.startsWith("wiki:")) {
            return new WikiReference(namespace.substring(5));
        }
        // Should never happen
        return null;
    }
}
