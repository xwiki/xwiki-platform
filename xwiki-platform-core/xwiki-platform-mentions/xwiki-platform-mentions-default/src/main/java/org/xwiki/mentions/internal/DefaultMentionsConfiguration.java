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
package org.xwiki.mentions.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.mentions.MentionsConfiguration;

/**
 * Default implementation of {@link MentionsConfiguration}.
 * The settings are retrieved from the instance of Mentions.ConfigurationClass stored in Mentions.Configuration. 
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Component
@Singleton
public class DefaultMentionsConfiguration implements MentionsConfiguration
{
    @Inject
    @Named("mentions")
    private ConfigurationSource configuration;

    @Override
    public String getMentionsColor()
    {
        // default color is rgba, not hexa to allow setting opacity and keep working on IE11
        return this.configuration.getProperty("mentionsColor", "rgba(194, 194, 194, 0.8)");
    }

    @Override
    public String getSelfMentionsColor()
    {
        // default color is rgba, not hexa to allow setting opacity and keep working on IE11
        return this.configuration.getProperty("selfMentionsColor", "rgba(255, 0, 1, 0.5)");
    }

    @Override
    public String getSelfMentionsForeground()
    {
        // default color is rgba, not hexa to allow setting opacity and keep working on IE11
        return this.configuration.getProperty("selfMentionsForeground", "rgba(255, 255, 255, 1)");
    }

    @Override
    public boolean isQuoteActivated()
    {
        return this.configuration.getProperty("quoteActivated", false);
    }
}
