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
package org.xwiki.mailSender;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.mailSender.internal.Mail;

/**
 * Interface (aka Role) of the Component.
 * 
 * @version $Id$
 */
@ComponentRole
public interface MailSender
{
    /**
     * Creates a new Mail object.
     * 
     * @param from Sender
     * @param to Recipients
     * @param cc Carbon copy recipients
     * @param bcc Hidden carbon copy recipients
     * @param subject Mail subject
     * 
     * @return a Mail object
     */
    Mail newMail(String from, String to, String cc, String bcc, String subject);
 
    /**
     * Send the mail passed as argument.
     * 
     * @param mail Mail to be send
     * @return 1 if the email has been sent
     */
    int send(Mail mail);

    /**
     * Uses an XWiki document to build the message subject and context, based on variables stored in the
     * VelocityContext. Sends the mail.
     * 
     * @param templateDocFullName Full name of the template to be used (example: XWiki.MyEmailTemplate). The template
     *            needs to have an XWiki.Email object attached
     * @param from Email sender
     * @param to Email recipient
     * @param cc Email Carbon Copy
     * @param bcc Email Hidden Carbon Copy
     * @param language Language of the email
     * @param vContext Velocity context passed to the velocity renderer.
     * @return True if the email has been sent
     */
    int sendMailFromTemplate(String templateDocFullName, String from, String to, String cc, String bcc,
        String language, VelocityContext vContext);
    
    /**
     * For test.
     * 
     * @param session Session
     * @param mail Mail
     * @throws MessagingException mex
     * @return Mime
     */
    MimeMessage createMimeMessage(Session session, Mail mail) throws MessagingException;
    
}
