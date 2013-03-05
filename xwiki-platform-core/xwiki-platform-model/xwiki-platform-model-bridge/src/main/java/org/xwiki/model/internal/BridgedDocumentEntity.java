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

import org.xwiki.model.*;
import org.xwiki.model.ObjectEntity;
import org.xwiki.model.reference.EntityReference;

import java.util.Locale;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @since 5.0M1
 */
public class BridgedDocumentEntity extends AbstractBridgedEntity implements DocumentEntity
{
    private XWikiDocument document;

    public BridgedDocumentEntity(XWikiDocument document, XWikiContext xcontext)
    {
        super(xcontext);
        this.document = document;
    }

    @Override
    public Version getVersion()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public EntityIterator<Entity> getChildren(EntityType type)
    {
        throw new ModelRuntimeException("Not supported");
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
    public Content getContent()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public Locale getLocale()
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
    public void setContent(Content content)
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public String getIdentifier()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public Entity getParent()
    {
        throw new ModelRuntimeException("Not supported");
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
    public boolean isNew()
    {
        return this.document.isNew();
    }

    @Override
    public void setNew(boolean isNew)
    {
        this.document.setNew(isNew);
    }

    @Override
    public boolean isRemoved()
    {
        throw new ModelRuntimeException("Not supported");
    }
}
