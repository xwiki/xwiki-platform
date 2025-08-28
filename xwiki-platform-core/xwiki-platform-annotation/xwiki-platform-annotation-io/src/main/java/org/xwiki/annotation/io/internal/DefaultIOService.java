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
package org.xwiki.annotation.io.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.AnnotationConfiguration;
import org.xwiki.annotation.io.IOService;
import org.xwiki.annotation.io.IOServiceException;
import org.xwiki.annotation.maintainer.AnnotationState;
import org.xwiki.annotation.reference.TypedStringEntityReferenceResolver;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Default {@link IOService} implementation, based on storing annotations in XWiki Objects in XWiki documents. The
 * targets manipulated by this implementation are XWiki references, such as xwiki:Space.Page for documents or with an
 * object and property reference if the target is an object property. Use the reference module to generate the
 * references passed to this module, so that they can be resolved to XWiki content back by this implementation.
 *
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Singleton
public class DefaultIOService implements IOService
{
    /**
     * The execution used to get the deprecated XWikiContext.
     */
    @Inject
    private Execution execution;

    /**
     * Entity reference handler to resolve the reference target.
     */
    @Inject
    private TypedStringEntityReferenceResolver referenceResolver;

    /**
     * Default entity reference serializer to create document full names.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Local entity reference serializer, to create references which are robust to import / export.
     */
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    /**
     * The logger to use for logging.
     */
    @Inject
    private Logger logger;

    /**
     * The Annotation Application's configuration.
     */
    @Inject
    private AnnotationConfiguration configuration;

