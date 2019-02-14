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
package org.xwiki.rest.internal;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.DefaultRequest;
import org.xwiki.job.Request;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attribute;
import org.xwiki.rest.model.jaxb.Class;
import org.xwiki.rest.model.jaxb.JobId;
import org.xwiki.rest.model.jaxb.JobLog;
import org.xwiki.rest.model.jaxb.JobProgress;
import org.xwiki.rest.model.jaxb.JobRequest;
import org.xwiki.rest.model.jaxb.JobStatus;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.MapEntry;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.ObjectFactory;
import org.xwiki.rest.model.jaxb.ObjectSummary;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.rest.model.jaxb.Space;
import org.xwiki.rest.model.jaxb.Translation;
import org.xwiki.rest.model.jaxb.Translations;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.resources.ModificationsResource;
import org.xwiki.rest.resources.SyntaxesResource;
import org.xwiki.rest.resources.attachments.AttachmentResource;
import org.xwiki.rest.resources.attachments.AttachmentVersionResource;
import org.xwiki.rest.resources.attachments.AttachmentsAtPageVersionResource;
import org.xwiki.rest.resources.attachments.AttachmentsResource;
import org.xwiki.rest.resources.classes.ClassPropertiesResource;
import org.xwiki.rest.resources.classes.ClassPropertyResource;
import org.xwiki.rest.resources.classes.ClassResource;
import org.xwiki.rest.resources.classes.ClassesResource;
import org.xwiki.rest.resources.comments.CommentsResource;
import org.xwiki.rest.resources.comments.CommentsVersionResource;
import org.xwiki.rest.resources.objects.AllObjectsForClassNameResource;
import org.xwiki.rest.resources.objects.ObjectAtPageVersionResource;
import org.xwiki.rest.resources.objects.ObjectPropertiesAtPageVersionResource;
import org.xwiki.rest.resources.objects.ObjectPropertiesResource;
import org.xwiki.rest.resources.objects.ObjectPropertyAtPageVersionResource;
import org.xwiki.rest.resources.objects.ObjectPropertyResource;
import org.xwiki.rest.resources.objects.ObjectResource;
import org.xwiki.rest.resources.objects.ObjectsAtPageVersionResource;
import org.xwiki.rest.resources.objects.ObjectsResource;
import org.xwiki.rest.resources.pages.PageChildrenResource;
import org.xwiki.rest.resources.pages.PageHistoryResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTagsResource;
import org.xwiki.rest.resources.pages.PageTranslationHistoryResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.rest.resources.pages.PagesResource;
import org.xwiki.rest.resources.spaces.SpaceResource;
import org.xwiki.rest.resources.spaces.SpaceSearchResource;
import org.xwiki.rest.resources.spaces.SpacesResource;
import org.xwiki.rest.resources.wikis.WikiSearchQueryResource;
import org.xwiki.rest.resources.wikis.WikiSearchResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.PropertyClass;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;

/**
 * Various common tools for resources.
 * 
 * @version $Id$
 * @since 7.3M1
 */
@Component(roles = ModelFactory.class)
@Singleton
public class ModelFactory
{
    private final ObjectFactory objectFactory;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private JAXBConverter jaxbConverter;

    public ModelFactory()
    {
        this.objectFactory = new ObjectFactory();
    }

    public boolean toDocument(Document doc, org.xwiki.rest.model.jaxb.Page restPage) throws XWikiException
    {
        boolean modified = false;

        if (restPage.getContent() != null) {
            doc.setContent(restPage.getContent());
            modified = true;
        }

        if (restPage.getTitle() != null) {
            doc.setTitle(restPage.getTitle());
            modified = true;
        }

        if (restPage.getParent() != null) {
            doc.setParent(restPage.getParent());
            modified = true;
        }

        if (restPage.getSyntax() != null) {
            doc.setSyntaxId(restPage.getSyntax());
            modified = true;
        }

        doc.setHidden(restPage.isHidden());

        // Set objects
        if (restPage.getObjects() != null) {
            Set<ObjectReference> newReferences = new HashSet<>();

            // Add/update objects
            for (ObjectSummary restObjectSummary : restPage.getObjects().getObjectSummaries()) {
                if (restObjectSummary != null) {
                    org.xwiki.rest.model.jaxb.Object restObject = (org.xwiki.rest.model.jaxb.Object) restObjectSummary;
                    com.xpn.xwiki.api.Object xwikiObject =
                        doc.getObject(restObject.getClassName(), restObject.getNumber());
                    if (xwikiObject == null) {
                        xwikiObject = doc.newObject(restObject.getClassName());
                    }
                    toObject(xwikiObject, restObject);
                    modified = true;

                    newReferences.add(xwikiObject.getReference());
                }
            }

            // Remove objects
            List<com.xpn.xwiki.api.Object> toRemove = new ArrayList<>();
            for (Vector<com.xpn.xwiki.api.Object> objects : doc.getxWikiObjects().values()) {
                for (com.xpn.xwiki.api.Object object : objects) {
                    if (!newReferences.contains(object.getReference())) {
                        toRemove.add(object);
                    }
                }
            }
            for (com.xpn.xwiki.api.Object obj : toRemove) {
                doc.removeObject(obj);
                modified = true;
            }
        }

        // TODO: add support for class, see https://jira.xwiki.org/browse/XWIKI-12597

        // TODO: add attachment content to the REST model ?

        return modified;
    }

