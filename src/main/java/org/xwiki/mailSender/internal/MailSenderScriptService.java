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

import java.util.Date;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.mailSender.MailSender;
import org.xwiki.script.service.ScriptService;
import org.xwiki.velocity.VelocityManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Make the MailSender API available to scripting.
 * 
 * @version $Id$
 */
@Component
@Named("mailSender")
@Singleton
public class MailSenderScriptService implements ScriptService
{
    /** The mailSender component. */
    @Inject
    private MailSender mailSender;

    /** Provides access to the velocity context. **/
    @Inject
    private VelocityManager velocityManager;

    /**
     * Create a new Mail object.
     * 
     * @param from Mail sender
     * @param to Mail recipient
     * @param subject Mail subject
     * @return a Mail object
     */
    public Mail newMail(String from, String to, String subject)
    {
        return this.mailSender.newMail(from, to, null, null, subject);
    }

    /**
     * Test.
     * 
     * @return a string to test
     */
    public String greet()
    {
        return "Hello!";
    }

    /**
     * Create a new Mail object.
     * 
     * @param from Mail sender
     * @param to Mail recipient
     * @param cc Mail CarbonCopy
     * @param bcc Mail Hidden Carbon Copy
     * @param subject Mail subject
     * @return a Mail object
     */
    public Mail newMail(String from, String to, String cc, String bcc, String subject)
    {
        return this.mailSender.newMail(from, to, cc, bcc, subject);
    }

    /**
     * Send the mail passed as argument.
     * 
     * @param mail Mail to be send
     * @return 1 if the email has been sent
     */
    public int send(Mail mail)
    {
        return this.mailSender.send(mail);
    }

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
    public int sendMailFromTemplate(String templateDocFullName, String from, String to, String cc, String bcc,
        String language, VelocityContext vContext)
    {

        return this.mailSender.sendMailFromTemplate(templateDocFullName, from, to, cc, bcc, language, vContext);
    }

    /**
     * Uses an XWiki document to build the message subject and context, based on variables stored in the current
     * VelocityContext and in the Map parameters. Sends the mail.
     * 
     * @param templateDocFullName Full name of the template to be used (example: XWiki.MyEmailTemplate). The template
     *            needs to have an XWiki.Email object attached
     * @param from Email sender
     * @param to Email recipient
     * @param cc Email Carbon Copy
     * @param bcc Email Hidden Carbon Copy
     * @param language Language of the email
     * @param parameters Parameters to add to the current velocity context
     * @return True if the email has been sent
     */
    public int sendMailFromTemplate(String templateDocFullName, String from, String to, String cc, String bcc,
        String language, Map<String, Object> parameters)
    {
        VelocityContext vContext = velocityManager.getVelocityContext();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            vContext.put(entry.getKey(), entry.getValue());
        }
        return this.mailSender.sendMailFromTemplate(templateDocFullName, from, to, cc, bcc, language, vContext);
    }

    /**
     * Send a mail having the properties specified.
     * 
     * @param from Mail sender
     * @param to Mail recipient
     * @param subject Mail subject
     * @param html Mail html content
     * @param alternative Mail alternative text content.
     * @return 1 if the email has been sent
     */
    public int sendHtmlMail(String from, String to, String subject, String html, String alternative)
    {
        Mail mail = new Mail(from, to, null, null, subject);
        if (alternative != null) {
            mail.addContent("text/plain", alternative);
        }
        mail.addContent("text/html", html);
        return this.mailSender.send(mail);
    }

    /**
     * Create a calendar to be embed in an email.
     * 
     * @param location Location of the event
     * @param summary Summary of the event
     * @param startDate Start date of the event
     * @param endDate End date of the event
     * @return a string representing the calendar
     */
    public String createCalendar(String location, String summary, Date startDate, Date endDate)
    {
        VCalendar vcal = new VCalendar(startDate, endDate, location, summary);
        return vcal.toString();
    }
}
