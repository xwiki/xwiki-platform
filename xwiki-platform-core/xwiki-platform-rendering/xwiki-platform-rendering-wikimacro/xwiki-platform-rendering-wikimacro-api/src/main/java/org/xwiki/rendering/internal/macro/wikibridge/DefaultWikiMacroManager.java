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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.wikibridge.InsufficientPrivilegesException;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroFactory;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;
import org.xwiki.rendering.macro.wikibridge.WikiMacroVisibility;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Default implementation of {@link WikiMacroManager}.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
@Singleton
public class DefaultWikiMacroManager implements WikiMacroManager
{
    /**
     * The Root {@link ComponentManager}, used to look up specific component manager for registering Wiki Macros against
     * the proper one (depending on the Macro visibility).
     */
    @Inject
    private ComponentManager rootComponentManager;

    /**
     * The {@link DocumentAccessBridge} component.
     */
    @Inject
    private DocumentAccessBridge bridge;

    /**
     * The {@link ModelContext} component.
     */
    @Inject
    private ModelContext modelContext;

    /**
     * Used to check the right depending of the macro visibility.
     */
    @Inject
    private WikiMacroFactory wikiMacroFactory;

    /**
     * Used to serialize references of documents.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private WikiDescriptorManager wikis;

    /**
     * Map of wiki macros against document names. This is used to de-register wiki macros when corresponding documents
     * are deleted.
     */
    private Map<DocumentReference, WikiMacroData> wikiMacroMap = new HashMap<>();

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
        WikiMacroData(String hint, WikiMacro wikiMacro)
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

    @Override
    public boolean hasWikiMacro(DocumentReference documentReference)
    {
        return (null != this.wikiMacroMap.get(documentReference));
    }

    @Override
    public void registerWikiMacro(DocumentReference documentReference, WikiMacro wikiMacro)
        throws InsufficientPrivilegesException, WikiMacroException
    {
        WikiMacroDescriptor macroDescriptor = (WikiMacroDescriptor) wikiMacro.getDescriptor();

        WikiMacroVisibility visibility = macroDescriptor.getVisibility();

        // GLOBAL means WIKI on subwikis (only macro stored on main wiki are allowed to affect the whole farm)
        if (visibility == WikiMacroVisibility.GLOBAL
            && !this.wikis.isMainWiki(documentReference.getWikiReference().getName())) {
            visibility = WikiMacroVisibility.WIKI;
        }

        // Verify that the user has the right to register this wiki macro the chosen visibility
        if (this.wikiMacroFactory.isAllowed(documentReference, visibility)) {
            DefaultComponentDescriptor<Macro> componentDescriptor = new DefaultComponentDescriptor<>();
            componentDescriptor.setRoleType(Macro.class);
            componentDescriptor.setRoleHint(wikiMacro.getDescriptor().getId().getId());

            // Save current context informations
            String currentUser = this.bridge.getCurrentUser();
            EntityReference currentEntityReference = this.modelContext.getCurrentEntityReference();
            try {
                // Put the proper context information to let components manager use the proper keys to find
                // components to unregister
                this.bridge.setCurrentUser(this.serializer.serialize(wikiMacro.getAuthorReference() != null
                    ? wikiMacro.getAuthorReference() : this.bridge.getCurrentUserReference()));
                this.modelContext.setCurrentEntityReference(documentReference);

                // Register the macro against the right Component Manager, depending on the defined macro visibility.
                findComponentManager(visibility).registerComponent(componentDescriptor, wikiMacro);
                this.wikiMacroMap.put(documentReference,
                    new WikiMacroData(componentDescriptor.getRoleHint(), wikiMacro));
            } catch (Exception e) {
                throw new WikiMacroException(String.format("Failed to register macro [%s] in [%s] for visibility [%s]",
                    wikiMacro.getDescriptor().getId().getId(), documentReference, visibility), e);
            } finally {
                // Restore previous context informations
                this.bridge.setCurrentUser(currentUser);
                this.modelContext.setCurrentEntityReference(currentEntityReference);
            }
        } else {
            throw new InsufficientPrivilegesException(String.format(
                "Unable to register macro [%s] in [%s] for visibility [%s] due to insufficient privileges",
                wikiMacro.getDescriptor().getId().getId(), documentReference, macroDescriptor.getVisibility()));
        }
    }