    private void fillObjectSummary(ObjectSummary objectSummary, Document doc, BaseObject xwikiObject,
        Boolean withPrettyNames)
    {
        objectSummary.setId(String.format("%s:%s", doc.getPrefixedFullName(), xwikiObject.getGuid()));
        objectSummary.setGuid(xwikiObject.getGuid());
        objectSummary.setPageId(doc.getPrefixedFullName());
        objectSummary.setPageVersion(doc.getVersion());
        objectSummary.setPageAuthor(doc.getAuthor());
        if (withPrettyNames) {
            XWikiContext xwikiContext = this.xcontextProvider.get();
            objectSummary
                .setPageAuthorName(xwikiContext.getWiki().getUserName(doc.getAuthor(), null, false, xwikiContext));
        }
        objectSummary.setWiki(doc.getWiki());
        objectSummary.setSpace(doc.getSpace());
        objectSummary.setPageName(doc.getDocumentReference().getName());
        objectSummary.setClassName(xwikiObject.getClassName());
        objectSummary.setNumber(xwikiObject.getNumber());

        String[] propertyNames = xwikiObject.getPropertyNames();
        if (propertyNames.length > 0) {
            try {
                objectSummary.setHeadline(serializePropertyValue(xwikiObject.get(propertyNames[0])));
            } catch (XWikiException e) {
                // Should never happen
            }
        }
    }

    public ObjectSummary toRestObjectSummary(URI baseUri, Document doc, BaseObject xwikiObject, boolean useVersion,
        Boolean withPrettyNames)
    {
        ObjectSummary objectSummary = objectFactory.createObjectSummary();
        fillObjectSummary(objectSummary, doc, xwikiObject, withPrettyNames);

        Link objectLink = getObjectLink(objectFactory, baseUri, doc, xwikiObject, useVersion, Relations.OBJECT);
        objectSummary.getLinks().add(objectLink);

        String propertiesUri;
        if (useVersion) {
            propertiesUri = Utils.createURI(baseUri, ObjectPropertiesAtPageVersionResource.class, doc.getWiki(),
                Utils.getSpacesFromSpaceId(doc.getSpace()),doc.getDocumentReference().getName(), doc.getVersion(),
                xwikiObject.getClassName(), xwikiObject.getNumber()).toString();
        } else {
            propertiesUri = Utils.createURI(baseUri, ObjectPropertiesResource.class, doc.getWiki(),
                Utils.getSpacesFromSpaceId(doc.getSpace()), doc.getDocumentReference().getName(),
                xwikiObject.getClassName(), xwikiObject.getNumber()).toString();
        }

        Link propertyLink = objectFactory.createLink();
        propertyLink.setHref(propertiesUri);
        propertyLink.setRel(Relations.PROPERTIES);
        objectSummary.getLinks().add(propertyLink);

        return objectSummary;
    }

    public void toObject(com.xpn.xwiki.api.Object xwikiObject, org.xwiki.rest.model.jaxb.Object restObject)
    {
        for (Property restProperty : restObject.getProperties()) {
            xwikiObject.set(restProperty.getName(), restProperty.getValue());
        }
    }

