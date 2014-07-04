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
package org.xwiki.mail.internal.template;

import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;

import org.apache.velocity.VelocityContext;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Default implementation evaluating template properties by taking them from {@code XWiki.Mail} and applying Velocity on
 * them.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@Component
@Singleton
public class DefaultMailTemplateManager implements MailTemplateManager
{
    private static final EntityReference MAIL_CLASS =
        new EntityReference("Mail", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    private static final String LANGUAGE_PROPERTY_NAME = "language";

    @Inject
    private DocumentAccessBridge documentBridge;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> resolver;

    @Inject
    private VelocityManager velocityManager;

    @Inject
    private Execution execution;

    @Override
    public String evaluate(DocumentReference documentReference, String property, Map<String, String> data,
        Locale language)
        throws MessagingException
    {
        Locale languageParameter = language;
        if (languageParameter == null || languageParameter == Locale.ROOT) {
            languageParameter = getDefaultLocale();
        }

        DocumentReference mailClassReference = this.resolver.resolve(MAIL_CLASS);

        VelocityContext velocityContext = createVelocityContext(data);

        String templateFullName = this.serializer.serialize(documentReference);

        int objectNumber = getObjectMailNumber(documentReference, mailClassReference, languageParameter);

        String content =
            this.documentBridge.getProperty(documentReference, mailClassReference, objectNumber, property)
                .toString();
        try {
            StringWriter writer = new StringWriter();
            velocityManager.getVelocityEngine().evaluate(velocityContext, writer, templateFullName, content);
            return writer.toString();
        } catch (XWikiVelocityException e) {
            throw new MessagingException(String.format("Failed to evaluate property [%s] for Document reference [%s]",
                property, documentReference));
        }
    }

    @Override
    public String evaluate(DocumentReference documentReference, String property, Map<String, String> data)
        throws MessagingException
    {
        return evaluate(documentReference, property, data, null);
    }

    /**
     * @return the number of the XWiki.Mail xobject with language xproperty is equal to the language parameter if not
     * exist return the XWiki.Mail xobject with language xproperty as default language if not exist return the first
     * XWiki.Mail xobject if there is only one XWiki.Mail xobject
     */
    private int getObjectMailNumber(DocumentReference documentReference, DocumentReference mailClassReference,
        Locale language) throws MessagingException
    {
        int number = this.documentBridge.getObjectNumber(documentReference, mailClassReference, LANGUAGE_PROPERTY_NAME,
            language.getLanguage());

        int mailObjectsCount = getMailObjectsCount(documentReference, mailClassReference);

        // Check that the language passed is not the default language
        if (!getDefaultLocale().equals(language) && number == -1) {
            number =
                this.documentBridge
                    .getObjectNumber(documentReference, mailClassReference, LANGUAGE_PROPERTY_NAME,
                        getDefaultLocale().getLanguage());
        } else if (mailObjectsCount == 1 && number == -1) {
            number = 0;
        } else if (mailObjectsCount == 0 && number == -1) {
            throw new MessagingException(
                String.format("No XWiki.Mail xobject found in the document reference [%s]", documentReference));
        } else if (number == -1) {
            throw new MessagingException(
                String.format(
                    "No XWiki.Mail xobject matches the language passed [%s] or the default language [%s] " +
                        "in the document reference [%s]",
                    language.getLanguage(), getDefaultLocale().getLanguage(), documentReference));
        }
        return number;
    }

    private int getMailObjectsCount(DocumentReference documentReference, DocumentReference mailClassReference)
        throws MessagingException
    {
        XWikiContext context = getXWikiContext();
        int objectsCount;
        try {
            objectsCount =
                context.getWiki().getDocument(documentReference, context).getXObjects(mailClassReference)
                    .size();
        } catch (XWikiException e) {
            throw new MessagingException(
                String
                    .format("Failed to retrieve XWiki.Mail xobjects from Document reference [%s]", documentReference));
        }
        return objectsCount;
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

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }

    private Locale getDefaultLocale()
    {
        XWikiContext context = getXWikiContext();
        return context.getWiki().getDefaultLocale(context);
    }
}
