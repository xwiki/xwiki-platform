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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.extension.index.security.ExtensionSecurityAnalysisResult;
import org.xwiki.extension.index.security.SecurityVulnerabilityDescriptor;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.AffectObject;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.EventObject;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.OsvResponse;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.RangeObject;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.SeverityObject;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.VulnObject;
import org.xwiki.extension.security.internal.analyzer.osv.model.response.VulnReferenceObject;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

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
    void analyzeOsvResponse()
    {
        OsvResponse osvResponse = new OsvResponse();
        VulnObject vulnObject = new VulnObject();
        AffectObject affectObject = new AffectObject();
        RangeObject rangeObject = new RangeObject();
        EventObject eventObject = new EventObject();
        eventObject.setFixed("15.7");
        rangeObject.setEvents(List.of(eventObject));
        affectObject.setRanges(List.of(rangeObject));
        vulnObject.setAffected(List.of(affectObject));
        vulnObject.setId("VULN_ID");
        VulnReferenceObject webReference = new VulnReferenceObject();
        webReference.setType("WEB");
        webReference.setUrl("https://main.ref/");
        vulnObject.setReferences(List.of(webReference));
        SeverityObject severityObject = new SeverityObject();
        severityObject.setType("CVSS_V3");
        severityObject.setScore("CVSS:3.0/AV:N/AC:L/PR:H/UI:R/S:C/C:H/I:L/A:L");
        vulnObject.setSeverity(List.of(severityObject));
        osvResponse.setVulns(List.of(vulnObject));
        ExtensionSecurityAnalysisResult expected = new ExtensionSecurityAnalysisResult();
        SecurityVulnerabilityDescriptor securityVulnerabilityDescriptor = new SecurityVulnerabilityDescriptor();
        securityVulnerabilityDescriptor.setId("VULN_ID");
        securityVulnerabilityDescriptor.setURL("https://main.ref/");
        securityVulnerabilityDescriptor.setScore(7.5);
        securityVulnerabilityDescriptor.setFixVersion(new DefaultVersion("15.7"));
        expected.setResults(List.of(securityVulnerabilityDescriptor));
        expected.setAdvice("extension.security.analysis.advice.upgradeFromEM");

        assertEquals(expected,
            this.analyzer.analyzeOsvResponse("org.test:my-ext", "7.5", osvResponse));
    }
}
