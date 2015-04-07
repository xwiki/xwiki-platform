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
package org.xwiki.mail.internal.factory.template;

import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.MessagingException;

import org.apache.velocity.VelocityContext;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
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
    private Provider<XWikiContext> xwikiContextProvider;

    @Override
    public String evaluate(DocumentReference templateReference, String property, Map<String, String> data,
        Locale locale) throws MessagingException
    {
        Locale computedLocale = locale;
        if (computedLocale == null || computedLocale == Locale.ROOT) {
            computedLocale = getDefaultLocale();
        }

        DocumentReference mailClassReference = this.resolver.resolve(MAIL_CLASS);

        VelocityContext velocityContext = createVelocityContext(data);

        String templateFullName = this.serializer.serialize(templateReference);

        int objectNumber = getObjectMailNumber(templateReference, mailClassReference, computedLocale);

        String content =
            this.documentBridge.getProperty(templateReference, mailClassReference, objectNumber, property).toString();
        try {
            StringWriter writer = new StringWriter();
            velocityManager.getVelocityEngine().evaluate(velocityContext, writer, templateFullName, content);
            return writer.toString();
        } catch (XWikiVelocityException e) {
            throw new MessagingException(String.format(
                "Failed to evaluate property [%s] for Document [%s] and locale [%s]",
                    property, templateReference, locale), e);
        }
    }

    @Override
    public String evaluate(DocumentReference templateReference, String property, Map<String, String> data)
        throws MessagingException
    {
        return evaluate(templateReference, property, data, null);
    }

    /**
     * @return the number of the XWiki.Mail xobject with language xproperty is equal to the language parameter if not
     * exist return the XWiki.Mail xobject with language xproperty as default language if not exist return the first
     * XWiki.Mail xobject if there is only one XWiki.Mail xobject
     */
    private int getObjectMailNumber(DocumentReference templateReference, DocumentReference mailClassReference,
        Locale language) throws MessagingException
    {
        int number = this.documentBridge.getObjectNumber(templateReference, mailClassReference, LANGUAGE_PROPERTY_NAME,
            language.getLanguage());

        int mailObjectsCount = getMailObjectsCount(templateReference, mailClassReference);

        // Check that the language passed is not the default language
        if (!getDefaultLocale().equals(language) && number == -1) {
            number = this.documentBridge.getObjectNumber(templateReference, mailClassReference, LANGUAGE_PROPERTY_NAME,
                getDefaultLocale().getLanguage());
        }

        if (mailObjectsCount == 1 && number == -1) {
            number = 0;
        } else if (mailObjectsCount == 0 && number == -1) {
            throw new MessagingException(String.format(
                "No [%s] object found in the Document [%s] for language [%s]", MAIL_CLASS.toString(),
                templateReference, language));
        } else if (number == -1) {
            throw new MessagingException(String.format(
                "No [%s] object matches the locale [%s] or the default locale [%s] in the Document [%s]",
                    MAIL_CLASS.toString(), language, getDefaultLocale(), templateReference));
        }
        return number;
    }

    private int getMailObjectsCount(DocumentReference templateReference, DocumentReference mailClassReference)
        throws MessagingException
    {
        XWikiContext context = this.xwikiContextProvider.get();
        int objectsCount;
        try {
            objectsCount = context.getWiki().getDocument(templateReference, context).getXObjects(mailClassReference)
                .size();
        } catch (XWikiException e) {
            throw new MessagingException(String.format(
                "Failed to find number of [%s] objects in Document [%s]", mailClassReference, templateReference), e);
        }
        return objectsCount;
    }

    private VelocityContext createVelocityContext(Map<String, String> data)
    {
        // Note: We create an inner Velocity Context to make it read only and try to prevent the script to modify
        // its bindings. We also clone its values to try try to protect them. It's not guaranteed though since the
        // clone() method of VelocityContext only performs shallow cloning.
        // However, this whole code is executed in a thread and we recreate the context for each email so it should be
        // pretty safe!
        VelocityContext existingVelocityContext = this.velocityManager.getVelocityContext();
        VelocityContext velocityContext;
        if (existingVelocityContext != null) {
            velocityContext = new VelocityContext((VelocityContext) existingVelocityContext.clone());
        } else {
            velocityContext = new VelocityContext();
        }
        if (data != null) {
            for (Map.Entry<String, String> header : data.entrySet()) {
                velocityContext.put(header.getKey(), header.getValue());
            }
        }
        return velocityContext;
    }

    private Locale getDefaultLocale()
    {
        XWikiContext context = this.xwikiContextProvider.get();
        return context.getWiki().getDefaultLocale(context);
    }
}
