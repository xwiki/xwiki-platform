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
package org.xwiki.model.internal;

import java.util.Locale;
import java.util.Map;

import org.xwiki.model.Entity;
import org.xwiki.model.EntityIterator;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelException;
import org.xwiki.model.Object;
import org.xwiki.model.Version;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.objects.BaseObject;

public class BridgedObject implements Object
{
    private BaseObject baseObject;

    public BridgedObject(BaseObject baseObject)
    {
        this.baseObject = baseObject;
    }

    @Override public EntityIterator<Entity> getChildren(EntityType type)
    {
        throw new ModelException("Not supported");
    }

    @Override public String getIdentifier()
    {
        throw new ModelException("Not supported");
    }

    @Override public Locale getLocale()
    {
        throw new ModelException("Not supported");
    }

    @Override public Entity getParent()
    {
        throw new ModelException("Not supported");
    }

    @Override public EntityType getType()
    {
        throw new ModelException("Not supported");
    }

    @Override public Version getVersion()
    {
        throw new ModelException("Not supported");
    }

    @Override public boolean isModified()
    {
        throw new ModelException("Not supported");
    }

    @Override public boolean isNew()
    {
        throw new ModelException("Not supported");
    }

    @Override public EntityReference getLinkReference()
    {
        throw new ModelException("Not supported");
    }

    @Override public void setLinkReference(EntityReference linkedReference)
    {
        throw new ModelException("Not supported");
    }

    @Override public void save(String comment, boolean isMinorEdit, Map<String, String> extraParameters)
    {
        throw new ModelException("Not supported");
    }

    @Override public EntityReference getReference()
    {
        throw new ModelException("Not supported");
    }
}
