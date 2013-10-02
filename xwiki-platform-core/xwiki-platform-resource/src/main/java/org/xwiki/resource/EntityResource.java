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
package org.xwiki.resource;

import java.util.Locale;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Represents an XWiki Resource pointing to an Entity (Document, Space, Wiki, Object, etc).
 * 
 * @version $Id$
 * @since 5.3M1
 */
@Unstable
public class EntityResource extends AbstractResource
{
    /**
     * The parameter name that represents a version of the entity.
     */
    private static final String REVISION_PARAMETER_NAME = "rev";

    /**
     * @see #getAction()
     */
    private String action;

    /**
     * @see #getEntityReference()
     */
    private EntityReference entityReference;

    /**
     * @see #getLocale()
     */
    private Locale locale;

    /**
     * @param entityReference the entity reference being wrapped
     */
    public EntityResource(EntityReference entityReference)
    {
        super(ResourceType.ENTITY);
        setEntityReference(entityReference);
    }

    /**
     * @return the action requested on this resource (e.g. "view", "download", etc). Note that We're not using a typed
     *         object since the action name can be anything and corresponds to an Action Component role hint.
     */
    public String getAction()
    {
        return this.action;
    }

    /**
     * @param action see {@link #getAction()}
     */
    public void setAction(String action)
    {
        this.action = action;
    }

    /**
     * @return the wrapped entity reference
     */
    public EntityReference getEntityReference()
    {
        return this.entityReference;
    }

    /**
     * @param entityReference see {@link #getEntityReference()}
     */
    public void setEntityReference(EntityReference entityReference)
    {
        this.entityReference = entityReference;
    }

    /**
     * @param locale see {@link #getLocale()}
     */
    public void setLocale(Locale locale)
    {
        this.locale = locale;
    }

    /**
     * @return the locale of the entity
     */
    public Locale getLocale()
    {
        return this.locale;
    }

    /**
     * @param revision see {@link #getRevision()}
     */
    public void setRevision(String revision)
    {
        addParameter(REVISION_PARAMETER_NAME, revision);
    }

    /**
     * @return the version of the resource (eg "2.1", etc)
     */
    public String getRevision()
    {
        return getParameterValue(REVISION_PARAMETER_NAME);
    }
}
