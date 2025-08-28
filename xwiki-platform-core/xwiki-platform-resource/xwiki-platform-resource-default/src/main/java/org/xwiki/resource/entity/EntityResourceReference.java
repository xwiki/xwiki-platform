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
package org.xwiki.resource.entity;

import java.util.Locale;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceType;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Represents an XWiki Resource Reference pointing to an Entity (Document, Space, Wiki, Object, etc).
 * 
 * @version $Id$
 * @since 6.1M2
 */
public class EntityResourceReference extends AbstractResourceReference
{
    /**
     * Represents an Entity Resource Type.
     */
    public static final ResourceType TYPE = new ResourceType("entity");

    /**
     * The parameter name that represents a version of the entity.
     */
    private static final String REVISION_PARAMETER_NAME = "rev";

    /**
     * @see #getEntityReference()
     */
    private EntityReference entityReference;

    /**
     * @see #getLocale()
     */
    private Locale locale;

    /**
     * @see #getAction()
     */
    private EntityResourceAction action;

    /**
     * @see #getAnchor()
     */
    private String anchor;

    /**
     * @param entityReference the entity reference being wrapped
     * @param action the instance representing the technical Action id (e.g. View, Download, etc)
     */
    public EntityResourceReference(EntityReference entityReference, EntityResourceAction action)
    {
        this(entityReference, action, null);
    }

    /**
     * @param entityReference the entity reference being wrapped
     * @param action the instance representing the technical Action id (e.g. View, Download, etc)
     * @param anchor the anchor
     * @since 15.4RC1
     */
    public EntityResourceReference(EntityReference entityReference, EntityResourceAction action, String anchor)
    {
        setType(TYPE);
        setEntityReference(entityReference);
        setAction(action);
        setAnchor(anchor);
    }

    /**
     * @return the action requested on this resource (e.g. View, Download, Edit, etc)
     */
    public EntityResourceAction getAction()
    {
        return this.action;
    }

    /**
     * @param action see {@link #getAction()}
     */
    public void setAction(EntityResourceAction action)
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
     * @return the anchor
     * @since 15.4RC1
     */
    public String getAnchor()
    {
        return this.anchor;
    }

    /**
     * @param anchor the anchor
     * @since 15.4RC1
     */
    public void setAnchor(String anchor)
    {
        this.anchor = anchor;
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

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(9, 9)
            .appendSuper(super.hashCode())
            .append(getEntityReference())
            .append(getLocale())
            .append(getAction())
            .append(getAnchor())
            .toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        EntityResourceReference rhs = (EntityResourceReference) object;
        return new EqualsBuilder()
            .appendSuper(super.equals(object))
            .append(getEntityReference(), rhs.getEntityReference())
            .append(getLocale(), rhs.getLocale())
            .append(getAction(), rhs.getAction())
            .append(getAnchor(), rhs.getAnchor())
            .isEquals();
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);
        builder.appendSuper(super.toString());
        builder.append("reference", getEntityReference());
        builder.append("action", getAction());
        builder.append("locale", getLocale());
        builder.append("anchor", getAnchor());
        return builder.toString();
    }
}
