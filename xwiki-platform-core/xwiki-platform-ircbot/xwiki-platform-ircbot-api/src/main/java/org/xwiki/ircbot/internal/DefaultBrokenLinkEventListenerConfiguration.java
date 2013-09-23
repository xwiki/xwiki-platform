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
package org.xwiki.ircbot.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.ircbot.BrokenLinkEventListenerConfiguration;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Provides configuration data for the {@link BrokenLinkEventListener} Event Listener.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultBrokenLinkEventListenerConfiguration implements BrokenLinkEventListenerConfiguration
{
    /**
     * Property to decide if the Broken Link Event Listener is active or not.
     */
    private static final String INACTIVE_PROPERTY = "inactive";

    /**
     * IRC.IRCBrokenLinkClass xwiki class.
     */
    private static final EntityReference CONFIGURATION_CLASS = new EntityReference("BrokenLinkClass",
        EntityType.DOCUMENT, new EntityReference("IRC", EntityType.SPACE));

    /**
     * Provides access to the configuration data stored in a wiki page.
     */
    @Inject
    private WikiIRCModel ircModel;

    @Override
    public boolean isActive()
    {
        boolean isActive = true;

        try {
            XWikiDocument configurationDocument = this.ircModel.getConfigurationDocument();
            BaseObject configurationObject = configurationDocument.getXObject(CONFIGURATION_CLASS);
            if (configurationObject != null) {
                isActive = !(configurationObject.getIntValue(INACTIVE_PROPERTY) == 1);
            }
        } catch (IRCBotException e) {
            // By default isActive is true
        }

        return isActive;
    }
}
