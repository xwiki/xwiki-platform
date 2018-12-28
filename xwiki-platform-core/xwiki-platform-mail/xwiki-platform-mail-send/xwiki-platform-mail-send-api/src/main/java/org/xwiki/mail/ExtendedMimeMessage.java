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

import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.util.encoders.Base64;

/**
 * Extension of the {@link javax.mail.internet.MimeMessage} in order to support processing by this mail API.
 *
 * @version $Id$
 * @since 7.4.1
 */
public class ExtendedMimeMessage extends MimeMessage
{
    private static final ThreadLocal<SHA1Digest> SHA1_DIGEST = new ThreadLocal<SHA1Digest>();

    private static final String MESSAGE_ID_HEADER = "Message-ID";
    private static final String TO_HEADER = "To";
    private static final String XMAIL_TYPE_HEADER = "X-MailType";

    private String uniqueMessageId;

    /**
     * Create a new extended MimeMessage.
     *
     * Note: We don't care about supporting Session here ATM since it's not required. MimeMessages will be
     * given a valid Session when it's deserialized from the mail content store for sending.
     */
    public ExtendedMimeMessage()
    {
        super((Session) null);
    }

    /**
     * Constructs a MimeMessage by reading and parsing the data from the specified MIME InputStream.
     *
     * @param session Session object for this message
     * @param is the message input stream
     * @throws MessagingException on error
     */
    public ExtendedMimeMessage(Session session, InputStream is) throws MessagingException
    {
        super(session, is);
    }

    /**
     * @param source see javadoc for {@link MimeMessage#MimeMessage(javax.mail.internet.MimeMessage)}
     * @throws MessagingException see javadoc for {@link MimeMessage#MimeMessage(javax.mail.internet.MimeMessage)}
     */
    public ExtendedMimeMessage(MimeMessage source) throws MessagingException
    {
        super(source);
    }

    /**
     * Helper method to wrap any {@link MimeMessage} into an {@link ExtendedMimeMessage}, without double wrapping.
     *
     * @param message the {@link MimeMessage} to wrap.
     * @return the {@link ExtendedMimeMessage}.
     * @throws RuntimeException if an error occurs during the conversion, which is unexpected if the initial message
     * is a fully formed MimeMessage or already an ExtendedMimeMessage.
     */
    public static ExtendedMimeMessage wrap(MimeMessage message)
    {
        if (message instanceof ExtendedMimeMessage) {
            return (ExtendedMimeMessage) message;
        } else {
            try {
                return new ExtendedMimeMessage(message);
            } catch (MessagingException e) {
                // We do not expect that an existing MimeMessage could not be wrapped in an extended one.
                throw new RuntimeException("Unexpected exception while wrapping a MimeMessage into an extended one", e);
            }
        }
    }

    /**
     * @return true if no body content has been defined yet for this message or false otherwise
     */
    public boolean isEmpty()
    {
        return this.dh == null;
    }

    /**
     * Specifies what type of email is being sent. This is useful for applications to specify a type when they send
     * mail. This allows (for example) to filter these emails in the Mail Sender Status Admin UI.
     *
     * @param mailType the type of mail being sent (e.g "Watchlist", "Reset Password", "Send Page by Mail", etc)
     */
    public void setType(String mailType)
    {
        try {
            addHeader(XMAIL_TYPE_HEADER, mailType);
        } catch (MessagingException e) {
            // Very unlikely to happen since the default implementation does not throw anything
            throw new RuntimeException(String.format("Failed to set Type header to [%s]", mailType), e);
        }
    }

    /**
     * Retrieve what type of email is being sent (see {@link #setType(String)}).
     *
     * @return the type of mail being sent (e.g "Watchlist", "Reset Password", "Send Page by Mail", etc)
     */
    public String getType()
    {
        try {
            return getHeader(XMAIL_TYPE_HEADER, null);
        } catch (MessagingException e) {
            // Very unlikely to happen since the default implementation does not throw anything
            throw new RuntimeException("Failed to get Type header", e);
        }
    }

    /**
     * Save the message and set the message-ID headers of the message to the provided value.
     *
     * @param messageId message identifier to be set on the message header.
     */
    public void setMessageId(String messageId)
    {
        try {
            ensureSaved();
            setHeader(MESSAGE_ID_HEADER, messageId);
        } catch (MessagingException e) {
            // Very unlikely to happen since the default implementation does not throw anything
            throw new RuntimeException(String.format("Failed to set Message ID header to [%s]", messageId), e);
        }
    }

    /**
     * Ensure that a message is saved to ensure the stability of its Message-ID header.
     *
     * @return true if the message has been saved, false if it was already saved.
     * @throws MessagingException on error
     */
    public boolean ensureSaved() throws MessagingException
    {
        if (!this.saved) {
            this.saveChanges();
            return true;
        }
        return false;
    }

    @Override
    public void setHeader(String name, String value) throws MessagingException
    {
        if (uniqueMessageId != null && MESSAGE_ID_HEADER.equals(name) || TO_HEADER.equals(name)) {
            // Clear cached unique messageId when the headers used to compute it are changed
            uniqueMessageId = null;
        }
        super.setHeader(name, value);
    }

    /**
     * Compute a unique message identifier for this mime message. The unique identifier is based on the
     * message-ID header and the recipients. If no message-ID is found, the message is first saved in order to
     * generate a message-ID.
     *
     * @return a unique identifier for this message
     */
    public String getUniqueMessageId()
    {
        if (uniqueMessageId == null) {
            try {
                StringBuilder sb = new StringBuilder(getNotNullMessageId());
                String recipients = InternetAddress.toString(getAllRecipients());
                if (recipients != null) {
                    sb.append(':').append(recipients);
                }
                uniqueMessageId = digest(sb.toString());
            } catch (MessagingException e) {
                // This should never happen since the implementation for getHeader() never throws an exception (even
                // though the interface specifies it can) and similarly getAllRecipients() will also never throw an
                // exception since the only reason would be if an address is malformed but there's a check when setting
                // it already in the MimeMessage and thus in practice it cannot happen.
                throw new RuntimeException("Unexpected exception while computing a unique id for a MimeMessage", e);
            }
        }
        return uniqueMessageId;
    }

    private String getNotNullMessageId() throws MessagingException
    {
        String messageId = getMessageID();
        if (messageId == null) {
            saveChanges();
            messageId = getMessageID();
        }
        return messageId;
    }

    private String digest(String data)
    {
        SHA1Digest digest = SHA1_DIGEST.get();
        if (digest == null) {
            digest = new SHA1Digest();
            SHA1_DIGEST.set(new SHA1Digest());
        }
        byte[] bytes = data.getBytes();
        digest.update(bytes, 0, bytes.length);
        byte[] dig = new byte[digest.getDigestSize()];
        digest.doFinal(dig, 0);
        return Base64.toBase64String(dig);
    }
}
