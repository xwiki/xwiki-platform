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
import org.xwiki.model.Server;
import org.xwiki.model.UniqueReference;
import org.xwiki.model.Wiki;
import org.xwiki.model.reference.WikiReference;

import java.util.Map;

public class BridgedServer implements Server
{
    private XWikiContext xcontext;

    private EntityManager entityManager;

    public BridgedServer(EntityManager entityManager, XWikiContext xcontext)
    {
        this.xcontext = xcontext;
        this.entityManager = entityManager;
    }

    @Override
    public Wiki addWiki(String wikiName)
    {
        UniqueReference uniqueReference = new UniqueReference(new WikiReference(wikiName));
        return this.entityManager.addEntity(uniqueReference);
    }

    @Override
    public Wiki getWiki(String wikiName)
    {
        UniqueReference uniqueReference = new UniqueReference(new WikiReference(wikiName));
        return this.entityManager.getEntity(uniqueReference);
    }

    @Override
    public EntityIterator<Wiki> getWikis()
    {
        throw new ModelException("Not supported");
    }

    @Override
    public boolean hasWiki(String wikiName)
    {
        UniqueReference uniqueReference = new UniqueReference(new WikiReference(wikiName));
        return this.entityManager.hasEntity(uniqueReference);
    }

    @Override
    public void removeWiki(String wikiName)
    {
        throw new ModelException("Not supported");
    }

    @Override
    public void save(String comment, boolean isMinorEdit, Map<String, String> extraParameters)
    {
        throw new ModelException("Not supported");
    }

    public XWikiContext getXWikiContext()
    {
        return this.xcontext;
    }
}
