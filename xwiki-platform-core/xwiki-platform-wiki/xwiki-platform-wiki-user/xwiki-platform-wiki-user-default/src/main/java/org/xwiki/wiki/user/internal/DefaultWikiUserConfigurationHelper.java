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
package org.xwiki.wiki.user.internal;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserConfiguration;
import org.xwiki.wiki.user.WikiUserManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of {@link WikiUserConfigurationHelper}.
 *
 * @version $Id$
 * @since 5.3M2
 */
@Component
@Singleton
public class DefaultWikiUserConfigurationHelper implements WikiUserConfigurationHelper
{
    private static final String CONFIGURATION_PAGE_NAME = "WikiUserConfiguration";

    private static final String CONFIGURATION_SPACE_NAME = WikiUserClassDocumentInitializer.DOCUMENT_SPACE;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    private XWikiDocument getDocument(String wikiId) throws WikiUserManagerException
    {
        try {
            XWikiContext context = xcontextProvider.get();
            XWiki xwiki = context.getWiki();

            DocumentReference reference = new DocumentReference(wikiId, CONFIGURATION_SPACE_NAME,
                    CONFIGURATION_PAGE_NAME);

            return xwiki.getDocument(reference, context);
        } catch (XWikiException e) {
            throw new WikiUserManagerException(String.format("Failed to get the configuration document for wiki [%s]",
                wikiId), e);
        }
    }

    @Override
    public WikiUserConfiguration getConfiguration(String wikiId) throws WikiUserManagerException
    {
        // Create the configuration object to return
        WikiUserConfiguration configuration = new WikiUserConfiguration();

        // Get the document
        XWikiDocument document = getDocument(wikiId);

        // Get the XWiki object
        BaseObject object = document.getXObject(WikiUserClassDocumentInitializer.CONFIGURATION_CLASS);
        if (object != null) {
            // Get the user scope
            String scopeValue = object.getStringValue(WikiUserClassDocumentInitializer.FIELD_USERSCOPE);
            UserScope userScope;
            try {
                userScope = UserScope.valueOf(scopeValue.toUpperCase());
            } catch (Exception e) {
                // Default value
                userScope = UserScope.LOCAL_AND_GLOBAL;
            }
            configuration.setUserScope(userScope);

            // Get the membershipType value
            String membershipTypeValue = object.getStringValue(
                    WikiUserClassDocumentInitializer.FIELD_MEMBERSHIPTYPE);
            MembershipType membershipType;
            try {
                membershipType = MembershipType.valueOf(membershipTypeValue.toUpperCase());
            } catch (Exception e) {
                // Default value
                membershipType = MembershipType.INVITE;
            }
            configuration.setMembershipType(membershipType);
        }

        return configuration;
    }

    @Override
    public void saveConfiguration(WikiUserConfiguration configuration, String wikiId)
        throws WikiUserManagerException
    {
        XWikiContext context = xcontextProvider.get();

        // Get the document
        XWikiDocument document = getDocument(wikiId);

        // Fill the object
        BaseObject object = document.getXObject(WikiUserClassDocumentInitializer.CONFIGURATION_CLASS, true, context);
        object.setStringValue(WikiUserClassDocumentInitializer.FIELD_USERSCOPE,
                configuration.getUserScope().name().toLowerCase());
        if (configuration.getMembershipType() != null) {
            object.setStringValue(WikiUserClassDocumentInitializer.FIELD_MEMBERSHIPTYPE,
                    configuration.getMembershipType().name().toLowerCase());
        }

        // Save the document
        try {
            XWiki xwiki = context.getWiki();
            document.setHidden(true);
            // The document must have a creator
            if (document.getCreatorReference() == null) {
                document.setCreatorReference(context.getUserReference());
            }
            // The document must have an author
            if (document.getAuthorReference() == null) {
                document.setAuthorReference(context.getUserReference());
            }
            xwiki.saveDocument(document, "Changed configuration.", context);
        } catch (XWikiException e) {
            throw new WikiUserManagerException(
                    String.format("Fail to save the confguration document for wiki [%s].", wikiId), e);
        }
    }
}
