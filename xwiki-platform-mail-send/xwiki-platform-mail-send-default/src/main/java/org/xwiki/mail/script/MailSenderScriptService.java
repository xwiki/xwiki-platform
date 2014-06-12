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
package org.xwiki.mail.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.XWikiAuthenticator;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Expose Mail Sending API to scripts.
 * </p>
 * Example for sending an HTML message with attachments and a text alternative:
 * <pre><code>
 *   #set ($message = $services.mailSender.createMessage(to, subject))
 *   #set ($discard = $message.addPart("html", "html message", {"alternate" : "text message",
 *     "attachments" : $attachments}))
 *   #set ($discard = $message.send())
 * </code></pre>
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("mailsender")
@Singleton
@Unstable
public class MailSenderScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    static final String ERROR_KEY = "scriptservice.mailsender.error";

    @Inject
    private MailSenderConfiguration configuration;

    @Inject
    private MailSender mailSender;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    public ScriptMimeMessage createMessage()
    {
        return createMessage(this.configuration.getFromAddress(), null, null);
    }

    public ScriptMimeMessage createMessage(String to, String subject)
    {
        return createMessage(this.configuration.getFromAddress(), to, subject);
    }

    public ScriptMimeMessage createMessage(String from, String to, String subject)
    {
        Session session;
        if (this.configuration.usesAuthentication()) {
            session = Session.getInstance(this.configuration.getAllProperties(),
                new XWikiAuthenticator(this.configuration));
        } else {
            session = Session.getInstance(this.configuration.getAllProperties());
        }

        ScriptMimeMessage message =
            new ScriptMimeMessage(session, this.mailSender, this.execution, this.componentManager);

        try {
            if (from != null) {
                message.setFrom(InternetAddress.parse(from)[0]);
            }
            if (to != null) {
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            }
            message.setSubject(subject);
        } catch (Exception e) {
            // An error occurred, save it and return null
            setError(e);
            return null;
        }

        return message;
    }

    // Error management

    /**
     * Get the error generated while performing the previously called action.
     *
     * @return the exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(ERROR_KEY, e);
    }
 }