    public Object toRestObject(URI baseUri, Document doc, BaseObject xwikiObject, boolean useVersion,
        Boolean withPrettyNames)
    {
        Object object = this.objectFactory.createObject();
        fillObjectSummary(object, doc, xwikiObject, withPrettyNames);

        XWikiContext xwikiContext = this.xcontextProvider.get();
        BaseClass xwikiClass = xwikiObject.getXClass(xwikiContext);

        for (java.lang.Object propertyClassObject : xwikiClass.getProperties()) {
            com.xpn.xwiki.objects.classes.PropertyClass propertyClass =
                (com.xpn.xwiki.objects.classes.PropertyClass) propertyClassObject;

            Property property = this.objectFactory.createProperty();

            for (java.lang.Object o : propertyClass.getProperties()) {
                BaseProperty baseProperty = (BaseProperty) o;
                Attribute attribute = this.objectFactory.createAttribute();
                attribute.setName(baseProperty.getName());

                /* Check for null values in order to prevent NPEs */
                if (baseProperty.getValue() != null) {
                    attribute.setValue(baseProperty.getValue().toString());
                } else {
                    attribute.setValue("");
                }

                property.getAttributes().add(attribute);
            }

            if (propertyClass instanceof ListClass) {
                ListClass listClass = (ListClass) propertyClass;

                List allowedValueList = listClass.getList(xwikiContext);

                if (!allowedValueList.isEmpty()) {
                    Formatter f = new Formatter();
                    for (int i = 0; i < allowedValueList.size(); i++) {
                        if (i != allowedValueList.size() - 1) {
                            f.format("%s,", allowedValueList.get(i).toString());
                        } else {
                            f.format("%s", allowedValueList.get(i).toString());
                        }
                    }

                    Attribute attribute = this.objectFactory.createAttribute();
                    attribute.setName(Constants.ALLOWED_VALUES_ATTRIBUTE_NAME);
                    attribute.setValue(f.toString());
                    property.getAttributes().add(attribute);
                }
            }

            property.setName(propertyClass.getName());
            property.setType(propertyClass.getClassType());
            try {
                property.setValue(serializePropertyValue(xwikiObject.get(propertyClass.getName())));
            } catch (XWikiException e) {
                // Should never happen
            }

            String propertyUri;

            if (useVersion) {
                propertyUri = Utils.createURI(baseUri, ObjectPropertyAtPageVersionResource.class, doc.getWiki(),
                    Utils.getSpacesFromSpaceId(doc.getSpace()), doc.getDocumentReference().getName(), doc.getVersion(),
                    xwikiObject.getClassName(), xwikiObject.getNumber(), propertyClass.getName()).toString();
            } else {
                propertyUri = Utils.createURI(baseUri, ObjectPropertyResource.class, doc.getWiki(),
                    Utils.getSpacesFromSpaceId(doc.getSpace()), doc.getDocumentReference().getName(),
                    xwikiObject.getClassName(), xwikiObject.getNumber(), propertyClass.getName()).toString();
            }
            Link propertyLink = this.objectFactory.createLink();
            propertyLink.setHref(propertyUri);
            propertyLink.setRel(Relations.SELF);
            property.getLinks().add(propertyLink);

            object.getProperties().add(property);
        }

        Link objectLink = getObjectLink(this.objectFactory, baseUri, doc, xwikiObject, useVersion, Relations.SELF);
        object.getLinks().add(objectLink);

        return object;
    }

    private static Link getObjectLink(ObjectFactory objectFactory, URI baseUri, Document doc, BaseObject xwikiObject,
        boolean useVersion, String relation)
    {
        String objectUri;

        if (useVersion) {
            objectUri = Utils.createURI(baseUri, ObjectAtPageVersionResource.class, doc.getWiki(),
                Utils.getSpacesFromSpaceId(doc.getSpace()), doc.getDocumentReference().getName(), doc.getVersion(),
                xwikiObject.getClassName(), xwikiObject.getNumber()).toString();
        } else {
            objectUri = Utils
                .createURI(baseUri, ObjectResource.class, doc.getWiki(), Utils.getSpacesFromSpaceId(doc.getSpace()),
                    doc.getDocumentReference().getName(), xwikiObject.getClassName(), xwikiObject.getNumber())
                .toString();
        }
        Link objectLink = objectFactory.createLink();
        objectLink.setHref(objectUri);
        objectLink.setRel(relation);

        return objectLink;
    }

    // To REST

    public Wiki toRestWiki(URI baseUri, String wikiName)
    {
        Wiki wiki = this.objectFactory.createWiki().withId(wikiName).withName(wikiName);

        String spacesUri = Utils.createURI(baseUri, SpacesResource.class, wikiName).toString();
        Link spacesLink = this.objectFactory.createLink();
        spacesLink.setHref(spacesUri);
        spacesLink.setRel(Relations.SPACES);
        wiki.getLinks().add(spacesLink);

        String classesUri = Utils.createURI(baseUri, ClassesResource.class, wikiName).toString();
        Link classesLink = this.objectFactory.createLink();
        classesLink.setHref(classesUri);
        classesLink.setRel(Relations.CLASSES);
        wiki.getLinks().add(classesLink);

        String modificationsUri = Utils.createURI(baseUri, ModificationsResource.class, wikiName).toString();
        Link modificationsLink = this.objectFactory.createLink();
        modificationsLink.setHref(modificationsUri);
        modificationsLink.setRel(Relations.MODIFICATIONS);
        wiki.getLinks().add(modificationsLink);

        String searchUri = Utils.createURI(baseUri, WikiSearchResource.class, wikiName).toString();
        Link searchLink = this.objectFactory.createLink();
        searchLink.setHref(searchUri);
        searchLink.setRel(Relations.SEARCH);
        wiki.getLinks().add(searchLink);

        String queryUri = Utils.createURI(baseUri, WikiSearchQueryResource.class, wikiName).toString();
        Link queryLink = this.objectFactory.createLink();
        queryLink.setHref(queryUri);
        queryLink.setRel(Relations.QUERY);
        wiki.getLinks().add(queryLink);

        return wiki;
    }

