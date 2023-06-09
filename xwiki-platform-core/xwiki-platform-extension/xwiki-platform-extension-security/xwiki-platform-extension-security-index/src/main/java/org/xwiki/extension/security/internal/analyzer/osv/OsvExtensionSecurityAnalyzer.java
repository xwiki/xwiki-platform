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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.index.internal.security.ExtensionAnalysisResult;
import org.xwiki.extension.index.internal.security.SecurityIssueDescriptor;
import org.xwiki.extension.security.analyzer.ExtensionSecurityAnalyzer;
import org.xwiki.extension.security.internal.ExtensionSecurityException;
import org.xwiki.extension.security.internal.analyzer.osv.model.PackageObject;
import org.xwiki.extension.security.internal.analyzer.osv.model.QueryObject;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.AffectObject;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.OsvResponse;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.RangeObject;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.VulnObject;

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

    @Inject
    private Logger logger;

    @Override
    public ExtensionAnalysisResult analyze(Extension extension) throws ExtensionSecurityException
    {
        String version = extension.getId().getVersion().getValue();
        String extensionId = extension.getId().getId();

        ObjectMapper objectMapper = new ObjectMapper();
        QueryObject queryObject = new QueryObject()
            .setPackage(new PackageObject()
                .setEcosystem("Maven")
                .setName(extensionId));
        boolean isPlatform = extensionId.startsWith(PLATFORM_PREFIX);
        if (!isPlatform) {
            // We currently have an issue regarding versions resolution of packages from xwiki-platform, because they 
            // are not published on maven central. Hence, we only filter explicitly by version for other group ids.
            queryObject.setVersion(version);
        } 
        try {
            String body = objectMapper
                .setSerializationInclusion(NON_NULL)
                .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                .writeValueAsString(queryObject);

            // TODO: allow to configure the actual url? 
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.osv.dev/v1/query"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpClient client = HttpClient.newHttpClient();
            
            HttpResponse<String> response = client.send(request, ofString());
            
            // TODO: handle responses with error.
            OsvResponse osvResponse = objectMapper.readValue(response.body(), OsvResponse.class);
            List<VulnObject> matchingVulns = new ArrayList<>();
            if (osvResponse.getVulns() != null && !osvResponse.getVulns().isEmpty()) {
                osvResponse.getVulns()
                    // TODO: remove: the 13.10 hack is to simulate having issues on the current version.
                    .forEach(vuln -> analyzeVuln(extensionId, isPlatform ? "13.10" : version, vuln).ifPresent(
                        matchingVulns::add));
            }
            return new ExtensionAnalysisResult().setResults(
                matchingVulns.stream().map(this::convert).collect(Collectors.toList()));
        } catch (JsonProcessingException e) {
            throw new ExtensionSecurityException(
                String.format("Failed to build the json for [%s/+%s]", extensionId, version),
                e);
        } catch (IOException e) {
            // TODO: throw exception instead?
            this.logger.warn("Failed to send a request for [{}/{}]. Cause: [{}]", extension, version,
                getRootCauseMessage(e));
            return new ExtensionAnalysisResult().setException(e);
        } catch (InterruptedException e) {
            // TODO
            throw new RuntimeException(e);
        }
    }

    private SecurityIssueDescriptor convert(VulnObject vulnObject)
    {
        // TODO: convert the osv specific vuln object to a generic security issue descriptor
        return new SecurityIssueDescriptor()
            .setId(vulnObject.getId())
            .setURL(vulnObject.getMainURL())
            .setSeverityScore(vulnObject.getSeverityCCSV3());
    }

    private Optional<VulnObject> analyzeVuln(String mavenId, String version, VulnObject vuln)
    {
        Optional<VulnObject> rvuln = Optional.empty();
        boolean isPlatform = mavenId.startsWith(PLATFORM_PREFIX);
        if (!isPlatform || isMatchesOneRange(mavenId, version, vuln)) {
            rvuln = Optional.of(vuln);
        }
        return rvuln;
    }

    private boolean isMatchesOneRange(String mavenId, String version, VulnObject vuln)
    {
        boolean matchesOneRange = false;
        List<AffectObject> affectedList = vuln.getAffected();
        for (AffectObject affected : affectedList) {
            if (Objects.equals(affected.getPackage().getName(), mavenId)) {
                matchesOneRange = checkRanges(version, affected);
            }

            if (matchesOneRange) {
                break;
            }
        }
        return matchesOneRange;
    }

    private boolean checkRanges(String version, AffectObject affected)
    {
        boolean matchesOneRange = false;
        if (!affected.getRanges().isEmpty()) {
            List<RangeObject> ranges = affected.getRanges();
            matchesOneRange = ranges.stream().anyMatch(range -> {
                String start = range.getStart();
                String end = range.getEnd();
                return isInRange(start, end, version);
            });
        }
        return matchesOneRange;
    }

    private boolean isInRange(Object introduced, Object fixed, String version)
    {
        String range = String.format("[%s,%s]", introduced, fixed == null ? "" : fixed);
        try {
            return VersionRange.createFromVersionSpec(range).containsVersion(new DefaultArtifactVersion(version));
        } catch (InvalidVersionSpecificationException e) {
            this.logger.warn("Failed to analyze range [{}]. Cause: [{}]", range, getRootCauseMessage(e));
            return false;
        }
    }
}
