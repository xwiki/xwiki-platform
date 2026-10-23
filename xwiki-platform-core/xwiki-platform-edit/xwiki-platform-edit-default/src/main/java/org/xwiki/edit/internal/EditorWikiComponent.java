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
package org.xwiki.edit.internal;

import java.lang.reflect.Type;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.edit.AbstractEditor;
import org.xwiki.edit.EditException;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorDescriptor;
import org.xwiki.edit.EditorDescriptorBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * An {@link Editor} implemented as a {@link WikiComponent}.
 * 
 * @param <D> the type of data that can be edited by this editor
 * @version $Id$
 * @since 8.2RC1
 */
@Component(roles = EditorWikiComponent.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class EditorWikiComponent<D> extends AbstractEditor<D> implements WikiComponent
{
    private static final String MISSING_EDITOR_OBJECT = "The document [%s] is missing the XWiki.EditorClass object.";

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    private UserReference authorReference;

    private DocumentReference editorReference;

    private Type roleType;

    private WikiComponentScope scope;

    @Inject
    private EditorDescriptorBuilder descriptorBuilder;

    @Inject
    private AuthorExecutor authorExecutor;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceDocumentReferenceSerializer;

    @Override
    public DocumentReference getAuthorReference()
    {
        return serializeAuthorReference(this.authorReference);
    }

    private DocumentReference serializeAuthorReference(UserReference userReference)
    {
        // The serializer resolves a null user reference to the current user, while the guest user is serialized to
        // null.
        return userReference != null ? this.userReferenceDocumentReferenceSerializer.serialize(userReference) : null;
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.editorReference;
    }

    @Override
    public String getRoleHint()
    {
        return this.descriptorBuilder.getId();
    }

    @Override
    public Type getRoleType()
    {
        return this.roleType;
    }

    @Override
    public WikiComponentScope getScope()
    {
        return this.scope;
    }

    @Override
    public EditorDescriptor getDescriptor()
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            XWikiDocument editorDocument = xcontext.getWiki().getDocument(this.getDocumentReference(), xcontext);
            XWikiDocument translatedEditorDocument = editorDocument.getTranslatedDocument(xcontext);
            this.descriptorBuilder.setName(translatedEditorDocument.getRenderedTitle(Syntax.PLAIN_1_0, xcontext));
            this.descriptorBuilder
                .setDescription(translatedEditorDocument.getRenderedContent(Syntax.PLAIN_1_0, xcontext));

        } catch (XWikiException e) {
            this.logger.warn("Failed to read the editor name and description. Root cause: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        }
        return this.descriptorBuilder.build();
    }

    @Override
    protected String render() throws EditException
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();

            XWikiDocument editorDocument = xcontext.getWiki().getDocument(this.getDocumentReference(), xcontext);
            BaseObject editorObject = editorDocument.getXObject(EditorClassDocumentInitializer.REFERENCE);
            if (editorObject == null) {
                throw new EditException(String.format(MISSING_EDITOR_OBJECT,
                    editorDocument.getDocumentReference()));
            }
            String editorCode = editorObject.getStringValue("code");
            // Execute the editor code with the rights of the author of the editor definition, i.e., the effective
            // metadata author of the editor document, which is also the author whose rights allowed registering the
            // editor. Take it from the same document instance as the code so that both always match.
            DocumentReference editorAuthorReference =
                serializeAuthorReference(editorDocument.getAuthors().getEffectiveMetadataAuthor());
            // Execute the editor code in the context of the current document (because the editor code needs to access
            // the data that has been put on the script context). The secure document is set by the author executor.
            return this.authorExecutor.call(
                () -> xcontext.getDoc().getRenderedContent(editorCode, editorDocument.getSyntax().toIdString(), false,
                    null, xcontext),
                editorAuthorReference, editorDocument.getDocumentReference());
        } catch (EditException e) {
            throw e;
        } catch (Exception e) {
            throw new EditException("Failed to render the editor code.", e);
        }
    }

    /**
     * Initializes the editor component based on the definition provided by the specified document.
     * 
     * @param editorReference the reference to the wiki page that defines the editor (i.e. that has a
     *            {@code XWiki.EditorClass} object)
     * @throws WikiComponentException if the editor component fails to be initialized
     */
    public void initialize(DocumentReference editorReference) throws WikiComponentException
    {
        if (this.editorReference != null) {
            throw new WikiComponentException("This editor component is already initialized.");
        }

        this.editorReference = editorReference;

        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            initialize(xcontext.getWiki().getDocument(editorReference, xcontext));
        } catch (XWikiException e) {
            throw new WikiComponentException("Failed to load the editor document.", e);
        }
    }

    private void initialize(XWikiDocument editorDocument) throws WikiComponentException
    {
        this.authorReference = editorDocument.getAuthors().getEffectiveMetadataAuthor();
        BaseObject editorObject = editorDocument.getXObject(EditorClassDocumentInitializer.REFERENCE);
        if (editorObject != null) {
            initialize(editorObject);
        } else {
            throw new WikiComponentException(String.format(MISSING_EDITOR_OBJECT,
                editorDocument.getDocumentReference()));
        }
    }

    private void initialize(BaseObject editorObject) throws WikiComponentException
    {
        this.descriptorBuilder.setId(editorObject.getStringValue("roleHint"));
        this.descriptorBuilder.setIcon(editorObject.getStringValue("icon"));
        this.descriptorBuilder.setCategory(editorObject.getStringValue("category"));

        this.scope = WikiComponentScope.fromString(editorObject.getStringValue("scope"));

        String dataTypeName = editorObject.getStringValue("dataType");
        try {
            Type dataType =
                ReflectionUtils.unserializeType(dataTypeName, Thread.currentThread().getContextClassLoader());
            this.roleType = new DefaultParameterizedType(null, Editor.class, dataType);
        } catch (ClassNotFoundException e) {
            throw new WikiComponentException(String.format("The [%s] data type does not exist.", dataTypeName), e);
        }
    }
}
