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
package com.xpn.xwiki.internal.objects.classes;

import java.util.Collection;

import org.xwiki.job.AbstractRequest;
import org.xwiki.model.reference.DocumentReference;

/**
 *
 * @since 18.9.0
 * @version $Id$
 */
class XClassMigratorRequest extends AbstractRequest
{
    private static final long serialVersionUID = 1L;

    private static final String CLASS_REFERENCE = "classReference";
    private static final String UPDATED_PROPERTIES = "updatedProperties";

    public DocumentReference getClassReference()
    {
        return getProperty(CLASS_REFERENCE);
    }

    public Collection<?> getUpdatedProperties()
    {
        return getProperty(UPDATED_PROPERTIES);
    }

    void setClassToMigrate(DocumentReference classReference)
    {
        setProperty(CLASS_REFERENCE, classReference);
    }

    void setUpdatedProperties(Collection<?> updatedProperties)
    {
        setProperty(UPDATED_PROPERTIES, updatedProperties);
    }
}