    /**
     * {@inheritDoc}
     * <p>
     * This implementation saves the added annotation in the document where the target of the annotation is.
     * </p>
     *
     * @see org.xwiki.annotation.io.IOService#addAnnotation(String, org.xwiki.annotation.Annotation)
     */
    @Override
    public void addAnnotation(String target, Annotation annotation) throws IOServiceException
    {
        try {
            // extract the document name from the passed target
            // by default the fullname is the passed target
            String documentFullName = target;
            EntityReference targetReference = this.referenceResolver.resolve(target, EntityType.DOCUMENT);
            // try to get a document reference from the passed target reference
            EntityReference docRef = targetReference.extractReference(EntityType.DOCUMENT);
            if (docRef != null) {
                documentFullName = this.serializer.serialize(docRef);
            }
            // now get the document with that name
            XWikiContext deprecatedContext = getXWikiContext();
            XWikiDocument document = deprecatedContext.getWiki().getDocument(documentFullName, deprecatedContext);
            // Avoid modifying the cached document
            document = document.clone();
            // create a new object in this document to hold the annotation
            // Make sure to use a relative reference when creating the XObject, since we can`t use absolute references
            // for an object's class. This avoids ugly log warning messages.
            EntityReference annotationClassReference = this.configuration.getAnnotationClassReference();
            annotationClassReference =
                annotationClassReference.removeParent(annotationClassReference.extractReference(EntityType.WIKI));
            int id = document.createXObject(annotationClassReference, deprecatedContext);
            BaseObject object = document.getXObject(this.configuration.getAnnotationClassReference(), id);
            updateObject(object, annotation, deprecatedContext);
            // and set additional data: author to annotation author, date to now and the annotation target
            object.set(Annotation.DATE_FIELD, new Date(), deprecatedContext);
            // TODO: maybe we shouldn't trust what we receive from the caller but set the author from the context.
            // Or the other way around, set the author of the document from the annotations author.
            object.set(Annotation.AUTHOR_FIELD, annotation.getAuthor(), deprecatedContext);
            // store the target of this annotation, serialized with a local serializer, to be exportable and importable
            // in a different wiki
            // TODO: figure out if this is the best idea in terms of target serialization
            // 1/ the good part is that it is a fixed value that can be searched with a query in all objects in the wiki
            // 2/ the bad part is that copying a document to another space will not also update its annotation targets
            // 3/ if annotations are stored in the same document they annotate, the targets are only required for object
            // fields
            // ftm don't store the type of the reference since we only need to recognize the field, not to also read it.
            if (targetReference.getType() == EntityType.OBJECT_PROPERTY
                || targetReference.getType() == EntityType.DOCUMENT)
            {
                object.set(Annotation.TARGET_FIELD, this.localSerializer.serialize(targetReference), deprecatedContext);
            } else {
                object.set(Annotation.TARGET_FIELD, target, deprecatedContext);
            }
            // set the author of the document to the current user
            document.setAuthor(deprecatedContext.getUser());
            // Note: We make sure to only provide a few characters of contextual information in order to control the
            // size of the comment (we display the first 30 characters).
            deprecatedContext.getWiki().saveDocument(document, "Added annotation on \""
                + StringUtils.abbreviate(annotation.getSelection(), 30) + "\"", deprecatedContext);
        } catch (XWikiException e) {
            throw new IOServiceException("An exception message has occurred while saving the annotation", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation retrieves all the objects of the annotation class in the document where target points to, and
     * which have the target set to {@code target}.
     * </p>
     *
     * @see org.xwiki.annotation.io.IOService#getAnnotations(String)
     */
    @Override
    public Collection<Annotation> getAnnotations(String target) throws IOServiceException
    {
        try {
            // parse the target and extract the local reference serialized from it, by the same rules
            EntityReference targetReference = this.referenceResolver.resolve(target, EntityType.DOCUMENT);
            // build the target identifier for the annotation
            String localTargetId = target;
            // and the name of the document where it should be stored
            String docName = target;
            if (targetReference.getType() == EntityType.DOCUMENT
                || targetReference.getType() == EntityType.OBJECT_PROPERTY)
            {
                localTargetId = this.localSerializer.serialize(targetReference);
                docName = this.serializer.serialize(targetReference.extractReference(EntityType.DOCUMENT));
            }
            // get the document
            XWikiContext deprecatedContext = getXWikiContext();
            XWikiDocument document = deprecatedContext.getWiki().getDocument(docName, deprecatedContext);
            // and the annotation class objects in it
            List<BaseObject> objects = document.getXObjects(this.configuration.getAnnotationClassReference());
            // and build a list of Annotation objects
            List<Annotation> result = new ArrayList<>();
            if (objects == null) {
                return Collections.emptySet();
            }
            for (BaseObject object : objects) {
                // if it's not on the required target, ignore it
                if (object == null || !localTargetId.equals(object.getStringValue(Annotation.TARGET_FIELD))) {
                    continue;
                }
                // use the object number as annotation id
                result.add(loadAnnotationFromObject(object, deprecatedContext));
            }
            return result;
        } catch (XWikiException e) {
            throw new IOServiceException("An exception has occurred while loading the annotations", e);
        }
    }

    @Override
    public Annotation getAnnotation(String target, String annotationID) throws IOServiceException
    {
        try {
            if (annotationID == null || target == null) {
                return null;
            }
            // parse the target and extract the local reference serialized from it, by the same rules
            EntityReference targetReference = this.referenceResolver.resolve(target, EntityType.DOCUMENT);
            // build the target identifier for the annotation
            String localTargetId = target;
            // and the name of the document where it should be stored
            String docName = target;
            if (targetReference.getType() == EntityType.DOCUMENT
                || targetReference.getType() == EntityType.OBJECT_PROPERTY)
            {
                localTargetId = this.localSerializer.serialize(targetReference);
                docName = this.serializer.serialize(targetReference.extractReference(EntityType.DOCUMENT));
            }
            // get the document
            XWikiContext deprecatedContext = getXWikiContext();
            XWikiDocument document = deprecatedContext.getWiki().getDocument(docName, deprecatedContext);
            // and the annotation class objects in it
            // parse the annotation id as object index
            BaseObject object =
                document.getXObject(this.configuration.getAnnotationClassReference(),
                    Integer.valueOf(annotationID));
            if (object == null || !localTargetId.equals(object.getStringValue(Annotation.TARGET_FIELD))) {
                return null;
            }
            // use the object number as annotation id
            return loadAnnotationFromObject(object, deprecatedContext);
        } catch (NumberFormatException e) {
            throw new IOServiceException("Could not parse annotation id " + annotationID, e);
        } catch (XWikiException e) {
            throw new IOServiceException("An exception has occurred while loading the annotation with id "
                + annotationID, e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation deletes the annotation object with the object number indicated by {@code annotationID} from
     * the document indicated by {@code target}, if its stored target matches the passed target.
     * </p>
     *
     * @see org.xwiki.annotation.io.IOService#removeAnnotation(String, String)
     */
    @Override
    public void removeAnnotation(String target, String annotationID) throws IOServiceException
    {
        try {
            if (annotationID == null || target == null) {
                return;
            }

            EntityReference targetReference = this.referenceResolver.resolve(target, EntityType.DOCUMENT);
            // get the target identifier and the document name from the parsed reference
            String localTargetId = target;
            String docName = target;
            if (targetReference.getType() == EntityType.DOCUMENT
                || targetReference.getType() == EntityType.OBJECT_PROPERTY)
            {
                localTargetId = this.localSerializer.serialize(targetReference);
                docName = this.serializer.serialize(targetReference.extractReference(EntityType.DOCUMENT));
            }
            // get the document
            XWikiContext deprecatedContext = getXWikiContext();
            XWikiDocument document = deprecatedContext.getWiki().getDocument(docName, deprecatedContext);
            if (document.isNew()) {
                // if the document doesn't exist already skip it
                return;
            }
            // Avoid modifying the cached document
            document = document.clone();

            // and the document object on it
            BaseObject annotationObject =
                document.getXObject(this.configuration.getAnnotationClassReference(),
                    Integer.valueOf(annotationID));

            // if object exists and its target matches the requested target, delete it
            if (annotationObject != null
                && localTargetId.equals(annotationObject.getStringValue(Annotation.TARGET_FIELD)))
            {
                document.removeObject(annotationObject);
                document.setAuthor(deprecatedContext.getUser());
                deprecatedContext.getWiki().saveDocument(document, "Deleted annotation " + annotationID,
                    deprecatedContext);
            }
        } catch (NumberFormatException e) {
            throw new IOServiceException("An exception has occurred while parsing the annotation id", e);
        } catch (XWikiException e) {
            throw new IOServiceException("An exception has occurred while removing the annotation", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation which gets all the annotation class objects in the document pointed by the target, and matches
     * their ids against the ids in the passed collection of annotations. If they match, they are updated with the new
     * data in the annotations in annotation.
     * </p>
     *
     * @see org.xwiki.annotation.io.IOService#updateAnnotations(String, java.util.Collection)
     */
    @Override
    public void updateAnnotations(String target, Collection<Annotation> annotations) throws IOServiceException
    {
        try {
            EntityReference targetReference = this.referenceResolver.resolve(target, EntityType.DOCUMENT);
            // get the document name from the parsed reference
            String docName = target;
            if (targetReference.getType() == EntityType.DOCUMENT
                || targetReference.getType() == EntityType.OBJECT_PROPERTY)
            {
                docName = this.serializer.serialize(targetReference.extractReference(EntityType.DOCUMENT));
            }
            // get the document pointed to by the target
            XWikiContext deprecatedContext = getXWikiContext();
            XWikiDocument document = deprecatedContext.getWiki().getDocument(docName, deprecatedContext);
            // Avoid modifying the cached document
            document = document.clone();
            List<String> updateNotifs = new ArrayList<>();
            boolean updated = false;
            for (Annotation annotation : annotations) {
                // parse annotation id as string. If cannot parse, then ignore annotation, is not valid
                int annId = 0;
                try {
                    annId = Integer.parseInt(annotation.getId());
                } catch (NumberFormatException e) {
                    continue;
                }
                BaseObject object = document.getXObject(this.configuration.getAnnotationClassReference(), annId);
                if (object == null) {
                    continue;
                }
                updated = updateObject(object, annotation, deprecatedContext) || updated;
                updateNotifs.add(annotation.getId());
            }
            if (updated) {
                // set the author of the document to the current user
                document.setAuthor(deprecatedContext.getUser());
                deprecatedContext.getWiki().saveDocument(document, "Updated annotations", deprecatedContext);
            }
        } catch (XWikiException e) {
            throw new IOServiceException("An exception has occurred while updating the annotation", e);
        }
    }

    /**
     * Helper function to load an annotation object from an xwiki object.
     *
     * @param object the xwiki object to load an annotation from
     * @param deprecatedContext XWikiContext to make operations on xwiki data
     * @return the Annotation instance for the annotation stored in BaseObject
     */
    protected Annotation loadAnnotationFromObject(BaseObject object, XWikiContext deprecatedContext)
    {
        // load the annotation with its ID, special handling of the state since it needs deserialization, special
        // handling of the original selection which shouldn't be set if it's empty
        Annotation annotation = new Annotation(String.valueOf(object.getNumber()));
        annotation.setState(AnnotationState.valueOf(object.getStringValue(Annotation.STATE_FIELD)));
        String originalSelection = object.getStringValue(Annotation.ORIGINAL_SELECTION_FIELD);
        if (originalSelection != null && originalSelection.length() > 0) {
            annotation.setOriginalSelection(originalSelection);
        }

        Collection<String> skippedFields =
            Arrays.asList(Annotation.ORIGINAL_SELECTION_FIELD, Annotation.STATE_FIELD);
        // go through all props and load them in the annotation, except for the ones already loaded
        // get all the props, filter those that need to be skipped and save the rest
        for (String propName : object.getPropertyNames()) {
            if (!skippedFields.contains(propName)) {
                try {
                    annotation.set(propName, ((BaseProperty) object.get(propName)).getValue());
                } catch (XWikiException e) {
                    this.logger.warn("Unable to get property " + propName + " from object " + object.getClassName()
                        + "[" + object.getNumber() + "]. Will not be saved in the annotation.", e);
                }
            }
        }
        return annotation;
    }

    /**
     * Helper function to update object from an annotation.
     *
     * @param object the object to update
     * @param annotation the annotation to marshal in the object
     * @param deprecatedContext the XWikiContext execute object operations
     * @return {@code true} if any modification was done on this object, {@code false} otherwise
     */
    protected boolean updateObject(BaseObject object, Annotation annotation, XWikiContext deprecatedContext)
    {
        boolean updated = false;
        // TODO: there's an issue here to solve with (custom) types which need to be serialized before saved. Some do,
        // some don't.... Custom field types in the annotation map should match the types accepted by the object
        // special handling for state which needs to be string serialized, since the prop in the class is string and the
        // state is an enum
        updated =
            setIfNotNull(object, Annotation.STATE_FIELD, annotation.getState() == null ? null : annotation.getState()
                .toString(), deprecatedContext)
                || updated;
        // don't reset the state, the date (which will be set now, upon save), and ignore anything that could overwrite
        // the target. Don't set the author either, will be set by caller, if needed
        Collection<String> skippedFields =
            Arrays.asList(Annotation.STATE_FIELD, Annotation.DATE_FIELD, Annotation.AUTHOR_FIELD,
                Annotation.TARGET_FIELD);
        // all fields in the annotation, try to put them in object (I wonder what happens if I can't...)
        for (String propName : annotation.getFieldNames()) {
            if (!skippedFields.contains(propName)) {
                updated = setIfNotNull(object, propName, annotation.get(propName), deprecatedContext) || updated;
            }
        }

        return updated;
    }

    /**
     * Helper function to set a field on an object only if the new value is not null. If you wish to reset the value of
     * a field, pass the empty string for the new value.
     *
     * @param object the object to set the value of the field
     * @param fieldName the name of the field to set
     * @param newValue the new value to set to the field. It will be ignored if it's {@code null}
     * @param deprecatedContext the XWikiContext
     * @return {@code true} if the field was set to newValue, {@code false} otherwise
     */
    protected boolean setIfNotNull(BaseObject object, String fieldName, Object newValue, XWikiContext deprecatedContext)
    {
        if (newValue != null) {
            object.set(fieldName, newValue, deprecatedContext);
            return true;
        }
        return false;
    }

    /**
     * @return the deprecated xwiki context used to manipulate xwiki objects
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
}
