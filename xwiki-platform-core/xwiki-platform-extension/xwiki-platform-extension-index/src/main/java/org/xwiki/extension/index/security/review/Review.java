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
package org.xwiki.extension.index.security.review;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Contains the metadata relative to the review of a vulnerability.
 *
 * @version $Id$
 * @since 15.6RC1
 */
public class Review
{
    private String emitter;

    private String explanation;

    private String filter;

    private ReviewResult result;

    /**
     * @return the {@code source} of the analysis (e.g., {@code xwiki-platform}, or the name of the extension for which
     *     the analysis has been done)
     */
    public String getEmitter()
    {
        return this.emitter;
    }

    /**
     * @param emitter the {@code source} of the analysis (e.g., {@code xwiki-platform}, or the name of the extension
     *     for which the analysis has been done)
     */
    public void setEmitter(String emitter)
    {
        this.emitter = emitter;
    }

    /**
     * @return the textual explanation, detailing why a given CVE should not be considered as a security vulnerability
     *     in the context of the {@code source}
     */
    public String getExplanation()
    {
        return this.explanation;
    }

    /**
     * @param explanation the textual explanation, detailing why a given CVE should not be considered as a security
     *     vulnerability in the context of the {@code source}
     */
    public void setExplanation(String explanation)
    {
        this.explanation = explanation;
    }

    /**
     * @return the result of the vulnerability review
     */
    public ReviewResult getResult()
    {
        return this.result;
    }

    /**
     * @param result the result of the vulnerability review
     */
    public void setResult(ReviewResult result)
    {
        this.result = result;
    }

    /**
     * @return a regex filter, when {@code null} the review is always used, otherwise it is only used if the regex is
     *     matching the extension id of an installed extension
     */
    public String getFilter()
    {
        return this.filter;
    }

    /**
     * @param filter a regex filter, when {@code null} the review is always used, otherwise it is only used if the
     *     regex is matching the extension id of an installed extension
     */
    public void setFilter(String filter)
    {
        this.filter = filter;
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

        Review review = (Review) o;

        return new EqualsBuilder()
            .append(this.emitter, review.emitter)
            .append(this.explanation, review.explanation)
            .append(this.result, review.result)
            .append(this.filter, review.filter)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.emitter)
            .append(this.explanation)
            .append(this.result)
            .append(this.filter)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("emitter", this.emitter)
            .append("explanation", this.explanation)
            .append("result", this.result)
            .append("filter", this.filter)
            .toString();
    }
}
