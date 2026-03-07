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
package org.xwiki.user.rest.internal.document;

import java.net.URI;
import java.util.Objects;

import javax.inject.Provider;
import javax.ws.rs.NotFoundException;

import org.xwiki.component.annotation.Component;
import org.xwiki.mail.EmailAddressObfuscator;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.rest.Relations;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.resources.pages.PageHistoryResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserReference;
import org.xwiki.user.internal.document.DocumentUserReference;
import org.xwiki.user.rest.UserReferenceModelSerializer;
import org.xwiki.user.rest.internal.AbstractUserReferenceModelSerializer;
import org.xwiki.user.rest.model.jaxb.User;
import org.xwiki.user.rest.model.jaxb.UserPreferences;
import org.xwiki.user.rest.model.jaxb.UserSummary;
import org.xwiki.user.rest.resources.UserResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * Implementation of {@link UserReferenceModelSerializer} for instances of {@link DocumentUserReference}.
 *
 * @since 18.2.0RC1
 * @version $Id$
 */
@Component
@Named("document")
@Singleton
public class DocumentUserReferenceModelSerializer extends AbstractUserReferenceModelSerializer
{
    @Inject
    private Provider<ContextualAuthorizationManager> authorizationManagerProvider;

    @Inject
    private GeneralMailConfiguration mailConfiguration;

    @Inject
    private EmailAddressObfuscator emailAddressObfuscator;

    private void toRestUserSummary(URI baseUri, UserSummary userSummary, String userId,
        DocumentUserReference userReference, UserProperties userProperties) throws XWikiException
    {
        userSummary.setId(userId);
        userSummary.setGlobal(userReference.isGlobal());
        userSummary.setFirstName(userProperties.getFirstName());
        userSummary.setLastName(userProperties.getLastName());

        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument xwikiDocument = xcontext.getWiki().getDocument(userReference.getReference(), xcontext);
        String avatarFileName = userProperties.getProperty("avatar");

        if (avatarFileName != null) {
            XWikiAttachment avatarAttachment = xwikiDocument.getAttachment(avatarFileName);
            userSummary.setAvatarUrl(xcontext.getWiki().getURL(avatarAttachment.getReference(), xcontext));
        } else {
            String defaultAvatarUrl = xcontext.getWiki().getSkinFile("icons/xwiki/noavatar.png", xcontext);
            userSummary.setAvatarUrl(defaultAvatarUrl);
        }
        userSummary.setXwikiRelativeUrl(xwikiDocument.getURL(Right.VIEW.getName(), xcontext));
        userSummary.setXwikiAbsoluteUrl(xwikiDocument.getExternalURL(Right.VIEW.getName(), xcontext));

        String pageUri = Utils.createURI(baseUri, PageResource.class,
                xwikiDocument.getDocumentReference().getWikiReference().getName(),
                Utils.getSpacesURLElements(xwikiDocument.getDocumentReference()),
                xwikiDocument.getDocumentReference().getName())
            .toString();
        Link pageLink = this.xwikiObjectFactory.createLink();
        pageLink.setHref(pageUri);
        pageLink.setRel(Relations.PAGE);
        userSummary.withLinks(pageLink);

        String historyUri = Utils.createURI(baseUri, PageHistoryResource.class,
                xwikiDocument.getDocumentReference().getWikiReference().getName(),
                Utils.getSpacesURLElements(xwikiDocument.getDocumentReference()),
                xwikiDocument.getDocumentReference().getName())
            .toString();
        Link historyLink = this.xwikiObjectFactory.createLink();
        historyLink.setHref(historyUri);
        historyLink.setRel(Relations.HISTORY);
        userSummary.withLinks(historyLink);
    }

    @Override
    public UserSummary toRestUserSummary(URI baseUri, String userId, UserReference userReference) throws XWikiException
    {
        DocumentUserReference documentUserReference = (DocumentUserReference) userReference;

        UserProperties userProperties = this.userPropertiesResolver.resolve(userReference);
        UserSummary userSummary = this.userObjectFactory.createUserSummary();
        toRestUserSummary(baseUri, userSummary, userId, documentUserReference, userProperties);

        String historyUri = Utils.createURI(baseUri, UserResource.class,
                documentUserReference.getReference().getWikiReference().getName(), userId)
            .toString();
        Link historyLink = this.xwikiObjectFactory.createLink();
        historyLink.setHref(historyUri);
        historyLink.setRel(Relations.USER);
        userSummary.withLinks(historyLink);

        return userSummary;
    }

    @Override
    public User toRestUser(URI baseUri, String userId, UserReference userReference, boolean preferences)
        throws XWikiException
    {
        if (userReference == GuestUserReference.INSTANCE) {
            return guestToRestUser();
        }

        DocumentUserReference documentUserReference = (DocumentUserReference) userReference;

        UserProperties userProperties = this.userPropertiesResolver.resolve(userReference);
        if (userProperties.isEmpty()) {
            throw new NotFoundException();
        }

        User user = this.userObjectFactory.createUser();
        toRestUserSummary(baseUri, user, userId, documentUserReference, userProperties);

        XWikiContext xcontext = this.xcontextProvider.get();

        // We switch the context's wiki to the fetched user's to access wiki-specific preferences.
        String oldWikiId = xcontext.getWikiId();
        xcontext.setWikiId(documentUserReference.getReference().getWikiReference().getName());

        // Handle email obfuscation based on wiki's configuration.
        String emailAddress = "";
        if (userProperties.getEmail() != null) {
            if (this.mailConfiguration.shouldObfuscate()) {
                emailAddress = this.emailAddressObfuscator.obfuscate(userProperties.getEmail());
            } else {
                emailAddress = userProperties.getEmail().toString();
            }
        }
        user.setEmail(emailAddress);

        user.setDisplayName(xcontext.getWiki().getUserName(documentUserReference.getReference(), null, false, true,
            xcontext));
        user.setCompany(Objects.toString(userProperties.getProperty("company"), ""));
        user.setAbout(Objects.toString(userProperties.getProperty("comment"), ""));
        user.setPhone(Objects.toString(userProperties.getProperty("phone"), ""));
        user.setAddress(Objects.toString(userProperties.getProperty("address"), ""));
        user.setBlog(Objects.toString(userProperties.getProperty("blog"), ""));
        user.setBlogFeed(Objects.toString(userProperties.getProperty("blogfeed"), ""));

        if (preferences) {
            user.setPreferences(toRestUserPreferences(userProperties, xcontext));
        }

        // We reset the context's wiki.
        xcontext.setWikiId(oldWikiId);

        return user;
    }

    private UserPreferences toRestUserPreferences(UserProperties userProperties, XWikiContext xcontext)
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

    @Override
    public boolean hasAccess(UserReference userReference)
    {
        DocumentUserReference documentUserReference = (DocumentUserReference) userReference;
        return this.authorizationManagerProvider.get().hasAccess(Right.VIEW, documentUserReference.getReference());
    }
}
