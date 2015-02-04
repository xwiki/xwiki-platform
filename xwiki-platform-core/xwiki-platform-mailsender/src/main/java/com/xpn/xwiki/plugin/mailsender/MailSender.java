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
package com.xpn.xwiki.plugin.mailsender;

import java.util.List;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.XWiki;

/**
 * Send mails to recipients (to, cc, bcc). Both text and HTML emails can be sent along with attachments. Also support
 * XWiki page templates and allows sending a collection of emails in one call.
 * 
 * @version $Id$
 */
public interface MailSender
{
    /**
     * A helper method for Velocity scripts since we cannot create Java objects from Velocity.
     * 
     * @return An empty mail message to be populated with recipient addresses, subject, message, etc.
     */
    Mail createMail();

    /**
     * A helper method for Velocity scripts since we cannot create Java objects from Velocity.
     * 
     * @param xwiki the XWiki object used to get the default values from the XWiki Preferences ("smtp_server" and
     *            "smtp_from").
     * @return A mail server configuration, initialized with values from XWiki Preferences, but which can be overriden
     *         by users.
     */
    MailConfiguration createMailConfiguration(XWiki xwiki);

    /**
     * Generic method for sending emails. The passed Mail object has to be populated by the caller to set the correct
     * fields. All the other <code>sendHtmlXXX()</code> and <code>sendTextXXX()</code> methods are specialized helper
     * versions of this generic method.
     * 
     * @param mail the already populated mail Object to be sent
     * @return 0 on success, -1 on failure. On failure the error message is stored in the XWiki context under the
     *         "error" key.
     */
    int sendMail(Mail mail);

    /**
     * Generic method for sending emails. The passed Mail object has to be populated by the caller to set the correct
     * fields. The passed Mail Configuration allows the user to override the default connection properties (SMTP host,
     * SMTP port, SMTP from, etc). All the other <code>sendHtmlXXX()</code> and <code>sendTextXXX()</code> methods are
     * specialized helper versions of this generic method.
     * 
     * @param mail the already populated mail Object to be sent
     * @param mailConfiguration the configuration to use
     * @return 0 on success, -1 on failure. On failure the error message is stored in the XWiki context under the
     *         "error" key.
     */
    int sendMail(Mail mail, MailConfiguration mailConfiguration);

    /**
     * Sends an HTML mail, with a list of attachments
     * 
     * @param to the recipient of the message
     * @param from the sender
     * @param cc carbon copy
     * @param bcc hidden carbon copy
     * @param subject the subject of the message
     * @param body the body content of the mail
     * @param alternative the alternative text offered to the mail client
     * @param attachments List of com.xpn.xwiki.api.Attachment that will be attached to the mail.
     * @return 0 on success, -1 on failure. On failure the error message is stored in the XWiki context under the
     *         "error" key.
     */
    int sendHtmlMessage(String from, String to, String cc, String bcc, String subject, String body, String alternative,
        List<Attachment> attachments);

    /**
     * Sends a simple text plain mail
     * 
     * @param to the recipient of the message
     * @param from the sender
     * @param subject the subject of the message
     * @param message the body of the message
     * @return 0 on success, -1 on failure. On failure the error message is stored in the XWiki context under the
     *         "error" key.
     */
    int sendTextMessage(String from, String to, String subject, String message);

    /**
     * Sends a simple text plain mail with a list of files attachments
     * 
     * @param to the recipient of the message
     * @param from the sender
     * @param cc carbon copy
     * @param bcc hidden carbon copy
     * @param subject the subject of the message
     * @param message the body of the message
     * @param attachments List of com.xpn.xwiki.api.Attachment that will be attached to the mail.
     * @return 0 on success, -1 on failure. On failure the error message is stored in the XWiki context under the
     *         "error" key.
     */
    int sendTextMessage(String from, String to, String cc, String bcc, String subject, String message,
        List<Attachment> attachments);

    /**
     * Sends a raw message. The message can contain additional headers at the start, which are parsed and correctly sent
     * as additional headers (Bcc, Subject, Reply-To, etc.). The actual message is treated as plain text.
     * 
     * @param from the sender
     * @param to the receiver
     * @param rawMessage the raw message, containing additional headers and the actual message
     * @return 0 on success, -1 on failure. On failure the error message is stored in the XWiki context under the
     *         "error" key.
     */
    int sendRawMessage(String from, String to, String rawMessage);

    /**
     * Uses an XWiki document to build the message subject and context, based on variables stored in the
     * VelocityContext. Sends the email.
     * 
     * @param from Email sender
     * @param to Email recipient
     * @param cc Email Carbon Copy
     * @param bcc Email Hidden Carbon Copy
     * @param language Language of the email
     * @param documentFullName Full name of the template to be used (example: XWiki.MyEmailTemplate). The template needs
     *            to have an XWiki.Email object attached
     * @param vcontext Velocity context passed to the velocity renderer
     * @return 0 on success, -1 on failure. On failure the error message is stored in the XWiki context under the
     *         "error" key.
     */
    int sendMessageFromTemplate(String from, String to, String cc, String bcc, String language,
        String documentFullName, VelocityContext vcontext);
}
