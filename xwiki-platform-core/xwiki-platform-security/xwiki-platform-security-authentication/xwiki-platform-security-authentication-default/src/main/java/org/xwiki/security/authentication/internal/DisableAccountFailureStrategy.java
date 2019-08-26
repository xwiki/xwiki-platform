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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.securityfilter.filter.SecurityRequestWrapper;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authentication.api.AuthenticationFailureManager;
import org.xwiki.security.authentication.api.AuthenticationFailureStrategy;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * A strategy to disable authentication in case of repeated failure with a login.
 * This strategy will do two things: if no user account can be found associated to the login, it will simply block any
 * further check. If a user account can be found, it will disable it automatically allowing an admin to take an action
 * such as changing the user password or at least informing the user about the attack. When the user account is enable
 * back, he can immediately login even if the record has not been removed from the authentication manager.
 *
 * @version $Id$
 * @since 11.6RC1
 */
@Component
@Named("disableAccount")
@Singleton
public class DisableAccountFailureStrategy implements AuthenticationFailureStrategy
{
    @Inject
    private ContextualLocalizationManager contextLocalization;

    @Inject
    private AuthenticationFailureManager authenticationFailureManager;

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
    public boolean validateForm(String username, SecurityRequestWrapper request)
    {
        DocumentReference userDocumentReference = this.authenticationFailureManager.findUser(username);
        if (userDocumentReference != null) {
            return !new XWikiUser(userDocumentReference).isDisabled(this.contextProvider.get());
        }
        return false;
    }

    /**
     * When the threshold is reached, we try to find the account of the user based on the username, and we disable its
     * account if we manage to do so.
     * @param username the username used for the authentication failure.
     */
    @Override
    public void notify(String username)
    {
        DocumentReference userDocumentReference = this.authenticationFailureManager.findUser(username);
        if (userDocumentReference != null) {
            new XWikiUser(userDocumentReference).setDisabled(true, this.contextProvider.get());
        }
    }
}
