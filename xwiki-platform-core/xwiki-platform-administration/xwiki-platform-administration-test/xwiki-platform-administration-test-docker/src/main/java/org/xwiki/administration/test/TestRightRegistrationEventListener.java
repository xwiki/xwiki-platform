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
package org.xwiki.administration.test;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.UnableToRegisterRightException;

/**
 * Event listener that registers the test extension right when the application is ready.
 *
 * @version $Id$
 */
@Component
@Named("org.xwiki.administration.test.TestRightRegistration")
@Singleton
public class TestRightRegistrationEventListener extends AbstractEventListener
{
    @Inject
    private Logger logger;

    @Inject
    private AuthorizationManager authorizationManager;

    /**
     * Default constructor.
     */
    public TestRightRegistrationEventListener()
    {
        super("TestRightRegistration", new ApplicationReadyEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        try {
            this.authorizationManager.register(TestRightDescription.INSTANCE);
        } catch (UnableToRegisterRightException e) {
            this.logger.error("Error while registering the test extension right", e);
        }
    }
}
