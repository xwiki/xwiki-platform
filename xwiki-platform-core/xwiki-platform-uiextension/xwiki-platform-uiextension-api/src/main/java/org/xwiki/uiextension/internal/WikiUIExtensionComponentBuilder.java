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
package org.xwiki.uiextension.internal;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.component.wiki.internal.bridge.WikiBaseObjectComponentBuilder;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Provides {@link org.xwiki.uiextension.UIExtension} components from definitions stored in XObjects.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Singleton
@Named(WikiUIExtensionConstants.CLASS_REFERENCE_STRING)
public class WikiUIExtensionComponentBuilder implements WikiBaseObjectComponentBuilder, WikiUIExtensionConstants
{
    /**
     * Used to generate a role hint for UI extensions based on their object reference.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Used to get the parser to transform the extension content to a XDOM.
     */
    @Inject
    @Named("wiki")
    private ComponentManager wikiComponentManager;

    @Inject
    private Provider<WikiUIExtension> extensionProvider;

    @Inject
    private DocumentAuthorizationManager authorization;

    @Override
    public EntityReference getClassReference()
    {
        return UI_EXTENSION_CLASS;
    }

    /**
     * Checks if the last author of the document holding the extension(s) has the rights required to register extensions
     * for the given scope. If the document author doesn't have the required rights a {@link WikiComponentException} is
     * thrown.
     *
     * @param extensionsDoc the document holding the extension(s)
     * @param scope the scope to check the rights for
     * @throws WikiComponentException if the document author doesn't have the required rights to register extensions
     */
    private void checkRights(XWikiDocument extensionsDoc, WikiComponentScope scope) throws WikiComponentException
    {
        if (scope == WikiComponentScope.GLOBAL) {
            if (!this.authorization.hasAccess(Right.PROGRAM, null, extensionsDoc.getAuthorReference(),
                extensionsDoc.getDocumentReference()))
            {
                throw new WikiComponentException("Registering global UI extensions requires programming rights");
            }
        } else if (scope == WikiComponentScope.WIKI &&
            !this.authorization.hasAccess(Right.ADMIN, EntityType.WIKI, extensionsDoc.getAuthorReference(),
                extensionsDoc.getDocumentReference()))
        {
            throw new WikiComponentException(
                "Registering UI extensions at wiki level requires wiki administration rights");
        }
    }

    @Override
    public List<WikiComponent> buildComponents(BaseObject baseObject) throws WikiComponentException
    {
        // Empty extension point id is invalid UIX
        String extensionPointId = baseObject.getStringValue(EXTENSION_POINT_ID_PROPERTY);

        if (StringUtils.isEmpty(extensionPointId)) {
            // TODO: put back when we stop using this has as a feature
            // throw new WikiComponentException("Invalid UI extension: non empty extension point id is required");

            return Collections.emptyList();
        }

        WikiComponentScope scope = WikiComponentScope.fromString(baseObject.getStringValue(SCOPE_PROPERTY));

        XWikiDocument ownerDocument = baseObject.getOwnerDocument();

        // Before going further we need to check the document author is authorized to register the extension
        checkRights(ownerDocument, scope);

        // Extract extension definition.
        String id = baseObject.getStringValue(ID_PROPERTY);

        String roleHint = this.serializer.serialize(baseObject.getReference());

        WikiUIExtension extension;
        try {
            extension = this.extensionProvider.get();
            extension.initialize(baseObject, roleHint, id, extensionPointId);
        } catch (Exception e) {
            throw new WikiComponentException(
                String.format("Failed to initialize Panel UI extension [%s]", baseObject.getReference()), e);
        }

        // It would be nice to have PER_LOOKUP components for UIX parameters but without constructor injection it's
        // safer to use a POJO and pass the Component Manager to it.
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters(baseObject, this.wikiComponentManager);
        extension.setParameters(parameters);
        extension.setScope(scope);

        return Collections.singletonList(extension);
    }
}
