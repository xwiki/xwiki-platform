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
package org.xwiki.extension.security.internal.analyzer.osv;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.index.security.ExtensionSecurityAnalysisResult;
import org.xwiki.extension.security.ExtensionSecurityConfiguration;
import org.xwiki.extension.security.analyzer.ExtensionSecurityAnalyzer;
import org.xwiki.extension.security.internal.ExtensionSecurityException;
import org.xwiki.extension.security.internal.analyzer.osv.model.PackageObject;
import org.xwiki.extension.security.internal.analyzer.osv.model.QueryObject;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.OsvResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Extension security analyzed based on <a href="https://osv.dev/">osv.dev</a> Rest API.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
@Named(OsvExtensionSecurityAnalyzer.ID)
public class OsvExtensionSecurityAnalyzer implements ExtensionSecurityAnalyzer
{
    /**
     * The id of this extension.
     */
    public static final String ID = "osv";

    private static final String PLATFORM_PREFIX = "org.xwiki.platform:";

    private static final String CONTRIB_PREFIX = "org.xwiki.contrib";

    @Inject
    private Logger logger;

    @Inject
    private ExtensionSecurityConfiguration extensionSecurityConfiguration;

    @Inject
    private OsvResponseAnalyzer osvResponseAnalyzer;

    @Override
    public ExtensionSecurityAnalysisResult analyze(Extension extension) throws ExtensionSecurityException
    {
        String version = extension.getId().getVersion().getValue();
        String extensionId = extension.getId().getId();

        ObjectMapper objectMapper = new ObjectMapper();
        QueryObject queryObject = new QueryObject()
            .setPackage(new PackageObject()
                .setEcosystem("Maven")
                .setName(extensionId));
        boolean isNotOnMavenCentralResult = isNotOnMavenCentral(extensionId);
        if (!isNotOnMavenCentralResult) {
            // We currently have an issue regarding versions resolution of packages from xwiki-platform, because they 
            // are not published on maven central. Hence, we only filter explicitly by version for other group ids.
            queryObject.setVersion(version);
        }
        try {
            String body = objectMapper
                .setSerializationInclusion(NON_NULL)
                .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                .writeValueAsString(queryObject);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.extensionSecurityConfiguration.getScanURL()))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, ofString());
            OsvResponse osvResponse = objectMapper.readValue(response.body(), OsvResponse.class);
            return this.osvResponseAnalyzer.analyzeOsvResponse(extensionId, version, osvResponse);
        } catch (JsonProcessingException e) {
            throw new ExtensionSecurityException(
                String.format("Failed to build the json for [%s/+%s]", extensionId, version), e);
        } catch (IOException e) {
            throw new ExtensionSecurityException(
                String.format("Failed then the query for [%s/+%s]", extensionId, version), e);
        } catch (InterruptedException e) {
            this.logger.warn("Can't finish the analysis as the thread was interrupted. Cause: [{}]",
                getRootCauseMessage(e));
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Checks if the given extension ID is not available on Maven Central, based on its extension id. This test is
     * useful because extensions that are not on maven central do not yield any results when requested with a version.
     * Consequently, we need to request them without a version and filter out the relevant vulnerabilities ourselves.
     *
     * @param extensionId the ID of the extension to check
     * @return {@code true} if the extension is not available on Maven Central, {@code false} otherwise
     */
    static boolean isNotOnMavenCentral(String extensionId)
    {
        return extensionId.startsWith(PLATFORM_PREFIX) || extensionId.startsWith(CONTRIB_PREFIX);
    }
}
