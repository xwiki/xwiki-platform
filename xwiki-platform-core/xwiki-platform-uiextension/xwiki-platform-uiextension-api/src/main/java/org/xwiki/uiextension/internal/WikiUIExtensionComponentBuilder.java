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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentBuilder;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Provides {@link org.xwiki.uiextension.UIExtension} components from definitions stored in XObjects.
 * 
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Singleton
@Named("uiextension")
public class WikiUIExtensionComponentBuilder implements WikiComponentBuilder, WikiUIExtensionConstants
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Used to transform the reference to the UI Extension XClass to a string usable in a query. {@see
     * #searchDocumentReferences()}
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiSerializer;

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
    private ComponentManager cm;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private AuthorizationManager authorization;

    @Inject
    private AuthorExecutor authorExecutor;

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
        if (scope == WikiComponentScope.GLOBAL
            && !this.authorization.hasAccess(Right.PROGRAM, extensionsDoc.getContentAuthorReference(), null)) {
            throw new WikiComponentException("Registering global UI extensions requires programming rights");
        } else if (scope == WikiComponentScope.WIKI
            && !this.authorization.hasAccess(Right.ADMIN, extensionsDoc.getContentAuthorReference(), extensionsDoc
                .getDocumentReference().getWikiReference())) {
            throw new WikiComponentException(
                "Registering UI extensions at wiki level requires wiki administration rights");
        }
    }

    /**
     * Retrieve the list of {@link BaseObject} defining UI extensions.
     *
     * @param extensionsDoc the document to retrieve the definitions from
     * @return the list of {@link BaseObject} defining UI extensions in the given document
     * @throws WikiComponentException if no extension definition can be found in the document
     */
    private List<BaseObject> getExtensionDefinitions(XWikiDocument extensionsDoc) throws WikiComponentException
    {
        // Check whether this document contains a listener definition.
        List<BaseObject> extensionDefinitions = extensionsDoc.getXObjects(UI_EXTENSION_CLASS);

        if (extensionDefinitions.size() == 0) {
            throw new WikiComponentException(String.format("No UI extension object could be found in document [%s]",
                extensionsDoc.getDocumentReference()));
        }

        return extensionDefinitions;
    }

    @Override
    public List<WikiComponent> buildComponents(DocumentReference reference) throws WikiComponentException
    {
        List<WikiComponent> extensions = new ArrayList<WikiComponent>();
        XWikiDocument doc = null;

        try {
            XWikiContext xcontext = xcontextProvider.get();
            doc = xcontext.getWiki().getDocument(reference, xcontext);
        } catch (XWikiException e) {
            throw new WikiComponentException(
                String.format("Failed to create UI Extension(s) document [%s]", reference), e);
        }

        for (BaseObject extensionDefinition : this.getExtensionDefinitions(doc)) {
            if (extensionDefinition == null) {
                continue;
            }
            // Extract extension definition.
            String id = extensionDefinition.getStringValue(ID_PROPERTY);
            String extensionPointId = extensionDefinition.getStringValue(EXTENSION_POINT_ID_PROPERTY);
            String content = extensionDefinition.getStringValue(CONTENT_PROPERTY);
            String rawParameters = extensionDefinition.getStringValue(PARAMETERS_PROPERTY);
            WikiComponentScope scope =
                WikiComponentScope.fromString(extensionDefinition.getStringValue(SCOPE_PROPERTY));

            // Before going further we need to check the document author is authorized to register the extension
            this.checkRights(doc, scope);

            String roleHint = this.serializer.serialize(extensionDefinition.getReference());

            WikiUIExtension extension =
                new WikiUIExtension(roleHint, id, extensionPointId, extensionDefinition.getReference(),
                    doc.getAuthorReference(), this.authorExecutor);

            // It would be nice to have PER_LOOKUP components for UIX parameters but without constructor injection it's
            // safer to use a POJO and pass the Component Manager to it.
            WikiUIExtensionParameters parameters = new WikiUIExtensionParameters(id, rawParameters, cm);
            extension.setParameters(parameters);
            // It would be nice to have PER_LOOKUP components for UIX renderers but without constructor injection it's
            // safer to use a POJO and pass the Component Manager to it.
            WikiUIExtensionRenderer renderer =
                new WikiUIExtensionRenderer(roleHint, content, doc.getDocumentReference(), cm);
            extension.setRenderer(renderer);
            extension.setScope(scope);
            extensions.add(extension);
        }

        return extensions;
    }

    /**
     * @return list of document references to documents containing a UI extension object.
     */
    @Override
    public List<DocumentReference> getDocumentReferences()
    {
        List<DocumentReference> results = new ArrayList<DocumentReference>();
        // Note that the query is made to work with Oracle which treats empty strings as null.
        String query =
            ", BaseObject as obj, StringProperty as epId where obj.className=? "
                + "and obj.name=doc.fullName and epId.id.id=obj.id and epId.id.name=? "
                + "and  (epId.value <> '' or (epId.value is not null and '' is null))";
        List<String> parameters = new ArrayList<String>();
        parameters.add(this.compactWikiSerializer.serialize(UI_EXTENSION_CLASS));
        parameters.add(EXTENSION_POINT_ID_PROPERTY);

        try {
            XWikiContext xcontext = xcontextProvider.get();
            results.addAll(xcontext.getWiki().getStore().searchDocumentReferences(query, parameters, xcontext));
        } catch (XWikiException e) {
            this.logger.warn("Search for UI extensions failed: [{}]", e.getMessage());
        }

        return results;
    }
}
