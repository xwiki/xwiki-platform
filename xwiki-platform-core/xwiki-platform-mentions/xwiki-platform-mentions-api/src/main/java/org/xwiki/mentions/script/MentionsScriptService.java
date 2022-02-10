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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.index.TaskManager;
import org.xwiki.mentions.DisplayStyle;
import org.xwiki.mentions.MentionsConfiguration;
import org.xwiki.mentions.MentionsFormatter;
import org.xwiki.mentions.internal.MentionFormatterProvider;
import org.xwiki.script.service.ScriptService;

import static org.xwiki.mentions.MentionsConfiguration.MENTION_TASK_ID;

/**
 * Script service for the Mentions application.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Component
@Singleton
@Named("mentions")
public class MentionsScriptService implements ScriptService
{
    @Inject
    private MentionsConfiguration configuration;

    @Inject
    private MentionFormatterProvider mentionFormatterProvider;

    @Inject
    private TaskManager eventExecutor;

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
     * @return the current size of the queue of elements (page, comments...) with mentions to analyze
     * @since 12.6
     */
    public long getQueueSize()
    {
        return this.eventExecutor.getQueueSize(MENTION_TASK_ID);
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
     * Format an actor mention according to its type.
     *
     * @param actorReference the reference of the mentioned actor
     * @param style the display style
     * @param type the type of the actor to format
     * @return the formatted actor mention
     * @see MentionsFormatter#formatMention(String, DisplayStyle)
     * @since 12.10
     */
    public String format(String actorReference, DisplayStyle style, String type)
    {
        // Uses the "user" type when the mention has an undefined type.
        String hint;
        if (StringUtils.isEmpty(type)) {
            hint = MentionsConfiguration.USER_MENTION_TYPE;
        } else {
            hint = type;
        }
        return this.mentionFormatterProvider.get(hint).formatMention(actorReference, style);
    }
}
