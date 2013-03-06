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
package org.xwiki.model;

import java.lang.*;
import java.lang.Object;
import java.util.Locale;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * TODO: Decide if this should be part of the current EntityReference implementation.
 *
 * @since 5.0M2
 */
@Unstable
public class UniqueReference
{
    private EntityReference reference;

    private Locale locale;

    private Version version;

    public UniqueReference(EntityReference reference)
    {
        this(reference, null, null);
    }

    public UniqueReference(EntityReference reference, Locale locale)
    {
        this(reference, locale, null);
    }

    public UniqueReference(EntityReference reference, Version version)
    {
        this(reference, null, version);
    }

    public UniqueReference(EntityReference reference, Locale locale, Version version)
    {
        this.reference = reference;
        this.locale = locale;
        this.version = version;
    }

    public EntityReference getReference()
    {
        return this.reference;
    }

    public Locale getLocale()
    {
        return this.locale;
    }

    public Version getVersion()
    {
        return this.version;
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);
        builder.append("reference", getReference());
        builder.append("locale", getLocale());
        builder.append("version", getVersion());
        return builder.toString();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(13, 5)
            .append(getReference())
            .append(getLocale())
            .append(getVersion())
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
        UniqueReference rhs = (UniqueReference) object;
        return new EqualsBuilder()
            .append(getReference(), rhs.getReference())
            .append(getLocale(), rhs.getLocale())
            .append(getVersion(), rhs.getVersion())
            .isEquals();
    }
}
