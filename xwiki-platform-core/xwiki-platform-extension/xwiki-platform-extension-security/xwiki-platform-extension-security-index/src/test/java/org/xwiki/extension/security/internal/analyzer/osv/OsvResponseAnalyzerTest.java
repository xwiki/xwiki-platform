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
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.extension.index.security.ExtensionSecurityAnalysisResult;
import org.xwiki.extension.index.security.SecurityVulnerabilityDescriptor;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.OsvResponse;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of {@link OsvResponseAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class OsvResponseAnalyzerTest
{
    @InjectMockComponents
    private OsvResponseAnalyzer analyzer;

    @Test
    void analyzeOsvResponseEmpty()
    {
        ExtensionSecurityAnalysisResult expected = new ExtensionSecurityAnalysisResult();
        expected.setResults(List.of());
        assertEquals(expected,
            this.analyzer.analyzeOsvResponse("org.test:my-ext", "7.5", new OsvResponse()));
    }

    @Test
    void analyzeOsvResponseOrgXWikiPlatform()
    {
        OsvResponse osvResponse = readJson("analyzeOsvResponseOrgXWikiPlatform.json");

        ExtensionSecurityAnalysisResult expected = new ExtensionSecurityAnalysisResult();
        SecurityVulnerabilityDescriptor securityVulnerabilityDescriptor0 = new SecurityVulnerabilityDescriptor();
        securityVulnerabilityDescriptor0.setId("CVE-2023-29510");
        securityVulnerabilityDescriptor0.setAliases(Set.of("GHSA-4v38-964c-xjmw"));
        securityVulnerabilityDescriptor0.setURL(
            "https://github.com/xwiki/xwiki-platform/security/advisories/GHSA-4v38-964c-xjmw");
        securityVulnerabilityDescriptor0.setScore(9.9);
        securityVulnerabilityDescriptor0.setFixVersion(new DefaultVersion("14.10.2"));
        SecurityVulnerabilityDescriptor securityVulnerabilityDescriptor1 = new SecurityVulnerabilityDescriptor();
        securityVulnerabilityDescriptor1.setId("CVE-2023-29514");
        securityVulnerabilityDescriptor1.setAliases(Set.of("GHSA-9j36-3cp4-rh4j"));
        securityVulnerabilityDescriptor1.setURL(
            "https://github.com/xwiki/xwiki-platform/security/advisories/GHSA-9j36-3cp4-rh4j");
        securityVulnerabilityDescriptor1.setScore(9.9);
        securityVulnerabilityDescriptor1.setFixVersion(new DefaultVersion("13.10.11"));
        SecurityVulnerabilityDescriptor securityVulnerabilityDescriptor2 = new SecurityVulnerabilityDescriptor();
        securityVulnerabilityDescriptor2.setId("CVE-2023-29511");
        securityVulnerabilityDescriptor2.setAliases(Set.of("GHSA-rfh6-mg6h-h668"));
        securityVulnerabilityDescriptor2.setURL(
            "https://github.com/xwiki/xwiki-platform/security/advisories/GHSA-rfh6-mg6h-h668");
        securityVulnerabilityDescriptor2.setScore(9.9);
        securityVulnerabilityDescriptor2.setFixVersion(new DefaultVersion("13.10.11"));
        expected.setResults(List.of(
            securityVulnerabilityDescriptor0,
            securityVulnerabilityDescriptor1,
            securityVulnerabilityDescriptor2
        ));

        assertEquals(expected,
            this.analyzer.analyzeOsvResponse("org.xwiki.platform:xwiki-platform-administration-ui", "13.10",
                osvResponse));
    }

    @Test
    void analyzeOsvResponse()
    {
        OsvResponse osvResponse = readJson("analyzeOsvResponse.json");

        ExtensionSecurityAnalysisResult expected = new ExtensionSecurityAnalysisResult();
        SecurityVulnerabilityDescriptor securityVulnerabilityDescriptor = new SecurityVulnerabilityDescriptor();
        securityVulnerabilityDescriptor.setId("CVE-1");
        securityVulnerabilityDescriptor.setAliases(Set.of("A1", "VULN_ID"));
        securityVulnerabilityDescriptor.setURL("https://main.ref/");
        securityVulnerabilityDescriptor.setScore(7.5);
        securityVulnerabilityDescriptor.setFixVersion(new DefaultVersion("15.7"));
        expected.setResults(List.of(securityVulnerabilityDescriptor));

        assertEquals(expected,
            this.analyzer.analyzeOsvResponse("org.test:my-ext", "7.5", osvResponse));
    }

    @Test
    void fixVersion()
    {
        OsvResponse osvResponse = readJson("org.apache.hadoop-hadoop-common-3.2.2.json");
        ExtensionSecurityAnalysisResult extensionSecurityAnalysisResult =
            this.analyzer.analyzeOsvResponse("org.apache.hadoop:hadoop-common", "3.2.2", osvResponse);
        assertEquals(new DefaultVersion("3.2.4"),
            extensionSecurityAnalysisResult.getSecurityVulnerabilities().get(0).getFixVersion());
    }

    private OsvResponse readJson(String name)
    {
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(name);
        try {
            return new ObjectMapper()
                .setSerializationInclusion(NON_NULL)
                .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(resourceAsStream, OsvResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
