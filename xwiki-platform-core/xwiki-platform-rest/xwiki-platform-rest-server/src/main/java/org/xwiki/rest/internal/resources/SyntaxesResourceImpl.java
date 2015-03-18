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
package org.xwiki.rest.internal.resources;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Syntaxes;
import org.xwiki.rest.resources.SyntaxesResource;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.SyntaxesResourceImpl")
public class SyntaxesResourceImpl extends XWikiResource implements SyntaxesResource
{
    @Override
    public Syntaxes getSyntaxes()
    {
        Syntaxes syntaxes = objectFactory.createSyntaxes();
        syntaxes.getSyntaxes().addAll(Utils.getXWiki(componentManager).getConfiguredSyntaxes());

        return syntaxes;
    }
}
