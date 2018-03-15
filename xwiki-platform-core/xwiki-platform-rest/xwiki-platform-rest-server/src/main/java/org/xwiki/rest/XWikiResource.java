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
package org.xwiki.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.ObjectFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * Base class for all XWiki-related JAX-RS resources. This class provides to subclasses a set of protected fields to
 * access the XWiki API and a method for retrieving documents in their different incarnations.
 *
 * @version $Id$
 */
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class XWikiResource implements XWikiRestComponent, Initializable
{
    /**
     * The actual URI information about the JAX-RS resource being called. This variable is useful when generating links
     * to other resources in representations.
     */
    @Context
    protected UriInfo uriInfo;

    /**
     * The logger to be used to output log messages.
     * 
     * @deprecated since 7.3M1, use {@link #slf4Jlogger} instead
     */
    @Deprecated
    protected java.util.logging.Logger logger;

    /**
     * The logger to be used to output log messages.
     * 
     * @since 7.4.5
     * @since 8.3C1
     */
    @Inject
    protected Logger slf4Jlogger;

    /**
     * The object factory for model objects to be used when creating representations.
     */
    protected ObjectFactory objectFactory;

    /**
     * The XWiki component manager that is used to lookup XWiki components and context.
     */
    @Inject
    @Named("context")
    protected ComponentManager componentManager;

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    /**
     * The query manager to be used to perform low-level queries for retrieving information about wiki content.
     */
    @Inject
    protected QueryManager queryManager;

    /**
     * A wrapper class for returning an XWiki document enriched with information about its status.
     */
    protected static class DocumentInfo
    {
        /**
         * The target XWiki document.
         */
        private Document document;

        /**
         * A boolean variable stating if the XWiki document existed already of it is being created. This variable is
         * used when building responses in order to understand if a created or modified status code should be sent.
         */
        private boolean created;

        public DocumentInfo(Document document, boolean created)
        {
            this.document = document;
            this.created = created;
        }

        public Document getDocument()
        {
            return document;
        }

        public boolean isCreated()
        {
            return created;
        }
    }

    /**
     * Resource initialization.
     */
    @Override
    public void initialize() throws InitializationException
    {
        logger = java.util.logging.Logger.getLogger(this.getClass().getName());

        objectFactory = new ObjectFactory();

        this.slf4Jlogger.trace("Resource {} initialized. Serving user: '{}'\n", getClass().getName(),
            Utils.getXWikiUser(componentManager));
    }

    /**
     * @param spaceSegments the space segments of the URL
     * @return the list of parent spaces
     * @throws XWikiRestException if the URL is malformed
     */
    public List<String> parseSpaceSegments(String spaceSegments) throws XWikiRestException
    {
        // The URL format is: "spaces/A/spaces/B/spaces/C" to actually point to the space "A.B.C".
        List<String> spaces = new ArrayList<>();
        // We actually don't get the first "spaces/" segment so we start from the first space
        int i = 1;
        for (String space : spaceSegments.split("/")) {
            if (i % 2 == 0) {
                // Every 2 segments, we should have "spaces". If not, the URL is malformed
                if (!"spaces".equals(space)) {
                    throw new XWikiRestException("Malformed URL: the spaces section is invalid.");
                }
            } else {
                spaces.add(space);
            }
            i++;
        }
        if (spaces.isEmpty()) {
            throw new XWikiRestException("Malformed URL: the spaces section is empty.");
        }
        return spaces;
    }

    /**
     * @param spaceSegments the space segments of the URL
     * @param wikiName the name of the wiki
     * @return the space reference
     * @throws XWikiRestException if the URL is malformed
     */
    public SpaceReference getSpaceReference(String spaceSegments, String wikiName) throws XWikiRestException
    {
        return Utils.getSpaceReference(parseSpaceSegments(spaceSegments), wikiName);
    }

    /**
     * Retrieve a document. This method never returns null. If something goes wrong with respect to some precondition an
     * exception is thrown.
     *
     * @param wikiName The wiki name. Cannot be null.
     * @param spaceName The space name. Cannot be null.
     * @param pageName The page name. Cannot be null.
     * @param language The language. Null for the default language.
     * @param version The version. Null for the latest version.
     * @param failIfDoesntExist True if an exception should be raised whenever the page doesn't exist.
     * @param failIfLocked True if an exception should be raised whenever the page is locked.
     * @return A DocumentInfo structure containing the actual document and additional information about it.
     * @throws IllegalArgumentException If a parameter has an incorrect value (e.g. null)
     * @throws WebApplicationException NOT_FOUND if failIfDoesntExist is true and the page doesn't exist.
     * @throws XWikiRestException if the URL is malformed PRECONDITION_FAILED if failIfLocked is true and the document
     *             is locked.
     */
    public DocumentInfo getDocumentInfo(String wikiName, String spaceName, String pageName, String language,
        String version, boolean failIfDoesntExist, boolean failIfLocked) throws XWikiException, XWikiRestException
    {
        return getDocumentInfo(wikiName, parseSpaceSegments(spaceName), pageName, language, version, failIfDoesntExist,
            failIfLocked);
    }

    /**
     * Retrieve a document. This method never returns null. If something goes wrong with respect to some precondition an
     * exception is thrown.
     *
     * @param wikiName The wiki name. Cannot be null.
     * @param spaces The space hierarchy. Cannot be null.
     * @param pageName The page name. Cannot be null.
     * @param localeString The language. Null for the default language.
     * @param version The version. Null for the latest version.
     * @param failIfDoesntExist True if an exception should be raised whenever the page doesn't exist.
     * @param failIfLocked True if an exception should be raised whenever the page is locked.
     * @return A DocumentInfo structure containing the actual document and additional information about it.
     * @throws IllegalArgumentException If a parameter has an incorrect value (e.g. null)
     * @throws WebApplicationException NOT_FOUND if failIfDoesntExist is true and the page doesn't exist.
     *             PRECONDITION_FAILED if failIfLocked is true and the document is locked.
     */
    public DocumentInfo getDocumentInfo(String wikiName, List<String> spaces, String pageName, String localeString,
        String version, boolean failIfDoesntExist, boolean failIfLocked) throws XWikiException
    {
        if ((wikiName == null) || (spaces == null || spaces.isEmpty()) || (pageName == null)) {
            throw new IllegalArgumentException(
                String.format("wikiName, spaceName and pageName must all be not null. Current values: (%s:%s.%s)",
                    wikiName, spaces, pageName));
        }

        // If the language of the translated document is not the one we requested, then the requested translation
        // doesn't exist. new translated document by hand.
        // TODO: Ideally this method should take a Locale as input and not a String
        Locale locale;
        if (localeString != null) {
            try {
                locale = LocaleUtils.toLocale(localeString);
            } catch (Exception e) {
                // Language is invalid, we consider that the translation has not been found.
                throw new WebApplicationException(Status.NOT_FOUND);
            }
        } else {
            locale = null;
        }

        // Create document reference
        DocumentReference reference = new DocumentReference(wikiName, spaces, pageName, locale);

        // Get document
        Document doc = Utils.getXWikiApi(componentManager).getDocument(reference);

        // If doc is null, we don't have the rights to access the document
        if (doc == null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        if (failIfDoesntExist && doc.isNew()) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        // Get a specific version if requested to
        if (version != null) {
            doc = doc.getDocumentRevision(version);
        }

        // Check if the doc is locked.
        if (failIfLocked && doc.getLocked()) {
            throw new WebApplicationException(Status.PRECONDITION_FAILED);
        }

        return new DocumentInfo(doc, doc.isNew());
    }

    /**
     * A special GET method that produces the ad-hoc "uritemplate" media type used for retrieving the URI template
     * associated to a resource. This is an auxiliary method that is used for documenting the REST API.
     *
     * @return the URI template string associated to the requested resource
     */
    @GET
    @Produces("uritemplate")
    public String getUriTemplate()
    {
        if (this.getClass().getAnnotation(Path.class) != null) {
            return this.getClass().getAnnotation(Path.class).value();
        }

        Class<?>[] interfaces = this.getClass().getInterfaces();

        for (Class<?> i : interfaces) {
            if (i.getAnnotation(Path.class) != null) {
                return i.getAnnotation(Path.class).value();
            }
        }

        return null;
    }

    /**
     * Retrieve the XWiki context from the current execution context.
     *
     * @return the XWiki context
     */
    protected XWikiContext getXWikiContext()
    {
        return this.xcontextProvider.get();
    }

    /**
     * @return the logger
     * @since 9.1RC1
     */
    protected Logger getLogger()
    {
        return this.slf4Jlogger;
    }

    /**
     * @param doc document to save
     * @since 10.2RC1
     * @since 9.11.4
     */
    protected void saveDocument(Document doc) throws XWikiException
    {
       saveDocument(doc, "");
    }

    /**
     * @param doc document to save
     * @param comment comment to save
     * @since 10.2RC1
     * @since 9.11.4
     */
    protected void saveDocument(Document doc, String comment) throws XWikiException
    {
        boolean isMinor = "true".equals(getXWikiContext().getRequest().get("minorRevision"));
        doc.save(comment, isMinor);
    }

}
