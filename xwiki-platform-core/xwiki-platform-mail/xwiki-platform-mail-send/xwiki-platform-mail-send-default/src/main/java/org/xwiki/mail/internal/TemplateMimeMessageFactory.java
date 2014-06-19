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
package org.xwiki.mail.internal;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.velocity.VelocityContext;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;

import static java.util.Arrays.asList;

/**
 * Creates mime message with the subject pre-filled with evaluated subject xproperty from an XWiki.Mail xobject in the
 * Document pointed to by the passed documentReference.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@Component
@Named("template")
@Singleton
public class TemplateMimeMessageFactory implements MimeMessageFactory
{
    private static final EntityReference MAIL_CLASS =
            new EntityReference("Mail", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    @Inject
    private DocumentAccessBridge documentBridge;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    @Named("current")
    private CurrentReferenceDocumentReferenceResolver resolver;

    @Inject
    private VelocityManager velocityManager;

    @Override
    public MimeMessage createMessage(Session session, Object... parameters) throws MessagingException
    {
        MimeMessage message = new MimeMessage(session);

        List<Object> parametersReversed = asList(parameters);
        Collections.reverse(parametersReversed);

        Map<String, String> data = (Map<String, String>) parametersReversed.get(0);
        DocumentReference documentReference = (DocumentReference) parametersReversed.get(1);

        message.setSubject(evaluateSubject(documentReference, data));

        if (parametersReversed.get(2) != 0) {
            String to = (String) parametersReversed.get(2);
            message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));
        }
        if (parametersReversed.get(3) != 0) {
            String from = (String) parametersReversed.get(3);
            message.setFrom(new InternetAddress(from));
        }

        return message;
    }

    private String evaluateSubject(DocumentReference documentReference, Map<String, String> data)
        throws MessagingException
    {
        VelocityContext velocityContext = createVelocityContext(data);
        DocumentReference mailClassReference = this.resolver.resolve(MAIL_CLASS);

        String templateFullName = this.serializer.serialize(documentReference);
        String subjectProperty = "subject";
        String content =
                this.documentBridge.getProperty(documentReference, mailClassReference, subjectProperty).toString();
        try {
            StringWriter writer = new StringWriter();
            velocityManager.getVelocityEngine().evaluate(velocityContext, writer, templateFullName, content);
            return writer.toString();
        } catch (XWikiVelocityException e) {
            throw new MessagingException(String.format("Failed to evaluate subject [%s] for Document reference [%s]",
                    subjectProperty, documentReference), e);
        }
    }

    private VelocityContext createVelocityContext(Map<String, String> data)
    {
        VelocityContext velocityContext = new VelocityContext();
        if (data != null) {
            for (Map.Entry<String, String> header : data.entrySet()) {
                velocityContext.put(header.getKey(), header.getValue());
            }
        }

        return velocityContext;
    }
}
