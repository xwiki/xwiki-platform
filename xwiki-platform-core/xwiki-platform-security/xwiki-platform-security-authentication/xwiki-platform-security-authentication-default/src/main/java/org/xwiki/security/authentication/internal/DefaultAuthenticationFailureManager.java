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

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.securityfilter.filter.SecurityRequestWrapper;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authentication.api.AuthenticationConfiguration;
import org.xwiki.security.authentication.api.AuthenticationFailureEvent;
import org.xwiki.security.authentication.api.AuthenticationFailureLimitReachedEvent;
import org.xwiki.security.authentication.api.AuthenticationFailureManager;
import org.xwiki.security.authentication.api.AuthenticationFailureStrategy;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * Default implementation for {@link AuthenticationFailureManager}.
 *
 * @version $Id$
 * @since 11.6RC1
 */
@Component
@Singleton
public class DefaultAuthenticationFailureManager implements AuthenticationFailureManager
{
    private static final String STRING_AGGREGATION_SEPARATOR = "\n";

    private static final AuthenticationFailureEvent AUTHENTICATION_FAILURE_EVENT = new AuthenticationFailureEvent();

    private static final AuthenticationFailureLimitReachedEvent AUTHENTICATION_FAILURE_LIMIT_REACHED_EVENT =
        new AuthenticationFailureLimitReachedEvent();

    @Inject
    private AuthenticationConfiguration configuration;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    private String[] failureStrategyNames;

    private List<AuthenticationFailureStrategy> failureStrategyList;

    private Map<String, AuthFailureRecord> authFailures = new HashMap<>();

    private Map<DocumentReference, String> userAndAssociatedUsernames = new HashMap<>();

    /**
     * Default constructor.
     */
    public DefaultAuthenticationFailureManager()
    {
    }

    private void buildStrategyList()
    {
        this.failureStrategyList = new LinkedList<>();
        for (String failureStrategyName : this.failureStrategyNames) {
            try {
                this.failureStrategyList.add(this.componentManager.getInstance(AuthenticationFailureStrategy.class,
                    failureStrategyName));
            } catch (ComponentLookupException e) {
                logger.error("Error while getting authentication failure strategy [{}]. ", failureStrategyName, e);
            }
        }
    }

    private List<AuthenticationFailureStrategy> getFailureStrategyList()
    {
        if (!Arrays.equals(configuration.getFailureStrategies(), this.failureStrategyNames)) {
            this.failureStrategyNames = this.configuration.getFailureStrategies();
            buildStrategyList();
        }

        return this.failureStrategyList;
    }

    private boolean isAuthenticationSecurityEnabled()
    {
        // historically the feature was considered as disabled if max attempts = 0, max time = 0 or the strategy list
        // was empty. We keep that as possible way to say it's disabled.
        return configuration.isAuthenticationSecurityEnabled()
            && getMaxNbAttempts() != 0
            && getMaxTime() != 0
            && !getFailureStrategyList().isEmpty();
    }

    @Override
    public boolean recordAuthenticationFailure(String username)
    {
        // An authentication failure just happened, so we trigger the appropriate event.
        observationManager.notify(AUTHENTICATION_FAILURE_EVENT, username);

        // If the config is set to 0 for max attempts or time window, it means the feature is disabled,
        // we can immediately return, and we clear the data.
        if (!isAuthenticationSecurityEnabled()) {
            this.authFailures.clear();
            return false;
        }

        // If the map already contains the login, we increment the counter of auth failures, else we create a record.
        if (authFailures.containsKey(username)) {
            authFailures.get(username).incrementAttemptOrReset();
        } else {
            authFailures.put(username, new AuthFailureRecord());
            DocumentReference userReference = findUser(username);
            if (userReference != null) {
                userAndAssociatedUsernames.put(userReference, username);
            }
        }

        boolean isThresholdReached = authFailures.get(username).isThresholdReached();

        // The threshold is reached: we need to notify the strategies and the listeners.
        if (isThresholdReached) {
            for (AuthenticationFailureStrategy authenticationFailureStrategy : getFailureStrategyList()) {
                authenticationFailureStrategy.notify(username);
            }
            observationManager.notify(AUTHENTICATION_FAILURE_LIMIT_REACHED_EVENT, username);
        }

        return isThresholdReached;
    }

    @Override
    public void resetAuthenticationFailureCounter(DocumentReference user)
    {
        if (this.userAndAssociatedUsernames.containsKey(user)) {
            authFailures.remove(this.userAndAssociatedUsernames.get(user));
        }
    }

