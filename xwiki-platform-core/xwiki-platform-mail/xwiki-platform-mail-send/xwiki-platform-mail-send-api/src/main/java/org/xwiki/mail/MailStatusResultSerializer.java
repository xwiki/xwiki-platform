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
package org.xwiki.mail;

import java.util.Iterator;

/**
 * Serialize {@link org.xwiki.mail.MailStatusResult}. Helps reporting errors.
 *
 * @version $Id$
 * @since 6.4RC1
 */
public final class MailStatusResultSerializer
{
    private MailStatusResultSerializer()
    {
        // Voluntarily empty.
    }

    /**
     * @param statusResult the mail status results retrieved from a Mail Listener
     * @return the errors serialized as a String
     */
    public static String serializeErrors(MailStatusResult statusResult)
    {
        String result;

        Iterator<MailStatus> statuses = statusResult.getAllErrors();
        if (statuses.hasNext()) {
            StringBuilder builder =
                new StringBuilder("Some messages have failed to be sent: [");
            while (statuses.hasNext()) {
                MailStatus status = statuses.next();
                builder.append('[').append(status.toString()).append(']');
            }
            builder.append(']');
            result = builder.toString();
        } else {
            result = null;
        }
        return result;
    }
}
