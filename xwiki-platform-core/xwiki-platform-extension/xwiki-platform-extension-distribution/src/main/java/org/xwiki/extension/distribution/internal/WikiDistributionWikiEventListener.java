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
package org.xwiki.extension.distribution.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.WikiCopiedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * Various reactions to wiki events.
 * 
 * @version $Id$
 * @since 5.0.2
 */
@Component
@Singleton
@Named("distribution.wiki.WikiDistributionWikiEventListener")
public class WikiDistributionWikiEventListener extends AbstractEventListener
{
    /**
     * The component used to get information about the current distribution.
     */
    @Inject
    private DistributionManager distributionManager;

    /**
     * Setup event listener properties.
     */
    public WikiDistributionWikiEventListener()
    {
        super("distribution.wiki.WikiDistributionWikiEventListener", new WikiCopiedEvent(), new WikiDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object o, Object context)
    {
        if (event instanceof WikiCopiedEvent) {
            onWikiCopied((WikiCopiedEvent) event);
        } else if (event instanceof WikiDeletedEvent) {
            onWikiDeleted((WikiDeletedEvent) event);
        }
    }

    /**
     * @param event copied wiki event
     */
    private void onWikiCopied(WikiCopiedEvent event)
    {
        this.distributionManager.copyPreviousWikiJobStatus(event.getSourceWikiId(), event.getTargetWikiId());
    }

    /**
     * @param event deleted wiki event
     */
    private void onWikiDeleted(WikiDeletedEvent event)
    {
        this.distributionManager.deletePreviousWikiJobStatus(event.getWikiId());
    }
}
