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

import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.xml.input.XMLInputProperties;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyHidden;
import org.xwiki.properties.annotation.PropertyName;

/**
 * XAR input properties.
 * 
 * @version $Id$
 * @since 6.2M1
 */
public class XARInputProperties extends XMLInputProperties
{
    /**
     * The type of the {@link InputSource}.
     * 
     * @version $Id$
     * @since 9.0RC1
     */
    public enum SourceType
    {
        /**
         * A XAR package.
         */
        XAR,

        /**
         * A document as XML.
         */
        DOCUMENT,

        /**
         * An attachment as XML.
         */
        ATTACHMENT,

        /**
         * A class as XML.
         */
        CLASS,

        /**
         * A class property as XML.
         */
        CLASSPROPERTY,

        /**
         * An object as XML.
         */
        OBJECT,

        /**
         * An object property as XML.
         */
        OBJECTPROPERTY
    }

    /**
     * @see #getEntities()
     */
    private EntityReferenceSet entities;

    /**
     * @see #getSourceType()
     */
    private SourceType sourceType;

    /**
     * @see #isWithHistory()
     */
    private boolean withHistory = true;

    /**
     * @see #isWithExtension()
     */
    private boolean withExtension = true;

    /**
     * @see #getObjectPropertyType()
     */
    private String objectPropertyType;

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
     * @return the type of the source
     * @since 9.0RC1
     */
    public SourceType getSourceType()
    {
        return this.sourceType;
    }

    /**
     * @param sourceType the type of the source
     * @since 9.0RC1
     */
    public void setSourceType(SourceType sourceType)
    {
        this.sourceType = sourceType;
    }

    /**
     * @return true if the input should be forced as document
     * @deprecated since 9.0RC1, use {@link #getSourceType()} instead
     */
    @PropertyName("Force document")
    @PropertyDescription("Force considering the input stream as a document")
    @Deprecated
    public boolean isForceDocument()
    {
        return this.sourceType == SourceType.DOCUMENT;
    }

    /**
     * @param forceDocument true if the input should be forced as document
     * @deprecated since 9.0RC1, use {@link #setSourceType(SourceType)} instead
     */
    @Deprecated
    public void setForceDocument(boolean forceDocument)
    {
        if (forceDocument) {
            this.sourceType = SourceType.DOCUMENT;
        }
    }

    /**
     * @return true indicates if extension event should be generated if possible
     * @since 6.2M1
     */
    @PropertyName("With extensions")
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

    /**
     * @return the type of the object property to parse
     * @since 9.0RC1
     */
    @PropertyName("Object property type")
    @PropertyDescription("The type of the object property to parse (when the input is an object property)")
    public String getObjectPropertyType()
    {
        return this.objectPropertyType;
    }

    /**
     * @param objectPropertyType the type of the object property to parse
     * @since 9.0RC1
     */
    public void setObjectPropertyType(String objectPropertyType)
    {
        this.objectPropertyType = objectPropertyType;
    }
}
