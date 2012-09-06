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

import java.util.Locale;

import org.xwiki.model.reference.EntityReference;

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
        return "reference = [" + getReference() + "], locale = [" + getLocale() + "], version = [" + getVersion() + "]";
    }

    // TODO: Implement hashcode, equals
}
