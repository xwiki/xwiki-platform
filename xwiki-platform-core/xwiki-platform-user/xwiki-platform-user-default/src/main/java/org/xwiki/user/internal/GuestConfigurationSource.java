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
package org.xwiki.user.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.configuration2.BaseConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.configuration.internal.CommonsConfigurationSource;
import org.xwiki.user.UserConfiguration;

import static org.xwiki.user.internal.UserPropertyConstants.ACTIVE;
import static org.xwiki.user.internal.UserPropertyConstants.DISPLAY_HIDDEN_DOCUMENTS;
import static org.xwiki.user.internal.UserPropertyConstants.EMAIL_CHECKED;
import static org.xwiki.user.internal.UserPropertyConstants.FIRST_NAME;

/**
 * Provide configuration data for the Guest user.
 *
 * @version $Id$
 * @since 12.2
 */
@Component
@Named("guestuser")
@Singleton
public class GuestConfigurationSource extends CommonsConfigurationSource implements Initializable
{
    @Inject
    private UserConfiguration userConfiguration;

    @Override
    public void initialize()
    {
        // Default preferences
        BaseConfiguration configuration = new BaseConfiguration();
        configuration.addProperty(DISPLAY_HIDDEN_DOCUMENTS, "0");
        configuration.addProperty(ACTIVE, "0");
        configuration.addProperty(FIRST_NAME, "Guest");
        configuration.addProperty(EMAIL_CHECKED, "0");

        // User-defined and overriding preferences
        this.userConfiguration.getGuestPreference().forEach((key, value)
            -> configuration.setProperty((String) key, value));

        setConfiguration(configuration);
    }
}