    @Override
    public void resetAuthenticationFailureCounter(String username)
    {
        authFailures.remove(username);
    }

    private boolean isThresholdReached(String username)
    {
        return this.authFailures.containsKey(username) && this.authFailures.get(username).isThresholdReached();
    }

    @Override
    public String getForm(String username)
    {
        StringBuilder builder = new StringBuilder();

        // We only call the strategies if the threshold is reached.
        if (isThresholdReached(username)) {
            for (AuthenticationFailureStrategy authenticationFailureStrategy : getFailureStrategyList()) {
                builder.append(authenticationFailureStrategy.getForm(username));
                builder.append(STRING_AGGREGATION_SEPARATOR);
            }
        }

        return builder.toString();
    }

    @Override
    public boolean validateForm(String username, SecurityRequestWrapper request)
    {
        boolean result = true;

        // If the config is set to 0 for max attempts or time window, it means the feature is disabled,
        // we can clear the data.
        if (!isAuthenticationSecurityEnabled()) {
            this.authFailures.clear();
        }

        // We only call the strategies if the threshold is reached.
        if (isThresholdReached(username)) {
            for (AuthenticationFailureStrategy authenticationFailureStrategy : getFailureStrategyList()) {

                // The form is validated if ALL strategies validated it.
                result = result && authenticationFailureStrategy.validateForm(username, request);
            }
        }

        // If the form is not validated we put an invalidcredentials message: this is done to avoid bruteforcing a
        // password by analyzing the error messages output on the login page.
        if (!result) {
            this.contextProvider.get().put("message", "invalidcredentials");
        }
        return result;
    }

    @Override
    public String getErrorMessage(String username)
    {
        StringBuilder builder = new StringBuilder();

        // We only call the strategies if the threshold is reached.
        if (isThresholdReached(username)) {
            for (AuthenticationFailureStrategy authenticationFailureStrategy : getFailureStrategyList()) {
                builder.append(authenticationFailureStrategy.getErrorMessage(username));
                builder.append(STRING_AGGREGATION_SEPARATOR);
            }
        }

        return builder.toString();
    }

    private DocumentReference buildUserDocumentReference(String wikiId, String username)
    {
        return new DocumentReference(wikiId, "XWiki", username);
    }

    @Override
    public DocumentReference findUser(String username)
    {
        DocumentReference result = null;
        XWikiContext context = this.contextProvider.get();
        String globalWiki = context.getMainXWiki();
        XWikiUser globalXWikiUser = new XWikiUser(buildUserDocumentReference(globalWiki, username));
        if (globalXWikiUser.exists(context)) {
            result = globalXWikiUser.getUserReference();
        } else {
            String localWiki = context.getWikiId();
            XWikiUser localXWikiUser = new XWikiUser(buildUserDocumentReference(localWiki, username));
            if (localXWikiUser.exists(context)) {
                result = localXWikiUser.getUserReference();
            }
        }
        return result;
    }

    private long getMaxTime()
    {
        return configuration.getTimeWindow() * 1000;
    }

    private int getMaxNbAttempts()
    {
        return configuration.getMaxAuthorizedAttempts();
    }

    /**
     * This class aims at storing the authentication failure record information about a login.
     * It only stores the first failing date and the number of failing attempts since then.
     * Those two are resetted if another failure happens outside of the given time window.
     * (See {@link AuthenticationConfiguration#getTimeWindow()})
     */
    class AuthFailureRecord
    {
        private long firstFailingDate;
        private int nbAttempts;

        AuthFailureRecord() {
            this.firstFailingDate = new Date().getTime();
            this.nbAttempts = 1;
        }

        void incrementAttemptOrReset()
        {
            // If the threshold is already reached, we have to wait until it's reset, so each new failure is the new
            // beginning of the time window.
            if (isThresholdReached()) {
                this.firstFailingDate = new Date().getTime();
                this.nbAttempts++;

            // If the threshold not reached yet and we're out of the time window, we can reset the data.
            } else if (firstFailingDate + getMaxTime() < new Date().getTime()) {
                this.firstFailingDate = new Date().getTime();
                this.nbAttempts = 1;

            // Else the threshold not reached but we are in the time window: we increment the number of attempts.
            } else {
                this.nbAttempts++;
            }
        }

        boolean isThresholdReached()
        {
            return this.nbAttempts >= getMaxNbAttempts();
        }
    }
}
