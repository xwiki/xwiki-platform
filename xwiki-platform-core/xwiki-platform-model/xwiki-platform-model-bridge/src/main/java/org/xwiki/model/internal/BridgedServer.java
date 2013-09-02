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

import com.xpn.xwiki.XWikiContext;

import org.xwiki.model.EntityIterator;
import org.xwiki.model.EntityManager;
import org.xwiki.model.ModelException;
import org.xwiki.model.ModelRuntimeException;
import org.xwiki.model.Server;
import org.xwiki.model.UniqueReference;
import org.xwiki.model.WikiEntity;
import org.xwiki.model.reference.WikiReference;

import java.util.Map;

/**
 * @since 5.2M2
 */
public class BridgedServer implements Server
{
    private XWikiContext xcontext;

    private EntityManager entityManager;

    public BridgedServer(EntityManager entityManager, XWikiContext xcontext)
    {
        this.xcontext = xcontext;
        this.entityManager = entityManager;
    }

    /**
     * Note: We'll create a wiki descriptor document when we save the Wiki Entity.
     */
    @Override
    public WikiEntity addWikiEntity(String wikiName)
    {
        UniqueReference uniqueReference = new UniqueReference(new WikiReference(wikiName));
        return this.entityManager.addEntity(uniqueReference);
    }

    @Override
    public WikiEntity getWikiEntity(String wikiName) throws ModelException
    {
        UniqueReference uniqueReference = new UniqueReference(new WikiReference(wikiName));
        return this.entityManager.getEntity(uniqueReference);
    }

    @Override
    public EntityIterator<WikiEntity> getWikiEntities()
    {
        throw new ModelRuntimeException("Not supported");
    }

    @Override
    public boolean hasWikiEntity(String wikiName) throws ModelException
    {
        UniqueReference uniqueReference = new UniqueReference(new WikiReference(wikiName));
        return this.entityManager.hasEntity(uniqueReference);
    }

    @Override
    public void removeWikiEntity(String wikiName)
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
}