    public Space toRestSpace(URI baseUri, String wikiName, List<String> spaces, Document home)
    {
        Space space = this.objectFactory.createSpace();
        space.setId(Utils.getSpaceId(wikiName, spaces));
        space.setWiki(wikiName);
        // the name of the space is the last element of the spaces list
        space.setName(spaces.get(spaces.size() - 1));
        if (home != null) {
            space.setHome(home.getPrefixedFullName());
            space.setXwikiRelativeUrl(home.getURL("view"));
            space.setXwikiAbsoluteUrl(home.getExternalURL("view"));
        }

        String pagesUri = Utils.createURI(baseUri, PagesResource.class, wikiName, spaces).toString();
        Link pagesLink = this.objectFactory.createLink();
        pagesLink.setHref(pagesUri);
        pagesLink.setRel(Relations.PAGES);
        space.getLinks().add(pagesLink);

        if (home != null) {
            String homeUri = Utils.createURI(baseUri, PageResource.class, wikiName, spaces,
                home.getDocumentReference().getName()).toString();
            Link homeLink = this.objectFactory.createLink();
            homeLink.setHref(homeUri);
            homeLink.setRel(Relations.HOME);
            space.getLinks().add(homeLink);
        }

        String searchUri = Utils.createURI(baseUri, SpaceSearchResource.class, wikiName, spaces).toString();
        Link searchLink = this.objectFactory.createLink();
        searchLink.setHref(searchUri);
        searchLink.setRel(Relations.SEARCH);
        space.getLinks().add(searchLink);

        return space;
    }

    public Translations toRestTranslations(URI baseUri, Document doc) throws XWikiException
    {
        Translations translations = this.objectFactory.createTranslations();

        List<String> languages = doc.getTranslationList();

        List<String> spaces = Utils.getSpacesFromSpaceId(doc.getSpace());

        if (!languages.isEmpty()) {
            if (!doc.getDefaultLanguage().equals("")) {
                translations.setDefault(doc.getDefaultLanguage());

                Translation translation = this.objectFactory.createTranslation();
                translation.setLanguage(doc.getDefaultLanguage());

                /* Add the default page with the default translation explicitely */
                String pageTranslationUri = Utils.createURI(baseUri, PageResource.class, doc.getWiki(), spaces,
                    doc.getDocumentReference().getName()).toString();
                Link pageTranslationLink = this.objectFactory.createLink();
                pageTranslationLink.setHref(pageTranslationUri);
                pageTranslationLink.setRel(Relations.PAGE);
                translation.getLinks().add(pageTranslationLink);

                String historyUri = Utils.createURI(baseUri, PageHistoryResource.class, doc.getWiki(), spaces,
                    doc.getDocumentReference().getName()).toString();
                Link historyLink = this.objectFactory.createLink();
                historyLink.setHref(historyUri);
                historyLink.setRel(Relations.HISTORY);
                translation.getLinks().add(historyLink);

                translations.getTranslations().add(translation);
            }
        }

        for (String language : languages) {
            Translation translation = this.objectFactory.createTranslation();
            translation.setLanguage(language);

            String pageTranslationUri = Utils.createURI(baseUri, PageTranslationResource.class, doc.getWiki(), spaces,
                doc.getDocumentReference().getName(), language).toString();
            Link pageTranslationLink = this.objectFactory.createLink();
            pageTranslationLink.setHref(pageTranslationUri);
            pageTranslationLink.setRel(Relations.PAGE);
            translation.getLinks().add(pageTranslationLink);

            String historyUri = Utils.createURI(baseUri, PageTranslationHistoryResource.class, doc.getWiki(), spaces,
                doc.getDocumentReference().getName(), language).toString();
            Link historyLink = this.objectFactory.createLink();
            historyLink.setHref(historyUri);
            historyLink.setRel(Relations.HISTORY);
            translation.getLinks().add(historyLink);

            translations.getTranslations().add(translation);
        }

        return translations;
    }

