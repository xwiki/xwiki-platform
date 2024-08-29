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
package org.xwiki.test.docker.internal.junit5;

import java.io.IOException;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.platform.wiki.creationjob.internal.WikiCreationJob;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.JobRequest;
import org.xwiki.test.docker.junit5.DockerTestException;
import org.xwiki.test.extension.JobExecutor;

/**
 * Create a sub wiki.
 *
 * @version $Id$
 * @since 14.5
 */
public class WikiCreator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WikiCreator.class);

    private final ExtensionContext context;

    private ComponentManager componentManager;

    /**
     * Initialize the Component Manager which is later needed to perform the REST calls.
     * 
     * @param context the context of the test
     */
    public WikiCreator(ExtensionContext context)
    {
        this.context = context;

        this.componentManager = DockerTestUtils.getComponentManager(context);
    }

    /**
     * @param credentials the xwiki user and password to use to connect for the REST endpoint
     * @param wikiId the identifier of the wiki to create
     * @param failOnExist whether or not the wiki creation should fail if the wiki already exists
     * @return true of the wiki was created, false if it already existed
     * @throws Exception when failing to create the wiki
     */
    public boolean createWiki(UsernamePasswordCredentials credentials, String wikiId, boolean failOnExist)
        throws Exception
    {
        String xwikiRESTURL = String.format("%s/rest", DockerTestUtils.getXWikiURL(this.context));

        // Check of the wiki already exists
        if (exists(xwikiRESTURL, credentials, wikiId)) {
            if (failOnExist) {
                throw new DockerTestException("Wiki [" + wikiId + "] already exit");
            }

            return false;
        }

        LOGGER.info("...Creating wiki [{}]...", wikiId);

        // Create the wiki
        WikiCreationRequest wikiRequest = new WikiCreationRequest();
        wikiRequest.setWikiId(wikiId);
        wikiRequest.setAlias(wikiId);
        wikiRequest.setFailOnExist(failOnExist);
        wikiRequest.setInteractive(false);

        JobExecutor jobExecutor = new JobExecutor();
        JobRequest request = getModelFactory().toRestJobRequest(wikiRequest);
        jobExecutor.execute(WikiCreationJob.JOB_TYPE, request, xwikiRESTURL, credentials);

        return true;
    }

    private boolean exists(String xwikiRESTURL, UsernamePasswordCredentials credentials, String wikiId)
        throws IOException
    {
        HttpClient httpClient = new HttpClient();
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        httpClient.getParams().setAuthenticationPreemptive(true);

        String uri = String.format("%s/wikis/xwiki/spaces/XWiki/pages/XWikiServer%s", xwikiRESTURL,
            StringUtils.capitalize(wikiId));
        GetMethod getMethod = new GetMethod(uri);
        httpClient.executeMethod(getMethod);
        getMethod.releaseConnection();

        return getMethod.getStatusCode() == Status.OK.getStatusCode();
    }

    private ModelFactory getModelFactory() throws Exception
    {
        return this.componentManager.getInstance(ModelFactory.class);
    }
}
