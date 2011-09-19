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
package org.xwiki.jira;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import com.atlassian.jira.rest.client.AuthenticationHandler;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.auth.AnonymousAuthenticationHandler;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;

/**
 * Expose Atlassian's JIRA REST service to XWiki scripts.
 *
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Named("jira")
public class JiraScriptService implements ScriptService
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Note that the password will be passed in clear over the network to the remote JIRA instance. Thus, only use this
     * method when connecting over HTTPS.
     *
     * @param jiraURL the URL to the remote JIRA instance to connect to
     * @param username the username to connect to JIRA
     * @param password the password to connect to JIRA
     * @return the client to interact with the remote JIRA instance
     */
    public JiraRestClient getJiraRestClient(String jiraURL, String username, String password)
    {
        return getJiraRestClient(jiraURL, new BasicHttpAuthenticationHandler(username, password));
    }

    /**
     * Connect anonymously to the remote JIRA instance.
     *
     * @param jiraURL the URL to the remote JIRA instance to connect to
     * @return the client to interact with the remote JIRA instance
     */
    public JiraRestClient getJiraRestClient(String jiraURL)
    {
        return getJiraRestClient(jiraURL, new AnonymousAuthenticationHandler());
    }

    /**
     * @param jiraURL the URL to the remote JIRA instance to connect to
     * @param authenticationHandler the authentication to use (anonymous, basic, etc)
     * @return the client to interact with the remote JIRA instance
     */
    private JiraRestClient getJiraRestClient(String jiraURL, AuthenticationHandler authenticationHandler)
    {
        JiraRestClient restClient;
        try {
            JerseyJiraRestClientFactory factory = new JerseyJiraRestClientFactory();
            URI jiraServerUri = new URI(jiraURL);
            restClient = factory.create(jiraServerUri, authenticationHandler);
        } catch (URISyntaxException e) {
            this.logger.warn("Invalid JIRA URL [{}]", jiraURL);
            restClient = null;
        }
        return restClient;
    }
}