    /**
     * This method is used to fill the "common part" of a Page and a PageSummary.
     */
    private void toRestPageSummary(PageSummary pageSummary, URI baseUri, Document doc, boolean useVersion,
        Boolean withPrettyNames) throws XWikiException
    {
        XWikiContext xwikiContext = this.xcontextProvider.get();

        pageSummary.setWiki(doc.getWiki());
        pageSummary.setFullName(doc.getFullName());
        pageSummary.setId(doc.getPrefixedFullName());
        pageSummary.setSpace(doc.getSpace());
        pageSummary.setName(doc.getDocumentReference().getName());
        pageSummary.setTitle(doc.getDisplayTitle());
        pageSummary.setXwikiRelativeUrl(doc.getURL("view"));
        pageSummary.setXwikiAbsoluteUrl(doc.getExternalURL("view"));
        pageSummary.setTranslations(toRestTranslations(baseUri, doc));
        pageSummary.setSyntax(doc.getSyntax().toIdString());
        pageSummary.setVersion(doc.getVersion());
        pageSummary.setAuthor(doc.getAuthor());
        if (withPrettyNames) {
            pageSummary.setAuthorName(xwikiContext.getWiki().getUserName(doc.getAuthor(), null, false, xwikiContext));
        }

        pageSummary.setParent(doc.getParent());
        DocumentReference parentReference = doc.getParentReference();
        boolean parentExist = parentReference != null && xwikiContext.getWiki().exists(parentReference, xwikiContext);
        // parentId must not be set if the parent document does not exist.
        if (parentExist) {
            pageSummary.setParentId(this.serializer.serialize(parentReference));
        } else {
            pageSummary.setParentId("");
        }

        List<String> spaces = Utils.getSpacesFromSpaceId(doc.getSpace());
        String spaceUri = Utils.createURI(baseUri, SpaceResource.class, doc.getWiki(), spaces).toString();
        Link spaceLink = this.objectFactory.createLink();
        spaceLink.setHref(spaceUri);
        spaceLink.setRel(Relations.SPACE);
        pageSummary.getLinks().add(spaceLink);

        if (parentExist) {
            String parentUri = Utils.createURI(baseUri, PageResource.class,
                parentReference.getWikiReference().getName(), spaces, parentReference.getName()).toString();
            Link parentLink = this.objectFactory.createLink();
            parentLink.setHref(parentUri);
            parentLink.setRel(Relations.PARENT);
            pageSummary.getLinks().add(parentLink);
        }

        String historyUri = Utils.createURI(baseUri, PageHistoryResource.class, doc.getWiki(),
            Utils.getSpacesFromSpaceId(doc.getSpace()), doc.getDocumentReference().getName()).toString();
        Link historyLink = this.objectFactory.createLink();
        historyLink.setHref(historyUri);
        historyLink.setRel(Relations.HISTORY);
        pageSummary.getLinks().add(historyLink);

        if (!doc.getChildren().isEmpty()) {
            String pageChildrenUri = Utils.createURI(baseUri, PageChildrenResource.class, doc.getWiki(), spaces,
                doc.getDocumentReference().getName()).toString();
            Link pageChildrenLink = this.objectFactory.createLink();
            pageChildrenLink.setHref(pageChildrenUri);
            pageChildrenLink.setRel(Relations.CHILDREN);
            pageSummary.getLinks().add(pageChildrenLink);
        }

        if (!doc.getComments().isEmpty()) {
            String commentsUri;
            if (useVersion) {
                commentsUri = Utils.createURI(baseUri, CommentsVersionResource.class, doc.getWiki(), spaces,
                    doc.getDocumentReference().getName(), doc.getVersion()).toString();
            } else {
                commentsUri = Utils.createURI(baseUri, CommentsResource.class, doc.getWiki(), spaces,
                    doc.getDocumentReference().getName()).toString();
            }

            Link commentsLink = this.objectFactory.createLink();
            commentsLink.setHref(commentsUri);
            commentsLink.setRel(Relations.COMMENTS);
            pageSummary.getLinks().add(commentsLink);
        }

        if (!doc.getAttachmentList().isEmpty()) {
            String attachmentsUri;
            if (useVersion) {
                attachmentsUri = Utils.createURI(baseUri, AttachmentsAtPageVersionResource.class, doc.getWiki(), spaces,
                    doc.getDocumentReference().getName(), doc.getVersion()).toString();
            } else {
                attachmentsUri = Utils.createURI(baseUri, AttachmentsResource.class, doc.getWiki(), spaces,
                    doc.getDocumentReference().getName()).toString();
            }

            Link attachmentsLink = this.objectFactory.createLink();
            attachmentsLink.setHref(attachmentsUri);
            attachmentsLink.setRel(Relations.ATTACHMENTS);
            pageSummary.getLinks().add(attachmentsLink);
        }

        if (!doc.getxWikiObjects().keySet().isEmpty()) {
            String objectsUri;

            if (useVersion) {
                objectsUri = Utils.createURI(baseUri, ObjectsAtPageVersionResource.class, doc.getWiki(), spaces,
                    doc.getDocumentReference().getName(), doc.getVersion()).toString();
            } else {
                objectsUri = Utils.createURI(baseUri, ObjectsResource.class, doc.getWiki(), spaces,
                    doc.getDocumentReference().getName()).toString();
            }
            Link objectsLink = this.objectFactory.createLink();
            objectsLink.setHref(objectsUri);
            objectsLink.setRel(Relations.OBJECTS);
            pageSummary.getLinks().add(objectsLink);
        }

        com.xpn.xwiki.api.Object tagsObject = doc.getObject("XWiki.TagClass", 0);
        if (tagsObject != null) {
            if (tagsObject.getProperty("tags") != null) {
                String tagsUri = Utils.createURI(baseUri, PageTagsResource.class, doc.getWiki(), spaces,
                    doc.getDocumentReference().getName()).toString();
                Link tagsLink = this.objectFactory.createLink();
                tagsLink.setHref(tagsUri);
                tagsLink.setRel(Relations.TAGS);
                pageSummary.getLinks().add(tagsLink);
            }
        }

        String syntaxesUri = Utils.createURI(baseUri, SyntaxesResource.class).toString();
        Link syntaxesLink = this.objectFactory.createLink();
        syntaxesLink.setHref(syntaxesUri);
        syntaxesLink.setRel(Relations.SYNTAXES);
        pageSummary.getLinks().add(syntaxesLink);
    }

    public PageSummary toRestPageSummary(URI baseUri, Document doc, Boolean withPrettyNames) throws XWikiException
    {
        PageSummary pageSummary = this.objectFactory.createPageSummary();
        toRestPageSummary(pageSummary, baseUri, doc, false, withPrettyNames);

        String pageUri = Utils.createURI(baseUri, PageResource.class, doc.getWiki(),
            Utils.getSpacesFromSpaceId(doc.getSpace()), doc.getDocumentReference().getName()).toString();
        Link pageLink = this.objectFactory.createLink();
        pageLink.setHref(pageUri);
        pageLink.setRel(Relations.PAGE);
        pageSummary.getLinks().add(pageLink);

        return pageSummary;
    }

