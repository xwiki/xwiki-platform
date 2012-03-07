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
package org.xwiki.ircbot.internal.wiki;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.wiki.IRCEventListenerConfiguration;
import org.xwiki.ircbot.wiki.WikiIRCBotConstants;
import org.xwiki.ircbot.wiki.WikiIRCModel;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Provides configuration data for the {@link IRCEventListener} Event Listener.
 *
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
public class DefaultIRCEventListenerConfiguration implements IRCEventListenerConfiguration, WikiIRCBotConstants
{
    @Inject
    private WikiIRCModel ircModel;

    @Override
    public List<Pattern> getExclusionPatterns() throws IRCBotException
    {
        List<Pattern> patterns = new ArrayList<Pattern>();

        // Look for IRC.IRCEventExclusionClass objects in the IRC.IRCBotConfiguration document
        XWikiDocument configurationDocument = this.ircModel.getConfigurationDocument();
        List<BaseObject> exclusionObjects = configurationDocument.getXObjects(EXCLUSION_CLASS);
        if (exclusionObjects != null) {
            for (BaseObject exclusionObject : exclusionObjects) {
                String pattern = exclusionObject.getStringValue(PATTERN_PROPERTY);
                patterns.add(Pattern.compile(pattern));
            }
        }

        return patterns;
    }
}
