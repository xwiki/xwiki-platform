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
package org.xwiki.skinx.internal.async;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.async.AsyncContext;

/**
 * Manager asynchronous rendering related informations.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Component(roles = SkinExtensionAsync.class)
@Singleton
public class SkinExtensionAsync
{
    /**
     * The name of the type in the {@link AsyncContext}.
     */
    public static final String USER_TYPE = "skinx";

    @Inject
    private AsyncContext asyncContext;

    /**
     * @param type the type of skin extension
     * @param resource the resource to register
     * @param parameters the parameters
     */
    public void use(String type, String resource, Map<String, Object> parameters)
    {
        this.asyncContext.use(USER_TYPE, new SkinExtensionInfo(type, resource, parameters));
    }
}
