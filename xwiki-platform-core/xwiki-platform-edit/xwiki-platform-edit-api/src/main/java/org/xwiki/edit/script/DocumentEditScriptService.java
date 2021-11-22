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
package org.xwiki.edit.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.edit.DocumentEditConfiguration;
import org.xwiki.script.service.ScriptService;

/**
 * Script service for XWiki document editing.
 * 
 * @version $Id$
 * @since 12.5
 */
@Component
@Singleton
@Named(EditScriptService.ROLE_HINT + ".document")
public class DocumentEditScriptService implements ScriptService
{
    @Inject
    private DocumentEditConfiguration docEditConfig;

    /**
     * @return {@code true} if XWiki documents should be edited in-place (from view mode) whenever possible,
     *         {@code false} otherwise (the stand-alone edit mode is preferred instead)
     */
    public boolean inPlaceEditingEnabled()
    {
        return this.docEditConfig.inPlaceEditingEnabled();
    }
}
