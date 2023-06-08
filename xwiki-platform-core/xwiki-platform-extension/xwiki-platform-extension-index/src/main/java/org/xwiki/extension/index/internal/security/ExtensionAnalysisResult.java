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
package org.xwiki.extension.index.internal.security;

import java.util.Comparator;
import java.util.List;

/**
 * Store the known security issues for a given extension.
 *
 * @version $Id$
 * @since 15.5RC1
 */
public class ExtensionAnalysisResult
{
    private Exception exception;

    private List<SecurityIssueDescriptor> securityIssues;

    /**
     * @param e the exception encountered during the analysis of the extension
     * @return the current object
     */
    public ExtensionAnalysisResult setException(Exception e)
    {
        this.exception = e;
        return this;
    }

    /**
     * @return the exception encountered during the analysis of the extension
     */
    public Exception getException()
    {
        return this.exception;
    }

    /**
     * @param securityIssues the security issues associated with the analyzed extension
     * @return the current object
     */
    public ExtensionAnalysisResult setResults(List<SecurityIssueDescriptor> securityIssues)
    {
        this.securityIssues = securityIssues;
        return this;
    }

    /**
     * @return the security issues associated with the analyzed extension
     */
    public List<SecurityIssueDescriptor> getSecurityIssues()
    {
        return this.securityIssues;
    }

    /**
     * @return the highest CCSV of a security issue of this object
     */
    public Double getMaxCCSV()
    {
        if (this.securityIssues != null) {
            return this.securityIssues.stream()
                .max(Comparator.comparingDouble(SecurityIssueDescriptor::getScore))
                .map(SecurityIssueDescriptor::getScore)
                .orElse(null);
        } else {
            return null;
        }
    }
}
