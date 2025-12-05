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

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.xwiki.http.internal.XWikiHTTPClient;
import org.xwiki.http.internal.XWikiCredentials;
import org.xwiki.job.JobException;
import org.xwiki.rest.model.jaxb.JobRequest;
import org.xwiki.rest.model.jaxb.JobStatus;

/**
 * Execute a Job to call the XWiki REST endpoint to install extensions.
 *
 * @version $Id$
 * @since 15.9-rc-1
 * @since 15.5.4
 */
public class JobExecutor
{
    /**
     * @param jobType the type of job to execute
     * @param request the Job request to send
     * @param xwikiRESTURL the XWiki REST URL (e.g. {@code http://localhsot:8080/xwiki/rest})
     * @param credentials the xwiki user and password to use to connect for the REST endpoint
     * @throws Exception if an error occured when connecting to the REST endpoint
     */
    public void execute(String jobType, JobRequest request, String xwikiRESTURL, XWikiCredentials credentials)
        throws Exception
    {
        JAXBContext jaxbContext = JAXBContext.newInstance("org.xwiki.rest.model.jaxb");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Marshaller marshaller = jaxbContext.createMarshaller();

        StringWriter writer = new StringWriter();
        marshaller.marshal(request, writer);

        XWikiHTTPClient httpClient = new XWikiHTTPClient();
        httpClient.setDefaultCredentials(credentials);

        String uri = String.format("%s/jobs?jobType=%s&async=false", xwikiRESTURL, jobType);

        HttpPut putMethod = new HttpPut(uri);
        putMethod.addHeader("Accept", MediaType.APPLICATION_XML);
        putMethod.setEntity(
            new StringEntity(writer.toString(), ContentType.APPLICATION_XML.withCharset(StandardCharsets.UTF_8)));

        httpClient.execute(putMethod, (response, context) -> {
            try {
                handleResponse(response, unmarshaller);
            } catch (Exception e) {
                throw new HttpException("Failed to handle response", e);
            }

            return null;
        });
    }

    private void handleResponse(ClassicHttpResponse response, Unmarshaller unmarshaller) throws Exception
    {
        // Verify results
        // Verify that we got a 200 response
        int statusCode = response.getCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new JobException(String.format("Job execution failed. Response status code [%s], reason [%s]",
                statusCode, EntityUtils.toString(response.getEntity())));
        }

        // Get the job status
        JobStatus jobStatus = (JobStatus) unmarshaller.unmarshal(response.getEntity().getContent());
        if (jobStatus.getErrorMessage() != null && jobStatus.getErrorMessage().length() > 0) {
            throw new JobException(String.format("Job execution failed. Reason [%s]", jobStatus.getErrorMessage()));
        }
    }
}
