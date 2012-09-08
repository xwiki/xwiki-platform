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

import org.xwiki.model.*;
import org.xwiki.model.Object;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * @since 4.3M1
 */
public class BridgedWiki implements Wiki
{
    private XWikiContext xcontext;

    public BridgedWiki(XWikiContext xcontext)
    {
        this.xcontext = xcontext;
    }

    @Override
    public Space addSpace(String spaceName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public Space getSpace(String spaceName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public EntityIterator<Space> getSpaces()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public boolean hasSpace(String spaceName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public void removeSpace(String spaceName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    public XWiki getXWiki()
    {
        return this.xcontext.getWiki();
    }

    @Override
    public org.xwiki.model.Object addObject(String objectName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public ObjectDefinition addObjectDefinition(String objectDefinitionName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public EntityIterator<Entity> getChildren(EntityType type)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public String getIdentifier()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public Object getObject(String objectName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public ObjectDefinition getObjectDefinition(String objectDefinitionName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public EntityIterator<ObjectDefinition> getObjectDefinitions()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public EntityIterator<Object> getObjects()
    {
        throw new ModelRuntimeException("Not supported");
    }

    /**
     * A Wiki Entity doesn't have any Parent; it's the topmost object in the Entity hierarchy. The user is supposed
     * to get hold of the {@link Server} instance in a different manner (instance in the Execution Context for
     * example).
     *
     * @return null
     */
    @Override
    public Entity getParent()
    {
        return null;
    }

    @Override
    public EntityReference getReference()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public EntityReference getLinkReference()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public void setLinkReference(EntityReference linkedReference)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public EntityType getType()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public boolean hasObject(String objectName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public boolean hasObjectDefinition(String objectDefinitionName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public void removeObject(String objectName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public void removeObjectDefinition(String objectDefinitionName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public void save(String comment, boolean isMinorEdit, Map<String, String> extraParameters)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public void discard()
    {
        throw new ModelRuntimeException("Not supported");
    }

    public XWikiContext getXWikiContext()
    {
        return this.xcontext;
    }

    @Override
    public boolean isModified()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public boolean isNew()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public boolean isRemoved()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public Locale getLocale()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public Version getVersion()
    {
        throw new ModelRuntimeException("Not supported");
    }
}
