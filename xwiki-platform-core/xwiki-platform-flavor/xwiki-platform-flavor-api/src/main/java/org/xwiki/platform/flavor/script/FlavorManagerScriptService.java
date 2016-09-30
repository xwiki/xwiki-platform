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
package org.xwiki.platform.flavor.script;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.script.AbstractExtensionScriptService;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.platform.flavor.FlavorManager;
import org.xwiki.platform.flavor.FlavorQuery;
import org.xwiki.platform.flavor.internal.job.FlavorSearchJob;
import org.xwiki.platform.flavor.internal.job.FlavorSearchStatus;
import org.xwiki.platform.flavor.job.FlavorSearchRequest;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.resources.job.JobStatusResource;
import org.xwiki.rest.url.ParametrizedRestURLGenerator;
import org.xwiki.stability.Unstable;

/**
 * Script service to find flavors.
 * 
 * @version $Id$
 * @since 7.1M2
 */
@Component
@Named(FlavorManagerScriptService.ROLEHINT)
@Singleton
@Unstable
public class FlavorManagerScriptService extends AbstractExtensionScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ROLEHINT = "flavor";

    /**
     * The id to use in the jobs searching for flavors.
     */
    public static final String SEARCH_ID = "search";

    @Inject
    private FlavorManager flavorManager;

    @Inject
    @Named(JobStatusResource.NAME)
    private ParametrizedRestURLGenerator<List<String>> jobstatusURLGenerator;

    private List<String> getSearchJobId(String namespace)
    {
        return Arrays.asList(ROLEHINT, SEARCH_ID, namespace);
    }

    /**
     * Creates a flavor query.
     * 
     * @return a new flavor query
     */
    public FlavorQuery createFlavorQuery()
    {
        return new FlavorQuery();
    }

    /**
     * Creates a flavor query.
     * 
     * @param query the query to execute
     * @return a new flavor query
     */
    public FlavorQuery createFlavorQuery(String query)
    {
        return new FlavorQuery(query);
    }

    /**
     * Get all flavors matching a query.
     * 
     * @param query query to execute
     * @return flavors matching the query
     * @deprecated since 8.0RC1, use {@link #searchFlavors(FlavorQuery)} instead
     */
    @Deprecated
    public IterableResult<Extension> getFlavors(FlavorQuery query)
    {
        setError(null);

        try {
            return this.flavorManager.getFlavors(query);
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * Search for all flavors matching a query.
     * 
     * @param query query to execute
     * @return flavors matching the query
     * @since 8.0RC1
     */
    public IterableResult<Extension> searchFlavors(FlavorQuery query)
    {
        setError(null);

        try {
            return this.flavorManager.searchFlavors(query);
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * @param namespace the namespace where to validate the flavors
     * @return the status of the current or last flavor search
     * @since 8.0
     */
    public FlavorSearchStatus getSearchValidFlavorsStatus(String namespace)
    {
        return (FlavorSearchStatus) getJobStatus(getSearchJobId(namespace));
    }

    /**
     * @return the status of the current or last flavor search
     * @since 8.0
     */
    public FlavorSearchStatus getSearchValidFlavorsStatus()
    {
        return getSearchValidFlavorsStatus(currentNamespace());
    }

    /**
     * @param namespace the namespace where to validate the flavors
     * @return the REST URL to access the job status
     * @since 8.0
     */
    public String getSearchValidFlavorsStatusURL(String namespace)
    {
        setError(null);

        try {
            return this.jobstatusURLGenerator.getURL(getSearchJobId(namespace)).getPath();
        } catch (XWikiRestException e) {
            setError(e);
        }

        return null;
    }

    /**
     * @return the valid flavors found in the currently running job status
     */
    public List<Extension> getValidExtensions()
    {
        FlavorSearchStatus status = getSearchValidFlavorsStatus();

        return status != null ? status.getFlavors() : null;
    }

    private String currentNamespace()
    {
        return "wiki:" + this.xcontextProvider.get().getWikiId();
    }

    /**
     * @return the REST URL to access the job status
     * @since 8.0
     */
    public String getSearchValidFlavorsStatusURL()
    {
        return getSearchValidFlavorsStatusURL(currentNamespace());
    }

    /**
     * Start searching for valid flavors.
     * 
     * @param namespace the namespace where to validate the flavors
     * @return the {@link Job} searching the flavors
     * @since 8.0RC1
     */
    public Job searchValidFlavors(String namespace)
    {
        setError(null);

        Job job = null;
        try {
            FlavorSearchRequest flavorRequest = new FlavorSearchRequest();

            flavorRequest.setId(getSearchJobId(namespace));
            flavorRequest.addNamespace(namespace);

            setRightsProperties(flavorRequest);

            job = this.jobExecutor.execute(FlavorSearchJob.JOBTYPE, flavorRequest);
        } catch (JobException e) {
            setError(e);
        }

        return job;
    }

    /**
     * Start searching for valid flavors in the context of the current wiki.
     * 
     * @return the {@link Job} searching the flavors
     * @since 8.0
     */
    public Job searchValidFlavors()
    {
        return searchValidFlavors(currentNamespace());
    }

    /**
     * Get the flavor installed on a given wiki.
     * 
     * @param wikiId id of the wiki
     * @return the id of the flavor installed on the given wiki or null if there is no flavor installed
     */
    public ExtensionId getFlavorOfWiki(String wikiId)
    {
        return this.flavorManager.getFlavorOfWiki(wikiId);
    }
}
