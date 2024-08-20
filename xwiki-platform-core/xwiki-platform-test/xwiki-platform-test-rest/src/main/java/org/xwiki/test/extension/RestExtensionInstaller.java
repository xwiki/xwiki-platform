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
package org.xwiki.test.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.JobRequest;
import org.xwiki.test.integration.maven.MavenResolver;

/**
 * Test helper to install a set of extensions from a rest endpoint.
 *
 * @version $Id$
 * @since 15.9RC1
 * @since 15.5.4
 */
public class RestExtensionInstaller
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RestExtensionInstaller.class);

    private static final String SEPARATOR = "/";

    private final ComponentManager componentManager;

    private final MavenResolver mavenResolver;

    /**
     * Initializes a new instance of the RestExtensionInstaller class with the specified component manager and no maven
     * resolver.
     *
     * @param componentManager The component manager to use for managing extensions.
     */
    public RestExtensionInstaller(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
        this.mavenResolver = null;
    }

    /**
     * Initializes a new instance of the RestExtensionInstaller class with the specified component manager and maven
     * resolver.
     *
     * @param componentManager The component manager to use for managing extensions.
     * @param mavenResolver The maven resolver to use for resolving dependencies.
     */
    public RestExtensionInstaller(ComponentManager componentManager, MavenResolver mavenResolver)
    {
        this.componentManager = componentManager;
        this.mavenResolver = mavenResolver;
    }

    /**
     * @param baseUrl the base url of the current xwiki instance
     * @param extensions the extensions to install
     * @param credentials the xwiki user and password to use to connect for the REST endpoint
     * @param installUserReference the reference to the user who will the user under which pages are installed (e.g.
     *     {@code superadmin})
     * @param namespaces the wikis in which to install the extensions (e.g. {@code wiki:xwiki} for the main wiki).
     *     If null they'll be installed in the main wiki
     * @param failOnExist true if the install should fail if one of the extension is already install on one of the
     *     namespaces
     * @throws Exception if there's a failure to install the extensions in the running XWiki instance
     */
    public void installExtensions(String baseUrl, Collection<ExtensionId> extensions,
        UsernamePasswordCredentials credentials,
        String installUserReference, List<String> namespaces, boolean failOnExist) throws Exception
    {
        String xwikiRESTURL = baseUrl;
        if (!baseUrl.endsWith(SEPARATOR)) {
            xwikiRESTURL += SEPARATOR;
        }
        xwikiRESTURL += "rest";

        // Resolve the extensions versions if needed
        List<ExtensionId> resolvedExtensions = new ArrayList<>(extensions.size());
        for (ExtensionId extensionId : extensions) {
            LOGGER.info("Getting extension id: {}", extensionId);
            String version;
            if (extensionId.getVersion() == null && this.mavenResolver != null) {
                // TODO: search the version of the extension in the dependency tree
                version = this.mavenResolver.getModelFromCurrentPOM().getVersion();
            } else if (this.mavenResolver != null) {
                version = this.mavenResolver.replacePropertiesFromCurrentPOM(extensionId.getVersion().getValue());
            } else {
                version = extensionId.getVersion().getValue();
            }

            resolvedExtensions.add(new ExtensionId(extensionId.getId(), version));
            LOGGER.info("End getting extension id: {}", extensionId);
        }

        // Install the extensions
        try {
            installExtensionsInternal(xwikiRESTURL, resolvedExtensions, credentials, installUserReference, namespaces,
                failOnExist);
        } catch (Exception e) {
            throw new Exception(String.format("Failed to install Extension(s) into XWiki at [%s]", xwikiRESTURL), e);
        }
    }

    private void installExtensionsInternal(String xwikiRESTURL, Collection<ExtensionId> extensions,
        UsernamePasswordCredentials credentials, String installUserReference, List<String> namespaces,
        boolean failOnExist) throws Exception
    {
        InstallRequest installRequest = new InstallRequest();

        // Set a job id to save the job result
        installRequest.setId("extension", "provision", UUID.randomUUID().toString());

        installRequest.setInteractive(false);
        installRequest.setFailOnExist(failOnExist);

        // Set the extension list to install
        for (ExtensionId extensionId : extensions) {
            if (CollectionUtils.isNotEmpty(namespaces)) {
                LOGGER.info("...Adding extension [{}] to the list of extensions to provision on namespaces {}...",
                    extensionId, namespaces);
            } else {
                LOGGER.info("...Adding extension [{}] to the list of extensions to provision...", extensionId);
            }
            installRequest.addExtension(extensionId);
        }

        // Set the namespaces into which to install the extensions
        if (namespaces == null || namespaces.isEmpty()) {
            installRequest.addNamespace("wiki:xwiki");
        } else {
            for (String namespace : namespaces) {
                installRequest.addNamespace(namespace);
            }
        }

        // Set any user for installing pages (if defined)
        if (installUserReference != null) {
            installRequest.setProperty("user.reference", new DocumentReference("xwiki", "XWiki", "superadmin"));
        }

        JobExecutor jobExecutor = new JobExecutor();
        JobRequest request = getModelFactory().toRestJobRequest(installRequest);
        LOGGER.info("Job request: {}", request);
        jobExecutor.execute(InstallJob.JOBTYPE, request, xwikiRESTURL, credentials);
        LOGGER.info("End job request: {}", request);
    }

    private ModelFactory getModelFactory() throws ComponentLookupException
    {
        return this.componentManager.getInstance(ModelFactory.class);
    }
}
