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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.inject.Inject;

import org.xwiki.edit.EditConfiguration;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorManager;
import org.xwiki.script.service.ScriptService;

/**
 * Base class for specialized edit script services that target specific types of editors.
 * 
 * @param <D> the type of data edited by the editors targeted by this script service
 * @version $Id$
 * @since 8.2RC1
 */
public abstract class AbstractTypedEditScriptService<D> implements ScriptService
{
    @Inject
    private EditorManager editorManager;

    @Inject
    private EditConfiguration editConfig;

    /**
     * @return the type of data edited by the editors targeted by this script service
     */
    protected Type getDataType()
    {
        return ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    /**
     * @return the list of editors that can edit the type of data bound to this script service
     */
    public List<Editor<D>> getEditors()
    {
        return this.editorManager.getEditors(this.getDataType());
    }

    /**
     * @param category the editor category
     * @return the list of editors that have the specified category and which can edit the type of data bound to this
     *         script service
     */
    public List<Editor<D>> getEditors(String category)
    {
        return this.editorManager.getEditors(this.getDataType(), category);
    }

    /**
     * @param hint the {@link Editor} component role hint
     * @return an editor that can edit the type of data bound to this script service and which has the given
     *         {@link Editor} component role hint, or {@code null} if no such editor can be found
     */
    public Editor<D> getEditor(String hint)
    {
        return this.editorManager.getEditor(this.getDataType(), hint);
    }

    /**
     * @return the configured default editor that can edit the type of data bound to this script service
     */
    public Editor<D> getDefaultEditor()
    {
        return this.editorManager.getDefaultEditor(this.getDataType());
    }

    /**
     * @param category the editor category
     * @return the configured default editor that has the specified category and which can edit the type of data bound
     *         to this script service
     */
    public Editor<D> getDefaultEditor(String category)
    {
        return this.editorManager.getDefaultEditor(this.getDataType(), category);
    }

    /**
     * @return the id of the configured default editor that can edit the type of data bound to this script service
     */
    public String getDefaultEditorId()
    {
        return this.editConfig.getDefaultEditor(this.getDataType());
    }

    /**
     * @param category the editor category
     * @return the id of the configured default editor for the specified category and which can edit the type of data
     *         bound to this script service
     */
    public String getDefaultEditorId(String category)
    {
        return this.editConfig.getDefaultEditor(this.getDataType(), category);
    }
}
