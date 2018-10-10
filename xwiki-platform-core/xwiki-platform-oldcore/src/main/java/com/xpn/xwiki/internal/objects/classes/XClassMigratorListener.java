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
import com.xpn.xwiki.internal.event.AbstractXClassPropertyEvent;
import com.xpn.xwiki.internal.event.XClassPropertyAddedEvent;
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
// TODO: could be optimized a bit by listening to XClassUpdatedEvent and redoing the comparison between the two
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
        super(XClassMigratorListener.class.getName(), new XClassPropertyUpdatedEvent(), new XClassPropertyAddedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        AbstractXClassPropertyEvent propertyEvent = (AbstractXClassPropertyEvent) event;
        XWikiDocument newDocument = (XWikiDocument) source;
        XWikiDocument previousDocument = newDocument.getOriginalDocument();

        PropertyClass newPropertyClass =
            (PropertyClass) newDocument.getXClass().getField(propertyEvent.getReference().getName());
        PropertyClass previousPropertyClass =
            (PropertyClass) previousDocument.getXClass().getField(propertyEvent.getReference().getName());

        boolean migrate = false;
        if (newPropertyClass != null) {
            BaseProperty<?> newProperty = newPropertyClass.newProperty();

            if (previousPropertyClass != null) {
                BaseProperty<?> previousProperty = previousPropertyClass.newProperty();

                // New and previous class property generate different kind of properties
                migrate = newProperty.getClass() != previousProperty.getClass();
            } else {
                migrate = true;
            }

            if (migrate) {
                try {
                    migrate(newPropertyClass, newProperty);
                } catch (QueryException e) {
                    this.logger.error("Failed to migrate XClass property [{}]", newPropertyClass.getReference(), e);
                }
            }
        }
    }

    private void migrate(PropertyClass newPropertyClass, BaseProperty<?> newProperty) throws QueryException
    {
        ClassPropertyReference propertyReference = newPropertyClass.getReference();
        EntityReference classReference = propertyReference.extractReference(EntityType.DOCUMENT);
        EntityReference wikiReference = propertyReference.extractReference(EntityType.WIKI);

        // Get all document containing object of modified class
        Query query = this.queryManager.createQuery("select distinct doc.fullName from Document doc, doc.object("
            + this.localSerializer.serialize(classReference) + ") as obj", Query.XWQL);
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
                        migrate(newPropertyClass, newProperty, documentName, xcontext);
                    } catch (XWikiException e) {
                        this.logger.error("Failed to migrate property [{}] in document [{}]", propertyReference,
                            documentName);
                    }
                }
            } finally {
                // Restore context wiki
                xcontext.setWikiId(currentWikiId);
            }
        }
    }

    private void migrate(PropertyClass newPropertyClass, BaseProperty<?> newProperty, String documentName,
        XWikiContext xcontext) throws XWikiException
    {
        ClassPropertyReference propertyReference = newPropertyClass.getReference();
        EntityReference classReference = propertyReference.extractReference(EntityType.DOCUMENT);

        XWikiDocument document =
            xcontext.getWiki().getDocument(this.resolver.resolve(documentName, classReference), xcontext);

        if (!document.isNew()) {
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

            // If anything changed save the document
            if (modified) {
                xcontext.getWiki().saveDocument(document, "Migrated property [" + propertyReference.getName()
                    + "] from class [" + this.localSerializer.serialize(classReference) + "]", xcontext);
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
        if (property.getClass() != newProperty.getClass()) {
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
