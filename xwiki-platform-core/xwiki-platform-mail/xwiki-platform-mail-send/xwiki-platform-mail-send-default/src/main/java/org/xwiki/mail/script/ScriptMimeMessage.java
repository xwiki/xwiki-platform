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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.mail.internal.script.ScriptMailSenderListener;

/**
 * Extends {@link javax.mail.internet.MimeMessage} to add helper APIs to add body part content and to send the message.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class ScriptMimeMessage extends MimeMessage
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptMimeMessage.class);

    private ComponentManager componentManager;

    private MailSender mailSender;

    private Execution execution;

    private Multipart multipart;

    private ScriptMailSenderListener listener;

    /**
     * @param session the JavaMail Session that is used to send the mail (contains all configuration such as SMTP
     *        server, SMTP port, etc)
     * @param mailSender the component to send the mail
     * @param execution used to get the Execution Context and store an error in it if the send fails
     * @param componentManager used to dynamically load all {@link MimeBodyPartFactory} components
     */
    public ScriptMimeMessage(Session session, MailSender mailSender, Execution execution,
        ComponentManager componentManager)
    {
        super(session);

        this.mailSender = mailSender;
        this.execution = execution;
        this.componentManager = componentManager;
        this.listener = new ScriptMailSenderListener(this.execution);
    }

    /**
     * Add some content to the mail to be sent. Can be called several times to add different content type to the mail.
     *
     * @param mimeType the mime type of the content parameter
     * @param content the content to include in the mail
     * @throws MessagingException when an error happens, for example if a body part factory fails to generate a valid
     *         Body Part
     */
    public void addPart(String mimeType, Object content) throws MessagingException
    {
        addPart(mimeType, content, Collections.<String, Object>emptyMap());
    }

    /**
     * Add some content to the mail to be sent. Can be called several times to add different content type to the mail.
     *
     * @param mimeType the mime type of the content parameter
     * @param content the content to include in the mail
     * @param parameters the list of extra parameters. This is used for example to pass alternate content for the mail
     *        using the {@code alternate} key in the HTML Mime Body Part Factory. Mail headers can also be passed using
     *        the {@code headers} key with a {@code Map<String, String>} value containing header keys and values
     * @throws MessagingException when an error happens, for example if a body part factory fails to generate a valid
     *         Body Part
     */
    public void addPart(String mimeType, Object content, Map<String, Object> parameters) throws MessagingException
    {
        MimeBodyPartFactory factory = getBodyPartFactory(mimeType, content.getClass());

        // If this is the first Part we create the MultiPart object.
        if (this.multipart == null) {
            this.multipart = new MimeMultipart("mixed");
        }
        MimeBodyPart part = factory.create(content, parameters);
        this.multipart.addBodyPart(part);
    }

    /**
     * Send the mail asynchronously. You should use {@link #waitTillSent(long)} to make it blocking.
     */
    public void send()
    {
        try {
            // Add the multi part to the content of the message to send. We do this when calling send() since the user
            // can call addPart() several times.
            setContent(this.multipart);

            this.mailSender.send(this, this.session, this.listener);

        } catch (MessagingException e) {
            // Save the exception for reporting through the script services's getError() API
            this.execution.getContext().setProperty(MailSenderScriptService.ERROR_KEY, e);
        }
    }

    /**
     * Wait for all messages on the sending queue to be sent before returning.
     *
     * @param timeout the maximum amount of time to wait in milliseconds
     */
    public void waitTillSent(long timeout)
    {
        this.mailSender.waitTillSent(timeout);
    }

    /**
     * @return a queue containing top errors raised during the send of all emails in the queue for the current thread
     */
    public BlockingQueue<Throwable> getErrors()
    {
        return this.listener.getExceptionQueue();
    }

    private MimeBodyPartFactory getBodyPartFactory(String mimeType, Class contentClass) throws MessagingException
    {
        MimeBodyPartFactory factory;
        try {
            // Look for a specific MimeBodyPartFactory for the passed Mime Type and Content type.
            factory = this.componentManager.getInstance(new DefaultParameterizedType(null, MimeBodyPartFactory.class,
                contentClass),  mimeType);
        } catch (ComponentLookupException e) {
            // No factory found for the passed Mime Type and type of Content.
            // If the content class is of type String then we default to the default MimeBodyPartFactory for String
            // content.
            try {
                factory = this.componentManager.getInstance(
                    new DefaultParameterizedType(null, MimeBodyPartFactory.class, String.class));
            } catch (ComponentLookupException ee) {
                // This shouldn't happen, if it does then it's an error and we want that error to bubble up till the
                // user since it would be pretty bad to send an email with some missing body part!
                throw new MessagingException(String.format(
                    "Failed to find default Mime Body Part Factory for mime type [%s] and Content type [%s]",
                        mimeType, contentClass.getName()), e);
            }
        }
        return factory;
    }
}
