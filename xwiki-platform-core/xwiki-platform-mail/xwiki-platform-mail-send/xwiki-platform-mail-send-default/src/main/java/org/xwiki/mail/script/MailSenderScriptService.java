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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.mail.internal.script.MimeMessageFactoryProvider;

/**
 * Expose Mail Sending API to scripts.
 * <p>
 * Example for sending an HTML message with attachments and a text
 * alternative:
 * <pre><code>
 *   #set ($message = $services.mail.sender.createMessage(to, subject))
 *   #set ($discard = $message.addPart("html", "html message", {"alternate" : "text message",
 *     "attachments" : $attachments}))
 *   #set ($mailResult = $services.mail.sender.send($message))
 * </code></pre>
 *
 * @version $Id$
 * @since 12.4RC1
 */
@Component
@Named("mail.sender")
@Singleton
public class MailSenderScriptService extends AbstractMailScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    static final String ERROR_KEY = "scriptservice.mail.sender.error";

    /**
     * Creates a pre-filled Mime Message by running the Component implementation of {@link
     * org.xwiki.mail.MimeMessageFactory} corresponding to the passed hint.
     *
     * @param hint the component hint of a {@link org.xwiki.mail.MimeMessageFactory} component
     * @param source the source from which to prefill the Mime Message (depends on the implementation)
     * @param parameters an optional generic list of parameters. The supported parameters depend on the implementation
     * @return the pre-filled Mime Message wrapped in a {@link ScriptMimeMessage} instance
     */
    public ScriptMimeMessage createMessage(String hint, Object source, Map<String, Object> parameters)
    {
        ScriptMimeMessage result;
        try {
            MimeMessageFactory<MimeMessage> factory = MimeMessageFactoryProvider.get(hint, MimeMessage.class,
                this.componentManagerProvider.get());

            MimeMessage message = factory.createMessage(source, parameters);
            result = new ScriptMimeMessage(message, this.execution, this.componentManagerProvider.get());
        } catch (Exception e) {
            // No factory found, set an error
            // An error occurred, save it and return null
            setError(e);
            return null;
        }

        return result;
    }

    /**
     * Construct an iterator of Mime Messages by running the Component implementation of {@link
     * org.xwiki.mail.MimeMessageFactory} corresponding to the passed hint.
     *
     * @param hint the component hint of a {@link org.xwiki.mail.MimeMessageFactory} component
     * @param source the source from which to prefill the Mime Messages (depends on the implementation)
     * @return the pre-filled Mime Message iterator
     */
    public Iterator<MimeMessage> createMessages(String hint, Object source)
    {
        return createMessages(hint, source, Collections.emptyMap());
    }

    /**
     * Construct an iterator of Mime Messages by running the Component implementation of {@link
     * org.xwiki.mail.MimeMessageFactory} corresponding to the passed hint.
     *
     * @param hint the component hint of a {@link org.xwiki.mail.MimeMessageFactory} component
     * @param source the source from which to prefill the Mime Messages (depends on the implementation)
     * @param parameters an optional generic list of parameters. The supported parameters depend on the implementation
     * @return the pre-filled Mime Message iterator
     */
    public Iterator<MimeMessage> createMessages(String hint, Object source, Map<String, Object> parameters)
    {
        Iterator<MimeMessage> result;
        try {
            MimeMessageFactory<Iterator<MimeMessage>> factory = MimeMessageFactoryProvider.get(hint,
                new DefaultParameterizedType(null, Iterator.class, MimeMessage.class),
                this.componentManagerProvider.get());
            result = factory.createMessage(source, parameters);
        } catch (Exception e) {
            // No factory found or error in constructing the message iterator, set an error
            // An error occurred, save it and return null
            setError(e);
            return null;
        }

        return result;
    }

    /**
     * Creates a pre-filled Mime Message by running the Component implementation of {@link
     * org.xwiki.mail.MimeMessageFactory} corresponding to the passed hint.
     *
     * @param hint the component hint of a {@link org.xwiki.mail.MimeMessageFactory} component
     * @param source the source from which to prefill the Mime Message (depends on the implementation)
     * @return the pre-filled Mime Message wrapped in a {@link ScriptMimeMessage} instance
     */
    public ScriptMimeMessage createMessage(String hint, Object source)
    {
        return createMessage(hint, source, Collections.<String, Object>emptyMap());
    }

    /**
     * Create an empty Mail message. The user of this API needs to set the recipients, the subject, and the message
     * content (aka Parts) before sending the mail. Note that if not specified, the "From" sender name will be taken
     * from the Mail Sender Configuration.
     *
     * @return the created Body Part or null if an error happened
     */
    public ScriptMimeMessage createMessage()
    {
        return createMessage(null, null, (String) null);
    }

    /**
     * Create a Mail message with the "To" recipient and the mail subject set. The user of this API needs to set the
     * message content (aka Parts) before sending the mail. Note that the "From" sender name is taken from the Mail
     * Sender Configuration.
     *
     * @param to the "To" email address
     * @param subject the subject of the mail to send
     * @return the created Body Part or null if an error happened
     */
    public ScriptMimeMessage createMessage(String to, String subject)
    {
        return createMessage(this.senderConfiguration.getFromAddress(), to, subject);
    }

    /**
     * Create a Mail message with the "To" recipient and the mail subject set. The user of this API needs to set the
     * message content (aka Parts) before sending the mail. Note that the "From" sender name from the Mail Sender
     * Configuration is overridden by the passed "From" parameter.
     *
     * @param from the email address of the sender
     * @param to the "To" email address
     * @param subject the subject of the mail to send
     * @return the created Body Part or null if an error happened
     */
    public ScriptMimeMessage createMessage(String from, String to, String subject)
    {
        ScriptMimeMessage scriptMessage = new ScriptMimeMessage(this.execution, this.componentManagerProvider.get());

        try {
            if (from != null) {
                scriptMessage.setFrom(InternetAddress.parse(from)[0]);
            }
            if (to != null) {
                scriptMessage.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            }
            scriptMessage.setSubject(subject);
        } catch (Exception e) {
            // An error occurred, save it and return null
            setError(e);
            return null;
        }

        return scriptMessage;
    }

    /**
     * Send one mail synchronously with Memory MailListener .
     *
     * @param message the message that was tried to be sent
     * @return the result and status of the send batch
     */
    public ScriptMailResult send(MimeMessage message)
    {
        return send(Arrays.asList(message));
    }

    /**
     * Send the list of mails synchronously, using a Memory {@link }MailListener} to store the results.
     *
     * @param messages the list of messages to send
     * @return the result and status of the send batch
     */
    public ScriptMailResult send(Iterable<? extends MimeMessage> messages)
    {
        return send(messages, "memory");
    }

    /**
     * Send the mail synchronously (wait till the message is sent). Any error can be retrieved by using the
     * returned {@link ScriptMailResult}.
     *
     * @param messages the list of messages to send
     * @param hint the component hint of a {@link org.xwiki.mail.MailListener} component
     * @return the result and status of the send batch
     */
    public ScriptMailResult send(Iterable<? extends MimeMessage> messages, String hint)
    {
        ScriptMailResult scriptMailResult = sendAsynchronously(messages, hint);

        if (scriptMailResult != null) {
            // Wait for all messages from this batch to have been sent before returning
            scriptMailResult.getStatusResult().waitTillProcessed(Long.MAX_VALUE);
        }

        return scriptMailResult;
    }

    /**
     * Send the mail asynchronously.
     *
     * @param messages the list of messages that was tried to be sent
     * @param hint the component hint of a {@link org.xwiki.mail.MailListener} component
     * @return the result and status of the send batch
     */
    public ScriptMailResult sendAsynchronously(Iterable<? extends MimeMessage> messages, String hint)
    {
        final MailListener listener;
        try {
            listener = getListener(hint);
        } catch (MessagingException e) {
            // Save the exception for reporting through the script services's getLastError() API
            setError(e);
            // Don't send the mail!
            return null;
        }
        return sendAsynchronously(messages, listener, true);
    }

    private MailListener getListener(String hint) throws MessagingException
    {
        MailListener listener;
        try {
            listener = this.componentManagerProvider.get().getInstance(MailListener.class, hint);
        } catch (ComponentLookupException e) {
            throw new MessagingException(String.format("Failed to locate [%s] Event listener. ", hint), e);
        }
        return listener;
    }

    /**
     * @return the configuration for sending mails (SMTP host, port, etc)
     */
    public MailSenderConfiguration getConfiguration()
    {
        return this.senderConfiguration;
    }

    @Override
    protected String getErrorKey()
    {
        return ERROR_KEY;
    }
}
