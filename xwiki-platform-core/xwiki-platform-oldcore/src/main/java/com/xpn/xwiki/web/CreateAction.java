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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

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
@Component
@Named("create")
@Singleton
public class CreateAction extends XWikiAction
{
    /**
     * The name of the create.vm template to render.
     */
    private static final String CREATE_TEMPLATE = "create";

    /**
     * The name of the parent parameter.
     */
    private static final String PARENT = "parent";

    /**
     * The name of the space reference parameter.
     */
    private static final String SPACE_REFERENCE = "spaceReference";

    /**
     * The name parameter.
     */
    private static final String NAME = "name";

    /**
     * The name of the template field inside the template provider, or the template parameter which can be sent
     * directly, without passing through the template provider.
     */
    private static final String TEMPLATE = "template";

    /**
     * Internal name for a flag determining if we are creating a Nested Space or a terminal document.
     */
    private static final String IS_SPACE = "isSpace";

    /**
     * Space homepage document name.
     */
    private static final String WEBHOME = "WebHome";

    /**
     * Local entity reference serializer hint.
     */
    private static final String LOCAL_SERIALIZER_HINT = "local";

    /**
     * The name of the parameter that contains the form token.
     */
    private static final String FORM_TOKEN_PARAMETER = "form_token";

    @Inject
    private CSRFToken csrf;

    /**
     * The action to perform when creating a new page from a template.
     *
     * @version $Id$
     */
    private enum ActionOnCreate
    {
        /**
         * Go to edit mode without saving.
         */
        EDIT("edit"),

        /**
         * Save and then go to edit mode.
         */
        SAVE_AND_EDIT("saveandedit"),

        /**
         * Save and then go to view mode.
         */
        SAVE_AND_VIEW("saveandview");

        private static final Map<String, ActionOnCreate> BY_ACTION = new HashMap<>();

        static {
            for (ActionOnCreate actionOnCreate : values()) {
                BY_ACTION.put(actionOnCreate.action, actionOnCreate);
            }
        }

        private final String action;

        ActionOnCreate(String action)
        {
            this.action = action;
        }

        public static ActionOnCreate valueOfAction(String action)
        {
            return BY_ACTION.get(action);
        }
    }

    /**
     * Default constructor.
     */
    public CreateAction()
    {
        this.waitForXWikiInitialization = false;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        CreateActionRequestHandler handler = new CreateActionRequestHandler(context);

        // Read the request and extract the passed information.
        handler.processRequest();

        // Save the determined values so we have them available in the action template.
        ScriptContext scontext = getCurrentScriptContext();
        scontext.setAttribute(SPACE_REFERENCE, handler.getSpaceReference(), ScriptContext.ENGINE_SCOPE);
        scontext.setAttribute(NAME, handler.getName(), ScriptContext.ENGINE_SCOPE);
        scontext.setAttribute(IS_SPACE, handler.isSpace(), ScriptContext.ENGINE_SCOPE);
        // put the available templates on the context, for the .vm to not compute them again
        scontext.setAttribute("availableTemplateProviders", handler.getAvailableTemplateProviders(),
            ScriptContext.ENGINE_SCOPE);
        scontext.setAttribute("recommendedTemplateProviders", handler.getRecommendedTemplateProviders(),
            ScriptContext.ENGINE_SCOPE);

        DocumentReference newDocumentReference = handler.getDocumentReference();

        XWikiDocument newDocument;
        if (newDocumentReference == null)
        {
            return CREATE_TEMPLATE;
        }
        // Checking the rights to create the new document.
        // Note: Note checking the logical spaceReference, but the space of the final actual document reference, since
        // that is where we are creating the new document.
        checkRights(newDocumentReference.getLastSpaceReference(), context);

        // Check if the document to create already exists and if it respects the name strategy
        // Also check the CSRF token.
        newDocument = context.getWiki().getDocument(newDocumentReference, context);
        if (handler.isDocumentAlreadyExisting(newDocument) || handler.isDocumentPathTooLong(newDocumentReference)
            || !this.isEntityReferenceNameValid(newDocumentReference)
            || !this.csrf.isTokenValid(context.getRequest().getParameter(FORM_TOKEN_PARAMETER)))
        {
            return CREATE_TEMPLATE;
        }

        if (!handler.isTemplateInfoProvided() || !handler.isTemplateProviderAllowedToCreateInCurrentSpace())
        {
            // No template is selected or the selected template provider is not usable in the selected location. Go
            // back to the template and pick something else.
            return CREATE_TEMPLATE;
        }

        // Verify if the "type" of document to create has been set, even if we currently do not use it in the action.
        // The goal is let the user be able to chose it, which have some consequences in the UI (thanks to javascript).
        // See: https://jira.xwiki.org/browse/XWIKI-12580
        // Note: we do not need the "type" if we have a template provider: the type of the new document will be the type
        // of the template.
        // TODO: handle this type in doCreate() that we call above (see: https://jira.xwiki.org/browse/XWIKI-12585).
        if (StringUtils.isBlank(handler.getType()) && !handler.hasTemplate()) {
            return CREATE_TEMPLATE;
        }

        // create is finally valid, can be executed
        doCreate(context, newDocument, handler.isSpace(), handler.getTemplateProvider());

        return null;
    }

