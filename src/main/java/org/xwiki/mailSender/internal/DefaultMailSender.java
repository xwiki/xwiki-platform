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
package org.xwiki.mailSender.internal;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mailSender.MailSender;
import org.xwiki.mailSender.MailSenderUtils;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.render.XWikiVelocityRenderer;

import java.util.Date;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of a <tt>Mail Sender</tt> component.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultMailSender implements MailSender
{
    /** The name of the Object Type holding mail templates. */
    private static final String EMAIL_XWIKI_CLASS_NAME = "XWiki.Mail";

    /** SMTP authentification. */
    private static final String SMTP_AUTH = "mail.smtp.auth";

    /** Mail SMTP Server Username. */
    private static final String MAIL_SMTP_SERVER_USERNAME = "mail.smtp.server.username";

    /** Mail SMTP Server Password. */
    private static final String MAIL_SMTP_SERVER_PASSWORD = "mail.smtp.server.password";

    /** For plain text content. */
    private static final String PLAIN_TEXT = "text/plain";

    /** For html content. */
    private static final String TEXT_HTML = "text/html";

    /** Name of the language field in Mail objects. */
    private static final String LANGUAGE = "language";

    /** True string. */
    private static final String TRUE = "true";

    /** Mail configuration. */
    private MailConfiguration mailConf;

    /** Provides access to documents. Injected by the Component Manager. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;
    
    /** Provides access to the logger. */
    @Inject
    private Logger logger;

    /** Provides access to the request context. Injected by the Component Manager. */
    @Inject
    private Execution execution;
    
   /** Provides various utilities functions. */
    @Inject
    private MailSenderUtils utils;

    @Override
    public Mail newMail(String from, String to, String cc, String bcc, String subject)
    {
        return new Mail(from, to, cc, bcc, subject);
    }

    @Override
    public int sendMailFromTemplate(String templateDocFullName, String from, String to, String cc, String bcc,
        String language, VelocityContext velocityContext)
    {
        /* Forbids the use of a template created by a user having no programming rights. */
        //MailSenderUtils utils = new MailSenderUtils();
        if (!documentAccessBridge.hasProgrammingRights()) {
            logger.error("No mail has been sent : The sendMailFromTemplate method requires Programming rights");
            return 0;
        }
        try {
            ExecutionContext context = this.execution.getContext();
            XWikiContext xwikiContext = (XWikiContext) context.getProperty("xwikicontext");
            VelocityContext vContext = utils.getVelocityContext(velocityContext, from, to, cc, bcc);

            DocumentReference template = utils.getDocumentRef(templateDocFullName);
            boolean hasRight = utils.checkAccess(template, xwikiContext);
            if (template == null || !hasRight) {
                /* If the current user isn't allowed to view the page of the template, he can't use it to send mails. */
                if (!hasRight) {
                    logger.error("You haven't the right to use this mail template !");
                } else {
                    logger.error("Template reference is invalid");
                }
                return 0;
            }

            DocumentReference mailClass = utils.getDocumentRef(EMAIL_XWIKI_CLASS_NAME);
            int n = getMailObject(template, mailClass, language);
            if (n == -1) {
                logger.error("No mail object found in the document " + templateDocFullName);
                return 0;
            }

            String subject = documentAccessBridge.getProperty(template, mailClass, n, "subject").toString();
            subject = XWikiVelocityRenderer.evaluate(subject, templateDocFullName, vContext, xwikiContext);
            String text = documentAccessBridge.getProperty(template, mailClass, n, "text").toString();
            text = XWikiVelocityRenderer.evaluate(text, templateDocFullName, vContext, xwikiContext);
            String html = documentAccessBridge.getProperty(template, mailClass, n, "html").toString();
            html = XWikiVelocityRenderer.evaluate(html, templateDocFullName, vContext, xwikiContext);

            Mail mail = new Mail(from, to, cc, bcc, subject);
            mail.addContent(PLAIN_TEXT, text);
            if (!StringUtils.isEmpty(html)) {
                mail.addContent(TEXT_HTML, html);
            }
            return this.send(mail);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Return the number of the mail object with the language required.
     * 
     * @param template Reference to the document containing the mail objects
     * @param mailClass Reference to the mail Class
     * @param language Language to find a mail object for
     * @return the number of the mail object in template having the right language. -1 if no mail object found
     */
    private int getMailObject(DocumentReference template, DocumentReference mailClass, String language)
    {
        int n = -1;

        n = documentAccessBridge.getObjectNumber(template, mailClass, LANGUAGE, language);

        if (n == -1) {
            n = documentAccessBridge.getObjectNumber(template, mailClass, LANGUAGE, "en");
            logger.warn("No mail object found with language = " + language);
        }
        return n;
    }

    @Override
    public int send(Mail mail)
    {
        Session session = null;
        Transport transport = null;
        int success = 0;
        if (!mail.isValid()) {
            logger.error("This mail is not valid. It should at least have one recipient and a content.");
            return 0;
        }
        try {
            logger.info("Sending mail : Initializing properties");
            this.mailConf = new MailConfiguration(documentAccessBridge);
            Properties props = mailConf.getProperties();
            session = Session.getInstance(props, null);
            transport = session.getTransport("smtp");
            if (session.getProperty(SMTP_AUTH).equals(TRUE)) {
                transport.connect(session.getProperty(MAIL_SMTP_SERVER_USERNAME),
                    session.getProperty(MAIL_SMTP_SERVER_PASSWORD));
            } else {
                transport.connect();
            }
            MimeMessage message = createMimeMessage(session, mail);
            transport.sendMessage(message, message.getAllRecipients());
            logger.info("Message sent");
            success = 1;
        } catch (SendFailedException sfex) {
            logger.error("SendFailedException encountered while trying to send the mail", sfex);
            success = 0;
        } catch (Exception e) {
            logger.error("Error encountered while trying to create the mail", e);
            success = 0;
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (MessagingException ex) {
                    logger.error("MessagingException has occured while trying to close the transport.", ex);
                }
            }
        }
        return success;
    }

    /**
     * Creates a Mime message for the mail passed as argument.
     * 
     * @param session Session
     * @param mail Mail to create a MimeMessage for
     * @return Mime message
     * @throws MessagingException if the message can't be made
     */
    public MimeMessage createMimeMessage(Session session, Mail mail) throws MessagingException
    {
        boolean hasTo = (mail.getTo() != null && !StringUtils.isEmpty(mail.getTo()));
        MimePartCreator creator = new MimePartCreator();

        Multipart wrapper = creator.generateMimeMultipart(mail, this.utils);
        InternetAddress[] adressesTo = utils.toInternetAddresses(mail.getTo());
        InternetAddress[] adressesReplyTo = utils.toInternetAddresses(mail.getReplyTo());
        InternetAddress[] adressesCc = utils.toInternetAddresses(mail.getCc());
        InternetAddress[] adressesBcc = utils.toInternetAddresses(mail.getBcc());

        MimeMessage message = new MimeMessage(session);
        message.setSentDate(new Date());
        message.setSubject(mail.getSubject());
        message.setFrom(new InternetAddress(mail.getFrom()));
        if (hasTo) {
            message.setRecipients(javax.mail.Message.RecipientType.TO, adressesTo);
        }
        if (adressesReplyTo != null) {
            message.setReplyTo(adressesReplyTo);
        }

        if (adressesCc != null) {
            message.setRecipients(javax.mail.Message.RecipientType.CC, adressesCc);
        }

        if (adressesBcc != null) {
            message.setRecipients(javax.mail.Message.RecipientType.BCC, adressesBcc);
        }
        message.setContent(wrapper);
        message.setSentDate(new Date());
        message.saveChanges();
        return message;
    }
}
