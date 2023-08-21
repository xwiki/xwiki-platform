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
package org.xwiki.notifications.preferences.internal.email;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.preferences.NotificationEmailInterval;
import org.xwiki.notifications.preferences.email.NotificationEmailDiffType;
import org.xwiki.notifications.preferences.email.NotificationEmailUserPreferenceManager;
import org.xwiki.notifications.preferences.internal.NotificationEmailGroupingStrategyPreferenceDocumentInitializer;
import org.xwiki.notifications.preferences.internal.NotificationEmailPreferenceDocumentInitializer;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Default implementation for {@link NotificationEmailUserPreferenceManager}.
 *
 * @version $Id$
 * @since 9.11RC1
 */
@Component
@Singleton
public class DefaultNotificationEmailUserPreferenceManager implements NotificationEmailUserPreferenceManager
{
    private static final String WIKI_SPACE = "XWiki";

    private static final String NOTIFICATIONS = "Notifications";

    private static final String CODE = "Code";

    private static final LocalDocumentReference GLOBAL_PREFERENCES = new LocalDocumentReference(
        Arrays.asList(WIKI_SPACE, NOTIFICATIONS, CODE), "NotificationAdministration"
    );

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserSerializer;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private NotificationConfiguration notificationConfiguration;

    @Inject
    private Logger logger;

    @Override
    public NotificationEmailDiffType getDiffType()
    {
        return getDiffType(CurrentUserReference.INSTANCE);
    }

    @Override
    public NotificationEmailDiffType getDiffType(String userId)
    {
        return getDiffType(userReferenceResolver.resolve(userId));
    }

    @Override
    public NotificationEmailDiffType getDiffType(UserReference userReference)
    {
        return getStaticListPropertyPreference(NotificationEmailPreferenceDocumentInitializer.FIELD_DIFF_TYPE,
                NotificationEmailDiffType.class, NotificationEmailDiffType.STANDARD, userReference);
    }

    @Override
    public NotificationEmailInterval getInterval()
    {
        return getInterval(CurrentUserReference.INSTANCE);
    }

    @Override
    public NotificationEmailInterval getInterval(UserReference userReference)
    {
        return getStaticListPropertyPreference(NotificationEmailPreferenceDocumentInitializer.FIELD_INTERVAL,
                NotificationEmailInterval.class, NotificationEmailInterval.DAILY, userReference);
    }

    @Override
    public String getEmailGroupingStrategy(UserReference userReference, NotificationEmailInterval interval)
    {
        DocumentReference userDocumentReference = documentUserSerializer.serialize(userReference);
        String result = getEmailGroupingStrategyFromDocument(userDocumentReference, interval);
        if (StringUtils.isEmpty(result)) {
            DocumentReference xwikiPref =
                    new DocumentReference(GLOBAL_PREFERENCES, userDocumentReference.getWikiReference());
            result = getEmailGroupingStrategyFromDocument(xwikiPref, interval);
        }

        WikiReference mainWikiReference = new WikiReference(this.wikiDescriptorManager.getMainWikiId());
        if (StringUtils.isEmpty(result) && !userDocumentReference.getWikiReference().equals(mainWikiReference)) {
            DocumentReference xwikiPref =
                    new DocumentReference(GLOBAL_PREFERENCES, mainWikiReference);
            result = getEmailGroupingStrategyFromDocument(xwikiPref, interval);
        }

        if (StringUtils.isEmpty(result)) {
            result = this.notificationConfiguration.getEmailGroupingStrategyHint();
        }
        return result;
    }

    private String getEmailGroupingStrategyFromDocument(DocumentReference documentReference,
                                                        NotificationEmailInterval interval)
    {
        DocumentReference xClassReference =
                new DocumentReference(NotificationEmailGroupingStrategyPreferenceDocumentInitializer.REFERENCE,
                        documentReference.getWikiReference());
        try {
            XWikiDocument userDocument =
                    (XWikiDocument) documentAccessBridge.getDocumentInstance(documentReference);
            List<BaseObject> objects = userDocument.getXObjects(xClassReference);
            for (BaseObject object : objects) {
                if (object != null && StringUtils.equalsIgnoreCase(interval.name(), object.getStringValue(
                        NotificationEmailGroupingStrategyPreferenceDocumentInitializer.FIELD_EMAIL_INTERVAL))) {
                    return object.getStringValue(
                            NotificationEmailGroupingStrategyPreferenceDocumentInitializer.FIELD_STRATEGY);
                }
            }

        } catch (Exception e) {
            logger.error("Error while accessing document [{}] for reading email grouping strategy for interval [{}].",
                    documentReference, interval, e);
        }
        return null;
    }

    private <T extends Enum<T>> T getStaticListPropertyPreference(String propertyName,
        Class<T> propertyEnum, T propertyDefaultValue, UserReference user)
    {
        T returnValue = propertyDefaultValue;

        try {
            DocumentReference userDocumentReference = documentUserSerializer.serialize(user);

            // Get the config of the user
            DocumentReference emailClassReference = new DocumentReference(
                    NotificationEmailPreferenceDocumentInitializer.REFERENCE,
                userDocumentReference.getWikiReference());
            Object value = documentAccessBridge.getProperty(userDocumentReference, emailClassReference, propertyName);
            if (StringUtils.isNotBlank((String) value)) {
                returnValue = Enum.valueOf(propertyEnum, ((String) value).toUpperCase());
            } else {
                // Get the config from the user wiki or from the main wiki
                T propertyValue = getWikiPreference(userDocumentReference, propertyEnum, propertyName);
                if (propertyValue != null) {
                    returnValue = propertyValue;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get the email property [{}] for the user [{}].", propertyName, user, e);
        }

        return returnValue;
    }

    /**
     * Gets preference property value from same wiki as @userDocumentReference if it exists, otherwise from the main
     * wiki.
     *
     * @param userDocumentReference a user document reference
     * @param propertyName a property name
     * @return
     */
    private <T extends Enum<T>> T getWikiPreference(DocumentReference userDocumentReference, Class<T> propertyEnum,
        String propertyName)
    {
        T returnValue = null;
        // Get the config of the wiki
        DocumentReference emailClassReference = new DocumentReference(
                NotificationEmailPreferenceDocumentInitializer.REFERENCE,
            userDocumentReference.getWikiReference());
        DocumentReference xwikiPref =
            new DocumentReference(GLOBAL_PREFERENCES, userDocumentReference.getWikiReference());
        Object value = documentAccessBridge.getProperty(xwikiPref, emailClassReference, propertyName);
        if (StringUtils.isNotBlank((String) value)) {
            returnValue = Enum.valueOf(propertyEnum, ((String) value).toUpperCase());
        } else {
            // Get the config of the main wiki
            WikiReference mainWiki = new WikiReference(wikiDescriptorManager.getMainWikiId());
            if (!userDocumentReference.getWikiReference().equals(mainWiki)) {
                xwikiPref = new DocumentReference(GLOBAL_PREFERENCES, mainWiki);
                emailClassReference = new DocumentReference(NotificationEmailPreferenceDocumentInitializer.REFERENCE,
                        mainWiki);
                value = documentAccessBridge.getProperty(xwikiPref, emailClassReference, propertyName);
                if (StringUtils.isNotBlank((String) value)) {
                    returnValue = Enum.valueOf(propertyEnum, ((String) value).toUpperCase());
                }
            }
        }
        return returnValue;
    }
}