    /**
     * @param context the XWiki context
     * @param spaceReference the reference of the space where the new document will be created
     * @throws XWikiException in case the permission to create a new document in the specified space is denied
     */
    private void checkRights(SpaceReference spaceReference, XWikiContext context) throws XWikiException
    {
        ContextualAuthorizationManager authManager = Utils.getComponent(ContextualAuthorizationManager.class);
        if (!authManager.hasAccess(Right.EDIT, spaceReference)) {
            Object[] args = { spaceReference.toString(), context.getUser() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "The creation of a document into the space {0} has been denied to user {1}", null, args);
        }
    }

    /**
     * Actually executes the create, after all preconditions have been verified.
     *
     * @param context the context of this action
     * @param newDocument the document to be created
     * @param isSpace whether the document is a space webhome or a page
     * @param templateProvider the template provider to create from
     * @throws XWikiException in case anything goes wrong accessing xwiki documents
     */
    private void doCreate(XWikiContext context, XWikiDocument newDocument, boolean isSpace, BaseObject templateProvider)
        throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();

        String parent = getParent(request, doc, isSpace, context);

        // get the title of the page to create, as specified in the parameters
        String title = getTitle(request, newDocument, isSpace);

        // get the template from the template parameter, to allow creation directly from template, without
        // forcing to create a template provider for each template creation
        String template = getTemplate(templateProvider, request);

        // Read from the template provide the action to perform when creating the page.
        ActionOnCreate actionOnCreate = getActionOnCreate(templateProvider);

        String action = null;
        if (actionOnCreate == ActionOnCreate.SAVE_AND_EDIT) {
            initAndSaveDocument(context, newDocument, title, template, parent);
            action = newDocument.getDefaultEditMode(context);
        } else {
            action = actionOnCreate == ActionOnCreate.SAVE_AND_VIEW ? "save" : getEditMode(template, context);
        }

        // The form token from the request (validated above).
        String formToken = context.getRequest().getParameter(FORM_TOKEN_PARAMETER);

