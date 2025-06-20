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
package org.xwiki.rest.internal.url.resources;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.url.AbstractParametrizedRestURLGenerator;

/**
 * Entity related Abstract class for ParametrizedRestURLGenerator.
 *
 * @param <T> the type of the resource for which the URL are created for.
 * @version $Id$
 * @since 8.0
 */
public abstract class AbstractEntityRestURLGenerator<T extends EntityReference>
    extends AbstractParametrizedRestURLGenerator<T>
{
    protected List<String> getRestSpaceList(SpaceReference spaceReference)
    {
        List<String> spaces = new ArrayList<>();
        for (EntityReference ref = spaceReference; ref != null && ref.getType() == EntityType.SPACE; ref =
            ref.getParent()) {
            if (!spaces.isEmpty()) {
                spaces.add("spaces");
            }
            spaces.add(ref.getName());
        }
        Collections.reverse(spaces);
        return spaces;
    }

    @Override
    public abstract URL getURL(T reference) throws XWikiRestException;
}
