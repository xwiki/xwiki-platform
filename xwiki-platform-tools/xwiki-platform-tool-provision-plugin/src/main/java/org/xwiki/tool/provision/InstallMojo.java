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
package org.xwiki.tool.provision;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionId;
import org.xwiki.http.internal.XWikiCredentials;
import org.xwiki.http.internal.XWikiHTTPClient;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.JobRequest;
import org.xwiki.rest.model.jaxb.JobStatus;

/**
 * Maven2 plugin to install one or several extensions into a running XWiki instance. This is useful for example
 * when provisioning an XWiki instance for executing functional tests.
 *
 * Example usage:
 *
 * <pre><code>
 * &#60;plugin&#62;
 *   &#60;groupId&#62;org.xwiki.platform&#60;/groupId&#62;
 *   &#60;artifactId&#62;xwiki-platform-tool-provision-plugin&#60;/artifactId&#62;
 *   &#60;version&#62;...version...&#60;/version&#62;
 *   &#60;configuration&#62;
 *     &#60;username&#62;Admin&#60;/username&#62;
 *     &#60;password&#62;admin&#60;/password&#62;
 *     &#60;extensionIds&#62;
 *       &#60;extensionId&#62;
 *         &#60;id&#62;org.xwiki.contrib.markdown:syntax-markdown-markdown12&#60;/id&#62;
 *         &#60;version&#62;8.5.1&#60;/version&#62;
 *       &#60;/extensionId&#62;
 *     &#60;/extensionIds&#62;
 *   &#60;/configuration&#62;
 *   &#60;executions&#62;
 *     &#60;execution&#62;
 *       &#60;id&#62;install&#60;/id&#62;
 *       &#60;goals&#62;
 *         &#60;goal&#62;install&#60;/goal&#62;
 *       &#60;/goals&#62;
 *     &#60;/execution&#62;
 *   &#60;/executions&#62;
 * &#60;/plugin&#62;
 * </code></pre>
 *
 * @version $Id$
 * @since 10.0
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class InstallMojo extends AbstractMojo
{
    /**
     * The REST URL to the running XWiki instance.
     */
    @Parameter(defaultValue = "http://localhost:8080/xwiki/rest", required = true)
    private String xwikiRESTURL;

    /**
     * The user name for the user under which the install will be done in the running XWiki instance.
     */
    @Parameter(defaultValue = "superadmin", required = true)
    private String username;

    /**
     * The password for the user under which the install will be done in the running XWiki instance.
     */
    @Parameter(defaultValue = "pass", required = true)
    private String password;

    /**
     * The namespaces in which the Extensions will be installed. If not specified then the Extensions will be installed
     * in the main wiki (i.e. in {@code wiki:xwiki}.
     */
    @Parameter(required = false)
    private List<String> namespaces;

    /**
     * List of extensions to install in the running XWiki instance.
     */
    @Parameter
    private List<ExtensionId> extensionIds;

    /**
     * If defined, all pages will be installed under that user.
     */
    @Parameter(required = false)
    private String installUserReference;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try {
            JAXBContext context = JAXBContext.newInstance("org.xwiki.rest.model.jaxb");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Marshaller marshaller = context.createMarshaller();

            try (XWikiHTTPClient httpClient = new XWikiHTTPClient()) {
                httpClient.setDefaultCredentials(new XWikiCredentials(this.username, this.password));

                installExtensions(marshaller, unmarshaller, httpClient);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to install Extension(s) into XWiki", e);
        }
    }

    private void addExtensions(InstallRequest installRequest)
    {
        for (ExtensionId extensionId : this.extensionIds) {
            org.xwiki.extension.ExtensionId extId =
                new org.xwiki.extension.ExtensionId(extensionId.getId(), extensionId.getVersion());
            getLog().info(String.format("Installing extension [%s]...", extId));
            installRequest.addExtension(extId);
        }
    }

    private void installExtensions(Marshaller marshaller, Unmarshaller unmarshaller, XWikiHTTPClient httpClient)
        throws Exception
    {
        InstallRequest installRequest = new InstallRequest();

        // Set a job id to save the job result
        installRequest.setId("extension", "provision", UUID.randomUUID().toString());

        installRequest.setInteractive(false);

        // Set the extension list to install
        addExtensions(installRequest);

        // Set the namespaces into which to install the extensions
        if (this.namespaces == null || this.namespaces.isEmpty()) {
            installRequest.addNamespace("wiki:xwiki");
        } else {
            for (String namespace : this.namespaces) {
                installRequest.addNamespace(namespace);
            }
        }

        // Set any user for installing pages (if defined)
        if (this.installUserReference != null) {
            installRequest.setProperty("user.reference", new DocumentReference("xwiki", "XWiki", "superadmin"));
        }

        JobRequest request = getModelFactory().toRestJobRequest(installRequest);

        String uri = String.format("%s/jobs?jobType=install&async=false", this.xwikiRESTURL);

        StringWriter writer = new StringWriter();
        marshaller.marshal(request, writer);

        httpClient.executePut(uri, new StringEntity(writer.toString(), ContentType.APPLICATION_XML),
            (response, context) -> installExtensions(response, unmarshaller));
    }

    private Void installExtensions(ClassicHttpResponse response, Unmarshaller unmarshaller)
        throws HttpException, IOException
    {
        // Verify results
        // Verify that we got a 200 response
        int statusCode = response.getCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new HttpException(String.format("Job execution failed. Response status code [%s], reason [%s]",
                statusCode, EntityUtils.toString(response.getEntity())));
        }

        // Get the job status
        try (InputStream stream = response.getEntity().getContent()) {
            JobStatus jobStatus;
            try {
                jobStatus = (JobStatus) unmarshaller.unmarshal(stream);
            } catch (JAXBException e) {
                throw new HttpException("Failed to parse the response body", e);
            }
            if (jobStatus.getErrorMessage() != null && !jobStatus.getErrorMessage().isEmpty()) {
                throw new HttpException(
                    String.format("Job execution failed. Reason [%s]", jobStatus.getErrorMessage()));
            }
        }

        return null;
    }

    private ModelFactory getModelFactory() throws Exception
    {
        // Initialize XWiki Component system
        EmbeddableComponentManager ecm = new EmbeddableComponentManager();
        ecm.initialize(this.getClass().getClassLoader());

        return ecm.getInstance(ModelFactory.class);
    }
}