        // Perform a redirection to the selected action of the document to create.
        String redirectParams = getRedirectParameters(parent, title, template, actionOnCreate, formToken);
        String redirectURL = newDocument.getURL(action, redirectParams, context);
        redirectURL = context.getResponse().encodeRedirectURL(redirectURL);
        if (context.getRequest().getParameterMap().containsKey("ajax")) {
            // If this template is displayed from a modal popup, send a header in the response notifying that a
            // redirect must be performed in the calling page.
            context.getResponse().setHeader("redirect", redirectURL);
        } else {
            // Perform the redirect
            sendRedirect(context.getResponse(), redirectURL);
        }
    }

    /**
     * Initialize and save the new document before editing it. Follow the steps done by the Save action.
     * 
     * @param context the XWiki context
     * @param newDocument the document being created
     * @param title the document title
     * @param template the template to copy
     * @param parent the parent document
     * @throws XWikiException if copying the template or saving the document fails
     */
    private void initAndSaveDocument(XWikiContext context, XWikiDocument newDocument, String title, String template,
        String parent) throws XWikiException
    {
        XWiki xwiki = context.getWiki();

        // Set the locale and default locale, considering that we're creating the original version of the document
        // (not a translation).
        newDocument.setLocale(Locale.ROOT);
        if (newDocument.getDefaultLocale() == Locale.ROOT) {
            newDocument.setDefaultLocale(xwiki.getLocalePreference(context));
        }

        // Copy the template.
        readFromTemplate(newDocument, template, context);

        // Set the parent field.
        if (!StringUtils.isEmpty(parent)) {
            DocumentReference parentReference = getCurrentMixedDocumentReferenceResolver().resolve(parent);
            newDocument.setParentReference(parentReference);
        }

        // Set the document title
        if (title != null) {
            newDocument.setTitle(title);
        }

        // Set the author and creator.
        DocumentReference currentUserReference = context.getUserReference();
        newDocument.setAuthorReference(currentUserReference);
        newDocument.setCreatorReference(currentUserReference);

        // Make sure the user is allowed to make this modification
        xwiki.checkSavingDocument(currentUserReference, newDocument, context);

        xwiki.saveDocument(newDocument, context);
    }

    private String getRedirectParameters(String parent, String title, String template, ActionOnCreate actionOnCreate,
        String formToken)
    {
        if (actionOnCreate == ActionOnCreate.SAVE_AND_EDIT) {
            // We don't need to pass any parameters because the document is saved before the redirect using the
            // parameter values.
            return null;
        }

        String redirectParams = "template=" + Util.encodeURI(template, null);
        if (parent != null) {
            redirectParams += "&parent=" + Util.encodeURI(parent, null);
        }
        if (title != null) {
            redirectParams += "&title=" + Util.encodeURI(title, null);
        }
        // Both the save and the edit action might require a CSRF token
        redirectParams += "&form_token=" + Util.encodeURI(formToken, null);

        return redirectParams;
    }

    /**
     * @param templateProvider the set template provider, if any
     * @param request the request on which to fallback
     * @return the string reference of the document to use as template or {@code ""} if none set
     */
    private String getTemplate(BaseObject templateProvider, XWikiRequest request)
    {
        String result = "";

        if (templateProvider != null) {
            result = templateProvider.getStringValue(TEMPLATE);
        } else if (request.getParameter(TEMPLATE) != null) {
            result = request.getParameter(TEMPLATE);
        }

        return result;
    }

    /**
     * @param request the current request for which this action is executed
     * @param doc the current document
     * @param isSpace {@code true} if the request is to create a space, {@code false} if a page should be created
     * @param context the XWiki context
     * @return the serialized reference of the parent to create the document for
     */
    private String getParent(XWikiRequest request, XWikiDocument doc, boolean isSpace, XWikiContext context)
    {
        // This template can be passed a parent document reference in parameter (using the "parent" parameter).
        // If a parent parameter is passed, use it to set the parent when creating the new Page or Space.
        // If no parent parameter was passed:
        // * use the current document
        // ** if we're creating a new page and if the current document exists or
        // * use the Main space's WebHome
        // ** if we're creating a new page and the current document does not exist.
        String parent = request.getParameter(PARENT);
        if (StringUtils.isEmpty(parent)) {
            EntityReferenceSerializer<String> localSerializer =
                Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, LOCAL_SERIALIZER_HINT);

            if (doc.isNew()) {
                // Use the Main space's WebHome.
                Provider<DocumentReference> defaultDocumentReferenceProvider =
                    Utils.getComponent(DocumentReference.TYPE_PROVIDER);

                DocumentReference parentRef =
                    defaultDocumentReferenceProvider.get().setWikiReference(context.getWikiReference());

                parent = localSerializer.serialize(parentRef);
            } else {
                // Use the current document.
                DocumentReference parentRef = doc.getDocumentReference();

                parent = localSerializer.serialize(parentRef);
            }
        }

        return parent;
    }

    /**
     * @param request the current request for which this action is executed
     * @param newDocument the document to be created
     * @param isSpace {@code true} if the request is to create a space, {@code false} if a page should be created
     * @return the title of the page to be created. If no request parameter is set, the page name is returned for a new
     *         page and the space name is returned for a new space
     */
    private String getTitle(XWikiRequest request, XWikiDocument newDocument, boolean isSpace)
    {
        String title = request.getParameter("title");
        if (StringUtils.isEmpty(title)) {
            if (isSpace) {
                title = newDocument.getDocumentReference().getLastSpaceReference().getName();
            } else {
                title = newDocument.getDocumentReference().getName();
                // Avoid WebHome titles for pages that are really space homepages.
                if (WEBHOME.equals(title)) {
                    title = newDocument.getDocumentReference().getLastSpaceReference().getName();
                }
            }
        }

        return title;
    }

    /**
     * @param templateProvider the template provider for this creation
     * @return {@code true} if the created document should be saved on create, before editing, {@code false} otherwise
     */
    private ActionOnCreate getActionOnCreate(BaseObject templateProvider)
    {
        if (templateProvider != null) {
            String action = templateProvider.getStringValue("action");
            ActionOnCreate actionOnCreate = ActionOnCreate.valueOfAction(action);
            if (actionOnCreate != null) {
                return actionOnCreate;
            }
        }

        // Default action when creating a page from a template.
        return ActionOnCreate.EDIT;
    }

    /**
     * @param template the template to create document from
     * @param context the context of the current request
     * @return the default edit mode for a document created from the passed template
     * @throws XWikiException in case something goes wrong accessing template document
     */
    private String getEditMode(String template, XWikiContext context)
        throws XWikiException
    {
        // Determine the edit action (edit/inline) for the newly created document, if a template is passed it is
        // used to determine the action. Default is 'edit'.
        String editAction = ActionOnCreate.EDIT.name().toLowerCase();
        XWiki xwiki = context.getWiki();
        if (!StringUtils.isEmpty(template)) {
            DocumentReference templateReference = getCurrentMixedDocumentReferenceResolver().resolve(template);
            if (xwiki.exists(templateReference, context)) {
                editAction = xwiki.getDocument(templateReference, context).getDefaultEditMode(context);
            }
        }

        return editAction;
    }
}
