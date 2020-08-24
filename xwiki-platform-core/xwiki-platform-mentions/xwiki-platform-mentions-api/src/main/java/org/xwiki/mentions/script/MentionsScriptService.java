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
package org.xwiki.mentions.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.mentions.DisplayStyle;
import org.xwiki.mentions.MentionsConfiguration;
import org.xwiki.mentions.internal.MentionsEventExecutor;
import org.xwiki.mentions.internal.MentionsFormatter;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Script service for the Mentions application.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Component
@Singleton
@Unstable
@Named("mentions")
public class MentionsScriptService implements ScriptService
{
    @Inject
    private MentionsConfiguration configuration;

    @Inject
    private MentionsFormatter formatter;

    @Inject
    private MentionsEventExecutor eventExecutor;

    /**
     *
     * @see MentionsConfiguration#getMentionsColor()
     * @return the mentions color configuration value.
     */
    public String getMentionsColor()
    {
        return this.configuration.getMentionsColor();
    }

    /**
     *
     * @see MentionsConfiguration#getSelfMentionsColor()
     * @return the mentions colors configuration value for the current user.
     */
    public String getSelfMentionsColor()
    {
        return this.configuration.getSelfMentionsColor();
    }

    /**
     * @see MentionsEventExecutor#getQueueSize()
     * @return the current size of the queue of elements (page, comments...) with mentions to analyze
     * @since 12.6
     */
    @Unstable
    public long getQueueSize()
    {
        return this.eventExecutor.getQueueSize();
    }


    /**
     *
     * @see MentionsConfiguration#isQuoteActivated()
     * @return {@code true} if the mentions quote feature is activated.
     * @since 12.6
     */
    public boolean isQuoteActivated()
    {
        return this.configuration.isQuoteActivated();
    }

    /**
     * Format a user mention.
     *
     * @see MentionsFormatter#formatMention(String, DisplayStyle)
     * @param userReference the user reference
     * @param style the display style
     * @return the formatted mention
     */
    public String format(String userReference, DisplayStyle style)
    {
        return this.formatter.formatMention(userReference, style);
    }
}
