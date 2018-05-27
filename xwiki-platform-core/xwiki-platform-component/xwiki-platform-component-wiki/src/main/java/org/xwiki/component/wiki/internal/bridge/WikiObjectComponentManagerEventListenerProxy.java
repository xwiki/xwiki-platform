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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiObjectComponentBuilder;
import org.xwiki.component.wiki.internal.WikiComponentManagerEventListenerHelper;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * This component allows the {@link DefaultWikiObjectComponentManagerEventListener} to easily register new XObject based
 * components against the {@link WikiComponentManagerEventListenerHelper}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component(roles = WikiObjectComponentManagerEventListenerProxy.class)
@Singleton
public class WikiObjectComponentManagerEventListenerProxy
{
    @Inject
    private Logger logger;

    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManager;

    @Inject
    private WikiComponentManagerEventListenerHelper wikiComponentManagerEventListenerHelper;

    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<XWikiContext> xWikiContextProvider;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    /**
     * This method is responsible look through every {@link WikiObjectComponentBuilder} and get their role hints, which
     * is also the class name of the wiki object they should be working with.
     */
    private List<EntityReference> collectWikiObjectsList()
    {
        List<EntityReference> wikiObjectsList = new ArrayList<>();

        try {
            // Get a list of WikiObjectComponentBuilder
            List<WikiObjectComponentBuilder> componentBuilders =
                    contextComponentManager.get().getInstanceList(WikiObjectComponentBuilder.class);

            for (WikiObjectComponentBuilder componentBuilder : componentBuilders) {
                wikiObjectsList.add(componentBuilder.getClassReference());
            }
        } catch (ComponentLookupException e) {
            logger.warn("Unable to collect a list of wiki objects components: %s", e);
        }

        return wikiObjectsList;
    }

    /**
     * This method goes through every XObject known
     * (using {@link WikiObjectComponentManagerEventListenerProxy#collectWikiObjectsList()})
     * to be able to create a component and then builds those components.
     */
    public void registerAllObjectComponents()
    {
        XWikiContext xWikiContext = this.xWikiContextProvider.get();
        String builderHelper;

        // For every classes subject to WikiComponents
        for (EntityReference xObjectClass : this.collectWikiObjectsList()) {
            try {
                // Query every document having an XObject of the given class attached
                Query query =
                        queryManager.createQuery("select distinct doc.fullName from Document doc, "
                                                + "doc.object(" + this.entityReferenceSerializer.serialize(xObjectClass)
                                                + ") as document", Query.XWQL);

                List<String> results = query.execute();

                DocumentReference sourceDocumentReference;
                for (String result : results) {
                    sourceDocumentReference = this.currentDocumentReferenceResolver.resolve(result);
                    XWikiDocument document = xWikiContext.getWiki().getDocument(sourceDocumentReference, xWikiContext);

                    for (BaseObject xObject : document.getXObjects(xObjectClass)) {
                        BaseObjectReference xObjectReference = xObject.getReference();
                        builderHelper = this.entityReferenceSerializer.serialize(xObjectReference.getXClassReference());

                        this.registerObjectComponents(xObjectReference, xObject,
                                contextComponentManager.get().getInstance(
                                        WikiObjectComponentBuilder.class, builderHelper));
                    }
                }
            } catch (Exception e) {
                logger.warn(
                        String.format("Unable to register the components for [%s] XObjects: %s",
                                xObjectClass,
                                e.getMessage()));
            }
        }
    }

    /**
     * This method uses the given objectReference and a XWikiDocument that is the source of every
     * {@link com.xpn.xwiki.internal.event.XObjectEvent} to build and register the component(s) contained in
     * this entity reference.
     *
     * @param objectReference the reference containing the parameters needed to instantiate the new component(s)
     * @param baseObject the base object corresponding to the XObject
     * @param componentBuilder the builder that should be used in order to build the component
     */
    public void registerObjectComponents(ObjectReference objectReference, BaseObject baseObject,
            WikiObjectComponentBuilder componentBuilder)
    {
        // Unregister all wiki components registered under the given entity. We do this as otherwise we would need to
        // handle the specific cases of elements added, elements updated and elements deleted, etc.
        // Instead we unregister all wiki components and re-register them all.
        this.wikiComponentManagerEventListenerHelper.unregisterComponents(objectReference);

        try {
            /* If we are dealing with a WikiBaseObjectComponentBuilder, we directly get the base object corresponding to
             * the current event and build the components from it. */
            List<WikiComponent> wikiComponents;
            if (componentBuilder instanceof WikiBaseObjectComponentBuilder) {
                wikiComponents = ((WikiBaseObjectComponentBuilder) componentBuilder).buildComponents(baseObject);
            } else {
                wikiComponents = componentBuilder.buildComponents(objectReference);
            }

            this.wikiComponentManagerEventListenerHelper.registerComponentList(wikiComponents);
        } catch (WikiComponentException e) {
            logger.warn(String.format(
                    "Unable to register the component associated to [%s]: %s", objectReference,
                    ExceptionUtils.getRootCauseMessage(e)));
        }
    }

    /**
     * Unregisters the component(s) linked to the given entity reference from the component manager.
     *
     * @param xObjectReference the reference containing every component that should be removed from the CM
     */
    public void unregisterObjectComponents(ObjectReference xObjectReference)
    {
        this.wikiComponentManagerEventListenerHelper.unregisterComponents(xObjectReference);
    }

    /**
     * Get a list of XClasses that are able to produce a component (sc : that have a
     * {@link WikiObjectComponentBuilder} bound to it).
     *
     * @return the list of classes
     */
    public List<EntityReference> getWikiObjectsList()
    {
        return this.collectWikiObjectsList();
    }
}
