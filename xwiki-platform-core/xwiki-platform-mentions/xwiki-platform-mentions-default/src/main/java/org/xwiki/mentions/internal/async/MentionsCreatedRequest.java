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
package org.xwiki.mentions.internal.async;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.job.AbstractRequest;
import org.xwiki.text.XWikiToStringBuilder;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Mention created request, send to create a mention analysis async job.
 *
 * @version $Id$
 * @since 12.5RC1
 */
public class MentionsCreatedRequest extends AbstractRequest
{
    private final XWikiDocument doc;

    /**
     *
     * @param doc created document.
     */
    public MentionsCreatedRequest(XWikiDocument doc)
    {
        this.doc = doc;
    }

    /**
     *
     * @return created document
     */
    public XWikiDocument getDoc()
    {
        return this.doc;
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

        MentionsCreatedRequest that = (MentionsCreatedRequest) o;

        return new EqualsBuilder()
                   .append(this.doc, that.doc)
                   .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                   .append(this.doc)
                   .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
                   .append("doc", this.getDoc())
                   .build();
    }
}
