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

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.util.encoders.Base64;

/**
 * Compute unique message identifiers from a given mime messages for the purpose of uniquely identifying messages
 * in this Mail API.
 *
 * @version $Id$
 * @since 7.4.1
 */
public class MessageIdComputer
{
    private final SHA1Digest sha1Digest = new SHA1Digest();

    /**
     * Compute a unique message identifier for the provided mime message.
     *
     * @param message the mime message for which an identifier should be computed
     * @return a unique identifier for the message
     */
    public String compute(MimeMessage message)
    {
        try {
            String id = message.getMessageID();
            if (id == null) {
                message.saveChanges();
                id = message.getMessageID();
            }
            return digest(id + ":" + InternetAddress.toString(message.getRecipients(Message.RecipientType.TO)));
        } catch (MessagingException e) {
            // This is very uncommon to happen since in practice since the current implementation never throws any
            // exception!
            return null;
        }
    }

    private String digest(String data)
    {
        byte[] bytes = data.getBytes();
        sha1Digest.update(bytes, 0, bytes.length);
        byte[] dig = new byte[sha1Digest.getDigestSize()];
        sha1Digest.doFinal(dig, 0);
        return Base64.toBase64String(dig);
    }
}
