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
import org.xwiki.model.ObjectEntity;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * @since 5.2M2
 */
public class BridgedWikiEntity extends AbstractBridgedEntity implements WikiEntity
{
    public BridgedWikiEntity(XWikiContext xcontext)
    {
        super(xcontext);
    }

    @Override
    public SpaceEntity addSpaceEntity(String spaceName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public SpaceEntity getSpaceEntity(String spaceName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public EntityIterator<SpaceEntity> getSpaceEntities()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public boolean hasSpaceEntity(String spaceName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public void removeSpaceEntity(String spaceName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    public XWiki getXWiki()
    {
        return getXWikiContext().getWiki();
    }

    @Override
    public ObjectEntity addObjectEntity(String objectName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public ClassEntity addClassEntity(String objectDefinitionName)
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
    public ObjectEntity getObjectEntity(String objectName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public ClassEntity getClassEntity(String objectDefinitionName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public EntityIterator<ClassEntity> getClassEntities()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public EntityIterator<ObjectEntity> getObjectEntities()
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
    public boolean hasObjectEntity(String objectName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public boolean hasClassEntity(String objectDefinitionName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public void removeObjectEntity(String objectName)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public void removeClassEntity(String objectDefinitionName)
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

    @Override
    public boolean isModified()
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
