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
package org.xwiki.test.docker.junit5;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.restlet.data.MediaType;
import org.xwiki.rest.model.jaxb.JobRequest;
import org.xwiki.rest.model.jaxb.JobStatus;

/**
 * Execute a Job to call the XWiki REST endpoint to install extensions.
 *
 * @version $Id$
 * @since 10.9
 */
public class JobExecutor
{
    /**
     * @param request the Job request to send
     * @param xwikiRESTURL the XWiki REST URL (e.g. {@code http://localhsot:8080/xwiki/rest})
     * @param credentials the xwiki user and password to use to connect for the REST endpoint
     * @throws Exception if an error occured when connecting to the REST endpoint
     */
    public void execute(JobRequest request, String xwikiRESTURL, UsernamePasswordCredentials credentials)
        throws Exception
    {
        JAXBContext context = JAXBContext.newInstance("org.xwiki.rest.model.jaxb");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Marshaller marshaller = context.createMarshaller();

        HttpClient httpClient = new HttpClient();
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        httpClient.getParams().setAuthenticationPreemptive(true);

        String uri = String.format("%s/jobs?jobType=install&async=false", xwikiRESTURL);
        PutMethod putMethod = executePutXml(uri, request, marshaller, httpClient);

        // Verify results
        // Verify that we got a 200 response
        int statusCode = putMethod.getStatusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new Exception(
                String.format("Job execution failed. Response status code [%s], reason [%s]", statusCode,
                    putMethod.getResponseBodyAsString()));
        }

        // Get the job status
        JobStatus jobStatus = (JobStatus) unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());
        if (jobStatus.getErrorMessage() != null && jobStatus.getErrorMessage().length() > 0) {
            throw new Exception(
                String.format("Job execution failed. Reason [%s]", jobStatus.getErrorMessage()));
        }

        // Release connection
        putMethod.releaseConnection();
    }

    private PutMethod executePutXml(String uri, Object object, Marshaller marshaller, HttpClient httpClient)
        throws Exception
    {
        PutMethod putMethod = new PutMethod(uri);
        putMethod.addRequestHeader("Accept", MediaType.APPLICATION_XML.toString());

        StringWriter writer = new StringWriter();
        marshaller.marshal(object, writer);

        RequestEntity entity =
            new StringRequestEntity(writer.toString(), MediaType.APPLICATION_XML.toString(), "UTF-8");
        putMethod.setRequestEntity(entity);

        httpClient.executeMethod(putMethod);

        return putMethod;
    }
}
