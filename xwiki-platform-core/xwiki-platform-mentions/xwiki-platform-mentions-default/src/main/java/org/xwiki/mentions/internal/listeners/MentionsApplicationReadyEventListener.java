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
package org.xwiki.mentions.internal.listeners;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.mentions.internal.MentionsEventExecutor;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * Listen for the application to be ready before starting the mentions task consumers.
 *
 * @version $Id$
 * @since 12.6RC1
 */
@Component
@Singleton
@Named("MentionsApplicationReadyEventListener")
public class MentionsApplicationReadyEventListener extends AbstractEventListener
{
    @Inject
    private MentionsEventExecutor eventExecutor;

    /**
     * Default constructor.
     */
    public MentionsApplicationReadyEventListener()
    {
        super("MentionsApplicationReadyEventListener", new ApplicationReadyEvent(),
            new ExtensionInstalledEvent(new ExtensionId("org.xwiki.platform:xwiki-platform-mentions-default"), null));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.eventExecutor.startThreads();
    }
}
