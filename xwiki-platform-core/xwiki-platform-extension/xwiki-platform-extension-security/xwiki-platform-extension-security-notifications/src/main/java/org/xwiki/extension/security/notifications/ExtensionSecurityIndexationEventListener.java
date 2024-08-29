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
package org.xwiki.extension.security.notifications;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.security.ExtensionSecurityIndexationEndEvent;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;

/**
 * Listen for {@link ExtensionSecurityIndexationEndEvent} and forward them as
 * {@link NewExtensionSecurityVulnerabilityTargetableEvent} to notify admins of the presence of new security
 * vulnerabilities.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
@Named(ExtensionSecurityIndexationEventListener.ID)
public class ExtensionSecurityIndexationEventListener extends AbstractLocalEventListener
{
    /**
     * The hint and name of this listener.
     */
    public static final String ID = "ExtensionSecurityIndexationEvent";

    @Inject
    private ObservationManager observationManager;

    /**
     * Default constructor.
     */
    public ExtensionSecurityIndexationEventListener()
    {
        super(ID, new ExtensionSecurityIndexationEndEvent());
    }

    @Override
    public String getName()
    {
        return ID;
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        if ((long) data > 0) {
            // Converted to string are otherwise the data is ignored during the conversion.
            String strData = String.valueOf(data);
            this.observationManager.notify(
                new NewExtensionSecurityVulnerabilityTargetableEvent(Set.of("xwiki:XWiki.XWikiAdminGroup")),
                "org.xwiki.platform:xwiki-platform-extension-security-notifications", strData);
        }
    }
}
