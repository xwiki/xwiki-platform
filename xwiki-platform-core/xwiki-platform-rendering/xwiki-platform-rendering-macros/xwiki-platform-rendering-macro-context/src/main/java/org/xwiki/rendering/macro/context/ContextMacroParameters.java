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
package org.xwiki.rendering.macro.context;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.annotation.PropertyAdvanced;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyDisplayType;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;

/**
 * Parameters for the Context macro.
 *
 * @version $Id$
 * @since 3.0M1
 */
public class ContextMacroParameters
{
    /**
     * @see #getDocument()
     */
    private String documentReference;

    /**
     * @see #getTransformationContext()
     * @since 8.3
     * @since 8.4RC1
     */
    private TransformationContextMode transformationContextMode = TransformationContextMode.CURRENT;

    private boolean restricted;

    private MacroContentSourceReference source;

    /**
     * @return the reference to the document that will be set as the current document to evaluate the macro's content
     */
    public String getDocument()
    {
        return this.documentReference;
    }

    /**
     * @param documentReference refer to {@link #getDocument()}
     */
    @PropertyDescription("The reference to the document serving as the current document")
    @PropertyDisplayType(DocumentReference.class)
    public void setDocument(String documentReference)
    {
        this.documentReference = documentReference;
    }

    /**
     * @return the mode to use for setting the Transformation Context in which the Context Macro will execute, see
     *         {@link TransformationContextMode} for details
     * @since 8.3
     * @since 8.4RC1
     */
    public TransformationContextMode getTransformationContext()
    {
        return this.transformationContextMode;
    }

    /**
     * @param mode refer to {@link #getTransformationContext()}
     * @since 8.3
     * @since 8.4RC1
     */
    @PropertyDescription("The Transformation Context mode to use. "
        + "Valid values are \"current\", \"document\" and \"transformations\"")
    public void setTransformationContext(TransformationContextMode mode)
    {
        this.transformationContextMode = mode;
    }

    /**
     * @return true if the content should be executed in a restricted context
     * @since 14.10.4
     * @since 15.0
     * @since 15.1RC1
     */
    @PropertyDescription("True if the content should be executed in a restricted context")
    public boolean isRestricted()
    {
        return this.restricted;
    }

    /**
     * @param restricted true if the content should be executed in a restricted context
     * @since 14.10.4
     * @since 15.0
     * @since 15.1RC1
     */
    public void setRestricted(boolean restricted)
    {
        this.restricted = restricted;
    }

    /**
     * @param source the reference of the content to parse
     * @since 15.1RC1
     * @since 14.10.5
     */
    @PropertyDescription("the reference of the content to parse instead of the content of the macro"
        + " (script:myvariable for the entry with name myvariable in the script context)")
    @PropertyAdvanced
    public void setSource(MacroContentSourceReference source)
    {
        this.source = source;
    }

    /**
     * @return the reference of the content to parse
     * @since 15.1RC1
     * @since 14.10.5
     */
    public MacroContentSourceReference getSource()
    {
        return this.source;
    }
}
