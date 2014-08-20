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
package org.xwiki.filter.xar.input;

import org.xwiki.filter.xml.input.XMLInputProperties;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyHidden;
import org.xwiki.properties.annotation.PropertyName;
import org.xwiki.stability.Unstable;

/**
 * XAR input properties.
 * 
 * @version $Id$
 * @since 6.2M1
 */
@Unstable
public class XARInputProperties extends XMLInputProperties
{
    /**
     * @see #getEntities()
     */
    private EntityReferenceSet entities;

    /**
     * @see #isWithHistory()
     */
    private boolean withHistory = true;

    /**
     * @see #isForceDocument()
     */
    private boolean forceDocument;

    /**
     * @see #isWithExtension()
     */
    private boolean withExtension = true;

    /**
     * @return The entities to take into account or skip
     */
    @PropertyName("Entities")
    @PropertyDescription("The entities to take into account or skip")
    // TODO: implement Converter for EntityReferenceSet
    @PropertyHidden
    public EntityReferenceSet getEntities()
    {
        return this.entities;
    }

    /**
     * @param entities The entities to take into account or skip
     */
    public void setEntities(EntityReferenceSet entities)
    {
        this.entities = entities;
    }

    /**
     * @return Indicate if events should be generated for history
     */
    @PropertyName("With history")
    @PropertyDescription("Indicate if events should be generated for history")
    public boolean isWithHistory()
    {
        return this.withHistory;
    }

    /**
     * @param withHistory Indicate if events should be generated for history
     */
    public void setWithHistory(boolean withHistory)
    {
        this.withHistory = withHistory;
    }

    /**
     * @return true if the input should be forced as document
     */
    @PropertyName("Force document")
    @PropertyDescription("Force considering the input stream as a document")
    public boolean isForceDocument()
    {
        return this.forceDocument;
    }

    /**
     * @param forceDocument true if the input should be forced as document
     */
    public void setForceDocument(boolean forceDocument)
    {
        this.forceDocument = forceDocument;
    }

    /**
     * @return true indicates if extension event should be generated if possible
     * @since 6.2M1
     */
    @PropertyName("Indicates if extension event should be generated if possible")
    @PropertyDescription("Indicates if extension event should be generated if possible")
    public boolean isWithExtension()
    {
        return this.withExtension;
    }

    /**
     * @param withExtension indicates if extension event should be generated if possible
     * @since 6.2M1
     */
    public void setWithExtension(boolean withExtension)
    {
        this.withExtension = withExtension;
    }
}
