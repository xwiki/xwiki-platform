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
package org.xwiki.rest.internal.url;

import java.lang.reflect.ParameterizedType;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.url.ParametrizedRestURLGenerator;
import org.xwiki.rest.url.RestURLGenerator;

/**
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Singleton
public class DefaultRestURLGenerator implements RestURLGenerator
{
    @Inject
    private ComponentManager componentManager;
    
    @Override
    public URL getURL(EntityReference entityReference) throws XWikiRestException
    {
        try {
            ParameterizedType type = new DefaultParameterizedType(null, ParametrizedRestURLGenerator.class,
                    entityReference.getClass());
            ParametrizedRestURLGenerator parametrizedRestURLGenerator = componentManager.getInstance(type);
            return parametrizedRestURLGenerator.getURL(entityReference);
        } catch (ComponentLookupException e) {
            throw new XWikiRestException(String.format("Unsupported entity type: [%s]", entityReference.getClass()), e);
        }
    }
}
