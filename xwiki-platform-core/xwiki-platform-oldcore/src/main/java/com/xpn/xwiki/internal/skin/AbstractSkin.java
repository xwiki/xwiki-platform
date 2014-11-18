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
package com.xpn.xwiki.internal.skin;

import java.util.HashSet;
import java.util.Set;

/**
 * @version $Id$
 * @since 6.4M1
 */
public abstract class AbstractSkin implements Skin
{
    protected SkinManager skinManager;

    protected SkinConfiguration configuration;

    protected String id;

    protected ResourceRepository parent;

    public AbstractSkin(String id, SkinManager skinManager, SkinConfiguration configuration)
    {
        this.id = id;
        this.skinManager = skinManager;
        this.configuration = configuration;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public ResourceRepository getParent()
    {
        if (this.parent == null) {
            this.parent = createParent();

            if (this.parent == null) {
                this.parent = this.skinManager.getSkin(this.configuration.getDefaultBaseSkinId());
            }

            if (this.parent == null) {
                this.parent = Skin.VOID;
            }
        }

        return this.parent;
    }

    @Override
    public Resource<?> getResource(String resourceName)
    {
        Resource<?> resource = getLocalResource(resourceName);

        if (resource == null) {
            // Make sure to not try several times the same skin
            Set<String> skins = new HashSet<String>();
            skins.add(getId());
            for (ResourceRepository parent = getParent(); parent != null && resource == null
                && !skins.contains(parent.getId()); parent = parent.getParent()) {
                resource = parent.getLocalResource(resourceName);
                skins.add(parent.getId());
            }
        }

        return resource;
    }

    protected abstract Skin createParent();
}
