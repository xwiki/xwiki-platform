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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.ClassPropertyReference;
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
import com.xpn.xwiki.internal.event.XClassPropertyUpdatedEvent;
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
// TODO: could probably be optimized a bit by listening to XClassUpdatedEvent and redoing the comparison between the two
// classes in case there is several changes to the class
public class XClassMigratorListener extends AbstractEventListener
{
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used for migrating the property values after a class is modified.
     */
    @Inject
    private PropertyConverter propertyConverter;

    @Inject
    private Logger logger;

    /**
     * Setup the listener.
     */
    public XClassMigratorListener()
    {
        super(XClassMigratorListener.class.getName(), new XClassPropertyUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XClassPropertyUpdatedEvent propertyEvent = (XClassPropertyUpdatedEvent) event;
        XWikiDocument newDocument = (XWikiDocument) source;
        XWikiDocument previousDocument = newDocument.getOriginalDocument();

        PropertyClass newPropertyClass =
            (PropertyClass) newDocument.getXClass().getField(propertyEvent.getReference().getName());
        PropertyClass previousPropertyClass =
            (PropertyClass) previousDocument.getXClass().getField(propertyEvent.getReference().getName());

        if (newPropertyClass != null && previousPropertyClass != null) {
            BaseProperty newProperty = newPropertyClass.newProperty();
            BaseProperty previousProperty = previousPropertyClass.newProperty();

            // New and previous class property generate different kind of properties
            if (newProperty.getClass() != previousProperty.getClass()) {
                try {
                    migrate(newPropertyClass);
                } catch (QueryException e) {
                    this.logger.error("Failed to migrate XClass property [{}]", newPropertyClass.getReference(), e);
                }
            }
        }
    }

    private void migrate(PropertyClass newPropertyClass) throws QueryException
    {
        ClassPropertyReference propertyReference = newPropertyClass.getReference();
        EntityReference classReference = propertyReference.extractReference(EntityType.DOCUMENT);
        EntityReference wikiReference = propertyReference.extractReference(EntityType.WIKI);

        // Get all document containing object of modified class
        Query query = this.queryManager
            .createQuery("from doc.object(" + this.localSerializer.serialize(classReference) + ") as obj", Query.XWQL);
        query.setWiki(wikiReference.getName());

        List<String> documents = query.execute();

        if (!documents.isEmpty()) {
            XWikiContext xcontext = this.xcontextProvider.get();

            String currentWikiId = xcontext.getWikiId();
            try {
                // Switch to class wiki to be safer
                xcontext.setWikiId(wikiReference.getName());

                for (String documentName : documents) {
                    try {
                        migrate(newPropertyClass, documentName, xcontext);
                    } catch (XWikiException e) {
                        this.logger.error("Failed to migrate property [{}] in document [{}]", propertyReference,
                            documentName, xcontext);
                    }
                }
            } finally {
                // Restore context wiki
                xcontext.setWikiId(currentWikiId);
            }
        }
    }

    private void migrate(PropertyClass newPropertyClass, String documentName, XWikiContext xcontext)
        throws XWikiException
    {
        BaseProperty newProperty = newPropertyClass.newProperty();

        ClassPropertyReference propertyReference = newPropertyClass.getReference();
        EntityReference classReference = propertyReference.extractReference(EntityType.DOCUMENT);

        XWikiDocument document =
            xcontext.getWiki().getDocument(this.resolver.resolve(documentName, classReference), xcontext);

        boolean modified = false;

        for (BaseObject xobject : document.getXObjects(classReference)) {
            if (xobject != null) {
                BaseProperty property = (BaseProperty) xobject.getField(propertyReference.getName());

                // If the existing field is of different kind than what is produced by the new class property
                if (property != null && property.getClass() != newProperty.getClass()) {
                    BaseProperty<?> convertedProperty =
                        this.propertyConverter.convertProperty(property, newPropertyClass);

                    // Set new field
                    if (convertedProperty != null) {
                        // Mark old field for removal, only if the conversion was successful, to avoid losing data.
                        xobject.removeField(propertyReference.getName());

                        // Don't set the new property if it's null (it means the property is not set).
                        xobject.safeput(propertyReference.getName(), convertedProperty);

                        modified = true;
                    }
                }
            }
        }

        // If anything changed save the document
        if (modified) {
            xcontext.getWiki().saveDocument(document, "Migrated property [" + propertyReference.getName()
                + "] from class [" + this.localSerializer.serialize(classReference) + "]", xcontext);
        }
    }
}
