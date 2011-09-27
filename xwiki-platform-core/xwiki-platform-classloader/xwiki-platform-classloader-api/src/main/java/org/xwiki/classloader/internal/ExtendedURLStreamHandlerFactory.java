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
package org.xwiki.classloader.internal;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.classloader.ExtendedURLStreamHandler;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

/**
 * Stream handler factory that uses components to lookup stream handler for protocols.
 * 
 * @version $Id$
 * @since 2.0.1
 */
@Component(roles={URLStreamHandlerFactory.class})
@Singleton
public class ExtendedURLStreamHandlerFactory implements URLStreamHandlerFactory
{
    /**
     * To dynamically lookup stream handler components. 
     */
    @Inject
    private ComponentManager componentManager;
    
    @Override
    public URLStreamHandler createURLStreamHandler(String protocol)
    {
        ExtendedURLStreamHandler result;
        try {
            result = this.componentManager.lookup(ExtendedURLStreamHandler.class, protocol);
        } catch (ComponentLookupException cle) {
            // No special protocol handler found, return null since code using this factory
            // should know how to deal when no protocol handler is found.
            result = null;
        }
        
        // All implementations of ExtendedURLStreamHandler must also extend URLStreamHandler
        // Note: we could make ExtendedURLStreamHandler extend URLStreamHandler since URLStreamHandler
        // is an abstract class and not an interface...
        return (URLStreamHandler) result;
    }
}
