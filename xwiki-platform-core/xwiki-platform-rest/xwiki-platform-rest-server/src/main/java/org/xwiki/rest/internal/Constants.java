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
package org.xwiki.rest.internal;

/**
 * @version $Id$
 */
public interface Constants
{
    /**
     * The key used to store allowed values for an object property in an XWiki object representation.
     *
     * @see DomainObjectFactory#createObject(org.xwiki.rest.model.jaxb.ObjectFactory, java.net.URI,
     *      com.xpn.xwiki.XWikiContext, com.xpn.xwiki.api.Document, com.xpn.xwiki.objects.BaseObject, boolean,
     *      com.xpn.xwiki.api.XWiki, Boolean)
     */
    String ALLOWED_VALUES_ATTRIBUTE_NAME = "allowedValues";
}
