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
package com.xpn.xwiki.web;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.util.Util;

/**
 * Create document action.
 * 
 * @version $Id$
 * @since 2.4M2
 */
public class CreateAction extends XWikiAction
{
    /**
     * Log used to report exceptions.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateAction.class);

    /**
     * The template provider class, to create documents from templates.
     */
    private static final EntityReference TEMPLATE_PROVIDER_CLASS = new EntityReference("TemplateProviderClass",
        EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    /**
     * The name of the space parameter. <br />
     * Note: if you change the value of this variable, change the value of {{@link #TOCREATE_SPACE} to the previous
     * value.
     */
    private static final String SPACE = "space";

    /**
     * The name of the page parameter. <br />
     */
    private static final String PAGE = "page";

    /**
     * The value of the toCreate parameter when a space is to be created. <br />
     * TODO: find a way to give this constant the same value as the constant above without violating checkstyle.
     */
    private static final String TOCREATE_SPACE = SPACE;

    /**
     * The name of the template provider parameter.
     */
    private static final String TEMPLATE_PROVIDER = "templateprovider";

    /**
     * The name of the template field inside the template provider, or the template parameter which can be sent
     * directly, without passing through the template provider.
     */
    private static final String TEMPLATE = "template";

    /**
     * The key used to add exceptions on the context, to be read by the template.
     */
    private static final String EXCEPTION = "createException";

    /**
     * The property name for the template type (page or spaces) in the template provider object.
     */
    private static final String TYPE_PROPERTY = "type";

    /**
     * The property name for the spaces in the template provider object.
     */
    private static final String SPACES_PROPERTY = "spaces";

    /**
     * The name of the velocity context in the context, to put variables used in the vms.
     */
    private static final String VELOCITY_CONTEXT_KEY = "vcontext";

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();
        // resolver to use to resolve references received in request parameters
        DocumentReferenceResolver<String> resolver =
            Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");

        // Since this template can be used for creating a Page or a Space, check the passed "tocreate" parameter
        // which can be either "page" or "space". If no parameter is passed then we default to creating a Page.
        String toCreate = request.getParameter("tocreate");
        boolean isSpace = false;
        if (!StringUtils.isEmpty(toCreate) && TOCREATE_SPACE.equals(toCreate)) {
            isSpace = true;
        }

        // get the template provider for creating this document, if any template provider is specified
        DocumentReferenceResolver<EntityReference> referenceResolver =
            Utils.getComponent(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        DocumentReference templateProviderClassReference = referenceResolver.resolve(TEMPLATE_PROVIDER_CLASS);
        BaseObject templateProvider = getTemplateProvider(context, resolver, templateProviderClassReference);

        // Set the space, page, title variables from the current doc if its new, from the passed parameters if any
        String space = "";
        String page = "";
        if (doc.isNew()) {
            space = doc.getDocumentReference().getSpaceReferences().get(0).getName();
            page = doc.getDocumentReference().getName();
        } else {
            space = request.getParameter(SPACE);
            page = request.getParameter(PAGE);
        }

        // get the available templates, in the current space, to check if all conditions to create a new document are
        // met
        List<Document> availableTemplates =
            getAvailableTemplates(doc.getDocumentReference().getSpaceReferences().get(0).getName(), isSpace, resolver,
                templateProviderClassReference, context);
        // put the available templates on the context, for the .vm to not compute them again
        ((VelocityContext) context.get(VELOCITY_CONTEXT_KEY)).put("createAvailableTemplates", availableTemplates);

        // get the reference to the new document
        DocumentReference newDocRef =
            getNewDocumentReference(context, space, page, isSpace, templateProvider, availableTemplates);

        if (newDocRef != null) {
            XWikiDocument newDoc = context.getWiki().getDocument(newDocRef, context);
            // if the document exists don't create it, put the exception on the context so that the template gets it and
            // re-requests the page and space, else create the document and redirect to edit
            if (!isEmptyDocument(newDoc)) {
                Object[] args = {space, page};
                XWikiException documentAlreadyExists =
                    new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                        XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY,
                        "Cannot create document {0}.{1} because it already has content", null, args);
                ((VelocityContext) context.get(VELOCITY_CONTEXT_KEY)).put(EXCEPTION, documentAlreadyExists);
            } else {
                // create is finally valid, can be executed
                doCreate(context, newDoc, isSpace, templateProvider, resolver);
            }
        }

        return "create";
    }

