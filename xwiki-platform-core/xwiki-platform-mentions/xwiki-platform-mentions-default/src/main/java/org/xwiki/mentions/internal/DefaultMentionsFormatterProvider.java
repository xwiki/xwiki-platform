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
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.mentions.MentionsFormatter;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.mentions.MentionsConfiguration.USER_MENTION_TYPE;

/**
 * Default implementation of {@link MentionFormatterProvider}.
 * <p>
 * A lookup of the requested formatter type is performed. If the type is not defined, the default {@link
 * org.xwiki.mentions.MentionsConfiguration#USER_MENTION_TYPE} type is used.
 * If the lookup fails, the default @link MentionFormatterProvider} component is returned.
 *
 * @version $Id$
 * @since 12.10
 */
@Component
@Singleton
public class DefaultMentionsFormatterProvider implements MentionFormatterProvider
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Inject
    private MentionsFormatter defaultFormatter;

    @Override
    public MentionsFormatter get(String type)
    {
        String hint = type;
        if (StringUtils.isEmpty(type)) {
            hint = USER_MENTION_TYPE;
        }

        MentionsFormatter ret;
        try {
            ret = this.componentManager.getInstance(MentionsFormatter.class, hint);
        } catch (ComponentLookupException e) {
            this.logger
                .debug("Unable to find a formatter with type [{}]. Fallback to the default formatter. Cause: [{}]",
                    type,
                    getRootCauseMessage(e));
            // Provide a default formatter if not found with the requested type.
            ret = this.defaultFormatter;
        }

        return ret;
    }
}
