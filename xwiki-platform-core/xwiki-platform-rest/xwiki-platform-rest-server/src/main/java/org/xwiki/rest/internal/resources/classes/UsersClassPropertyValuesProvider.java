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
package org.xwiki.rest.internal.resources.classes;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconManager;
import org.xwiki.icon.IconType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryBuilder;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.user.UserConfiguration;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.wiki.user.WikiUserManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.UsersClass;

/**
 * Provides values for the "List of Users" type of properties.
 *
 * @version $Id$
 * @since 9.8
 */
@Component
@Named("Users")
@Singleton
public class UsersClassPropertyValuesProvider extends AbstractUsersAndGroupsClassPropertyValuesProvider<UsersClass>
{
    private static final String DEFAULT_ICON_NAME = "user";

    @Inject
    private WikiUserManager wikiUserManager;

    @Inject
    private QueryBuilder<UsersClass> allowedValuesQueryBuilder;

    @Inject
    private UserConfiguration userConfiguration;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Inject
    private UserPropertiesResolver userPropertiesResolver;

    @Override
    protected Class<UsersClass> getPropertyType()
    {
        return UsersClass.class;
    }

    @Override
    protected QueryBuilder<UsersClass> getAllowedValuesQueryBuilder()
    {
        return this.allowedValuesQueryBuilder;
    }

    @Override
    protected PropertyValues getAllowedValues(UsersClass propertyDefinition, int limit, String filter) throws Exception
    {
        String wikiId = propertyDefinition.getOwnerDocument().getDocumentReference().getWikiReference().getName();
        switch (this.wikiUserManager.getUserScope(wikiId)) {
            case LOCAL_AND_GLOBAL:
                return getLocalAndGlobalAllowedValues(propertyDefinition, limit, filter);
            case GLOBAL_ONLY:
                return getGlobalAllowedValues(propertyDefinition, limit, filter);
            default:
                return getLocalAllowedValues(propertyDefinition, limit, filter);
        }
    }

    @Override
    protected Map<String, Object> getIcon(DocumentReference userReference)
    {
        Map<String, Object> icon = new HashMap<>();
        XWikiContext xcontext = this.xcontextProvider.get();
        try {
            XWikiDocument userProfileDocument = xcontext.getWiki().getDocument(userReference, xcontext);
            String avatar = userProfileDocument.getStringValue("avatar");
            XWikiAttachment avatarAttachment = userProfileDocument.getAttachment(avatar);
            if (avatarAttachment != null && avatarAttachment.isImage(xcontext)) {
                icon.put(IconManager.META_DATA_URL, xcontext.getWiki().getURL(avatarAttachment.getReference(),
                    "download", "width=30&height=30&keepAspectRatio=true", null, xcontext));
                icon.put(IconManager.META_DATA_ICON_SET_TYPE, IconType.IMAGE.name());
            }
        } catch (XWikiException e) {
            this.logger.warn(
                "Failed to read the avatar of user [{}]. Root cause is [{}]. Using the default avatar instead.",
                userReference.getName(), ExceptionUtils.getRootCauseMessage(e));
        }
        if (!icon.containsKey(IconManager.META_DATA_URL)) {
            try {
                icon = this.iconManager.getMetaData(DEFAULT_ICON_NAME);
            } catch (IconException e) {
                this.logger.warn("Error getting the icon [{}]. Root cause is [{}].", DEFAULT_ICON_NAME,
                    ExceptionUtils.getRootCause(e));
            }
        }

        return icon;
    }

    @Override
    protected String getLabel(DocumentReference userReference, Object currentLabel)
    {
        // The current label shouldn't be empty when the property values are retrieved from the data base. It's empty
        // when we resolve a raw property value (exact match).
        String label = currentLabel == null ? "" : currentLabel.toString().trim();
        if (label.isEmpty()) {
            XWikiContext xcontext = this.xcontextProvider.get();
            label = xcontext.getWiki().getPlainUserName(userReference, xcontext);
        }

        return label;
    }

    @Override
    protected String getHint(DocumentReference userReference)
    {
        String userQualifierProperty = this.userConfiguration.getUserQualifierProperty();
        if (StringUtils.isEmpty(userQualifierProperty)) {
            return super.getHint(userReference);
        } else {
            return getUserProperties(userReference).getProperty(userQualifierProperty);
        }
    }

    private UserProperties getUserProperties(DocumentReference userReference, Object... parameters)
    {
        return this.userPropertiesResolver.resolve(this.userReferenceResolver.resolve(userReference), parameters);
    }
}
