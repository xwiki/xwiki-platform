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
package org.xwiki.component.wiki.internal.bridge;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentBuilder;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiObjectComponentBuilder;
import org.xwiki.component.wiki.internal.DefaultWikiObjectComponentManagerEventListener;
import org.xwiki.component.wiki.internal.WikiComponentManagerRegistrationHelper;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * This component allows the {@link DefaultWikiObjectComponentManagerEventListener} to easily register new XObject based
 * components against the {@link WikiComponentManagerRegistrationHelper}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component(roles = WikiObjectComponentManagerRegistererProxy.class)
@Singleton
public class WikiObjectComponentManagerRegistererProxy
{
    @Inject
    private Logger logger;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private WikiComponentManagerRegistrationHelper wikiComponentManagerRegistrationHelper;

    @Inject
    private QueryManager queryManager;

    @Inject
    private WikiComponentBridge wikiComponentBridge;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    /**
     * This list contains every XObject classes that can produce a component through a {@link WikiComponentBuilder}.
     * The list is initialized through {@link #collectWikiObjectsList()}, which is called on
     * {@link ApplicationReadyEvent} and {@link WikiReadyEvent}.
     */
    private List<String> wikiObjectsList;

    /**
     * This method is responsible look through every {@link WikiObjectComponentBuilder} and get their role hints, which
     * is also the class name of the wiki object they should be working with.
     */
    public void collectWikiObjectsList()
    {
        try {
            // Get a list of WikiObjectComponentBuilder
            List<WikiObjectComponentBuilder> componentBuilders =
                    this.componentManager.getInstanceList(WikiObjectComponentBuilder.class);

            this.wikiObjectsList = new ArrayList<>();
            for (WikiObjectComponentBuilder componentBuilder : componentBuilders) {
                this.wikiObjectsList.add(componentBuilder.getClassReference().getName());
            }
        } catch (ComponentLookupException e) {
            logger.warn("Unable to collect a list of wiki objects components: %s", e);
        }
    }

    /**
     * This method goes through every XObject known
     * (using {@link WikiObjectComponentManagerRegistererProxy#collectWikiObjectsList()})
     * to be able to create a component and then builds those components.
     */
    public void registerAllObjectComponents()
    {
        // For every classes subject to WikiComponents
        for (String xObjectClass : this.wikiObjectsList) {
            LocalDocumentReference xObjectLocalDocumentReference = new LocalDocumentReference(
                    this.currentDocumentReferenceResolver.resolve(xObjectClass));

            try {
                // Query every document having an XObject of the given class attached
                Query query =
                        queryManager.createQuery("select distinct doc.fullName from Document doc, "
                                                + "doc.object(" + xObjectClass + ") as document", Query.XWQL);

                List<String> results = query.execute();

                DocumentReference sourceDocumentReference;
                for (String result : results) {
                    sourceDocumentReference = this.currentDocumentReferenceResolver.resolve(result);
                    XWikiDocument document = this.wikiComponentBridge.getDocument(sourceDocumentReference);

                    for (BaseObject xObject : document.getXObjects(xObjectLocalDocumentReference)) {
                        this.registerObjectComponents(
                                xObject.getReference(), this.wikiComponentBridge.getDocument(sourceDocumentReference));
                    }
                }
            } catch (Exception e) {
                logger.warn(
                        String.format("Unable to fetch document references for [%s] XObjects: %s",
                                xObjectClass,
                                e.getMessage()));
            }
        }
    }

    /**
     * This method uses the given entityReference and a XWikiDocument that is the source of every
     * {@link com.xpn.xwiki.internal.event.XObjectEvent} to build and register the component(s) contained in
     * this entity reference.
     *
     * @param entityReference the reference containing the parameters needed to instanciate the new component(s)
     * @param source the source of the event triggering this method
     */
    public void registerObjectComponents(EntityReference entityReference, XWikiDocument source)
    {
        // Unregister all wiki components registered under the given entity. We do this as otherwise we would need to
        // handle the specific cases of elements added, elements updated and elements deleted, etc.
        // Instead we unregister all wiki components and re-register them all.
        this.wikiComponentManagerRegistrationHelper.unregisterComponents(entityReference);

        try {
            // Try to retrieve a WikiObjectComponentBuilder related to the XObject
            WikiObjectComponentBuilder componentBuilder =
                    this.componentManager.getInstance(WikiObjectComponentBuilder.class,
                            entityReferenceSerializer.serialize(
                                    ((BaseObjectReference) entityReference).getXClassReference()));

            /* If we are dealing with a WikiBaseObjectComponentBuilder, we directly get the base object corresponding to
             * the current event and build the components from it. */
            List<WikiComponent> wikiComponents;
            if (componentBuilder instanceof WikiBaseObjectComponentBuilder) {
                wikiComponents = ((WikiBaseObjectComponentBuilder) componentBuilder)
                        .buildComponents(source.getXObject(entityReference));
            } else {
                wikiComponents = componentBuilder.buildComponents(entityReference);
            }

            this.wikiComponentManagerRegistrationHelper.registerComponentList(wikiComponents);
        } catch (ComponentLookupException e) {
            logger.warn(String.format(
                    "Unable to retrieve the WikiObjectComponentBuilder associated with [%s]: %s",
                    entityReference, ExceptionUtils.getRootCauseMessage(e)));
        } catch (WikiComponentException e) {
            logger.warn(String.format(
                    "Unable to register the component associated to [%s]: %s", entityReference,
                    ExceptionUtils.getRootCauseMessage(e)));
        }
    }

    /**
     * Unregisters the component(s) linked to the given entity reference from the component manager.
     *
     * @param xObjectReference the reference containing every component that should be removed from the CM
     */
    public void unregisterObjectComponents(EntityReference xObjectReference)
    {
        this.wikiComponentManagerRegistrationHelper.unregisterComponents(xObjectReference);
    }

    /**
     * Get a list of XClasses that are able to produce a component (sc : that have a
     * {@link WikiObjectComponentBuilder} bound to it).
     *
     * @return the list of classes
     */
    public List<String> getWikiObjectsList()
    {
        return this.wikiObjectsList;
    }
}
