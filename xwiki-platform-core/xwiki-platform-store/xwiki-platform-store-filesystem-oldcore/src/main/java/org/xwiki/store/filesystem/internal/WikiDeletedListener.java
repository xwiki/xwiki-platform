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
package org.xwiki.store.filesystem.internal;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * Automatically delete store corresponding to deleted wiki.
 * 
 * @version $Id$
 * @since 10.1RC1
 */
@Component
@Named(WikiDeletedListener.NAME)
@Singleton
public class WikiDeletedListener extends AbstractEventListener
{
    public static final String NAME = "org.xwiki.store.filesystem.internal.WikiDeletedListener";

    @Inject
    private FilesystemStoreTools store;

    @Inject
    private Logger logger;

    public WikiDeletedListener()
    {
        super(NAME, new WikiDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        String wikiId = ((WikiDeletedEvent) event).getWikiId();

        File directory = this.store.getWikiDir(wikiId);

        if (directory.exists() && directory.isDirectory()) {
            try {
                FileUtils.deleteDirectory(directory);
            } catch (IOException e) {
                this.logger.error("Failed to delete storage for the wiki [{}]", wikiId, e);
            }
        }
    }
}
