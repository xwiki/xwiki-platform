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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.index.security.ExtensionSecurityAnalysisResult;
import org.xwiki.extension.index.security.SecurityVulnerabilityDescriptor;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.AffectObject;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.OsvResponse;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.RangeObject;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.VulnObject;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.DefaultVersion;

import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Analyze the provided {@link OsvResponse} and return an {@link ExtensionSecurityAnalysisResult}.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component(roles = OsvResponseAnalyzer.class)
@Singleton
public class OsvResponseAnalyzer
{
    @Inject
    private Logger logger;

    /**
     * Analyze the provided {@link OsvResponse} and return an {@link ExtensionSecurityAnalysisResult}.
     *
     * @param extensionId the id of the analyzed extension
     * @param version the version of the extension
     * @param osvResponse the osv response to analyze
     * @return a service-agnostic object synthesizing the vulnerabilities found for the extension
     */
    public ExtensionSecurityAnalysisResult analyzeOsvResponse(String extensionId, String version,
        OsvResponse osvResponse)
    {
        List<VulnObject> matchingVulns = new ArrayList<>();
        if (osvResponse.getVulns() != null && !osvResponse.getVulns().isEmpty()) {
            osvResponse.getVulns()
                .forEach(vulnerability -> analyzeVulnerability(extensionId, version,
                    vulnerability)
                    .ifPresent(matchingVulns::add));
        }

        return new ExtensionSecurityAnalysisResult()
            .setResults(
                matchingVulns.stream().flatMap(vulnObject -> convert(vulnObject, new DefaultVersion(version)).stream())
                    .collect(Collectors.toList()));
    }

    private Optional<SecurityVulnerabilityDescriptor> convert(VulnObject vulnObject, Version currentVersion)
    {
        return resolveId(vulnObject)
            .map(id -> new SecurityVulnerabilityDescriptor()
                .setId(id)
                .setAliases(resolveAliases(vulnObject, id))
                .setURL(vulnObject.getMainURL())
                .setSeverityScore(vulnObject.getSeverityCCSV3())
                .setFixVersion(vulnObject.getMaxFixVersion(currentVersion).orElse(null)));
    }

    private Set<String> resolveAliases(VulnObject vulnObject, String id)
    {
        Set<String> aliases = new HashSet<>();
        aliases.add(vulnObject.getId());
        aliases.addAll(vulnObject.getAliases());
        aliases.remove(id);
        return aliases;
    }

    /**
     * Resolve the ID of the provided {@link VulnObject} by looking for an alias starting with "CVE-".
     *
     * @param vulnObject the vulnerability object
     * @return an alias starting with "CVE-", or the original ID if no appropriate alias is found
     */
    private Optional<String> resolveId(VulnObject vulnObject)
    {
        Optional<String> first = vulnObject.getAliases()
            .stream()
            .filter(it -> startsWith(it, "CVE-"))
            .findFirst();
        if (first.isEmpty()) {
            // We fall back to the first id if no CVE id is found.
            first = vulnObject.getAliases().stream().findFirst();
        }
        return first;
    }

    private Optional<VulnObject> analyzeVulnerability(String mavenId, String version, VulnObject vuln)
    {
        Optional<VulnObject> rvuln = Optional.empty();
        boolean isPlatform = OsvExtensionSecurityAnalyzer.isNotOnMavenCentral(mavenId);
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
