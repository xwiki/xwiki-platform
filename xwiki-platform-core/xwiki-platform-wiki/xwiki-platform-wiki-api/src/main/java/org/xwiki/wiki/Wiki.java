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
 * A wiki.
 *
 * @since 5.3M1
 */
@Unstable
public class Wiki
{
    /**
     * The ID is the unique identifier that designate this wiki.
     */
    private String id;

    /**
     * Alias are names that can be used to designate this wiki in several places, like the URL.
     */
    private List<String> aliases;;

    /**
     * Constructor.
     * @param id Unique Id of the wiki
     * @param defaultAlias Default alias of the wiki
     */
    public Wiki(String id, String defaultAlias)
    {
        this.id = id;
        this.aliases = new ArrayList<String>();
        setDefaultAlias(defaultAlias);
    }

    /**
     * @return the unique Id of the wiki.
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * The default alias is the alias used to generate URL for that wiki.
     * @return the default alias.
     */
    public String getDefaultAlias()
    {
        return aliases.get(0);
    }

    /**
     * Set the default alias.
     * @param alias new alias
     */
    public void setDefaultAlias(String alias) {
        aliases.set(0, alias);
    }

    /**
     * Add an alias.
     * @param alias alias to add
     */
    public void addAlias(String alias)
    {
        aliases.add(alias);
    }

    /**
     * Returns all aliases. Aliases are used for the URL decoding.
     * @return all aliases
     */
    public List<String> getAliases()
    {
        return aliases;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(3, 3)
            .append(getDefaultAlias())
            .append(getId())
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
        Wiki rhs = (Wiki) object;
        return new EqualsBuilder()
            .append(getDefaultAlias(), rhs.getDefaultAlias())
            .append(getId(), rhs.getId())
            .isEquals();
    }
}
