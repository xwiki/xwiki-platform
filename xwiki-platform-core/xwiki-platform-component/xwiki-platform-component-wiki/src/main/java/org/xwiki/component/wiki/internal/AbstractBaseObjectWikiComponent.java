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
package org.xwiki.component.wiki.internal;

import java.lang.reflect.Type;

import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectReference;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Base class used by various xobject based implementations of {@link WikiComponent}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public abstract class AbstractBaseObjectWikiComponent implements WikiComponent
{
    /**
     * Extension scope property.
     */
    public static final String XPROPERTY_SCOPE = "scope";

    /**
     * @see {@link #getEntityReference()}
     */
    protected ObjectReference objectReference;

    /**
     * @see {@link #getAuthorReference()}
     */
    protected DocumentReference authorReference;

    /**
     * @see {@link #getRoleType()}
     */
    protected Type roleType;

    /**
     * @see {@link #getRoleHint()}
     */
    protected String roleHint;

    /**
     * @see {@link #getScope()}
     */
    protected WikiComponentScope scope = WikiComponentScope.WIKI;

    /**
     * @param baseObject the object containing the component definition
     * @param roleType the role Type implemented
     * @param roleHint the role hint for this role implementation
     */
    public AbstractBaseObjectWikiComponent(BaseObject baseObject, Type roleType, String roleHint)
    {
        this.objectReference = baseObject.getReference();

        this.authorReference = baseObject.getOwnerDocument().getAuthorReference();

        this.roleType = roleType;
        this.roleHint = roleHint;

        this.scope = WikiComponentScope.fromString(baseObject.getStringValue(XPROPERTY_SCOPE));
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.objectReference.getDocumentReference();
    }

    @Override
    public EntityReference getEntityReference()
    {
        return this.objectReference;
    }

    @Override
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
    }

    @Override
    public Type getRoleType()
    {
        return this.roleType;
    }

    @Override
    public String getRoleHint()
    {
        return this.roleHint;
    }

    @Override
    public WikiComponentScope getScope()
    {
        return this.scope;
    }
}
