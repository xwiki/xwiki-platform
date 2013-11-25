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
package org.xwiki.wikistream.xar.input;

import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyName;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.xml.input.XMLInputProperties;

/**
 * XAR input properties.
 * 
 * @version $Id$
 * @since 5.3RC1
 */
@Unstable
public class XARInputProperties extends XMLInputProperties
{
    /**
     * @see #isReferencesOnly()
     */
    private boolean referencesOnly;

    /**
     * @see #getEntities()
     */
    private EntityReferenceSet entities;

    /**
     * @see #isWithHistory()
     */
    private boolean withHistory;

    /**
     * @return if true events should be generated only for the document references (skip anything else)
     */
    @PropertyName("References only")
    @PropertyDescription("Indicate if events should be generated only for the document references (skip enything else)")
    public boolean isReferencesOnly()
    {
        return this.referencesOnly;
    }

    /**
     * @param referencesOnly if true events should be generated only for the document references (skip anything else)
     */
    public void setReferencesOnly(boolean referencesOnly)
    {
        this.referencesOnly = referencesOnly;
    }

    /**
     * @return The entities to take into account or skip
     */
    @PropertyName("Entities")
    @PropertyDescription("The entities to take into account or skip")
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
}
