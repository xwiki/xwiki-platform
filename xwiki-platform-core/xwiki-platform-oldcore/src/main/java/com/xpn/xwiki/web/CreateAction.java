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
 *
 */
package com.xpn.xwiki.web;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
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
     * The template provider class, to create documents from templates.
     */
    private static final EntityReference TEMPLATE_PROVIDER_CLASS =
        new EntityReference("TemplateProviderClass", EntityType.DOCUMENT,
            new EntityReference("XWiki", EntityType.SPACE));

    /**
     * The name of the space parameter. <br />
     * Note: if you change the value of this variable, change the value of {{@link #TOCREATE_SPACE} to the previous
     * value.
     */
    private static final String SPACE = "space";

    /**
     * The name of the page parameter. <br />
     * Note: if you change the value of this variable, change the value of {{@link #TOCREATE_PAGE} to the previous
     * value.
     */
    private static final String PAGE = "page";

    /**
     * The value of the toCreate parameter when a space is to be created. <br />
     * TODO: find a way to give this constant the same value as the constant above without violating checkstyle.
     */
    private static final String TOCREATE_SPACE = SPACE;

    /**
     * The value of the toCreate parameter when a page is to be created. <br />
     * TODO: find a way to give this constant the same value as the constant above without violating checkstyle.
     */
    private static final String TOCREATE_PAGE = PAGE;

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
     * {@inheritDoc}
     * 
     * @see XWikiAction#render(XWikiContext)
     */
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();
        @SuppressWarnings("unchecked")
        DocumentReferenceResolver<String> resolver =
            Utils.getComponent(DocumentReferenceResolver.class, "currentmixed");

        // Since this template can be used for creating a Page or a Space, check the passed "tocreate" parameter
        // which can be either "page" or "space". If no parameter is passed then we default to creating a Page.
        String toCreate = request.getParameter("tocreate");
        boolean isSpace = false;
        if (StringUtils.isEmpty(toCreate) || !TOCREATE_SPACE.equals(toCreate)) {
            toCreate = TOCREATE_PAGE;
        } else {
            isSpace = true;
        }

        String parent = getParent(request, doc, isSpace);

        // get the template to use for creation of this document
        @SuppressWarnings("unchecked")
        DocumentReferenceResolver<EntityReference> referenceResolver =
            Utils.getComponent(DocumentReferenceResolver.class, "current/reference");
        BaseObject templateProvider =
            getTemplateProvider(context, resolver, referenceResolver.resolve(TEMPLATE_PROVIDER_CLASS));

        // get the template from the template parameter, to allow creation directly from template, without forcing
        // to create a template provider for each template creation
        String template =
            (templateProvider != null) ? templateProvider.getStringValue(TEMPLATE) : (request.getParameterMap()
                .containsKey(TEMPLATE) ? request.getParameter(TEMPLATE) : "");

        String redirectParams =
            "parent=" + Util.encodeURI(parent, context) + "&template=" + Util.encodeURI(template, context);

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

        // get the title of the page to create, as specified in the parameters
        String title = getTitle(request, space, page, isSpace);
        redirectParams = redirectParams + "&title=" + Util.encodeURI(title, context);

        // Get the edit mode of the document to create from the specified template
        String editMode = getEditMode(template, resolver, context);

        // Perform a redirection to the edit mode if required
        String redirectURL = getRedirectURL(context, isSpace, space, page, templateProvider, editMode, redirectParams);

        // if there is a set redirect url, perform the redirect
        if (!StringUtils.isEmpty(redirectURL)) {
            redirectURL = context.getResponse().encodeRedirectURL(redirectURL);
            if (request.getParameterMap().containsKey("ajax")) {
                // If this template is displayed from a modal popup, send a header in the response notifying that a
                // redirect
                // must be performed in the calling page.
                context.getResponse().setHeader("redirect", redirectURL);
            } else {
                // Perform the redirect
                sendRedirect(context.getResponse(), redirectURL);
            }
        }

        return "create";
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
        // if we're creating a new space.
        String parent = request.getParameter("parent");
        if (StringUtils.isEmpty(parent) && !isSpace) {
            DocumentReference parentRef = doc.getDocumentReference();
            @SuppressWarnings("unchecked")
            EntityReferenceSerializer<String> localSerializer =
                Utils.getComponent(EntityReferenceSerializer.class, "local");
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
     * @param space the space of the document to be created, as specified in the request parameters
     * @param page the page of the document to be created, as specified in the request parameters
     * @param isSpace {@code true} if the request is to create a space, {@code false} if a page should be created
     * @return the title of the page to be created
     */
    private String getTitle(XWikiRequest request, String space, String page, boolean isSpace)
    {
        String title = request.getParameter("title");
        if (StringUtils.isEmpty(title)) {
            title = isSpace ? space : page;
        }

        return title;
    }

    /**
     * Verifies if the creation inside space {@code space} is allowed by the template provider described by {@code
     * templateProvider}. If the creation is not allowed, an exception will be set on the context.
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
            List<String> allowedSpaces = templateProvider.getListValue("spaces");
            // if there is no allowed spaces set, all spaces are allowed
            if (allowedSpaces.size() > 0 && !allowedSpaces.contains(space)) {
                // put an exception on the context, for create.vm to know to display an error
                Object[] args = {templateProvider.getStringValue(TEMPLATE), space, page};
                XWikiException exception =
                    new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                        XWikiException.ERROR_XWIKI_APP_TEMPLATE_NOT_AVAILABLE,
                        "Template {0} cannot be used in space {1} when creating page {2}", null, args);
                context.put("exception", exception);
                context.put("allowedSpaces", allowedSpaces);
                return false;
            }
        }
        // if no template is specified, creation is allowed
        return true;
    }

    /**
     * Computes the redirect URL for the created document.
     * 
     * @param context the context of this request
     * @param isSpace {@code true} if the request is to create a space, {@code false} if a page should be created
     * @param space the space of the created document
     * @param page the page of the created document
     * @param templateProvider the template provider for this creation
     * @param redirectParams the redirect params, as computed previously
     * @param editMode the edit mode of the document to be created from the template
     * @return the URL to redirect after the creation
     */
    private String getRedirectURL(XWikiContext context, boolean isSpace, String space, String page,
        BaseObject templateProvider, String editMode, String redirectParams)
    {
        String redirectURL = null;

        XWiki xwiki = context.getWiki();
        if (isSpace && !StringUtils.isEmpty(space)) {
            // If a space is ready to be created, redirect to the space home in edit mode
            DocumentReference newDocRef = new DocumentReference(context.getDatabase(), space, "WebHome");
            redirectURL = xwiki.getURL(newDocRef, editMode, redirectParams, null, context);
        }

        boolean hasTemplate =
            context.getRequest().getParameterMap().containsKey(TEMPLATE_PROVIDER)
                || context.getRequest().getParameterMap().containsKey(TEMPLATE);
        if (!isSpace && !StringUtils.isEmpty(page) && !StringUtils.isEmpty(space) && hasTemplate) {
            DocumentReference newDocRef = new DocumentReference(context.getDatabase(), space, page);
            redirectURL = xwiki.getURL(newDocRef, editMode, redirectParams, null, context);

            if (!checkAllowedSpace(space, page, templateProvider, context)) {
                // reset the redirect url
                redirectURL = null;
            }
        }

        return redirectURL;
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
}
