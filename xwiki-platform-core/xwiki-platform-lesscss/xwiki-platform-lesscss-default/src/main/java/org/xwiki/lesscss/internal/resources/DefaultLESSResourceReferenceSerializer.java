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
package org.xwiki.lesscss.internal.resources;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.lesscss.LESSResourceReference;
import org.xwiki.lesscss.LESSResourceReferenceSerializer;

/**
 * Default implementation of {@link LESSResourceReferenceSerializer}. It looks-up the component corresponding to the
 * type of the LESS resource to serialize every type of resources.
 *
 * @version $Id$
 * @since 6.4M2
 */
@Component
@Singleton
public class DefaultLESSResourceReferenceSerializer implements LESSResourceReferenceSerializer
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Override
    public String serialize(LESSResourceReference lessResourceReference)
    {
        try {
            LESSResourceReferenceSerializer serializer =
                    componentManager.getInstance(LESSResourceReferenceSerializer.class,
                            lessResourceReference.getClass().getName());
            return serializer.serialize(lessResourceReference);
        } catch (ComponentLookupException e) {
            logger.warn("The LESS Resource Reference [{}] is not handled by the compiler.", lessResourceReference, e);
        }

        return null;
    }
}
