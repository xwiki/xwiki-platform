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
package org.xwiki.wiki;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.stability.Unstable;

/**
 * Contains data associated to a wiki (its id, alias, owner, etc).
 *
 * @version $Id$
 * @since 5.3M1
 */
@Unstable
public class WikiDescriptor
{
    private String wikiId;

    private String wikiAlias;

    private List<WikiDescriptorAlias> descriptorAliases = new ArrayList<WikiDescriptorAlias>();

    public WikiDescriptor(String wikiId, String wikiAlias)
    {
        this.wikiId = wikiId;
        this.wikiAlias = wikiAlias;
    }

    public String getWikiId()
    {
        return this.wikiId;
    }

    public String getWikiAlias()
    {
        return this.wikiAlias;
    }

    public void addDescriptorAlias(WikiDescriptorAlias descriptorAlias)
    {
        this.descriptorAliases.add(descriptorAlias);
    }

    public List<WikiDescriptorAlias> getDescriptorAliases()
    {
        return this.descriptorAliases;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(3, 3)
            .append(getWikiAlias())
            .append(getWikiId())
            .toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        WikiDescriptor rhs = (WikiDescriptor) object;
        return new EqualsBuilder()
            .append(getWikiAlias(), rhs.getWikiAlias())
            .append(getWikiId(), rhs.getWikiId())
            .isEquals();
    }
}
