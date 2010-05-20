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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;
import org.xwiki.rendering.macro.wikibridge.WikiMacroVisibility;

/**
 * Default implementation of {@link WikiMacroManager}.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
public class DefaultWikiMacroManager implements WikiMacroManager
{
    /**
     * The Root {@link ComponentManager}, used to look up specific component manager for registering
     * Wiki Macros against the proper one (depending on the Macro visibility).
     */
    @Requirement
    private ComponentManager rootComponentManager;

    /**
     * The {@link org.xwiki.bridge.DocumentAccessBridge} component.
     */
    @Requirement
    private DocumentAccessBridge bridge;

    /**
     * Map of wiki macros against document names. This is used to de-register wiki macros when corresponding documents
     * are deleted.
     */
    private Map<DocumentReference, WikiMacroData> wikiMacroMap = new HashMap<DocumentReference, WikiMacroData>();

    /**
     * Internal helper class to hold a wiki macro component role hint and the wiki Macro definition itself.
     */
    private static class WikiMacroData
    {
        /**
         * @see #getHint()
         */
        private String hint;

        /**
         * @see #getWikiMacro()
         */
        private WikiMacro wikiMacro;

        /**
         * @param hint see {@link #getHint()}
         * @param wikiMacro see {@link #getWikiMacro()}
         */
        public WikiMacroData(String hint, WikiMacro wikiMacro)
        {
            this.hint = hint;
            this.wikiMacro = wikiMacro;
        }

        /**
         * @return the wiki Macro component hint
         */
        public String getHint()
        {
            return this.hint;
        }

        /**
         * @return the wiki Macro definition
         */
        public WikiMacro getWikiMacro()
        {
            return this.wikiMacro;
        }
    }
    
    /**
     * {@inheritDoc}
     * @see WikiMacroManager#hasWikiMacro(org.xwiki.model.reference.DocumentReference)
     * @since 2.2M1
     */
    public boolean hasWikiMacro(DocumentReference documentReference)
    {
        return (null != wikiMacroMap.get(documentReference));
    }

    /**
     * {@inheritDoc}
     * @see WikiMacroManager#registerWikiMacro(org.xwiki.model.reference.DocumentReference , WikiMacro)
     * @since 2.2M1
     */
    public void registerWikiMacro(DocumentReference documentReference, WikiMacro wikiMacro) throws WikiMacroException
    {
        WikiMacroDescriptor macroDescriptor = (WikiMacroDescriptor) wikiMacro.getDescriptor();

        // Verify that the user has the right to register this wiki macro the chosen visibility
        if (isAllowed(documentReference, macroDescriptor.getVisibility())) {

            DefaultComponentDescriptor<Macro> componentDescriptor = new DefaultComponentDescriptor<Macro>();
            componentDescriptor.setRole(Macro.class);
            componentDescriptor.setRoleHint(wikiMacro.getDescriptor().getId().getId());

            try {
                // Register the macro against the right Component Manager, depending on the defined macro visibility.
                findComponentManager(macroDescriptor.getVisibility()).registerComponent(componentDescriptor, wikiMacro);
                this.wikiMacroMap.put(documentReference,
                    new WikiMacroData(componentDescriptor.getRoleHint(), wikiMacro));
            } catch (Exception e) {
                throw new WikiMacroException(String.format("Failed to register macro [%s] in [%s] for visibility [%s]",
                    wikiMacro.getDescriptor().getId().getId(), documentReference, macroDescriptor.getVisibility()), e);
            }
        } else {
            throw new WikiMacroException(String.format("Unable to register macro [%s] in [%s] for visibility [%s] "
                + "due to insufficient privileges", wikiMacro.getDescriptor().getId().getId(), documentReference,
                macroDescriptor.getVisibility()));
        }
    }

    /**
     * {@inheritDoc}
     * @see WikiMacroManager#unregisterWikiMacro(org.xwiki.model.reference.DocumentReference)
     * @since 2.2M1
     */
    public void unregisterWikiMacro(DocumentReference documentReference) throws WikiMacroException
    {
        WikiMacroData macroData = this.wikiMacroMap.get(documentReference);
        if (macroData != null) {
            WikiMacroDescriptor macroDescriptor = (WikiMacroDescriptor) macroData.getWikiMacro().getDescriptor();

            // Verify that the user has the right to register this wiki macro the chosen visibility
            if (isAllowed(documentReference, macroDescriptor.getVisibility())) {
                try {
                    findComponentManager(macroDescriptor.getVisibility()).unregisterComponent(Macro.class,
                        macroData.getHint());
                    this.wikiMacroMap.remove(documentReference);
                } catch (Exception e) {
                    throw new WikiMacroException(String.format("Failed to unregister macro [%s] in [%s] for "
                        + "visibility [%s]", macroData.getHint(), documentReference, macroDescriptor.getVisibility()),
                        e);
                }
            } else {
                throw new WikiMacroException(String.format("Unable to unregister macro [%s] in [%s] for visibility "
                    + "[%s] due to insufficient privileges", macroData.getWikiMacro().getDescriptor().getId().getId(),
                    documentReference, macroDescriptor.getVisibility()));
            }
        } else {
            throw new WikiMacroException(String.format("Macro [%s] in [%s] isn't registered",
                macroData.getWikiMacro().getDescriptor().getId().getId(), documentReference));
        }
    }

    /**
     * @param documentReference the name of the document containing the wiki macro definition
     * @param visibility the visibility required
     * @return true if the current user is allowed to register or unregister the wiki macro contained in the passed
     *         document name and with the passed visibility. Global visibility require programming rights on the
     *         document (to ensure they cannot be defined by standard users in a wiki farm - since only farm admins
     *         have programming rights in a farm). Current user and current wiki visibility simply require edit rights
     *         on the document.
     * @since 2.2M1
     */
    private boolean isAllowed(DocumentReference documentReference, WikiMacroVisibility visibility)
    {
        boolean isAllowed = false;

        switch (visibility) {
            case GLOBAL:
                // Verify that the user has programming rights since XWiki doesn't have a Wiki Farm Admin right yet
                // and the programming rights is the closest to it.
                if (this.bridge.hasProgrammingRights()) {
                    isAllowed = true;
                }
                break;
            default:
                // Verify the user has edit rights on the document containing the Wiki Macro definition
                if (this.bridge.isDocumentEditable(documentReference)) {
                    isAllowed = true;
                }
        }

        return isAllowed;
    }

    /**
     * @param visibility the visibility required
     * @return the Component Manager to use to register/unregister the wiki macro. The Component Manager to use depends
     *         on the macro visibility. For example a macro that has the "current user" visibility must be
     *         registered against the User Component Manager.    
     * @throws ComponentLookupException if the Component Manager for the specified visibility cannot be found
     */
    private ComponentManager findComponentManager(WikiMacroVisibility visibility) throws ComponentLookupException
    {
        ComponentManager cm;

        switch (visibility) {
            case USER:
                cm = this.rootComponentManager.lookup(ComponentManager.class, "user");
                break;
            case WIKI:
                cm = this.rootComponentManager.lookup(ComponentManager.class, "wiki");
                break;
            default:
                cm = this.rootComponentManager;
        }
        
        return cm;
    }
}
