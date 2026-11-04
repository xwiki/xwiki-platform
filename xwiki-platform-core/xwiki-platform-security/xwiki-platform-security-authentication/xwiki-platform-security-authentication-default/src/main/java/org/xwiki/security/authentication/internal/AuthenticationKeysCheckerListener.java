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
package org.xwiki.security.authentication.internal;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authentication.AuthenticationConfiguration;

/**
 * Resolves the authentication keys when the application starts, so that they are generated (and the configuration
 * problems they may reveal, like a generated key on a clustered instance, are reported) at startup instead of at the
 * first request that goes through the authentication.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Component
@Singleton
@Named(AuthenticationKeysCheckerListener.NAME)
public class AuthenticationKeysCheckerListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.security.authentication.internal.AuthenticationKeysCheckerListener";

    @Inject
    private Provider<AuthenticationConfiguration> authenticationConfigurationProvider;

    /**
     * Default constructor.
     */
    public AuthenticationKeysCheckerListener()
    {
        super(NAME, new ApplicationStartedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        AuthenticationConfiguration authenticationConfiguration = this.authenticationConfigurationProvider.get();

        // The returned values are deliberately ignored: the getters are called only for their side effects. Resolving
        // a key generates and stores it when it is not configured, and logs the configuration problems it reveals (a
        // key that is not shared on a clustered instance, an example value). Without this call, that happens only at
        // the first request that goes through the authentication (guests and probes included), which can be after the
        // startup.
        authenticationConfiguration.getValidationKey();
        authenticationConfiguration.getEncryptionKey();
    }
}
