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
 *
 */
package org.xwiki.security.internal;

import com.xpn.xwiki.objects.BaseObject;
import org.xwiki.model.reference.DocumentReferenceResolver;

/**
 * Java class corresponding to XWiki.XWikiGlobalRightsClass.
 * @version $Id$
 */
public class GlobalRightsObject extends AbstractRightsObject
{
    /**
     * Construct a more manageable java object from the corresponding
     * xwiki object.
     * @param obj An xwiki rights object.
     * @param resolver A document reference resolver.
     * @param wikiName The name of the current wiki.
     */
    public GlobalRightsObject(BaseObject obj, DocumentReferenceResolver resolver, String wikiName)
    {
        super(obj, resolver, wikiName);
    }
}
