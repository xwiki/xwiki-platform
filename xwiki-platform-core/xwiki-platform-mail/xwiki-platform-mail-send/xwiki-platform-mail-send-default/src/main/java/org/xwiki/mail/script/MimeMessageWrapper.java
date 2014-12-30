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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.mail.internal.ExtendedMimeMessage;
import org.xwiki.stability.Unstable;

/**
 * Simulates a, {@link javax.mail.internet.MimeMessage} with additional helper methods to add body part content and to
 * send the message.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@Unstable
public class MimeMessageWrapper extends MimeMessage
{
    private ComponentManager componentManager;

    private Execution execution;

    private MailSenderConfiguration configuration;

    private Session session;

    private ExtendedMimeMessage message;
    /**
     * @param message the wrapped {@link javax.mail.internet.MimeMessage}
     * @param session the JavaMail session used to send the mail
     * @param execution used to get the Execution Context and store an error in it if the send fails
     * @param configuration the mail sender configuration component
     * @param componentManager used to dynamically load all {@link MimeBodyPartFactory} components
     */
    // Note: This method is package private voluntarily so that it's not part of the API (as this class is public),
    // since it's only needed by the MailSenderScriptService and nobody else should be able to construct an instance
    // of it!
    MimeMessageWrapper(ExtendedMimeMessage message, Session session, Execution execution,
        MailSenderConfiguration configuration, ComponentManager componentManager)
    {
        super(session);
        this.message = message;
        this.session = session;
        this.execution = execution;
        this.configuration = configuration;
        this.componentManager = componentManager;
    }

    /**
     * @return the wrapped {@link javax.mail.internet.MimeMessage}
     */
    public ExtendedMimeMessage getMessage()
    {
        return this.message;
    }

    /**
     * @return the JavaMail session used to send the mail
     */
    public Session getSession()
    {
        return this.session;
    }
    /**
     * Add some content to the mail to be sent. Can be called several times to add different content type to the mail.
     *
     * @param mimeType the mime type of the content parameter
     * @param content the content to include in the mail
     */
    public void addPart(String mimeType, Object content)
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
     *        the {@code headers} key with a {@code Map&lt;String, String&gt;} value containing header keys and values
     */
    public void addPart(String mimeType, Object content, Map<String, Object> parameters)
    {
        try {
            MimeBodyPartFactory factory = getBodyPartFactory(mimeType, content.getClass());

            // Pass the mime type in the parameters so that generic Mime Body Part factories can use it.
            // Note that if the use has already passed a "mimetype" parameter then we don't override it!
            Map<String, Object> enhancedParameters = new HashMap<>();
            enhancedParameters.put("mimetype", mimeType);
            enhancedParameters.putAll(parameters);

            Multipart multipart = getMultipart();
            multipart.addBodyPart(factory.create(content, enhancedParameters));
        } catch (Exception e) {
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
                contentClass), String.format("%s/secure", mimeType));
        } catch (ComponentLookupException e) {
            // Look for a specific MimeBodyPartFactory for the passed Mime Type and Content type (non secure).
            factory = this.componentManager.getInstance(
                new DefaultParameterizedType(null, MimeBodyPartFactory.class, contentClass), mimeType);
        }
        return factory;
    }

    private Multipart getMultipart() throws MessagingException, IOException
    {
        Multipart multipart;
        ExtendedMimeMessage mimeMessage = getMessage();
        if (mimeMessage.isEmpty()) {
            multipart = new MimeMultipart("mixed");
            mimeMessage.setContent(multipart);
        } else {
            Object contentObject = mimeMessage.getContent();
            if (contentObject instanceof Multipart) {
                multipart = (Multipart) contentObject;
            } else {
                throw new MessagingException(String.format("Unknown mail content type [%s]: [%s]",
                    contentObject.getClass().getName(), contentObject));
            }
        }
        return multipart;
    }

    protected void checkPermissions() throws MessagingException
    {
        // Load the configured permission checker
        ScriptServicePermissionChecker checker;
        String hint = this.configuration.getScriptServicePermissionCheckerHint();
        try {
            checker = this.componentManager.getInstance(ScriptServicePermissionChecker.class, hint);
        } catch (ComponentLookupException e) {
            // Failed to load the user-configured hint, in order not to have a security hole, consider that we're not
            // authorized to send emails!
            throw new MessagingException(String.format("Failed to locate Permission Checker [%s]. "
                + "The mail has not been sent.", hint), e);
        }

        try {
            checker.check(getMessage());
        } catch (MessagingException e) {
            throw new MessagingException(String.format("Not authorized to send mail with subject [%s], using "
                + "Permission Checker [%s]. The mail has not been sent.", getMessage().getSubject(), hint), e);
        }
    }
}
