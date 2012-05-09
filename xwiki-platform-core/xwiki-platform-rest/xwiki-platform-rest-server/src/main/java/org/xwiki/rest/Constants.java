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
package org.xwiki.rest;

/**
 * @version $Id$
 */
public class Constants
{
    /**
     * The key used to store the XWiki component manager in the current Restlet context.
     */
    public static final String XWIKI_COMPONENT_MANAGER = "xwikiComponentManager";

    /**
     * The key used to store the the list of JAX-RS resources that are implemented as components with per-lookup policy
     * and that have been instantiated during this request. These components are released at the end of the request.
     */
    public static final String RELEASABLE_COMPONENT_REFERENCES = "rest.releasableComponentReferences";

    /**
     * The key used to store allowed values for an object property in an XWiki object representation.
     * 
     * @see DomainObjectFactory#createObject(org.xwiki.rest.model.jaxb.ObjectFactory, java.net.URI,
     *      com.xpn.xwiki.XWikiContext, com.xpn.xwiki.api.Document, com.xpn.xwiki.objects.BaseObject, boolean)
     */
    public static final String ALLOWED_VALUES_ATTRIBUTE_NAME = "allowedValues";

    /**
     * The key used to store the current HTTP request object in the current Restlet context.
     */
    public static final String HTTP_REQUEST = "httpRequest";
}
