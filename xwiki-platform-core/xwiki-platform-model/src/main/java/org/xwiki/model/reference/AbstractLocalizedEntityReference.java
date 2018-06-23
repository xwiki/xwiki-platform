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
package org.xwiki.model.reference;

import java.util.Locale;

import org.xwiki.model.EntityType;

/**
 * Represents an entity reference which contains a locale.
 * 
 * @version $Id$
 * @since 10.6RC1
 */
public abstract class AbstractLocalizedEntityReference extends EntityReference
{
    /**
     * Parameter key for the locale.
     */
    public static final String LOCALE = "locale";

    /**
     * Create a new root EntityReference.
     *
     * @param name name for the newly created entity reference, could not be null.
     * @param type type for the newly created entity reference, could not be null.
     */
    public AbstractLocalizedEntityReference(String name, EntityType type)
    {
        super(name, type);
    }

    /**
     * Create a new root EntityReference.
     *
     * @param name name for the newly created entity reference, could not be null.
     * @param type type for the newly created entity reference, could not be null.
     * @param locale the {@link Locale} of the entity.
     */
    public AbstractLocalizedEntityReference(String name, EntityType type, Locale locale)
    {
        super(name, type);

        setLocale(locale);
    }

    /**
     * Create a new EntityReference.
     *
     * @param name name for the newly created entity reference, could not be null.
     * @param type type for the newly created entity reference, could not be null.
     * @param parent parent reference for the newly created entity reference, may be null.
     */
    public AbstractLocalizedEntityReference(String name, EntityType type, EntityReference parent)
    {
        super(name, type, parent);
    }

    /**
     * Create a new EntityReference.
     *
     * @param name name for the newly created entity reference, could not be null.
     * @param type type for the newly created entity reference, could not be null.
     * @param parent parent reference for the newly created entity reference, may be null.
     * @param locale the {@link Locale} of the entity.
     */
    public AbstractLocalizedEntityReference(String name, EntityType type, EntityReference parent, Locale locale)
    {
        super(name, type, parent);

        setLocale(locale);
    }

    /**
     * Clone an EntityReference.
     *
     * @param reference the reference to clone
     */
    public AbstractLocalizedEntityReference(EntityReference reference)
    {
        super(reference);
    }

    /**
     * Clone an EntityReference and change/add the passed Locale.
     *
     * @param reference the reference to clone
     * @param locale the {@link Locale} of the new reference
     */
    public AbstractLocalizedEntityReference(EntityReference reference, Locale locale)
    {
        super(reference);

        setLocale(locale);
    }

    /**
     * Clone an EntityReference, but replace one of the parent in the chain by an other one.
     *
     * @param reference the reference that is cloned
     * @param oldReference the old parent that will be replaced
     * @param newReference the new parent that will replace oldReference in the chain
     */
    protected AbstractLocalizedEntityReference(EntityReference reference, EntityReference oldReference,
        EntityReference newReference)
    {
        super(reference, oldReference, newReference);
    }

    /**
     * Set the locale of this reference.
     *
     * @param locale the locale of this document reference
     */
    protected void setLocale(Locale locale)
    {
        setParameter(LOCALE, locale);
    }

    /**
     * @return the locale of this reference
     */
    public Locale getLocale()
    {
        return (Locale) getParameter(LOCALE);
    }
}
