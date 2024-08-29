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
package org.xwiki.whatsnew;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Defines a news source.
 *
 * @version $Id$
 * @since 15.1RC1
 */
@Unstable
public class NewsSourceDescriptor
{
    private String id;

    private String sourceTypeHint;

    private Map<String, String> parameters;

    /**
     * @param id see {@link #getId()}
     * @param sourceTypeHint  see {@link #getSourceTypeHint()}
     * @param parameters see {@link #getParameters()}
     */
    public NewsSourceDescriptor(String id, String sourceTypeHint, Map<String, String> parameters)
    {
        this.id = id;
        this.sourceTypeHint = sourceTypeHint;
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    /**
     * @return the source id (used to identify a source instance, can be any value)
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return the hint to locate the source factory component to create a source object
     */
    public String getSourceTypeHint()
    {
        return this.sourceTypeHint;
    }

    /**
     * @return the source-specific parameters (e.g. the XWiki Blog source requires a {@code rssURL} parameter)
     */
    public Map<String, String> getParameters()
    {
        return this.parameters;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(5, 7)
            .append(this.id)
            .append(this.sourceTypeHint)
            .append(this.parameters)
            .toHashCode();
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

        NewsSourceDescriptor that = (NewsSourceDescriptor) o;

        return new EqualsBuilder()
            .append(this.id, that.id)
            .append(this.sourceTypeHint, that.sourceTypeHint)
            .append(this.parameters, that.parameters)
            .isEquals();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("id", this.id)
            .append("sourceTypeHint", this.sourceTypeHint)
            .append("parameters", this.parameters)
            .toString();
    }
}
