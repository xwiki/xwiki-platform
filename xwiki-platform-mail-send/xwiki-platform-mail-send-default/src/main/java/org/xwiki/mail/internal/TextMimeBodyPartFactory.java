/*
 *
 *  * See the NOTICE file distributed with this work for additional
 *  * information regarding copyright ownership.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.xwiki.mail.internal;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MimeBodyPartFactory;

/**
 * Creates text message body Part to be added to a Multi Part message.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("text")
@Singleton
public class TextMimeBodyPartFactory implements MimeBodyPartFactory<String>
{
    /** Provides access to the logger. */
    @Inject
    private Logger logger;

    @Override public MimeBodyPart create(String content)
    {
        return this.create(content, Collections.<String, Object>emptyMap());
    }

    @Override public MimeBodyPart create(String content, Map<String, Object> parameters)
    {
        // Check if existing headers
        Boolean hasHeaders = parameters.containsKey("headers");

        // Create the text part of the email
        MimeBodyPart textPart = new MimeBodyPart();
        try {
            textPart.setContent(content, "text/plain; charset=" + StandardCharsets.UTF_8.name());
            if (hasHeaders && parameters.get("headers") instanceof Map) {
                Map<String, String> headers = (Map<String, String>) parameters.get("headers");
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    textPart.setHeader(header.getKey(), header.getValue());
                }
            }
        } catch (MessagingException e) {
            logger.error("MessagingException has occurred [{}]", e.getMessage());

        }
        return textPart;
    }
}
