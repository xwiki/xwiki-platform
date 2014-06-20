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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.mail.internal.script.ScriptMailSenderListener;
import org.xwiki.stability.Unstable;

/**
 * Simulates a, {@link javax.mail.internet.MimeMessage} with additional helper methods to add body part content and to
 * send the message.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@Unstable
public class MimeMessageWrapper
{
    private ComponentManager componentManager;

    private MailSender mailSender;

    private Execution execution;

    private Multipart multipart;

    private ScriptMailSenderListener listener;

    private Session session;

    private MimeMessage message;

    /**
     * @param message the wrapped {@link MimeMessage}
     * @param session the JavaMail session used to send the mail
     * @param mailSender the component to send the mail
     * @param execution used to get the Execution Context and store an error in it if the send fails
     * @param componentManager used to dynamically load all {@link MimeBodyPartFactory} components
     */
    public MimeMessageWrapper(MimeMessage message, Session session, MailSender mailSender, Execution execution,
        ComponentManager componentManager)
    {
        this.message = message;
        this.session = session;
        this.mailSender = mailSender;
        this.execution = execution;
        this.componentManager = componentManager;
        this.listener = new ScriptMailSenderListener(this.execution.getContext());
    }

    /**
     * @return the wrapped {@link javax.mail.internet.MimeMessage}
     */
    public MimeMessage getMessage()
    {
        return this.message;
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

        // Pass the mime type in the parameters so that generic Mime Body Part factories can use it.
        // Note that if the use has already passed a "mimetype" parameter then we don't override it!
        Map<String, Object> enhancedParameters = new HashMap<>();
        enhancedParameters.put("mimetype", mimeType);
        enhancedParameters.putAll(parameters);

        MimeBodyPart part = factory.create(content, enhancedParameters);
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
            getMessage().setContent(this.multipart);

            this.mailSender.send(getMessage(), this.session, this.listener);

        } catch (MessagingException e) {
            // Save the exception for reporting through the script services's getError() API
            setError(e);
        }
    }

    /**
     * @param subject the subject to set in the Mime Message
     */
    public void setSubject(String subject)
    {
        try {
            getMessage().setSubject(subject);
        } catch (Exception e) {
            setError(e);
        }
    }

    /**
     * @param address the address from which this message will be sent
     */
    public void setFrom(Address address)
    {
        try {
            getMessage().setFrom(address);
        } catch (Exception e) {
            setError(e);
        }
    }

    /**
     * @param type the type of recipients (to, cc, bcc, newsgroups)
     * @param addresses the email addresses of the recipients
     */
    public void addRecipients(Message.RecipientType type, Address[] addresses)
    {
        try {
            getMessage().addRecipients(type, addresses);
        } catch (Exception e) {
            setError(e);
        }
    }

    /**
     * @param type the type of recipient (to, cc, bcc, newsgroups)
     * @param address the email address of the recipient
     */
    public void addRecipient(Message.RecipientType type, Address address)
    {
        try {
            getMessage().addRecipient(type, address);
        } catch (Exception e) {
            setError(e);
        }
    }

    /**
     * Add a mail header.
     *
     * @param name the header's name  (eg "Message-Id")
     * @param value the header's value
     */
    public void addHeader(String name, String value)
    {
        try {
            getMessage().addHeader(name, value);
        } catch (Exception e) {
            setError(e);
        }
    }

    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(MailSenderScriptService.ERROR_KEY, e);
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
            factory = getSpecificBodyPartFactory(mimeType, contentClass);
        } catch (ComponentLookupException e) {
            // No factory found for the passed Mime Type and type of Content.
            // If the content class is of type String then we default to the default MimeBodyPartFactory for String
            // content.
            if (String.class.isAssignableFrom(contentClass)) {
                try {
                    factory = this.componentManager.getInstance(
                        new DefaultParameterizedType(null, MimeBodyPartFactory.class, String.class));
                } catch (ComponentLookupException eee) {
                    // This shouldn't happen, if it does then it's an error and we want that error to bubble up till
                    // the user since it would be pretty bad to send an email with some missing body part!
                    throw new MessagingException(String.format(
                        "Failed to find default Mime Body Part Factory for mime type [%s] and Content type [%s]",
                            mimeType, contentClass.getName()), e);
                }
            } else {
                throw new MessagingException(String.format(
                    "Failed to a Mime Body Part Factory matching the mime type [%s] and the Content type [%s]",
                        mimeType, contentClass.getName()), e);
            }
        }
        return factory;
    }

    private MimeBodyPartFactory getSpecificBodyPartFactory(String mimeType, Class contentClass)
        throws ComponentLookupException
    {
        MimeBodyPartFactory factory;
        try {
            // Look a secure version of a specific MimeBodyPartFactory for the passed Mime Type and Content type.
            factory = this.componentManager.getInstance(new DefaultParameterizedType(null, MimeBodyPartFactory.class,
                contentClass),  String.format("%s/secure", mimeType));
        } catch (ComponentLookupException e) {
            // Look for a specific MimeBodyPartFactory for the passed Mime Type and Content type (non secure).
            factory = this.componentManager.getInstance(
                new DefaultParameterizedType(null, MimeBodyPartFactory.class, contentClass), mimeType);
        }
        return factory;
    }
}
