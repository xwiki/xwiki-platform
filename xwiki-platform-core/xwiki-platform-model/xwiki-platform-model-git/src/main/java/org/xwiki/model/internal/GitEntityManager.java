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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.Entity;
import org.xwiki.model.EntityManager;
import org.xwiki.model.UniqueReference;
import org.xwiki.model.reference.EntityReference;

/**
 * @since 5.2M1
 */
@Component
@Named("git")
@Singleton
public class GitEntityManager implements EntityManager
{
    @Inject
    private GitStore gitStore;

    @Override
    public <T extends Entity> T getEntity(UniqueReference uniqueReference)
    {
        T result = null;

        EntityReference reference = uniqueReference.getReference();
        switch (reference.getType()) {
            case DOCUMENT:
                // TODO: Create an EntityReferenceSerializer to convert from an EntityReference to a Git path
                // specification. Then call gitStore.getContent(). Then write a loader to load the content into
                // a DocumentEntity object.
                result = (T) new GitDocumentEntity();
                break;
        }

        return result;
    }

    @Override
    public boolean hasEntity(UniqueReference reference)
    {
        return false;
    }

    @Override
    public void removeEntity(UniqueReference reference)
    {

    }

    @Override
    public <T extends Entity> T addEntity(UniqueReference reference)
    {
        return null;
    }
}