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
package org.xwiki.extension.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.index.ExtensionIndex;
import org.xwiki.extension.index.IndexedExtensionQuery;
import org.xwiki.extension.repository.search.ExtensionQuery;

/**
 * Various script APIs related to installed extensions.
 * 
 * @version $Id$
 * @since 5.3M1
 */
@Component
@Named(ExtensionManagerScriptService.ROLEHINT + '.' + ExtensionIndexScriptService.ID)
@Singleton
public class ExtensionIndexScriptService extends AbstractExtensionScriptService
{
    /**
     * The identifier of the sub extension {@link org.xwiki.script.service.ScriptService}.
     */
    public static final String ID = "index";

    /**
     * The repository containing installed extensions.
     */
    @Inject
    private ExtensionIndex index;

    /**
     * @return the extensions index
     */
    public ExtensionIndex getRepository()
    {
        return safe(this.index);
    }

    /**
     * Create a new instance of a {@link IndexedExtensionQuery} to be used in other APIs.
     * 
     * @param query the query to execute
     * @return a {@link ExtensionQuery} instance
     * @since 7.1RC1
     */
    public IndexedExtensionQuery newQuery(String query)
    {
        return new IndexedExtensionQuery(query);
    }
}
