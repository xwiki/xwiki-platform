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
package org.xwiki.user.rest.internal;

import java.util.Objects;

import javax.inject.Provider;

import jakarta.inject.Inject;

import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.rest.model.jaxb.ObjectFactory;
import org.xwiki.user.rest.model.jaxb.UserPreferences;

import com.xpn.xwiki.XWikiContext;

/**
 * Abstract implementation of {@link UserReferenceModelSerializer}, providing some common helpers.
 *
 * @since 18.2.0RC1
 * @version $Id$
 */
public abstract class AbstractUserReferenceModelSerializer implements UserReferenceModelSerializer
{
    protected final ObjectFactory userObjectFactory = new ObjectFactory();
    protected final org.xwiki.rest.model.jaxb.ObjectFactory xwikiObjectFactory =
        new org.xwiki.rest.model.jaxb.ObjectFactory();

    @Inject
    protected UserPropertiesResolver userPropertiesResolver;

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    protected UserPreferences toRestUserPreferences(UserProperties userProperties, XWikiContext xcontext)
    {
        UserPreferences userPreferences = this.userObjectFactory.createUserPreferences();
        userPreferences.setDisplayHiddenDocuments(userProperties.displayHiddenDocuments());

        String underlineProperty = "underline";
        userPreferences.setUnderlineLinks(Objects.toString(userProperties.getProperty(underlineProperty),
            xcontext.getWiki().getXWikiPreference(underlineProperty, xcontext)));

        String timezoneProperty = "timezone";
        userPreferences.setTimezone(Objects.toString(userProperties.getProperty(timezoneProperty),
            xcontext.getWiki().getXWikiPreference(timezoneProperty, xcontext)));

        String editorProperty = "editor";
        userPreferences.setEditor(Objects.toString(userProperties.getProperty(editorProperty),
            xcontext.getWiki().getXWikiPreference(editorProperty, xcontext)));

        userPreferences.setAdvanced("Advanced".equals(userProperties.getProperty("usertype")));

        return userPreferences;
    }
}
