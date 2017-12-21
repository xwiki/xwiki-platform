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

import java.lang.reflect.Type;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.edit.EditConfiguration;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;

/**
 * Script oriented edit APIs.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Component
@Singleton
@Named(EditScriptService.ROLE_HINT)
public class EditScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ROLE_HINT = "edit";

    @Inject
    private EditorManager editorManager;

    @Inject
    private EditConfiguration editConfig;

    @Inject
    private ScriptServiceManager scriptServiceManager;

    /**
     * @param dataType the data type
     * @param <D> the data type
     * @return the list of editors that can edit the specified type of data
     */
    public <D> List<Editor<D>> getEditors(Type dataType)
    {
        return this.editorManager.getEditors(dataType);
    }

    /**
     * @param dataType the data type
     * @param category the editor category
     * @param <D> the data type
     * @return the list of editors that have the specified category and which are associated with the given data type
     */
    public <D> List<Editor<D>> getEditors(Type dataType, String category)
    {
        return this.editorManager.getEditors(dataType, category);
    }

    /**
     * @param dataType the data type
     * @param hint the {@link Editor} component role hint
     * @param <D> the data type
     * @return an editor that can edit the specified data type and which has the given {@link Editor} component role
     *         hint, or {@code null} if no such editor can be found
     */
    public <D> Editor<D> getEditor(Type dataType, String hint)
    {
        return this.editorManager.getEditor(dataType, hint);
    }

    /**
     * @param dataType the data type
     * @param <D> the data type
     * @return the configured default editor associated with the specified data type
     */
    public <D> Editor<D> getDefaultEditor(Type dataType)
    {
        return this.editorManager.getDefaultEditor(dataType);
    }

    /**
     * @param dataType the data type
     * @param category the editor category
     * @param <D> the data type
     * @return the configured default editor that has the specified category and which is associated with the given data
     *         type
     */
    public <D> Editor<D> getDefaultEditor(Type dataType, String category)
    {
        return this.editorManager.getDefaultEditor(dataType, category);
    }

    /**
     * @param dataType the data type
     * @return the id of the configured default editor associated with the specified data type
     */
    public String getDefaultEditorId(Type dataType)
    {
        return this.editConfig.getDefaultEditor(dataType);
    }

    /**
     * @param dataType the data type
     * @param category the editor category
     * @return the id of the configured default editor for the specified category and which is associated with the given
     *         data type
     */
    public String getDefaultEditorId(Type dataType, String category)
    {
        return this.editConfig.getDefaultEditor(dataType, category);
    }

    /**
     * @param serviceName the name of the sub {@link ScriptService}
     * @param <S> the {@link ScriptService} type
     * @return the sub {@link ScriptService} with the specified name, or {@code null} if none could be found
     */
    @SuppressWarnings("unchecked")
    public <S extends ScriptService> S get(String serviceName)
    {
        return (S) this.scriptServiceManager.get(ROLE_HINT + "." + serviceName);
    }
}
