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
package org.xwiki.tag.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.tag.TagException;
import org.xwiki.tag.TagOperationResult;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.tag.TagOperationResult.NO_EFFECT;
import static org.xwiki.tag.TagOperationResult.OK;

/**
 * Provides the operation to manipulate the Tag XObjects attached to documents.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@Component(roles = { TagDocumentManager.class })
@Singleton
public class TagDocumentManager
{
    /**
     * The XClass of the Tag objects.
     */
    public static final String XWIKI_TAG_CLASS = "XWiki.TagClass";

    /**
     * The name of the field that holds the tag names in the TagClass XClass.
     */
    public static final String TAGS_FIELD_NAME = "tags";

    private static final String DOC_COMMENT_TAG_ADDED = "plugin.tag.editcomment.added";

    private static final String DOC_COMMENT_TAG_REMOVED = "plugin.tag.editcomment.removed";

    private static final String DOC_COMMENT_TAG_RENAMED = "plugin.tag.editcomment.renamed";

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private TagQueryManager tagQueryManager;

    /**
     * Returns the list of tags of a document.
     *
     * @param documentReference the document reference of the document
     * @return the list of tags of the document
     * @throws TagException in case of error when loading the document from its reference
     */
    public List<String> getTagsFromDocument(DocumentReference documentReference) throws TagException
    {
        XWikiDocument document = loadDocument(documentReference);
        return getTagsFromDocument(document);
    }

    /**
     * Update the list of tags of a document with a list of possibly new tags. Tags are only added if the document is
     * not already tagged with them. The case is ignored when looking for existing tags.
     *
     * @param tags the list of tags to add
     * @param documentReference the document reference of the document to update
     * @return {@link TagOperationResult#OK} if at least one tag was added to the document, {@link
     *     TagOperationResult#NO_EFFECT} otherwise
     * @throws TagException in case of error when loading or saving the document
     */
    public TagOperationResult addTagsToDocument(List<String> tags, DocumentReference documentReference)
        throws TagException
    {
        XWikiDocument document = loadDocument(documentReference);
        List<String> documentTags = getTagsFromDocument(document);

        boolean newTags = false;
        for (String tag : tags) {
            if (!StringUtils.isBlank(tag) && !containsIgnoreCase(documentTags, tag)) {
                documentTags.add(tag);
                newTags = true;
            }
        }

        if (newTags) {
            setDocumentTags(document, documentTags);
            String joinedTags = StringUtils.join(documentTags, ", ");
            String comment = this.contextualLocalizationManager.getTranslationPlain(DOC_COMMENT_TAG_ADDED, joinedTags);

            // Since we're changing the document we need to set the new author.
            document.setAuthorReference(this.xcontextProvider.get().getUserReference());

            XWikiContext context = this.xcontextProvider.get();
            try {
                context.getWiki().saveDocument(document, comment, true, context);
            } catch (XWikiException e) {
                throw new TagException(
                    MessageFormat.format("Failed to save the document after adding tags [{0}]", joinedTags), e);
            }

            return OK;
        }

        return NO_EFFECT;
    }

    /**
     * Remove a tag from a document. Nothing is done if the tag is not found (the case is not relevant).
     *
     * @param tag the to remove
     * @param documentReference the document reference of the document to update
     * @return {@link TagOperationResult#OK} if the tag was actually removed, {@link TagOperationResult#NO_EFFECT} if
     *     the tag was not found in the document
     * @throws TagException in case of error when loading or saving the document
     */
    public TagOperationResult removeTagFromDocument(String tag, DocumentReference documentReference) throws TagException
    {
        return removeTagFromDocument(tag, loadDocument(documentReference));
    }

    /**
     * @param tag the tag to delete
     * @return {@link TagOperationResult#OK} if the tag was deleted from all the document
     * @throws TagException in case of error when retrieving or saving the documents containing the delete tag
     */
    public TagOperationResult deleteTag(String tag) throws TagException
    {
        // Since we're deleting a tag, we want to delete it even if the document is hidden. A hidden document is still
        // accessible to users, it's just not visible for simple users; it doesn't change permissions.
        List<String> docsToProcess = this.tagQueryManager.getDocumentsWithTag(tag, true);
        if (docsToProcess.isEmpty()) {
            return NO_EFFECT;
        }

        boolean hadEffectOnOneDoc = false;
        boolean someStepFailed = false;
        for (String docName : docsToProcess) {
            try {
                TagOperationResult tagOperationResult =
                    removeTagFromDocument(tag, this.documentReferenceResolver.resolve(docName));
                hadEffectOnOneDoc = tagOperationResult == OK;
            } catch (TagException e) {
                // Continue even if one document failed.
                this.logger.warn("Failed to remove tag [{}] on document [{}]. Cause: [{}].", tag, docName,
                    getRootCauseMessage(e));
                someStepFailed = true;
            }
        }
        if (someStepFailed) {
            return TagOperationResult.FAILED;
        } else if (hadEffectOnOneDoc) {
            return OK;
        } else {
            return NO_EFFECT;
        }
    }

    /**
     * Rename a tag. If a document already contains the new tag, the tag with the old name is simply removed. Otherwise,
     * the old tag is renamed to the new one.
     *
     * @param tag the current name of the tag
     * @param newTag the new name of the tag
     * @return {@link TagOperationResult#OK} if the operation completed successfully, {@link
     *     TagOperationResult#NO_EFFECT} if no page contains the tag to rename
     * @throws TagException in case of error when loading or updating the pages containing the tag to rename
     */
    public TagOperationResult renameTag(String tag, String newTag) throws TagException
    {
        // Since we're renaming a tag, we want to rename it even if the document is hidden. A hidden document is still
        // accessible to users, it's just not visible for simple users; it doesn't change permissions.
        List<String> docNamesToProcess = this.tagQueryManager.getDocumentsWithTag(tag, true);
        if (StringUtils.equals(tag, newTag) || docNamesToProcess.isEmpty() || StringUtils.isBlank(newTag)) {
            return NO_EFFECT;
        }

        XWikiContext context = this.xcontextProvider.get();

        for (String docName : docNamesToProcess) {
            XWikiDocument doc = loadDocument(this.documentReferenceResolver.resolve(docName));
            List<String> tags = getTagsFromDocument(doc);

            if (tags.stream().anyMatch(it -> it.equalsIgnoreCase(newTag))) {
                // The new tag might already be present in the document, in this case we just need to remove the old one
                removeTagFromDocument(tag, doc);
            } else {
                for (int i = 0; i < tags.size(); i++) {
                    if (tags.get(i).equalsIgnoreCase(tag)) {
                        tags.set(i, newTag);
                    }
                }
                setDocumentTags(doc, tags);

                // Since we're changing the document we need to set the new author
                doc.setAuthorReference(context.getUserReference());

                String comment =
                    this.contextualLocalizationManager.getTranslationPlain(DOC_COMMENT_TAG_RENAMED, tag, newTag);
                try {
                    context.getWiki().saveDocument(doc, comment, true, context);
                } catch (XWikiException e) {
                    throw new TagException(
                        MessageFormat
                            .format("Failed to save document [{0}] while renaming tag [{1}] to [{2}]", docName, tag,
                                newTag), e);
                }
            }
        }

        return OK;
    }

    private TagOperationResult removeTagFromDocument(String tag, XWikiDocument document) throws TagException
    {
        List<String> tags = getTagsFromDocument(document);

        List<String> newTags = tags.stream().filter(it2 -> !it2.equalsIgnoreCase(tag)).collect(Collectors.toList());
        boolean needsUpdate = tags.size() > newTags.size();

        if (needsUpdate) {
            setDocumentTags(document, newTags);
            String comment = this.contextualLocalizationManager.getTranslationPlain(DOC_COMMENT_TAG_REMOVED, tag);

            // Since we're changing the document we need to set the new author
            XWikiContext context = this.xcontextProvider.get();
            document.setAuthorReference(context.getUserReference());

            try {
                context.getWiki().saveDocument(document, comment, true, context);
            } catch (XWikiException e) {
                throw new TagException(
                    MessageFormat.format("Failed to save the document after removing tag [{0}]", tag), e);
            }

            return OK;
        } else {
            // Document doesn't contain this tag.
            return NO_EFFECT;
        }
    }

    private List<String> getTagsFromDocument(XWikiDocument document) throws TagException
    {
        return getTagListFromObject(getTagObject(document, false));
    }

    private List<String> getTagListFromObject(BaseObject tagObject) throws TagException
    {
        if (tagObject == null) {
            return new ArrayList<>();
        }
        try {
            List<String> tagList;
            PropertyInterface propertyInterface = tagObject.get(TAGS_FIELD_NAME);
            if (propertyInterface == null) {
                tagList = new ArrayList<>();
            } else {
                // It is important that the returned list is not the same instance as the list of the tags field, 
                // otherwise we are at risk to see it modified (persistently) by the caller (e.g., a velocity template).
                tagList = new ArrayList<>((List<String>) ((ListProperty) propertyInterface).getValue());
            }
            return tagList;
        } catch (XWikiException e) {
            throw new TagException(String.format("Failed to get the tags list of [%s]", tagObject), e);
        }
    }

    private BaseObject getTagObject(XWikiDocument document, boolean create)
    {
        return document.getXObject(getTagXClassReference(), create, this.xcontextProvider.get());
    }

    private XWikiDocument loadDocument(DocumentReference documentReference) throws TagException
    {
        try {
            return this.xcontextProvider.get().getWiki().getDocument(documentReference, this.xcontextProvider.get());
        } catch (XWikiException e) {
            throw new TagException(
                String.format("Failed to load a document from the document reference [%s]", documentReference),
                e);
        }
    }

    /**
     * @param collection a collection of strings
     * @param item a string
     * @return {@code true} if there is an item in the given collection that equals ignoring case the given string
     */
    private boolean containsIgnoreCase(List<String> collection, String item)
    {
        for (String existingItem : collection) {
            if (existingItem.equalsIgnoreCase(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set tags of the given document.
     *
     * @param document document to put the tags to.
     * @param tags list of tags.
     */
    private void setDocumentTags(XWikiDocument document, List<String> tags) throws TagException
    {
        BaseObject tagObject = getTagObject(document, true);

        try {
            BaseProperty baseProperty = (BaseProperty) tagObject.get(TAGS_FIELD_NAME);
            if (baseProperty == null) {
                baseProperty = createTagProperty(tagObject);
            }

            baseProperty.setValue(tags);
        } catch (XWikiException e) {
            throw new TagException(
                String.format("Failed to access the field [%s] of object [%s]", TAGS_FIELD_NAME, tagObject),
                e);
        }
    }

    private BaseProperty createTagProperty(BaseObject tagObject) throws TagException
    {
        try {
            BaseClass tagClass =
                this.xcontextProvider.get().getWiki().getXClass(getTagXClassReference(), this.xcontextProvider.get());
            PropertyClass tagPropertyDefinition = (PropertyClass) tagClass.getField(TAGS_FIELD_NAME);
            BaseProperty tagProperty = tagPropertyDefinition.newProperty();

            tagProperty.setName(TAGS_FIELD_NAME);
            tagProperty.setObject(tagObject);

            tagObject.put(TAGS_FIELD_NAME, tagProperty);
            return tagProperty;
        } catch (XWikiException e) {
            throw new TagException(String.format("Failed to initialize field [%s] on [%s]", TAGS_FIELD_NAME, tagObject),
                e);
        }
    }

    private DocumentReference getTagXClassReference()
    {
        return new DocumentReference(this.xcontextProvider.get().getWikiId(), "XWiki", "TagClass");
    }
}