    @Override
    public void unregisterWikiMacro(DocumentReference documentReference) throws WikiMacroException
    {
        WikiMacroData macroData = this.wikiMacroMap.get(documentReference);
        if (macroData != null) {
            WikiMacroDescriptor macroDescriptor = (WikiMacroDescriptor) macroData.getWikiMacro().getDescriptor();

            WikiMacroVisibility visibility = macroDescriptor.getVisibility();

            // GLOBAL means WIKI on subwikis (only macro stored on main wiki are allowed to affect the whole farm)
            if (visibility == WikiMacroVisibility.GLOBAL
                && !this.wikis.isMainWiki(documentReference.getWikiReference().getName())) {
                visibility = WikiMacroVisibility.WIKI;
            }

            // Verify that the user has the right to unregister this wiki macro for the chosen visibility
            if (this.wikiMacroFactory.isAllowed(documentReference, visibility)) {
                String currentUser = this.bridge.getCurrentUser();
                EntityReference currentEntityReference = this.modelContext.getCurrentEntityReference();
                try {
                    // Put the proper context information to let components manager use the proper keys to find
                    // components to unregister
                    this.bridge
                        .setCurrentUser(this.serializer.serialize(macroData.getWikiMacro().getAuthorReference()));
                    this.modelContext.setCurrentEntityReference(documentReference);

                    ComponentManager componentManager = findComponentManager(visibility);

                    // Remove the currently registered component only if it's really the same (might have been
                    // overwritten but another component)
                    if (isRegistered(macroData, componentManager)) {
                        componentManager.unregisterComponent(Macro.class, macroData.getHint());
                    }
                    this.wikiMacroMap.remove(documentReference);
                } catch (Exception e) {
                    throw new WikiMacroException(
                        String.format("Failed to unregister macro [%s] in [%s] for visibility [%s]",
                            macroData.getHint(), documentReference, visibility),
                        e);
                } finally {
                    this.bridge.setCurrentUser(currentUser);
                    this.modelContext.setCurrentEntityReference(currentEntityReference);
                }
            } else {
                throw new WikiMacroException(String.format(
                    "Unable to unregister macro [%s] in [%s] for visibility [%s] due to insufficient privileges",
                    macroData.getWikiMacro().getDescriptor().getId().getId(), documentReference,
                    macroDescriptor.getVisibility()));
            }
        } else {
            throw new WikiMacroException(String.format("Macro in [%s] isn't registered", documentReference));
        }
    }

    private boolean isRegistered(WikiMacroData macroData, ComponentManager componentManager)
    {
        ComponentDescriptor<Macro> descriptor =
            componentManager.getComponentDescriptor(Macro.class, macroData.getHint());
        if (descriptor != null && descriptor.getImplementation() == null) {
            try {
                WikiMacro registeredWikiMacro = componentManager.getInstance(Macro.class, macroData.getHint());

                return registeredWikiMacro.getDocumentReference()
                    .equals(macroData.getWikiMacro().getDocumentReference());
            } catch (ComponentLookupException e) {
                // Should never happen if there is no implementation
            }
        }

        return false;
    }

    /**
     * @param visibility the visibility required
     * @return the Component Manager to use to register/unregister the wiki macro. The Component Manager to use depends
     *         on the macro visibility. For example a macro that has the "current user" visibility must be registered
     *         against the User Component Manager.
     * @throws ComponentLookupException if the Component Manager for the specified visibility cannot be found
     */
    private ComponentManager findComponentManager(WikiMacroVisibility visibility) throws ComponentLookupException
    {
        ComponentManager cm;

        switch (visibility) {
            case USER:
                cm = this.rootComponentManager.getInstance(ComponentManager.class, "user");
                break;
            case WIKI:
                cm = this.rootComponentManager.getInstance(ComponentManager.class, "wiki");
                break;
            default:
                cm = this.rootComponentManager;
        }

        return cm;
    }
}
