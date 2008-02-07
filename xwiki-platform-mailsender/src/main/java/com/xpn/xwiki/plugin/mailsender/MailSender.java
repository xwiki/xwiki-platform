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

import org.apache.velocity.VelocityContext;

import java.util.List;

/**
 * Send mails to recipients (to, cc, bcc). Both text and HTML emails can be sent along with
 * attachments. Also support XWiki page templates and allows sending a collection of emails
 * in one call.
 * 
 * @version $Id: $
 */
public interface MailSender
{
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
     * @return 0 on success, -1 on failure. on failure the error message is stored in XWiki context
     */
    int sendHtmlMessage(String from, String to, String cc, String bcc, String subject,
        String body, String alternative, List attachments);

    /**
     * Sends a simple text plain mail
     *
     * @param to the recipient of the message
     * @param from the sender
     * @param subject the subject of the message
     * @param message the body of the message
     * @return 0 on success, -1 on failure. on failure the error message is stored in XWiki context
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
     * @return 0 on success, -1 on failure. on failure the error message is stored in XWiki context
     */
    int sendTextMessage(String from, String to, String cc, String bcc, String subject,
        String message, List attachments);

    /**
     * Uses an XWiki document to build the message subject and context, based on variables stored in
     * the VelocityContext. Sends the email.
     *
     * @param from Email sender
     * @param to Email recipient
     * @param cc Email Carbon Copy
     * @param bcc Email Hidden Carbon Copy
     * @param language Language of the email
     * @param documentFullName Full name of the template to be used (example:
     * XWiki.MyEmailTemplate). The template needs to have an XWiki.Email object attached
     * @param vcontext Velocity context passed to the velocity renderer
     * @return True if the email has been sent
     */
    int sendMessageFromTemplate(String from, String to, String cc, String bcc,
        String language, String documentFullName, VelocityContext vcontext);
}