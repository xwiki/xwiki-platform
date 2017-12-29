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
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MimeBodyPartFactory;

/**
 * Extends {@link javax.mail.internet.MimeMessage} with additional helper methods for scripts.
 *
 * @version $Id$
 * @since 7.1M2
 */
public class ScriptMimeMessage extends ExtendedMimeMessage
{
    private ComponentManager componentManager;

    private Execution execution;

    /**
     * @param execution used to get the Execution Context and store an error in it if the send fails
     * @param componentManager used to dynamically load all {@link MimeBodyPartFactory} components
     */
    // Note: This method is package private voluntarily so that it's not part of the API (as this class is public),
    // since it's only needed by the MailSenderScriptService and nobody else should be able to construct an instance
    // of it!
    ScriptMimeMessage(MimeMessage sourceMessage, Execution execution, ComponentManager componentManager)
        throws MessagingException
    {
        super(sourceMessage);
        this.execution = execution;
        this.componentManager = componentManager;
    }

    /**
     * @param execution used to get the Execution Context and store an error in it if the send fails
     * @param componentManager used to dynamically load all {@link MimeBodyPartFactory} components
     */
    // Note: This method is package private voluntarily so that it's not part of the API (as this class is public),
    // since it's only needed by the MailSenderScriptService and nobody else should be able to construct an instance
    // of it!
    ScriptMimeMessage(Execution execution, ComponentManager componentManager)
    {
        super();
        this.execution = execution;
        this.componentManager = componentManager;
    }

    /**
     * Add some content to the mail to be sent. Can be called several times to add different content type to the mail.
     *
     * @return the Mime Body Part object that was added. Returning it allows script to make modifications to that body
     *         part after it's been set (get/set some headers, etc)
     * @param mimeType the mime type of the content parameter
     * @param content the content to include in the mail
     */
    public BodyPart addPart(String mimeType, Object content)
    {
        return addPart(mimeType, content, Collections.<String, Object>emptyMap());
    }

    /**
     * Add some content to the mail to be sent. Can be called several times to add different content type to the mail.
     *
     * @return the Mime Body Part object that was added. Returning it allows script to make modifications to that body
     *         part after it's been set (get/set some headers, etc)
     * @param mimeType the mime type of the content parameter
     * @param content the content to include in the mail
     * @param parameters the list of extra parameters. This is used for example to pass alternate content for the mail
     *        using the {@code alternate} key in the HTML Mime Body Part Factory. Mail headers can also be passed using
     *        the {@code headers} key with a {@code Map&lt;String, String&gt;} value containing header keys and values
     */
    public BodyPart addPart(String mimeType, Object content, Map<String, Object> parameters)
    {
        BodyPart bodyPart;

        try {
            MimeBodyPartFactory factory = getBodyPartFactory(mimeType, content.getClass());

            // Pass the mime type in the parameters so that generic Mime Body Part factories can use it.
            // Note that if the user has already passed a "mimetype" parameter then we don't override it!
            Map<String, Object> enhancedParameters = new HashMap<>();
            enhancedParameters.put("mimetype", mimeType);
            enhancedParameters.putAll(parameters);

            Multipart multipart = getMultipart();
            bodyPart = factory.create(content, enhancedParameters);
            multipart.addBodyPart(bodyPart);
        } catch (Exception e) {
            // Save the exception for reporting through the script services's getError() API
            setError(e);
            bodyPart = null;
        }

        return bodyPart;
    }

    /**
     * @param subject the subject to set in the Mime Message
     */
    @Override
    public void setSubject(String subject)
    {
        try {
            super.setSubject(subject);
        } catch (Exception e) {
            setError(e);
        }
    }

    /**
     * @param address the address from which this message will be sent
     */
    @Override
    public void setFrom(Address address)
    {
        try {
            super.setFrom(address);
        } catch (Exception e) {
            setError(e);
        }
    }

    /**
     * @param type the type of recipients (to, cc, bcc, newsgroups)
     * @param addresses the email addresses of the recipients
     */
    @Override
    public void addRecipients(Message.RecipientType type, Address[] addresses)
    {
        try {
            super.addRecipients(type, addresses);
        } catch (Exception e) {
            setError(e);
        }
    }

    /**
     * @param type the type of recipient (to, cc, bcc, newsgroups)
     * @param address the email address of the recipient
     */
    @Override
    public void addRecipient(Message.RecipientType type, Address address)
    {
        try {
            super.addRecipient(type, address);
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
    @Override
    public void addHeader(String name, String value)
    {
        try {
            super.addHeader(name, value);
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
        if (isEmpty()) {
            multipart = new MimeMultipart("mixed");
            setContent(multipart);
        } else {
            Object contentObject = getContent();
            if (contentObject instanceof Multipart) {
                multipart = (Multipart) contentObject;
            } else {
                throw new MessagingException(String.format("Unknown mail content type [%s]: [%s]",
                    contentObject.getClass().getName(), contentObject));
            }
        }
        return multipart;
    }
}
