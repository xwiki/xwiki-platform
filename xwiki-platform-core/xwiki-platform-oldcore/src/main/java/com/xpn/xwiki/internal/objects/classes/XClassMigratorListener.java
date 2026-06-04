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
package com.xpn.xwiki.internal.objects.classes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.store.PropertyConverter;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Listen to classes modifications and automatically update objects accordingly when needed.
 * <p>
 * The actual conversion is done in {@link PropertyConverter}.
 *
 * @version $Id$
 * @since 7.1RC1
 */
// TODO: could be optimized a bit by listening to XClassUpdatedEvent and redoing the comparison between the two
// classes in case there is several changes to the class
@Component
@Singleton
@Named("XClassMigratorListener")
public class XClassMigratorListener extends AbstractEventListener
{
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private QueryManager queryManager;

    /**
     * Used for migrating the property values after a class is modified.
     */
    @Inject
    private PropertyConverter propertyConverter;

    @Inject
    private Logger logger;

    private record PropertyToUpdate(PropertyClass newPropertyClass, BaseProperty<?> newProperty) {};

    /**
     * Set up the listener.
     */
    public XClassMigratorListener()
    {
        super(XClassMigratorListener.class.getName(), new DocumentCreatedEvent(), new DocumentUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        if (event instanceof DocumentUpdatedEvent) {
            onDocumentUpdatedEvent(doc.getOriginalDocument(), doc, (XWikiContext) data);
        } else if (event instanceof DocumentCreatedEvent) {
            onDocumentCreatedEvent(doc, (XWikiContext) data);
        }
    }

    /**
     * @param doc the new version of the document
     * @param context the XWiki context
     */
    private void onDocumentCreatedEvent(XWikiDocument doc, XWikiContext context)
    {
        BaseClass baseClass = doc.getXClass();
        Collection<PropertyInterface> fieldList = baseClass.getFieldList();
        List<PropertyToUpdate> propertiesToUpdate = new ArrayList<>(fieldList.size());
        for (PropertyInterface property : fieldList) {
            maybeAddPropertyToUpdate((PropertyClass) property, null, propertiesToUpdate);
        }
        updateProperties(context, baseClass.getReference(), propertiesToUpdate);
    }

    /**
     * @param originalDoc the previous version of the document
     * @param doc the new version of the document
     * @param context the XWiki context
     */
    private void onDocumentUpdatedEvent(XWikiDocument originalDoc, XWikiDocument doc, XWikiContext context)
    {
        BaseClass baseClass = doc.getXClass();
        BaseClass baseClassOriginal = originalDoc.getXClass();

        List<PropertyToUpdate> propertiesToUpdate = new ArrayList<>();
        for (List<ObjectDiff> objectChanges : doc.getClassDiff(originalDoc, doc, context)) {
            for (ObjectDiff diff : objectChanges) {
                PropertyClass property = (PropertyClass) baseClass.getField(diff.getPropName());
                PropertyClass propertyOriginal = (PropertyClass) baseClassOriginal.getField(diff.getPropName());
                maybeAddPropertyToUpdate(property, propertyOriginal, propertiesToUpdate);
            }
        }
        updateProperties(context, baseClass.getReference(), propertiesToUpdate);
    }

    private void maybeAddPropertyToUpdate(PropertyClass newPropertyClass, PropertyClass previousPropertyClass,
        List<PropertyToUpdate> propertiesToUpdate)
    {
        boolean migrate = false;
        if (newPropertyClass != null) {
            BaseProperty<?> newProperty = newPropertyClass.newProperty();

            if (newProperty == null) {
                migrate = false;                
            } else if (previousPropertyClass != null) {
                BaseProperty<?> previousProperty = previousPropertyClass.newProperty();

                // New and previous class property generate different kind of properties
                migrate = previousProperty == null || newProperty.getClass() != previousProperty.getClass();
            } else {
                migrate = true;
            }

            if (migrate) {
                propertiesToUpdate.add(new PropertyToUpdate(newPropertyClass, newProperty));
            }
        }
    }

    private void updateProperties(XWikiContext context, DocumentReference classReference,
        List<PropertyToUpdate> propertiesToUpdate)
    {
        if (propertiesToUpdate.isEmpty()) {
            // to property to update in the end
            return;
        }

        // Get all the documents containing at least one object of the modified class
        List<XWikiDocument> documents;
        XWiki wiki = context.getWiki();
        String className = this.localSerializer.serialize(classReference);
        String wikiName = classReference.getWikiReference().getName();
        this.logger.info("Migrating objects in the [{}] wiki after the update of the [{}] class", wikiName, className);
        try {
            Query query = this.queryManager.createQuery(
                    "select distinct obj.name from BaseObject as obj where obj.className = :className", Query.HQL);
            query.bindValue("className", className);
            query.setWiki(wikiName);
            List<String> docs = query.execute();
            documents = new ArrayList<>(docs.size());
            for (String documentReference : docs) {
                try {
                    DocumentReference ref = this.resolver.resolve(documentReference, classReference);
                    XWikiDocument document = wiki.getDocument(ref, context);
                    if (!document.isNew()) {
                        // Avoid modifying the cached document
                        documents.add(document.clone());
                    }
                } catch (XWikiException e) {
                    this.logger.error("Failed to get document [{}] to apply the update of class [{}]",
                            documentReference, className, e);
                }
            }

            if (documents.isEmpty()) {
                this.logger.info("No documents to migrate after the update of class [{}] in the [{}] wiki",
                        className, wikiName);
                return;
            }
            this.logger.info("[{}] documents will be migrated in the [{}] wiki after the [{}] class update",
                    documents.size(), wikiName, className);
        } catch (QueryException e) {
            this.logger.error("Failed to get the documents to migrate after the update of class [{}] in the [{}] wiki",
                    className, wiki, e);
            return;
        }

        String currentWikiId = context.getWikiId();
        // Switch to class wiki to be safer
        context.setWikiId(wikiName);
        try {
            Set<XWikiDocument> documentsToSave = new HashSet<>(documents.size());
            for (PropertyToUpdate p : propertiesToUpdate) {
                updateProperty(documents, p.newPropertyClass, p.newProperty, documentsToSave);
            }

            this.logger.info("Migration of the [{}] class in the [{}] wiki: saving [{}] documents",
                    className, wikiName, documentsToSave.size());
            for (XWikiDocument document : documentsToSave) {
                try {
                    wiki.saveDocument(document, "Migrated class [" + className + "]", context);
                } catch (XWikiException e) {
                    this.logger.error("Failed to migrate document [{}] after update of class [{}]",
                            document.getDocumentReference(), className, e);
                }
            }
        } finally {
            // Restore context wiki
            context.setWikiId(currentWikiId);
        }

        this.logger.info("Migration of the [{}] class finished in the [{}] wiki", className, wikiName);
    }

    private void updateProperty(List<XWikiDocument> documents, PropertyClass newPropertyClass,
        BaseProperty<?> newProperty, Set<XWikiDocument> documentsToSave)
    {
        for (XWikiDocument document : documents) {
            ClassPropertyReference propertyReference = newPropertyClass.getReference();
            EntityReference classReference = propertyReference.extractReference(EntityType.DOCUMENT);

            boolean modified = false;

            for (BaseObject xobject : document.getXObjects(classReference)) {
                if (xobject != null) {
                    BaseProperty<?> property = (BaseProperty<?>) xobject.getField(propertyReference.getName());

                    // If the existing field is of different kind than what is produced by the new class property
                    if (property != null) {
                        modified = convert(xobject, property, newProperty, newPropertyClass);
                    } else {
                        modified = add(xobject, newPropertyClass);
                    }
                }
            }

            // If anything changed, save the document
            if (modified) {
                documentsToSave.add(document);
            }
        }
    }

    private boolean add(BaseObject xobject, PropertyClass newPropertyClass)
    {
        xobject.safeput(newPropertyClass.getName(), newPropertyClass.newProperty());

        return true;
    }

    private boolean convert(BaseObject xobject, BaseProperty<?> property, BaseProperty<?> newProperty,
        PropertyClass newPropertyClass)
    {
        if (newProperty != null && property.getClass() != newProperty.getClass()) {
            BaseProperty<?> convertedProperty = this.propertyConverter.convertProperty(property, newPropertyClass);

            // Set new field
            if (convertedProperty != null) {
                // Mark old field for removal, only if the conversion was successful, to avoid losing data.
                xobject.removeField(newPropertyClass.getName());

                xobject.safeput(newPropertyClass.getName(), convertedProperty);

                return true;
            }
        }

        return false;
    }
}
