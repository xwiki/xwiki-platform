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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authentication.api.AuthenticationConfiguration;
import org.xwiki.security.authentication.api.AuthenticationFailureManager;
import org.xwiki.security.authentication.api.AuthenticationFailureStrategy;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authentication.api.AuthenticationResourceReference;
import org.xwiki.security.authentication.api.ResetPasswordException;
import org.xwiki.security.authentication.api.ResetPasswordManager;
import org.xwiki.security.script.SecurityScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.url.ExtendedURL;
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
     * See {@link AuthenticationResourceReference.AuthenticationAction} for more information.
     *
     * @param action the authentication action from which to build the right URL.
     * @param params the query string parameters of the URL.
     * @return a relative URL for the current wiki.
     * @since 13.0RC1
     */
    @Unstable
    public String getAuthenticationURL(String action, Map<String, Object> params)
    {
        try {
            AuthenticationResourceReference.AuthenticationAction authenticationAction =
                AuthenticationResourceReference.AuthenticationAction.getFromRequestParameter(action);

            AuthenticationResourceReference resourceReference =
                new AuthenticationResourceReference(authenticationAction);
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    resourceReference.addParameter(entry.getKey(), entry.getValue());
                }
            }
            ExtendedURL extendedURL = this.defaultResourceReferenceSerializer.serialize(resourceReference);
            return extendedURL.serialize();
        } catch (IllegalArgumentException | SerializeResourceReferenceException
            | UnsupportedResourceReferenceException e) {
            logger.error("Error while getting authentication URL for action [{}]: [{}]", action,
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    /**
     * Compute an absolute URL for an {@link AuthenticationResourceReference} based on the given action string.
     * See {@link AuthenticationResourceReference.AuthenticationAction} for more information.
     * @param action the authentication action from which to build the right URL.
     * @param params the query string parameters of the URL.
     * @return an absolute URL.
     * @since 13.0RC1
     */
    @Unstable
    public String getAuthenticationExternalURL(String action, Map<String, Object> params)
    {
        XWikiContext xWikiContext = this.contextProvider.get();
        String authenticationURL = getAuthenticationURL(action, params);
        try {
            URL serverURL = xWikiContext.getURLFactory().getServerURL(xWikiContext);
            URL result = new URL(serverURL, authenticationURL);

            return result.toExternalForm();
        } catch (IllegalArgumentException | MalformedURLException e) {
            logger.error("Error while getting external authentication URL for action [{}]: [{}]", action,
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    public InternetAddress requestResetPassword(UserReference user) throws ResetPasswordException
    {
        return this.resetPasswordManager.requestResetPassword(user);
    }

    public void resetPassword(UserReference user, String verificationCode, String newPassword)
        throws ResetPasswordException
    {
        this.resetPasswordManager.checkVerificationCode(user, verificationCode, false);
        this.resetPasswordManager.resetPassword(user, newPassword);
    }

    public String checkAndResetVerificationCode(UserReference user, String verificationCode)
        throws ResetPasswordException
    {
        return this.resetPasswordManager.checkVerificationCode(user, verificationCode, true);
    }
}
