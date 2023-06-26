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
package org.xwiki.mail.internal.factory;

import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.xwiki.mail.MimeBodyPartFactory;

/**
 * Helper that knows how to handle mail headers by extracting them from the passed parameters.
 *
 * @param <T> the type of content to be added to a Multi Part message
 * @version $Id$
 * @since 6.1M2
 */
public abstract class AbstractMimeBodyPartFactory<T> implements MimeBodyPartFactory<T>
{
    private static final String HEADERS_PARAMETER_KEY = "headers";

    /**
     * Add the mail headers passed as parameters into the Mime Body part also passed as parameter.
     *
     * @param part the body part to which we're adding the headers
     * @param parameters the parameters from which to extract the headers
     * @throws MessagingException in case an error happens when setting a header
     */
    protected void addHeaders(MimeBodyPart part, Map<String, Object> parameters) throws MessagingException
    {
        Map<String, String> headers = (Map<String, String>) parameters.get(HEADERS_PARAMETER_KEY);
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                part.setHeader(header.getKey(), header.getValue());
            }
        }
    }
}
