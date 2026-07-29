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

import javax.ws.rs.NotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.mail.EmailAddressObfuscator;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.Relations;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.resources.pages.PageHistoryResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.user.internal.document.DocumentUserReference;
import org.xwiki.user.rest.internal.AbstractUserReferenceModelSerializer;
import org.xwiki.user.rest.internal.UserReferenceModelSerializer;
import org.xwiki.user.rest.model.jaxb.User;
import org.xwiki.user.rest.model.jaxb.UserSummary;
import org.xwiki.user.rest.resources.UserResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

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
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @Inject
    private UserReferenceSerializer<String> stringUserReferenceSerializer;

    @Inject
    private GeneralMailConfiguration mailConfiguration;

    @Inject
    private EmailAddressObfuscator emailAddressObfuscator;

    private void toRestUserSummary(URI baseUri, UserSummary userSummary, String userId, DocumentReference userReference,
        boolean global, UserProperties userProperties) throws XWikiException
    {
        userSummary.setId(userId);
        userSummary.setGlobal(global);
        userSummary.setFirstName(userProperties.getFirstName());
        userSummary.setLastName(userProperties.getLastName());

        XWikiContext xcontext = this.xcontextProvider.get();

        String avatarFileName = null;
        // The DocumentReference might be null (guest user)
        if (userReference != null) {
            XWikiDocument xwikiDocument = xcontext.getWiki().getDocument(userReference, xcontext);
            avatarFileName = userProperties.getProperty("avatar");
            if (avatarFileName != null) {
                XWikiAttachment avatarAttachment = xwikiDocument.getAttachment(avatarFileName);
                userSummary.setAvatarUrl(xcontext.getWiki().getURL(avatarAttachment.getReference(), xcontext));
            }

            userSummary.setXwikiRelativeUrl(xwikiDocument.getURL(Right.VIEW.getName(), xcontext));
            userSummary.setXwikiAbsoluteUrl(xwikiDocument.getExternalURL(Right.VIEW.getName(), xcontext));

            String pageUri = Utils.createURI(baseUri, PageResource.class,
                xwikiDocument.getDocumentReference().getWikiReference().getName(),
                Utils.getSpacesURLElements(xwikiDocument.getDocumentReference()),
                xwikiDocument.getDocumentReference().getName()).toString();
            Link pageLink = this.xwikiObjectFactory.createLink();
            pageLink.setHref(pageUri);
            pageLink.setRel(Relations.PAGE);
            userSummary.withLinks(pageLink);

            String historyUri = Utils.createURI(baseUri, PageHistoryResource.class,
                xwikiDocument.getDocumentReference().getWikiReference().getName(),
                Utils.getSpacesURLElements(xwikiDocument.getDocumentReference()),
                xwikiDocument.getDocumentReference().getName()).toString();
            Link historyLink = this.xwikiObjectFactory.createLink();
            historyLink.setHref(historyUri);
            historyLink.setRel(Relations.HISTORY);
            userSummary.withLinks(historyLink);
        }

        if (avatarFileName == null) {
            String defaultAvatarUrl = xcontext.getWiki().getSkinFile("icons/xwiki/noavatar.png", xcontext);
            userSummary.setAvatarUrl(defaultAvatarUrl);
        }
    }

    @Override
    public UserSummary toRestUserSummary(URI baseUri, UserReference userReference) throws XWikiException
    {
        DocumentReference documentUserReference = this.userReferenceSerializer.serialize(userReference);
        String userId = this.stringUserReferenceSerializer.serialize(userReference);

        UserProperties userProperties = this.userPropertiesResolver.resolve(userReference);
        UserSummary userSummary = this.userObjectFactory.createUserSummary();
        toRestUserSummary(baseUri, userSummary, userId, documentUserReference, userReference.isGlobal(),
            userProperties);

        // The DocumentReference might be null (guest user)
        if (documentUserReference != null) {
            String historyUri =
                Utils.createURI(baseUri, UserResource.class, documentUserReference.getWikiReference().getName(), userId)
                    .toString();
            Link userLink = this.xwikiObjectFactory.createLink();
            userLink.setHref(historyUri);
            userLink.setRel(Relations.USER);
            userSummary.withLinks(userLink);
        }

        return userSummary;
    }

    @Override
    public User toRestUser(URI baseUri, UserReference userReference, boolean preferences) throws XWikiException
    {
        UserProperties userProperties = this.userPropertiesResolver.resolve(userReference);
        if (userProperties.isEmpty()) {
            throw new NotFoundException();
        }

        DocumentReference documentUserReference = this.userReferenceSerializer.serialize(userReference);
        String userId = this.stringUserReferenceSerializer.serialize(userReference);

        User user = this.userObjectFactory.createUser();
        toRestUserSummary(baseUri, user, userId, documentUserReference, userReference.isGlobal(), userProperties);

        XWikiContext xcontext = this.xcontextProvider.get();

        // Remember the context's wiki
        String oldWikiId = xcontext.getWikiId();

        try {
            // The DocumentReference might be null (guest user)
            if (documentUserReference != null) {
                // We switch the context's wiki to the fetched user's to access wiki-specific preferences.
                xcontext.setWikiId(documentUserReference.getWikiReference().getName());
            }

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

            user.setDisplayName(xcontext.getWiki().getUserName(documentUserReference, null, false, true, xcontext));

            user.setCompany(Objects.toString(userProperties.getProperty("company"), ""));
            user.setAbout(Objects.toString(userProperties.getProperty("comment"), ""));
            user.setPhone(Objects.toString(userProperties.getProperty("phone"), ""));
            user.setAddress(Objects.toString(userProperties.getProperty("address"), ""));
            user.setBlog(Objects.toString(userProperties.getProperty("blog"), ""));
            user.setBlogFeed(Objects.toString(userProperties.getProperty("blogfeed"), ""));

            if (preferences) {
                user.setPreferences(toRestUserPreferences(userProperties, xcontext));
            }
        } finally {
            // We reset the context's wiki.
            xcontext.setWikiId(oldWikiId);
        }

        return user;
    }
}
