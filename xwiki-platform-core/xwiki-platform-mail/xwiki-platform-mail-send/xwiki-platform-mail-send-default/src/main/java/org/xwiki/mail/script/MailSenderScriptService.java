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

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.mail.XWikiAuthenticator;
import org.xwiki.mail.internal.ExtendedMimeMessage;
import org.xwiki.mail.internal.MemoryMailListener;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Expose Mail Sending API to scripts.
 * <p/>
 * Example for sending an HTML message with attachments and a text
 * alternative:
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
    private Provider<ComponentManager> componentManagerProvider;

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    /**
     * Creates a pre-filled Mime Message by running the Component implementation of {@link
     * org.xwiki.mail.MimeMessageFactory} corresponding to the passed hint.
     *
     * @param hint the component hint of a {@link org.xwiki.mail.MimeMessageFactory} component
     * @param source the source from which to prefill the Mime Message (depends on the implementation)
     * @param parameters an optional generic list of parameters. The supported parameters depend on the implementation
     * @return the pre-filled Mime Message wrapped in a {@link org.xwiki.mail.script.MimeMessageWrapper} instance
     */
    public MimeMessageWrapper createMessage(String hint, Object source, Map<String, Object> parameters)
    {
        MimeMessageWrapper messageWrapper;
        try {
            MimeMessageFactory factory = getMimeMessageFactory(hint, source.getClass());
            Session session = createSession();

            // If the factory hasn't created an ExtendedMimeMessage we wrap it in one so that we can add body parts
            // easily as they are added by the users and construct a MultiPart out of it when we send the mail.
            ExtendedMimeMessage extendedMimeMessage;
            MimeMessage message = factory.createMessage(session, source, parameters);
            if (message instanceof ExtendedMimeMessage) {
                extendedMimeMessage = (ExtendedMimeMessage) message;
            } else {
                extendedMimeMessage = new ExtendedMimeMessage(message);
            }

            messageWrapper = new MimeMessageWrapper(extendedMimeMessage, session, this.mailSender, this.execution,
                this.configuration, componentManagerProvider.get());
        } catch (Exception e) {
            // No factory found, set an error
            // An error occurred, save it and return null
            setError(e);
            return null;
        }

        return messageWrapper;
    }

    /**
     * @param hint the hint of Iterator factories
     * @param source the source from which to prefill the Mime Message iterator (depends on the implementation)
     * @param factoryHint the component hint of a {@link org.xwiki.mail.MimeMessageFactory} component
     * @param parameters an optional generic list of parameters. The supported parameters depend on the implementation
     * @return the ist of the pre-filled Mime Message wrapped
     */
    public Iterator<? extends MimeMessage> createMessages(String hint, Object source, String factoryHint,
        Map<String, Object> parameters)
    {
        MimeMessageFactory factory;
        try {
            factory = getMimeMessageFactory(factoryHint, parameters.get("source").getClass());
        } catch (ComponentLookupException e) {
            setError(e);
            return null;
        }
        try {
            MimeMessageIteratorFactory iteratorFactory = new MimeMessageIteratorFactory();
            return
                iteratorFactory.createIterator(hint, source, factory, parameters, this.componentManagerProvider.get());
        } catch (Exception e) {
            setError(e);
            return null;
        }
    }

    private MimeMessageFactory getMimeMessageFactory(String hint, Type type) throws ComponentLookupException
    {
        MimeMessageFactory factory;

        // Step 1: Look for a secure version first
        ComponentManager componentManager = this.componentManagerProvider.get();
        try {
            factory = componentManager.getInstance(
                new DefaultParameterizedType(null, MimeMessageFactory.class, type),
                String.format("%s/secure", hint));
        } catch (ComponentLookupException e) {
            // Step 2: Look for a non secure version if a secure one doesn't exist...
            factory = componentManager.getInstance(
                new DefaultParameterizedType(null, MimeMessageFactory.class, type, null), hint);
        }

        return factory;
    }

    /**
     * Creates a pre-filled Mime Message by running the Component implementation of {@link
     * org.xwiki.mail.MimeMessageFactory} corresponding to the passed hint.
     *
     * @param hint the component hint of a {@link org.xwiki.mail.MimeMessageFactory} component
     * @param source the source from which to prefill the Mime Message (depends on the implementation)
     * @return the pre-filled Mime Message wrapped in a {@link org.xwiki.mail.script.MimeMessageWrapper} instance
     */
    public MimeMessageWrapper createMessage(String hint, Object source)
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
    public MimeMessageWrapper createMessage()
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
    public MimeMessageWrapper createMessage(String to, String subject)
    {
        return createMessage(this.configuration.getFromAddress(), to, subject);
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
    public MimeMessageWrapper createMessage(String from, String to, String subject)
    {
        Session session = createSession();
        ExtendedMimeMessage message = new ExtendedMimeMessage(session);
        MimeMessageWrapper messageWrapper = new MimeMessageWrapper(message, session, this.mailSender, this.execution,
            this.configuration, this.componentManagerProvider.get());

        try {
            if (from != null) {
                messageWrapper.setFrom(InternetAddress.parse(from)[0]);
            }
            if (to != null) {
                messageWrapper.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            }
            message.setSubject(subject);
        } catch (Exception e) {
            // An error occurred, save it and return null
            setError(e);
            return null;
        }

        return messageWrapper;
    }

    /**
     * Send one mail synchronously with Memory MailListener .
     *
     * @param message the message that was tried to be sent
     * @return UUID of the Batch mail
     */
    public UUID send(MimeMessage message)
    {
        return send(Arrays.asList(message));
    }

    /**
     * Send the list of mails synchronously with Memory MailListener.
     *
     * @param messages the list of messages that was tried to be sent
     * @return UUID of the Batch mail
     */
    public UUID send(Iterable<? extends MimeMessage> messages)
    {
        return send(messages, "memory");
    }

    /**
     * Send the mail synchronously (wait till the message is sent). Any error can be retrieved by calling {@link
     * #getErrors(UUID)}.
     *
     * @param messages the list of messages that was tried to be sent
     * @param hint the component hint of a {@link org.xwiki.mail.MailListener} component
     * @return UUID of the Batch mail
     */
    public UUID send(Iterable<? extends MimeMessage> messages, String hint)
    {
        MailListener listener;
        try {
            checkPermissions();
            listener = getListener(hint);
            return this.mailSender.send(messages, createSession());
        } catch (MessagingException e) {
            // Save the exception for reporting through the script services's getLastError() API
            setError(e);
        }
        return null;
    }

    /**
     * Send the mail asynchronously.
     *
     * @param messages the list of messages that was tried to be sent
     * @param hint the component hint of a {@link org.xwiki.mail.MailListener} component
     * @return UUID of the Batch mail
     */
    public UUID sendAsynchronously(List<? extends MimeMessage> messages, String hint)
    {
        final MailListener listener;
        try {
            listener = getListener(hint);
            checkPermissions();
        } catch (MessagingException e) {
            // Save the exception for reporting through the script services's getLastError() API
            setError(e);
            // Don't send the mail!
            return null;
        }

        // NOTE: we don't throw any error since the message is sent asynchronously. All errors can be found using
        // the passed listener.
        UUID sender = this.mailSender.sendAsynchronously(messages, createSession(), listener);
        return sender;
    }

    /**
     * Check authorization to send mail.
     *
     * @throws MessagingException when not authorized to send mail
     */
    private void checkPermissions() throws MessagingException
    {
        /*
         * TODO: we need check permissions once, so we can't use ScriptServicePermissionChecker#check(MimeMessage)
         */
    }

    /**
     * @param batchID the UUID of the Batch mail
     * @return errors raised during the send of all emails
     */
    public Iterator<MailStatus> getErrors(UUID batchID)
    {
        return MemoryMailListener.getErrors(batchID);
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
        return this.configuration;
    }

    private Session createSession()
    {
        Session session;
        if (this.configuration.usesAuthentication()) {
            session = Session.getInstance(this.configuration.getAllProperties(),
                new XWikiAuthenticator(this.configuration));
        } else {
            session = Session.getInstance(this.configuration.getAllProperties());
        }
        return session;
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