    public Page toRestPage(URI baseUri, URI self, Document doc, boolean useVersion, Boolean withPrettyNames,
        Boolean withObjects, Boolean withXClass, Boolean withAttachments) throws XWikiException
    {
        Page page = this.objectFactory.createPage();
        toRestPageSummary(page, baseUri, doc, useVersion, withPrettyNames);

        XWikiContext xwikiContext = this.xcontextProvider.get();

        page.setMajorVersion(doc.getRCSVersion().at(0));
        page.setMinorVersion(doc.getRCSVersion().at(1));
        page.setHidden(doc.isHidden());
        page.setLanguage(doc.getLocale().toString());
        page.setCreator(doc.getCreator());
        if (withPrettyNames) {
            page.setCreatorName(xwikiContext.getWiki().getUserName(doc.getCreator(), null, false, xwikiContext));
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(doc.getCreationDate());
        page.setCreated(calendar);

        page.setModifier(doc.getContentAuthor());
        if (withPrettyNames) {
            page.setModifierName(xwikiContext.getWiki().getUserName(doc.getContentAuthor(), null, false, xwikiContext));
        }

        calendar = Calendar.getInstance();
        calendar.setTime(doc.getContentUpdateDate());
        page.setModified(calendar);

        page.setComment(doc.getComment());
        page.setContent(doc.getContent());

        if (self != null) {
            Link pageLink = this.objectFactory.createLink();
            pageLink.setHref(self.toString());
            pageLink.setRel(Relations.SELF);
            page.getLinks().add(pageLink);
        }

        com.xpn.xwiki.api.Class xwikiClass = doc.getxWikiClass();
        if (xwikiClass != null) {
            String classUri =
                Utils.createURI(baseUri, ClassResource.class, doc.getWiki(), xwikiClass.getName()).toString();
            Link classLink = this.objectFactory.createLink();
            classLink.setHref(classUri);
            classLink.setRel(Relations.CLASS);
            page.getLinks().add(classLink);
        }

        XWikiContext xcontext = xcontextProvider.get();

        // Add attachments
        if (withAttachments) {
            page.setAttachments(objectFactory.createAttachments());

            for (com.xpn.xwiki.api.Attachment attachment : doc.getAttachmentList()) {
                URL url = xcontext.getURLFactory().createAttachmentURL(attachment.getFilename(), doc.getSpace(),
                    doc.getDocumentReference().getName(), "download", null, doc.getWiki(), xcontext);
                String attachmentXWikiAbsoluteUrl = url.toString();
                String attachmentXWikiRelativeUrl = xcontext.getURLFactory().getURL(url, xcontext);

                page.getAttachments().getAttachments().add(toRestAttachment(baseUri, attachment,
                    attachmentXWikiRelativeUrl, attachmentXWikiAbsoluteUrl, withPrettyNames, false));
            }
        }

        // Add objects
        if (withObjects) {
            page.setObjects(objectFactory.createObjects());

            XWikiDocument xwikiDocument = xcontext.getWiki().getDocument(doc.getDocumentReference(), xcontext);

            for (List<BaseObject> objects : xwikiDocument.getXObjects().values()) {
                for (BaseObject object : objects) {
                    // Deleting an object leads to a null entry in the list of objects.
                    if (object != null) {
                        page.getObjects().getObjectSummaries()
                            .add(toRestObject(baseUri, doc, object, false, withPrettyNames));
                    }
                }
            }
        }

        // Add xclass
        if (withXClass) {
            page.setClazz(toRestClass(baseUri, doc.getxWikiClass()));
        }

        return page;
    }

    public Class toRestClass(URI baseUri, com.xpn.xwiki.api.Class xwikiClass)
    {
        Class clazz = this.objectFactory.createClass();
        clazz.setId(xwikiClass.getName());
        clazz.setName(xwikiClass.getName());

        DocumentReference reference = xwikiClass.getReference();
        String wikiName = reference.getWikiReference().getName();

        for (java.lang.Object xwikiPropertyClassObject : xwikiClass.getProperties()) {
            PropertyClass xwikiPropertyClass = (PropertyClass) xwikiPropertyClassObject;

            Property property = this.objectFactory.createProperty();
            property.setName(xwikiPropertyClass.getName());
            property.setType(xwikiPropertyClass.getxWikiClass().getName());

            for (java.lang.Object xwikiPropertyObject : xwikiPropertyClass.getProperties()) {
                com.xpn.xwiki.api.Property xwikiProperty = (com.xpn.xwiki.api.Property) xwikiPropertyObject;
                java.lang.Object value = xwikiProperty.getValue();

                Attribute attribute = this.objectFactory.createAttribute();
                attribute.setName(xwikiProperty.getName());

                if (value != null) {
                    attribute.setValue(value.toString());
                } else {
                    attribute.setValue("");
                }

                property.getAttributes().add(attribute);
            }

            String propertyUri = Utils.createURI(baseUri, ClassPropertyResource.class, wikiName, xwikiClass.getName(),
                xwikiPropertyClass.getName()).toString();
            Link propertyLink = this.objectFactory.createLink();
            propertyLink.setHref(propertyUri);
            propertyLink.setRel(Relations.SELF);
            property.getLinks().add(propertyLink);

            clazz.getProperties().add(property);
        }

        String classUri = Utils.createURI(baseUri, ClassResource.class, wikiName, xwikiClass.getName()).toString();
        Link classLink = this.objectFactory.createLink();
        classLink.setHref(classUri);
        classLink.setRel(Relations.SELF);
        clazz.getLinks().add(classLink);

        String propertiesUri =
            Utils.createURI(baseUri, ClassPropertiesResource.class, wikiName, xwikiClass.getName()).toString();
        Link propertyLink = this.objectFactory.createLink();
        propertyLink.setHref(propertiesUri);
        propertyLink.setRel(Relations.PROPERTIES);
        clazz.getLinks().add(propertyLink);

        String objectsUri =
            Utils.createURI(baseUri, AllObjectsForClassNameResource.class, wikiName, xwikiClass.getName()).toString();
        Link objectsLink = this.objectFactory.createLink();
        objectsLink.setHref(objectsUri);
        objectsLink.setRel(Relations.OBJECTS);
        clazz.getLinks().add(objectsLink);

        return clazz;
    }

    public Attachment toRestAttachment(URI baseUri, com.xpn.xwiki.api.Attachment xwikiAttachment,
        String xwikiRelativeUrl, String xwikiAbsoluteUrl, Boolean withPrettyNames, boolean versionURL)
    {
        Attachment attachment = this.objectFactory.createAttachment();

        Document doc = xwikiAttachment.getDocument();

        attachment.setId(String.format("%s@%s", doc.getPrefixedFullName(), xwikiAttachment.getFilename()));
        attachment.setName(xwikiAttachment.getFilename());
        attachment.setSize(xwikiAttachment.getFilesize());
        attachment.setVersion(xwikiAttachment.getVersion());
        attachment.setPageId(doc.getPrefixedFullName());
        attachment.setPageVersion(doc.getVersion());
        attachment.setMimeType(xwikiAttachment.getMimeType());
        attachment.setAuthor(xwikiAttachment.getAuthor());
        if (withPrettyNames) {
            XWikiContext xcontext = xcontextProvider.get();
            attachment
                .setAuthorName(xcontext.getWiki().getUserName(xwikiAttachment.getAuthor(), null, false, xcontext));
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(xwikiAttachment.getDate());
        attachment.setDate(calendar);

        attachment.setXwikiRelativeUrl(xwikiRelativeUrl);
        attachment.setXwikiAbsoluteUrl(xwikiAbsoluteUrl);

        String pageUri = Utils.createURI(baseUri, PageResource.class, doc.getWiki(),
            Utils.getSpacesFromSpaceId(doc.getSpace()), doc.getDocumentReference().getName()).toString();
        Link pageLink = objectFactory.createLink();
        pageLink.setHref(pageUri);
        pageLink.setRel(Relations.PAGE);
        attachment.getLinks().add(pageLink);

        String attachmentUri;
        if (versionURL) {
            attachmentUri = Utils.createURI(baseUri, AttachmentVersionResource.class, doc.getWiki(),
                Utils.getSpacesFromSpaceId(doc.getSpace()), doc.getDocumentReference().getName(),
                xwikiAttachment.getFilename(), xwikiAttachment.getVersion()).toString();
        } else {
            attachmentUri = Utils.createURI(baseUri, AttachmentResource.class, doc.getWiki(),
                Utils.getSpacesFromSpaceId(doc.getSpace()), doc.getDocumentReference().getName(),
                xwikiAttachment.getFilename()).toString();
        }

        Link attachmentLink = objectFactory.createLink();
        attachmentLink.setHref(attachmentUri);
        attachmentLink.setRel(Relations.ATTACHMENT_DATA);
        attachment.getLinks().add(attachmentLink);

        return attachment;
    }

    /**
     * Serializes the value of the given XObject property.
     * 
     * @param property an XObject property
     * @return the String representation of the property value
     */
    private static String serializePropertyValue(PropertyInterface property)
    {
        if (property == null) {
            return "";
        }

        java.lang.Object value = ((BaseProperty) property).getValue();
        if (value instanceof List) {
            return StringUtils.join((List) value, "|");
        } else if (value != null) {
            return value.toString();
        } else {
            return "";
        }
    }

    public JobRequest toRestJobRequest(Request request) throws XWikiRestException
    {
        JobRequest restJobRequest = this.objectFactory.createJobRequest();

        restJobRequest.setId(toRestJobId(request.getId()));
        restJobRequest.setInteractive(request.isInteractive());
        restJobRequest.setRemote(request.isRemote());
        restJobRequest.setVerbose(request.isVerbose());
        restJobRequest.setStatusSerialized(request.isStatusSerialized());
        restJobRequest.setStatusLogIsolated(request.isStatusLogIsolated());

        for (String key : request.getPropertyNames()) {
            restJobRequest.getProperties().add(toRestMapEntry(key, request.getProperty(key)));
        }

        return restJobRequest;
    }

    public JobId toRestJobId(List<String> id)
    {
        if (id == null) {
            return null;
        }

        JobId restJobId = this.objectFactory.createJobId();

        restJobId.withElements(id);

        return restJobId;
    }

    public MapEntry toRestMapEntry(String key, java.lang.Object value) throws XWikiRestException
    {
        MapEntry restMapEntry = this.objectFactory.createMapEntry();

        restMapEntry.setKey(key);
        try {
            restMapEntry.setValue(this.jaxbConverter.serializeAny(value));
        } catch (ParserConfigurationException e) {
            throw new XWikiRestException("Failed to serialize property [" + key + "] with value [" + value + "]", e);
        }

        return restMapEntry;
    }

    public DefaultRequest toJobRequest(JobRequest restJobRequest)
    {
        DefaultRequest request = new DefaultRequest();

        if (restJobRequest.getId() != null) {
            request.setId(restJobRequest.getId().getElements());
        }

        request.setInteractive(restJobRequest.isInteractive());
        request.setVerbose(restJobRequest.isVerbose());
        request.setStatusSerialized(restJobRequest.isStatusSerialized());
        request.setStatusLogIsolated(restJobRequest.isStatusLogIsolated());

        for (MapEntry restEntry : restJobRequest.getProperties()) {
            request.setProperty(restEntry.getKey(), this.jaxbConverter.unserializeAny(restEntry.getValue()));
        }

        return request;
    }

    public JobStatus toRestJobStatus(org.xwiki.job.event.status.JobStatus jobStatus, URI self, boolean request,
        boolean progress, boolean log, String logFromLevel) throws XWikiRestException
    {
        JobStatus status = this.objectFactory.createJobStatus();
        status.setId(StringUtils.join(jobStatus.getRequest().getId(), "/"));
        status.setState(jobStatus.getState().name());
        if (jobStatus.getStartDate() != null) {
            Calendar calendarStartDate = Calendar.getInstance();
            calendarStartDate.setTime(jobStatus.getStartDate());
            status.setStartDate(calendarStartDate);
        }
        if (jobStatus.getEndDate() != null) {
            Calendar calendarEndDate = Calendar.getInstance();
            calendarEndDate.setTime(jobStatus.getEndDate());
            status.setEndDate(calendarEndDate);
        }
        if (jobStatus.getError() != null) {
            status.setErrorMessage(ExceptionUtils.getStackTrace(jobStatus.getError()));
        }

        // Request
        if (request) {
            status.setRequest(toRestJobRequest(jobStatus.getRequest()));
        }

        // Progress
        if (progress) {
            status.setProgress(toRestJobProgress(jobStatus.getProgress()));
        }

        // Log
        if (log) {
            status.setLog(toRestJobLog(jobStatus.getLog(), self, null, logFromLevel));
        }

        // Link
        if (self != null) {
            Link link = objectFactory.createLink();
            link.setHref(self.toString());
            link.setRel(Relations.SELF);
            status.getLinks().add(link);
        }

        // Log isolation
        status.setIsolated(jobStatus.isIsolated());
        // Status serialization
        status.setSerialized(jobStatus.isSerialized());

        return status;
    }

    public JobProgress toRestJobProgress(org.xwiki.job.event.status.JobProgress progress)
    {
        JobProgress restJobProgress = this.objectFactory.createJobProgress();

        restJobProgress.setOffset(progress.getOffset());
        restJobProgress.setCurrentLevelOffset(progress.getCurrentLevelOffset());

        // TODO: add support for steps

        return restJobProgress;
    }

    public JobLog toRestJobLog(LogQueue logQueue, URI self, String level, String fromLevel)
    {
        // Filter log
        Collection<LogEvent> logs;
        if (level != null) {
            logs = logQueue.getLogs(LogLevel.valueOf(level.toUpperCase()));
        } else if (fromLevel != null) {
            logs = logQueue.getLogsFrom(LogLevel.valueOf(fromLevel.toUpperCase()));
        } else {
            logs = logQueue;
        }

        return toRestJobLog(logs, self);
    }

    public JobLog toRestJobLog(Collection<LogEvent> logs, URI self)
    {
        JobLog log = this.objectFactory.createJobLog();

        // Serialize log
        for (LogEvent logEvent : logs) {
            // TODO: add support for log arguments
            // TODO: add support for log Marker
            org.xwiki.rest.model.jaxb.LogEvent event = this.objectFactory.createLogEvent();
            event.setLevel(logEvent.getLevel().name());
            Calendar calendarDate = Calendar.getInstance();
            calendarDate.setTimeInMillis(logEvent.getTimeStamp());
            event.setDate(calendarDate);
            event.setFormattedMessage(logEvent.getFormattedMessage());
            log.getLogEvents().add(event);
        }

        // Set link
        if (self != null) {
            Link link = this.objectFactory.createLink();
            link.setHref(self.toString());
            link.setRel(Relations.SELF);
            log.getLinks().add(link);
        }

        return log;
    }
}
