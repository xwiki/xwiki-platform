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

import us.springett.cvss.Cvss;

/**
 * An individual security issue descriptor.
 *
 * @version $Id$
 * @since 15.5RC1
 */
public class SecurityIssueDescriptor
{
    private String id;

    private String url;

    private double score;

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
}
