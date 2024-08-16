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
package org.xwiki.security.authentication.internal;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authentication.AuthenticationFailureManager;
import org.xwiki.security.authentication.AuthenticationFailureStrategy;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.user.api.XWikiUser;

import jakarta.servlet.http.HttpServletRequest;

/**
 * A strategy to disable authentication in case of repeated failure with a login.
 * <p>
 * This strategy will do two things: if no user account can be found associated to the login, it will simply block any
 * further check. If a user account can be found, it will disable it automatically allowing an admin to take an action
 * such as changing the user password or at least informing the user about the attack. When the user account is enable
 * back, he can immediately login even if the record has not been removed from the authentication manager.
 *
 * @version $Id$
 * @since 11.6RC1
 */
@Component
@Named(DisableAccountFailureStrategy.NAME)
@Singleton
public class DisableAccountFailureStrategy implements AuthenticationFailureStrategy, EventListener
{
    /**
     * The component name.
     */
    public static final String NAME = "disableAccount";

    protected static final LocalDocumentReference USER_CLASS_REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "XWikiUsers");

    @Inject
    private ContextualLocalizationManager contextLocalization;

    /**
     * We access the {@link AuthenticationFailureManager} through a provider because it depends on the
     * {@link ObservationManager} and thus we want to avoid a dependency loop while event listeners are initialized.
     */
    @Inject
    private Provider<AuthenticationFailureManager> authenticationFailureManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public String getErrorMessage(String username)
    {
        return contextLocalization.getTranslationPlain("security.authentication.strategy.disableAccount.errorMessage");
    }

    @Override
    public String getForm(String username)
    {
        return "";
    }

    /**
     * Here the validate form is used to remove the authentication failure record if the user account associated to the
     * username has been enabled back.
     *
     * @param username the username used for the authentication failure.
     * @param request the authentication request.
     * @return true if the user account associated to the username is enabled. False in other cases.
     */
    @Override
    public boolean validateForm(String username, HttpServletRequest request)
    {
        DocumentReference userDocumentReference = this.authenticationFailureManager.get().findUser(username);
        if (userDocumentReference != null) {
            return !new XWikiUser(userDocumentReference).isDisabled(this.contextProvider.get());
        }

        return false;
    }

    /**
     * When the threshold is reached, we try to find the account of the user based on the username, and we disable its
     * account if we manage to do so.
     * 
     * @param username the username used for the authentication failure.
     */
    @Override
    public void notify(String username)
    {
        DocumentReference userDocumentReference = this.authenticationFailureManager.get().findUser(username);
        if (userDocumentReference != null) {
            new XWikiUser(userDocumentReference).setDisabled(true, this.contextProvider.get());
        }
    }

    @Override
    public List<Event> getEvents()
    {
        return Collections.singletonList(new DocumentUpdatedEvent());
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument updatedDocument = (XWikiDocument) source;
        BaseObject updatedUserObject = updatedDocument.getXObject(USER_CLASS_REFERENCE);
        XWikiDocument originalDocument = updatedDocument.getOriginalDocument();
        BaseObject originalUserObject = originalDocument.getXObject(USER_CLASS_REFERENCE);

        if (originalUserObject != null && updatedUserObject != null
            && (propertyValueChanged(originalUserObject, updatedUserObject, XWikiUser.ACTIVE_PROPERTY, 1))) {
            // Remove the authentication failure record when the user account is re-enabled or activated.
            this.authenticationFailureManager.get()
                .resetAuthenticationFailureCounter(updatedDocument.getDocumentReference());
        }
    }

    private boolean propertyValueChanged(BaseObject originalObject, BaseObject updatedObject, String propertyName,
        int expectedValue)
    {
        return originalObject.getIntValue(propertyName) != expectedValue
            && updatedObject.getIntValue(propertyName) == expectedValue;
    }
}
