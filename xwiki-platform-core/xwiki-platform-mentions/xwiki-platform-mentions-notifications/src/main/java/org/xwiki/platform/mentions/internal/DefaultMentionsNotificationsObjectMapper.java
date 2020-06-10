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
package org.xwiki.platform.mentions.internal;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.platform.mentions.events.MentionEventParams;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Default implementation of {@link MentionsNotificationsObjectMapper}.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Component
@Singleton
public class DefaultMentionsNotificationsObjectMapper implements MentionsNotificationsObjectMapper, Initializable
{
    @Inject
    private Logger logger;

    private ObjectMapper objectMapper;

    @Override
    public Optional<MentionEventParams> unserialize(String data)
    {
        try {
            MentionEventParams mentionEventParams =
                this.objectMapper.readValue(new StringReader(data), MentionEventParams.class);
            return Optional.of(mentionEventParams);
        } catch (IOException e) {
            this.logger.warn("Failed to parse [{}]. Cause [{}].", data, getRootCauseMessage(e));
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> serialize(MentionEventParams params)
    {
        Optional<String> ret;
        try {
            StringWriter w = new StringWriter();
            this.objectMapper.writeValue(w, params);
            ret = Optional.of(w.toString());
        } catch (IOException e) {
            this.logger.warn("Failed to serialize [{}]. Cause [{}].", params, getRootCauseMessage(e));
            ret = Optional.empty();
        }
        return ret;
    }

    @Override
    public void initialize()
    {
        this.objectMapper = new ObjectMapper();
    }
}
