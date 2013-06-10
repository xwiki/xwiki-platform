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

import org.xwiki.model.ClassEntity;
import org.xwiki.model.Content;
import org.xwiki.model.DocumentEntity;
import org.xwiki.model.Entity;
import org.xwiki.model.EntityIterator;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelException;
import org.xwiki.model.ObjectEntity;
import org.xwiki.model.Version;
import org.xwiki.model.reference.EntityReference;

public class GitDocumentEntity implements DocumentEntity
{
    @Override public Content getContent()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public void setContent(Content content)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public String getIdentifier()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public EntityType getType()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public Locale getLocale()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public Version getVersion()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public Entity getParent()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public EntityIterator<Entity> getChildren(EntityType type) throws ModelException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public boolean isModified()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public boolean isNew()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public boolean isRemoved()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public EntityIterator<ClassEntity> getClassEntities() throws ModelException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public ClassEntity getClassEntity(String objectDefinitionName) throws ModelException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public ClassEntity addClassEntity(String objectDefinitionName)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public void removeClassEntity(String objectDefinitionName)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public boolean hasClassEntity(String objectDefinitionName) throws ModelException
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public EntityIterator<ObjectEntity> getObjectEntities() throws ModelException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public ObjectEntity getObjectEntity(String objectName) throws ModelException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public ObjectEntity addObjectEntity(String objectName)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public void removeObjectEntity(String objectName)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public boolean hasObjectEntity(String objectName) throws ModelException
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public EntityReference getLinkReference()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public void setLinkReference(EntityReference linkedReference)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public void save(String comment, boolean isMinorEdit, Map<String, String> extraParameters)
        throws ModelException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public void discard()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public EntityReference getReference()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