    /**
     * Checks if a document is empty, that is, if a document with that name could be created from a template. <br />
     * TODO: move this function to a more accessible place, to be used by the readFromTemplate method as well, so that
     * we have consistency.
     * 
     * @param document the document to check
     * @return {@code true} if the document is empty (i.e. a document with the same name can be created (from
     *         template)), {@code false} otherwise
     */
    private boolean isEmptyDocument(XWikiDocument document)
    {
        // if it's a new document, it's fine
        if (document.isNew()) {
            return true;
        }
        // otherwise, check content and objects (only empty newline content allowed and no objects)
        String content = document.getContent();
        if (!content.equals("\n") && !content.equals("") && !content.equals("\\\\")) {
            return false;
        }

        // go through all the objects and when finding the first one which is not null (because of the remove gaps),
        // return false, we cannot re-create this doc
        for (Map.Entry<DocumentReference, List<BaseObject>> objList : document.getXObjects().entrySet()) {
            for (BaseObject obj : objList.getValue()) {
                if (obj != null) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * @param request the current request for which this action is executed
     * @param doc the current document
     * @param isSpace {@code true} if the request is to create a space, {@code false} if a page should be created
     * @return the serialized reference of the parent to create the document for
     */
    private String getParent(XWikiRequest request, XWikiDocument doc, boolean isSpace)
    {
        // This template can be passed a parent document reference in parameter (using the "parent" parameter).
        // If a parent parameter is passed, use it to set the parent when creating the new Page or Space.
        // If no parent parameter was passed, use the current document if we're creating a new page, keep it blank
        // if we're creating a new space. Also don't set current document as parent if it's a new doc
        String parent = request.getParameter("parent");
        if (StringUtils.isEmpty(parent) && !isSpace && !doc.isNew()) {
            DocumentReference parentRef = doc.getDocumentReference();

            EntityReferenceSerializer<String> localSerializer =
                Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");
            parent = localSerializer.serialize(parentRef);
        }

        return parent;
    }

    /**
     * @param context the context of the execution of this action
     * @param referenceResolver the reference resolver to use to resolve the parameter value
     * @param templateProviderClass the class of the template provider object
     * @return the object which holds the template provider to be used for creation
     * @throws XWikiException in case anything goes wrong manipulating documents
     */
    private BaseObject getTemplateProvider(XWikiContext context, DocumentReferenceResolver<String> referenceResolver,
        DocumentReference templateProviderClass) throws XWikiException
    {
        // set the template, from the template provider param
        String templateProviderDocReferenceString = context.getRequest().getParameter(TEMPLATE_PROVIDER);
        BaseObject templateProvider = null;
        if (!StringUtils.isEmpty(templateProviderDocReferenceString)) {
            // parse this document reference
            DocumentReference templateProviderRef = referenceResolver.resolve(templateProviderDocReferenceString);
            // get the document of the template provider and the object
            XWikiDocument templateProviderDoc = context.getWiki().getDocument(templateProviderRef, context);
            templateProvider = templateProviderDoc.getXObject(templateProviderClass);
        }
        return templateProvider;
    }

    /**
     * @param request the current request for which this action is executed
     * @param newDocument the document to be created
     * @param isSpace {@code true} if the request is to create a space, {@code false} if a page should be created
     * @return the title of the page to be created
     */
    private String getTitle(XWikiRequest request, XWikiDocument newDocument, boolean isSpace)
    {
        String title = request.getParameter("title");
        if (StringUtils.isEmpty(title)) {
            title =
                isSpace ? newDocument.getDocumentReference().getSpaceReferences().get(0).getName() : newDocument
                    .getDocumentReference().getName();
        }

        return title;
    }

    /**
     * @param context the context to execute this action
     * @param space the space of the new document
     * @param page the page of the new document
     * @param isSpace whether the new document to be created is a space homepage (create space) or a regular page
     * @param templateProvider the template provider for creating this page
     * @param availableTemplates the templates available
     * @return the document reference of the new document to be created, {@code null} if a no document can be created
     *         (because the conditions are not met)
     */
    private DocumentReference getNewDocumentReference(XWikiContext context, String space, String page, boolean isSpace,
        BaseObject templateProvider, List<Document> availableTemplates)
    {
        DocumentReference newDocRef = null;

        if (isSpace && !StringUtils.isEmpty(space)) {
            // If a space is ready to be created, redirect to the space home in edit mode
            newDocRef = new DocumentReference(context.getDatabase(), space, "WebHome");
        }

        // check whether there is a template parameter set, be it an empty one. If a page should be created and there is
        // no template parameter, it means the create action is not supposed to be executed, but only display the
        // available templates and let the user choose
        boolean hasTemplate =
            context.getRequest().getParameterMap().containsKey(TEMPLATE_PROVIDER)
                || context.getRequest().getParameterMap().containsKey(TEMPLATE);
        // if there's no passed template check if there are any available templates. If none available, then the fact
        // that there is no template is ok
        if (!hasTemplate) {
            boolean canHasTemplate = availableTemplates.size() > 0;
            hasTemplate = !canHasTemplate;
        }
        if (!isSpace && !StringUtils.isEmpty(page) && !StringUtils.isEmpty(space) && hasTemplate) {
            // check if the creation in this space is allowed, and only set the new document reference if the creation
            // is allowed
            if (checkAllowedSpace(space, page, templateProvider, context)) {
                newDocRef = new DocumentReference(context.getDatabase(), space, page);
            }
        }

        return newDocRef;
    }

    /**
     * Verifies if the creation inside space {@code space} is allowed by the template provider described by
     * {@code templateProvider}. If the creation is not allowed, an exception will be set on the context.
     * 
     * @param space the space to create page in
     * @param page the page to create
     * @param templateProvider the template provider to use for the creation
     * @param context the context of the request
     * @return {@code true} if the creation is allowed, {@code false} otherwise
     */
    private boolean checkAllowedSpace(String space, String page, BaseObject templateProvider, XWikiContext context)
    {
        // Check that the chosen space is allowed with the given template, if not:
        // - Cancel the redirect
        // - set an error on the context, to be read by the create.vm
        if (templateProvider != null) {
            @SuppressWarnings("unchecked")
            List<String> allowedSpaces = templateProvider.getListValue(SPACES_PROPERTY);
            // if there is no allowed spaces set, all spaces are allowed
            if (allowedSpaces.size() > 0 && !allowedSpaces.contains(space)) {
                // put an exception on the context, for create.vm to know to display an error
                Object[] args = {templateProvider.getStringValue(TEMPLATE), space, page};
                XWikiException exception =
                    new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                        XWikiException.ERROR_XWIKI_APP_TEMPLATE_NOT_AVAILABLE,
                        "Template {0} cannot be used in space {1} when creating page {2}", null, args);
                VelocityContext vcontext = (VelocityContext) context.get(VELOCITY_CONTEXT_KEY);
                vcontext.put(EXCEPTION, exception);
                vcontext.put("createAllowedSpaces", allowedSpaces);
                return false;
            }
        }
        // if no template is specified, creation is allowed
        return true;
    }

    /**
     * @param template the template to create document from
     * @param resolver the resolver to use to resolve the template document reference
     * @param context the context of the current request
     * @return the default edit mode for a document created from the passed template
     * @throws XWikiException in case something goes wrong accessing template document
     */
    private String getEditMode(String template, DocumentReferenceResolver<String> resolver, XWikiContext context)
        throws XWikiException
    {
        // Determine the edit action (edit/inline) for the newly created document, if a template is passed it is
        // used to determine the action. Default is 'edit'.
        String editAction = "edit";
        XWiki xwiki = context.getWiki();
        if (!StringUtils.isEmpty(template)) {
            DocumentReference templateReference = resolver.resolve(template);
            if (xwiki.exists(templateReference, context)) {
                editAction = xwiki.getDocument(templateReference, context).getDefaultEditMode(context);
            }
        }

        return editAction;
    }

    /**
     * @param templateProvider the template provider for this creation
     * @return {@code true} if the created document should be saved on create, before editing, {@code false} otherwise
     */
    boolean getSaveBeforeEdit(BaseObject templateProvider)
    {
        boolean toSave = false;

        if (templateProvider != null) {
            // get the action to execute and compare it to saveandedit value
            String action = templateProvider.getStringValue("action");
            if ("saveandedit".equals(action)) {
                toSave = true;
            }
        }

        return toSave;
    }

    /**
     * Actually executes the create, after all preconditions have been verified.
     * 
     * @param context the context of this action
     * @param newDocument the document to be created
     * @param isSpace whether the document is a space webhome or a page
     * @param templateProvider the template provider to create from
     * @param resolver the reference resolver to use to resolve template references and other document references
     *            received in parameters
     * @throws XWikiException in case anything goes wrong accessing xwiki documents
     */
    private void doCreate(XWikiContext context, XWikiDocument newDocument, boolean isSpace,
        BaseObject templateProvider, DocumentReferenceResolver<String> resolver) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();
        String parent = getParent(request, doc, isSpace);

        // get the title of the page to create, as specified in the parameters
        String title = getTitle(request, newDocument, isSpace);

        // get the template from the template parameter, to allow creation directly from template, without
        // forcing to create a template provider for each template creation
        String template =
            (templateProvider != null) ? templateProvider.getStringValue(TEMPLATE) : (request.getParameterMap()
                .containsKey(TEMPLATE) ? request.getParameter(TEMPLATE) : "");

        // from the template provider, find out if the document should be saved before edited
        boolean toSave = getSaveBeforeEdit(templateProvider);

        String redirectParams = null;
        String editMode = null;
        if (toSave) {
            XWiki xwiki = context.getWiki();

            DocumentReference templateReference = resolver.resolve(template);
            newDocument.readFromTemplate(templateReference, context);
            if (!StringUtils.isEmpty(parent)) {
                DocumentReference parentReference = resolver.resolve(parent);
                newDocument.setParentReference((EntityReference) parentReference);
            }
            if (title != null) {
                newDocument.setTitle(title);
            }
            DocumentReference currentUserReference = context.getUserReference();
            newDocument.setAuthorReference(currentUserReference);
            newDocument.setCreatorReference(currentUserReference);

            xwiki.saveDocument(newDocument, context);
            editMode = newDocument.getDefaultEditMode(context);
        } else {
            // put all the data in the redirect params, to be passed to the edit mode
            redirectParams = "template=" + Util.encodeURI(template, context);
            if (parent != null) {
                redirectParams += "&parent=" + Util.encodeURI(parent, context);
            }
            if (title != null) {
                redirectParams += "&title=" + Util.encodeURI(title, context);
            }

            // Get the edit mode of the document to create from the specified template
            editMode = getEditMode(template, resolver, context);
        }

        // Perform a redirection to the edit mode of the new document
        String redirectURL = newDocument.getURL(editMode, redirectParams, context);
        redirectURL = context.getResponse().encodeRedirectURL(redirectURL);
        if (context.getRequest().getParameterMap().containsKey("ajax")) {
            // If this template is displayed from a modal popup, send a header in the response notifying that a
            // redirect
            // must be performed in the calling page.
            context.getResponse().setHeader("redirect", redirectURL);
        } else {
            // Perform the redirect
            sendRedirect(context.getResponse(), redirectURL);
        }
    }

    /**
     * @param space the space to check if there are available templates for
     * @param isSpace {@code true} if a space should be created instead of a page
     * @param resolver the resolver to solve template document references
     * @param context the context of the current request
     * @param templateClassReference the reference to the template provider class
     * @return the available templates for the passed space, as {@link Document}s
     */
    private List<Document> getAvailableTemplates(String space, boolean isSpace,
        DocumentReferenceResolver<String> resolver, DocumentReference templateClassReference, XWikiContext context)
    {
        XWiki wiki = context.getWiki();
        List<Document> templates = new ArrayList<Document>();
        try {
            QueryManager queryManager = Utils.getComponent((Type) QueryManager.class, "secure");
            Query query =
                queryManager.createQuery("from doc.object(XWiki.TemplateProviderClass) as template "
                    + "where doc.fullName not like 'XWiki.TemplateProviderTemplate'", Query.XWQL);
            List<String> templateProviderDocNames = query.execute();
            for (String templateProviderName : templateProviderDocNames) {
                // get the document
                DocumentReference reference = resolver.resolve(templateProviderName);
                XWikiDocument templateDoc = wiki.getDocument(reference, context);
                BaseObject templateObject = templateDoc.getXObject(templateClassReference);
                if (isSpace && SPACE.equals(templateObject.getStringValue(TYPE_PROPERTY))) {
                    templates.add(new Document(templateDoc, context));
                } else if (!isSpace && !SPACE.equals(templateObject.getStringValue(TYPE_PROPERTY))) {
                    @SuppressWarnings("unchecked")
                    List<String> allowedSpaces = templateObject.getListValue(SPACES_PROPERTY);
                    // if no space is checked or the current space is in the list of allowed spaces
                    if (allowedSpaces.size() == 0 || allowedSpaces.contains(space)) {
                        // create a Document and put it in the list
                        templates.add(new Document(templateDoc, context));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("There was an error gettting the available templates for space " + space, e);
        }

        return templates;
    }
}
