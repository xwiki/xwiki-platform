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
package org.xwiki.filter.instance.output;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyName;
import org.xwiki.rendering.syntax.Syntax;

/**
 * @version $Id$
 * @since 6.2M1
 */
public class DocumentInstanceOutputProperties extends EntityInstanceOutputProperties
{
    /**
     * @see #getDefaultReference()
     */
    private EntityReference defaultReference;

    /**
     * @see #getDefaultSyntax()
     */
    private Syntax defaultSyntax;

    /**
     * @see #isPreviousDeleted()
     */
    private boolean previousDeleted = true;

    /**
     * @see #isAuthorPreserved()
     */
    private boolean authorPreserved = true;

    /**
     * @see #isStoppedWhenSaveFail()
     */
    private boolean stoppedWhenSaveFail = true;

    /**
     * @return The base reference to use to resolve reference from events
     */
    @PropertyName("Default reference")
    @PropertyDescription("The base reference to use to resolve reference from events")
    public EntityReference getDefaultReference()
    {
        return this.defaultReference;
    }

    /**
     * @param defaultReference The base reference to use to resolve reference from events
     */
    public void setDefaultReference(EntityReference defaultReference)
    {
        this.defaultReference = defaultReference;
    }

    /**
     * @return The default syntax if not is provided in events
     */
    @PropertyName("Default syntax")
    @PropertyDescription("The default syntax if not is provided in events")
    public Syntax getDefaultSyntax()
    {
        return this.defaultSyntax;
    }

    /**
     * @param defaultSyntax The default syntax if not is provided in events
     */
    public void setDefaultSyntax(Syntax defaultSyntax)
    {
        this.defaultSyntax = defaultSyntax;
    }

    /**
     * @return Indicate if existing document should be deleted before importing the new one
     */
    @PropertyName("Delete existing document")
    @PropertyDescription("Indicate if existing document should be deleted before importing the new one")
    public boolean isPreviousDeleted()
    {
        return this.previousDeleted;
    }

    /**
     * @param previousDeleted Indicate if existing document should be deleted before importing the new one
     */
    public void setPreviousDeleted(boolean previousDeleted)
    {
        this.previousDeleted = previousDeleted;
    }

    /**
     * @return true if the authors coming from the events should be kept.
     */
    @PropertyName("Preserve author")
    @PropertyDescription("Indicate if the authors comming from the events should be kept."
        + " If false the current user is used.")
    public boolean isAuthorPreserved()
    {
        return !isAuthorSet() && this.authorPreserved;
    }

    /**
     * @param authorPreserved indicate if the authors coming from the events should be kept. Not taken into account if
     *            {@link #setAuthor(DocumentReference)} is used.
     */
    public void setAuthorPreserved(boolean authorPreserved)
    {
        this.authorPreserved = authorPreserved;
    }

    /**
     * @return Indicate if an exception should be thrown if a document save fails.
     * @since 6.2.6
     * @since 6.4.2
     */
    @PropertyName("Stop when document save fails")
    @PropertyDescription("Indicate if an exception should be thrown if a document save fails")
    public boolean isStoppedWhenSaveFail()
    {
        return this.stoppedWhenSaveFail;
    }

    /**
     * @param stoppedWhenSaveFail Indicate if an exception should be thrown if a document save fail.
     * @since 6.2.6
     * @since 6.4.2
     */
    public void setStoppedWhenSaveFail(boolean stoppedWhenSaveFail)
    {
        this.stoppedWhenSaveFail = stoppedWhenSaveFail;
    }
}
