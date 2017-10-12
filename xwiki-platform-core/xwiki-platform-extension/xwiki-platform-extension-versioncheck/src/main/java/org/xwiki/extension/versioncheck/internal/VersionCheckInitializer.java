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
package org.xwiki.extension.versioncheck.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

/**
 * This component is responsible for initializing the version check timers.
 * Currently, this initializer is only used for the initialization of {@link EnvironmentVersionCheckTimer}. In the
 * future, it could be used to initialize other checker components for more standard extensions
 * (see https://jira.xwiki.org/browse/XWIKI-14748).
 *
 * @version $Id$
 * @since 9.9RC1
 */
@Singleton
@Component
@Named(VersionCheckInitializer.LISTENER_NAME)
public class VersionCheckInitializer extends AbstractEventListener
{
    /**
     * The listener name.
     */
    static final String LISTENER_NAME = "VersionCheckInitializer";

    @Inject
    private EnvironmentVersionCheckTimer environmentVersionCheckTimer;

    @Inject
    private Logger logger;

    /**
     * Constructs a new {@link VersionCheckInitializer}.
     */
    public VersionCheckInitializer()
    {
        super(LISTENER_NAME, new ApplicationStartedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof ApplicationStartedEvent) {
            try {
                environmentVersionCheckTimer.initialize();
            } catch (InitializationException e) {
                logger.warn("Failed to initialize timer for checking new environment versions: [{}]", e);
            }
        }
    }
}
