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

import org.xwiki.extension.version.Version;
import org.xwiki.stability.Unstable;

import us.springett.cvss.Cvss;

/**
 * An individual security issue descriptor.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Unstable
public class SecurityIssueDescriptor
{
    private String id;

    private String url;

    private double score;

    private Version fixVersion;

    /**
     * @param id the security issue id
     * @return the current object
     */
    public SecurityIssueDescriptor setId(String id)
    {
        this.id = id;
        return this;
    }

    /**
     * @return the security issue id
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @param url an external URL providing more details on the issue
     * @return the current object
     */
    public SecurityIssueDescriptor setURL(String url)
    {
        this.url = url;
        return this;
    }

    /**
     * @return an external URL providing more details on the issue
     */
    public String getURL()
    {
        return this.url;
    }

    /**
     * Compute and store the score from the provided CVSS vector.
     *
     * @param vector a CVSS vector to parse and compute the based score from
     * @return the current object
     * @see #getScore()
     */
    public SecurityIssueDescriptor setSeverityScore(String vector)
    {
        this.score = Cvss.fromVector(vector).calculateScore().getBaseScore();
        return this;
    }

    /**
     * @return the CVSS score of the security issue
     */
    public double getScore()
    {
        return this.score;
    }

    /**
     * @param score the CVSS score of the security issue
     * @return the current object
     */
    public SecurityIssueDescriptor setScore(double score)
    {
        this.score = score;
        return this;
    }

    /**
     * @return the minimal version to which to upgrade to get the issue fixed automatically
     */
    public Version getFixVersion()
    {
        return this.fixVersion;
    }

    /**
     * @param fixVersion the minimal version to which to upgrade to get the issue fixed automatically
     * @return the current object
     */
    public SecurityIssueDescriptor setFixVersion(Version fixVersion)
    {
        this.fixVersion = fixVersion;
        return this;
    }
}
