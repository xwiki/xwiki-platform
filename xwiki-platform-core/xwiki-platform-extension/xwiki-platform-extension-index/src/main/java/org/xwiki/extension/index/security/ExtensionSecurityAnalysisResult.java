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
package org.xwiki.extension.index.security;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

import static java.util.Comparator.comparingDouble;

/**
 * Store the known security vulnerabilities for a given extension.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Unstable
public class ExtensionSecurityAnalysisResult
{
    private List<SecurityVulnerabilityDescriptor> securityVulnerabilities;

    private String advice;

    private boolean coreExtension;

    /**
     * @param securityVulnerabilities the security vulnerabilities associated with the analyzed extension
     * @return the current object
     */
    public ExtensionSecurityAnalysisResult setResults(List<SecurityVulnerabilityDescriptor> securityVulnerabilities)
    {
        this.securityVulnerabilities = securityVulnerabilities;
        return this;
    }

    /**
     * @return the security vulnerabilities associated with the analyzed extension
     */
    public List<SecurityVulnerabilityDescriptor> getSecurityVulnerabilities()
    {
        return this.securityVulnerabilities;
    }

    /**
     * @return the translation key of the advice applicable on the security analysis (e.g., how to upgrade the extension
     *     to fix the identified security vulnerabilities)
     */
    public String getAdvice()
    {
        return this.advice;
    }

    /**
     * @param advice the translation key of the advice applicable on the security analysis (e.g., how to upgrade the
     *     extension to fix the identified security vulnerabilities)
     * @return the current object
     */
    public ExtensionSecurityAnalysisResult setAdvice(String advice)
    {

        this.advice = advice;
        return this;
    }

    /**
     * @return the highest CVSS of this security vulnerabilities in this analysis
     */
    public Double getMaxCVSS()
    {
        if (this.securityVulnerabilities != null) {
            return this.securityVulnerabilities.stream()
                .max(comparingDouble(SecurityVulnerabilityDescriptor::getScore))
                .map(SecurityVulnerabilityDescriptor::getScore)
                .orElse(null);
        } else {
            return null;
        }
    }

    /**
     * @return {@code true} when the extension is core, {@code false} otherwise
     */
    public boolean isCoreExtension()
    {
        return this.coreExtension;
    }

    /**
     * Sets the flag indicating whether the extension is a core extension.
     *
     * @param coreExtension {@code true} to indicate that the extension is a core extension, {@code false}
     *     otherwise
     * @return the current object
     */
    public ExtensionSecurityAnalysisResult setCoreExtension(boolean coreExtension)
    {
        this.coreExtension = coreExtension;
        return this;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExtensionSecurityAnalysisResult that = (ExtensionSecurityAnalysisResult) o;

        return new EqualsBuilder()
            .append(this.securityVulnerabilities, that.securityVulnerabilities)
            .append(this.advice, that.advice)
            .append(this.coreExtension, that.coreExtension)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.securityVulnerabilities)
            .append(this.advice)
            .append(this.coreExtension)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("securityVulnerabilities", this.securityVulnerabilities)
            .append("advice", this.advice)
            .append("coreExtension", this.coreExtension)
            .toString();
    }
}
