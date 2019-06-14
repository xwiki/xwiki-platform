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
package org.xwiki.rendering.macro.include;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyDisplayType;
import org.xwiki.properties.annotation.PropertyFeature;

/**
 * Legacy code for {@link IncludeMacroParameters}.
 *
 * @version $Id$
 * @since 6.0M1
 */
public privileged aspect IncludeMacroParametersCompatibilityAspect
{
    /**
     * @param document the name of the document to include.
     * @deprecated since 3.4M1 use {@link #setReference(String)} instead
     */
    @PropertyDescription("the name of the document to include")
    @PropertyFeature("reference")
    @PropertyDisplayType(DocumentReference.class)
    @Deprecated
    public void IncludeMacroParameters.setDocument(String document)
    {
        this.reference = document;
    }

    /**
     * @return the name of the document to include.
     * @deprecated since 3.4M1 use {@link #getReference()} instead
     */
    @Deprecated
    public String IncludeMacroParameters.getDocument()
    {
        return this.reference;
    }

}
