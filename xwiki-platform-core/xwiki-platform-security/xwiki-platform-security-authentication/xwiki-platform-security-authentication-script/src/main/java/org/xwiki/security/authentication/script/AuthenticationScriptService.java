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
package org.xwiki.security.authentication.script;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authentication.AuthenticationAction;
import org.xwiki.security.authentication.AuthenticationConfiguration;
import org.xwiki.security.authentication.AuthenticationFailureManager;
import org.xwiki.security.authentication.AuthenticationFailureStrategy;
import org.xwiki.security.authentication.AuthenticationResourceReference;
import org.xwiki.security.authentication.ResetPasswordException;
import org.xwiki.security.authentication.ResetPasswordManager;
import org.xwiki.security.authentication.ResetPasswordRequestResponse;
import org.xwiki.security.authentication.RetrieveUsernameException;
import org.xwiki.security.authentication.RetrieveUsernameManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptService;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Security Authentication Script service.
 *
 * @version $Id$
 * @since 11.6RC1
 */
@Component
@Named(SecurityScriptService.ROLEHINT + '.' + AuthenticationScriptService.ID)
@Singleton
public class AuthenticationScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ID = "authentication";

    @Inject
    private AuthenticationFailureManager authenticationFailureManager;

    @Inject
    private AuthenticationConfiguration authenticationConfiguration;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Inject
    private ResourceReferenceSerializer<ResourceReference, ExtendedURL> defaultResourceReferenceSerializer;

    @Inject
    private ResetPasswordManager resetPasswordManager;

    @Inject
    private RetrieveUsernameManager retrieveUsernameManager;

    @Inject
    @Named("contextpath")
    private URLNormalizer<ExtendedURL> urlNormalizer;

    @Inject
    private Logger logger;

    /**
     * @param username the login used in the request for authentication.
     * @return the aggregated form field to validate for the authentication
     *         (see {@link AuthenticationFailureManager#getForm(String, javax.servlet.http.HttpServletRequest)}
     */
    public String getForm(String username)
    {
        return this.authenticationFailureManager.getForm(username, contextProvider.get().getRequest());
    }

    /**
     * @param username the login used in the request for authentication.
     * @return the aggregated error messages to display for the user
     *          (see {@link AuthenticationFailureManager#getErrorMessage(String)}).
     */
    public String getErrorMessage(String username)
    {
        return this.authenticationFailureManager.getErrorMessage(username);
    }

    /**
     * @return name of all available authentication failure strategies.
     */
    public Set<String> getAuthenticationFailureAvailableStrategies()
    {
        try {
            return this.componentManager.getInstanceMap(AuthenticationFailureStrategy.class).keySet();
        } catch (ComponentLookupException e) {
            logger.error("Error while getting the list of available authentication strategies.");
            return Collections.emptySet();
        }
    }

    /**
     * @return the current configuration.
     */
    public AuthenticationConfiguration getAuthenticationConfiguration()
    {
        return this.authenticationConfiguration;
    }

    /**
     * Reset the authentication failure record for the given username.
     * @param username the username for which to remove the record.
     */
    public void resetAuthenticationFailureCounter(String username)
    {
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            this.authenticationFailureManager.resetAuthenticationFailureCounter(username);
        }
    }

    /**
     * Compute a relative URL for an {@link AuthenticationResourceReference} based on the given action string.
     * See {@link AuthenticationAction} for more information.
     *
     * @param action the authentication action from which to build the right URL.
     * @param params the query string parameters of the URL.
     * @return a relative URL for the current wiki or {@code null} if an error occurs.
     * @since 13.1RC1
     */
    public String getAuthenticationURL(String action, Map<String, Object> params)
    {
        try {
            AuthenticationAction authenticationAction = AuthenticationAction.getFromRequestParameter(action);

            AuthenticationResourceReference resourceReference = new AuthenticationResourceReference(
                this.contextProvider.get().getWikiReference(),
                authenticationAction);
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    resourceReference.addParameter(entry.getKey(), entry.getValue());
                }
            }
            ExtendedURL extendedURL = this.defaultResourceReferenceSerializer.serialize(resourceReference);
            return this.urlNormalizer.normalize(extendedURL).serialize();
        } catch (IllegalArgumentException | SerializeResourceReferenceException
            | UnsupportedResourceReferenceException e)
        {
            logger.error("Error while getting authentication URL for action [{}].", action, e);
            return null;
        }
    }

    /**
     * Request a password reset for the given user.
     * This will result in computing a verification code and sending the appropriate link by email to the user.
     * This method returns the email address used, so that we can display it to the user.
     *
     * @param user the user for which to perform a reset password request.
     * @throws ResetPasswordException if any error occurs for performing the reset password request.
     * @since 13.1RC1
     */
    public void requestResetPassword(UserReference user) throws ResetPasswordException
    {
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            ResetPasswordRequestResponse resetPasswordRequestResponse =
                this.resetPasswordManager.requestResetPassword(user);
            this.resetPasswordManager.sendResetPasswordEmailRequest(resetPasswordRequestResponse);
        }
    }

    /**
     * Check that the given verification code is correct.
     * Note that we don't need to protect this API for programming rights: if the verificationCode is not correct a
     * {@link ResetPasswordException} is thrown and the verificationCode is reset. So a script attacker with wrong
     * credentials cannot access the verification code, or bruteforce it.
     *
     * @param user the user for which to check the verification code.
     * @param verificationCode the code to check.
     * @return the same verification code if it is correct.
     * @throws ResetPasswordException if the code is not correct or if an error occurs.
     * @since 13.1RC1
     */
    public String checkVerificationCode(UserReference user, String verificationCode)
        throws ResetPasswordException
    {
        return this.resetPasswordManager.checkVerificationCode(user, verificationCode).getVerificationCode();
    }

    /**
     * Reset the password of the given user, iff the given verification code is correct.
     * This methods throws a {@link ResetPasswordException} if the verification code is wrong.
     *
     * @param user the user for which to reset the password.
     * @param verificationCode the code to check before resetting the passord.
     * @param newPassword the new password to user.
     * @throws ResetPasswordException if the verification code is wrong, or if an error occurs.
     * @since 13.1RC1
     */
    public void resetPassword(UserReference user, String verificationCode, String newPassword)
        throws ResetPasswordException
    {
        this.resetPasswordManager.checkVerificationCode(user, verificationCode);
        this.resetPasswordManager.resetPassword(user, newPassword);
    }

    /**
     * Retrieve users information associated to the given email address and send them by email.
     *
     * @param userEmail the email address for which to find associated accounts
     * @throws RetrieveUsernameException in case of problem for finding the information or for sending the email
     * @since 14.9
     * @since 13.10.10
     * @since 14.4.6
     */
    public void retrieveUsernameAndSendEmail(String userEmail) throws RetrieveUsernameException
    {
        Set<UserReference> users = this.retrieveUsernameManager.findUsers(userEmail);
        if (!users.isEmpty()) {
            this.retrieveUsernameManager.sendRetrieveUsernameEmail(userEmail, users);
        }
    }
}
