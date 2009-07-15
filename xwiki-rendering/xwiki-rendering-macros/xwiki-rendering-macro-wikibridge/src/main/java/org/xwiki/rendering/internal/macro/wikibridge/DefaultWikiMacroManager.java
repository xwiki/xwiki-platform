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

package org.xwiki.rendering.internal.macro.wikibridge;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;

/**
 * Default implementation of {@link WikiMacroManager}.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public class DefaultWikiMacroManager extends AbstractLogEnabled implements WikiMacroManager
{
    /**
     * The {@link ComponentManager} component.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Map of wiki macros against document names. This is used to de-register wiki macros when corresponding documents
     * are deleted.
     */
    @SuppressWarnings("unchecked")
    private Map<String, ComponentDescriptor<Macro>> wikiMacroMap;

    /**
     * Creates a new {@link WikiMacroEventListener} component.
     */
    @SuppressWarnings("unchecked")
    public DefaultWikiMacroManager()
    {
        wikiMacroMap = new HashMap<String, ComponentDescriptor<Macro>>();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasWikiMacro(String documentName)
    {
        return (null != wikiMacroMap.get(documentName));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void registerWikiMacro(String documentName, WikiMacro wikiMacro)
    {
        DefaultComponentDescriptor<Macro> descriptor = new DefaultComponentDescriptor<Macro>();
        descriptor.setRole(Macro.class);
        descriptor.setRoleHint(wikiMacro.getName());
        try {
            componentManager.registerComponent(descriptor, wikiMacro);
            wikiMacroMap.put(documentName, descriptor);
            getLogger().info(
                String.format("Macro [%s] in [%s] successfully registered", wikiMacro.getName(), documentName));
        } catch (ComponentRepositoryException ex) {
            getLogger().error(
                String.format("Unable to register macro [%s] in [%s]", wikiMacro.getName(), documentName), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void unregisterWikiMacro(String documentName)
    {
        ComponentDescriptor<Macro> macroDescriptor = wikiMacroMap.get(documentName);
        componentManager.unregisterComponent(macroDescriptor.getRole(), macroDescriptor.getRoleHint());
        wikiMacroMap.remove(documentName);
        getLogger()
            .info(
                String.format("Macro [%s] in [%s] successfully de-registered", macroDescriptor.getRoleHint(),
                    documentName));
    }
}
