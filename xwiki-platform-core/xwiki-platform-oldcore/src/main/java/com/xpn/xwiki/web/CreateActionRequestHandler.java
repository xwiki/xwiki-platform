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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.ScriptContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Helper class used to handle one individual create action request.
 *
 * @version $Id$
 */
public class CreateActionRequestHandler
{
    /**
     * Log used to report exceptions.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateActionRequestHandler.class);

    /**
     * The name of the space reference parameter.
     */
    private static final String SPACE_REFERENCE = "spaceReference";

    /**
     * The name parameter.
     */
    private static final String NAME = "name";

    /**
     * The name of the deprecated space parameter. <br>
     * Note: if you change the value of this variable, change the value of {{@link #TOCREATE_SPACE} to the previous
     * value.
     *
     * @deprecated Use {@value #SPACE_REFERENCE} as parameter name instead.
     */
    @Deprecated
    private static final String SPACE = "space";

    /**
     * The name of the page parameter.
     *
     * @deprecated Use {@value #NAME} as parameter name instead.
     */
    @Deprecated
    private static final String PAGE = "page";

    /**
     * The value of the tocreate parameter when a space is to be created. <br>
     * TODO: find a way to give this constant the same value as the constant above without violating checkstyle.
     */
    private static final String TOCREATE_SPACE = SPACE;

    /**
     * The name of the "type" parameter.
     */
    private static final String TYPE = "type";

    /**
     * The value of the tocreate parameter when a terminal/regular document is to be created.
     */
    private static final String TOCREATE_TERMINAL = "terminal";

    /**
     * The value of the tocreate parameter when a non-terminal document is to be created.
     */
    private static final String TOCREATE_NONTERMINAL = "nonterminal";

    /**
     * The name of the template field inside the template provider, or the template parameter which can be sent
     * directly, without passing through the template provider.
     */
    private static final String TEMPLATE = "template";

    /**
     * The name of the template provider parameter.
     */
    private static final String TEMPLATE_PROVIDER = "templateprovider";

    /**
     * The template provider class, to create documents from templates.
     */
    private static final EntityReference TEMPLATE_PROVIDER_CLASS = new EntityReference("TemplateProviderClass",
        EntityType.DOCUMENT, new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    /**
     * The redirect class, used to mark pages that are redirect place-holders, i.e. hidden pages that serve only for
     * redirecting the user to a different page (e.g. when a page has been moved).
     */
    private static final EntityReference REDIRECT_CLASS = new EntityReference("RedirectClass", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    /**
     * The property name for the spaces in the template provider object.
     *
     * @deprecated since 8.3M2. Use {@link #TP_CREATION_RESTRICTIONS_PROPERTY} or
     *             {@value #TP_VISIBILITY_RESTRICTIONS_PROPERTY} instead for the explicit restriction you need to add.
     */
    @Deprecated
    private static final String SPACES_PROPERTY = "spaces";

    /**
     * The key used to add exceptions on the context, to be read by the template.
     */
    private static final String EXCEPTION = "createException";

    /**
     * Current entity reference resolver hint.
     */
    private static final String CURRENT_RESOLVER_HINT = "current";

    /**
     * Current entity reference resolver hint.
     */
    private static final String CURRENT_MIXED_RESOLVER_HINT = "currentmixed";

    /**
     * Local entity reference serializer hint.
     */
    private static final String LOCAL_SERIALIZER_HINT = "local";

    private static final String TP_TERMINAL_PROPERTY = TOCREATE_TERMINAL;

    private static final String TP_TYPE_PROPERTY = TYPE;

    private static final String TP_TYPE_PROPERTY_SPACE_VALUE = SPACE;

    private static final String TP_CREATION_RESTRICTIONS_PROPERTY = "creationRestrictions";

    private static final String TP_VISIBILITY_RESTRICTIONS_PROPERTY = "visibilityRestrictions";

    private static final String TP_CREATION_RESTRICTIONS_ARE_SUGGESTIONS_PROPERTY =
        "creationRestrictionsAreSuggestions";

    /**
     * Space homepage document name.
     */
    private static final String WEBHOME = "WebHome";
    
    /**
     * Authorization manager.
     */
    private AuthorizationManager authorizationManager;

    private ScriptContextManager scriptContextManager;

    private SpaceReference spaceReference;

    private String name;

    private boolean isSpace;

    private XWikiContext context;

    private XWikiDocument document;

    private XWikiRequest request;

    private BaseObject templateProvider;

    private List<Document> availableTemplateProviders;

    private List<Document> recommendedTemplateProviders;

    private String type;

    /**
     * @param context the XWikiContext for which to handle the request.
     */
    public CreateActionRequestHandler(XWikiContext context)
    {
        this.context = context;
        this.document = context.getDoc();
        this.request = context.getRequest();
    }

    /**
     * Process the request and extract from the given parameters the data needed to create the new document.
     *
     * @throws XWikiException if problems occur
     */
    public void processRequest() throws XWikiException
    {
        // Get the template provider for creating this document, if any template provider is specified
        DocumentReferenceResolver<EntityReference> referenceResolver =
            Utils.getComponent(DocumentReferenceResolver.TYPE_REFERENCE, CURRENT_RESOLVER_HINT);
        DocumentReference templateProviderClassReference = referenceResolver.resolve(TEMPLATE_PROVIDER_CLASS);
        templateProvider = getTemplateProvider(templateProviderClassReference);

        // Get the available templates, in the current space, to check if all conditions to create a new document are
        // met
        availableTemplateProviders =
            loadAvailableTemplateProviders(document.getDocumentReference().getLastSpaceReference(),
                templateProviderClassReference, context);

        // Get the type of document to create
        type = request.get(TYPE);

        // Since this template can be used for creating a Page or a Space, check the passed "tocreate" parameter
        // which can be either "page" or "space". If no parameter is passed then we default to creating a Page.
        String toCreate = request.getParameter("tocreate");

        if (document.isNew()) {
            processNewDocument(toCreate);
        } else {
            // We are on an existing document...

            if (request.getParameter(SPACE) != null || request.getParameter(PAGE) != null) {
                // We are in Backwards Compatibility mode and we are using the deprecated parameter names.
                processDeprecatedParameters(toCreate);
            } else {
                // Determine the new document values from the request.

                String spaceReferenceParameter = request.getParameter(SPACE_REFERENCE);
                // We can have an empty spaceReference parameter symbolizing that we are creating a top level space or
                // non-terminal document.
                if (StringUtils.isNotEmpty(spaceReferenceParameter)) {
                    EntityReferenceResolver<String> genericResolver =
                        Utils.getComponent(EntityReferenceResolver.TYPE_STRING, CURRENT_RESOLVER_HINT);

                    EntityReference resolvedEntityReference =
                        genericResolver.resolve(spaceReferenceParameter, EntityType.SPACE);
                    spaceReference = new SpaceReference(resolvedEntityReference);
                }

                // Note: We leave the spaceReference variable intentionally null to symbolize a top level space or
                // non-terminal document.

                name = request.getParameter(NAME);

                // Determine the type of document we are creating (terminal vs non-terminal).

                if (TOCREATE_TERMINAL.equals(toCreate) || TOCREATE_NONTERMINAL.equals(toCreate)) {
                    // Look at the request to see what the user wanted to create (terminal or non-terminal).

                    isSpace = !TOCREATE_TERMINAL.equals(toCreate);
                } else if (templateProvider != null) {
                    // A template provider is specified. Use it and extract the type of document.

                    boolean providerTerminal = getTemplateProviderTerminalValue();
                    isSpace = !providerTerminal;
                } else {
                    // Default to creating non-terminal documents.
                    isSpace = true;
                }
            }
        }
    }

    /**
     * @param toCreate the value of the "tocreate" request parameter
     */
    private void processNewDocument(String toCreate)
    {
        // Current space and page name.
        spaceReference = document.getDocumentReference().getLastSpaceReference();
        name = document.getDocumentReference().getName();

        // Determine if the current document is in a top-level space.
        EntityReference parentSpaceReference = spaceReference.getParent();
        boolean isTopLevelSpace = parentSpaceReference.extractReference(EntityType.SPACE) == null;

        // Remember this since we might update it below.
        String originalName = name;

        // Since WebHome is a convention, determine the real name and parent of our document.
        if (WEBHOME.equals(name)) {
            // Determine its name from the space name.
            name = spaceReference.getName();

            // Determine its space reference by looking at the space's parent.
            if (!isTopLevelSpace) {
                // The parent reference is a space reference. Use it.
                spaceReference = new SpaceReference(parentSpaceReference);
            } else {
                // Top level document, i.e. the parent reference is a wiki reference. Clear the spaceReference variable
                // so that this case is properly handled later on (as if an empty value was passed as parameter in the
                // request).
                spaceReference = null;
            }
        }

        // Determine the type of document we are creating (terminal vs non-terminal).

        if (TOCREATE_TERMINAL.equals(toCreate) || TOCREATE_NONTERMINAL.equals(toCreate)) {
            // Look at the request to see what the user wanted to create (terminal or non-terminal).

            isSpace = !TOCREATE_TERMINAL.equals(toCreate);
        } else if (templateProvider != null) {
            // A template provider is specified. Use it and extract the type of document.

            boolean providerTerminal = getTemplateProviderTerminalValue();
            isSpace = !providerTerminal;
        } else {
            // Last option is to check the document's original name and see if it was "WebHome".
            isSpace = WEBHOME.equals(originalName);
        }
    }

    /**
     * @return
     */
    private boolean getTemplateProviderTerminalValue()
    {
        boolean providerTerminal;
        int providerTerminalValue = templateProvider.getIntValue(TP_TERMINAL_PROPERTY, -1);
        if (providerTerminalValue == -1) {
            // Backwards compatibility with providers that did not have the "terminal" property. We are deducing it
            // from the value of the "type" property.
            String providerType = templateProvider.getStringValue(TP_TYPE_PROPERTY);
            if (TP_TYPE_PROPERTY_SPACE_VALUE.equals(providerType)) {
                providerTerminal = false;
            } else {
                // 'page' or NULL both resolve to true, for backwards compatibility reasons.
                providerTerminal = true;
            }
        } else {
            // Use the "terminal" value from the template provider.
            providerTerminal = (1 == providerTerminalValue);
        }
        return providerTerminal;
    }

    /**
     * @param toCreate the value of the "tocreate" request parameter
     */
    private void processDeprecatedParameters(String toCreate)
    {
        // Note: The most important details is that the deprecated "space" parameter stores unescaped space
        // names, not references!
        String spaceParameter = request.getParameter(SPACE);

        isSpace = TOCREATE_SPACE.equals(toCreate);
        if (isSpace) {
            // Always creating top level spaces in this mode. Adapt to the new implementation.
            spaceReference = null;
            name = spaceParameter;
        } else {
            if (StringUtils.isNotEmpty(spaceParameter)) {
                // Always creating documents in top level spaces in this mode.
                spaceReference = new SpaceReference(spaceParameter, document.getDocumentReference().getWikiReference());
            }

            name = request.getParameter(PAGE);
        }
    }

    /**
     * @param templateProviderClass the class of the template provider object
     * @return the object which holds the template provider to be used for creation
     * @throws XWikiException in case anything goes wrong manipulating documents
     */
    private BaseObject getTemplateProvider(DocumentReference templateProviderClass) throws XWikiException
    {
        BaseObject result = null;

        // set the template, from the template provider param
        String templateProviderDocReferenceString = request.getParameter(TEMPLATE_PROVIDER);

        if (!StringUtils.isEmpty(templateProviderDocReferenceString)) {
            // parse this document reference
            DocumentReference templateProviderRef =
                getCurrentMixedResolver().resolve(templateProviderDocReferenceString);
            // get the document of the template provider and the object
            XWikiDocument templateProviderDoc = context.getWiki().getDocument(templateProviderRef, context);
            result = templateProviderDoc.getXObject(templateProviderClass);
        }

        return result;
    }

    /**
     * @param spaceReference the space to check if there are available templates for
     * @param context the context of the current request
     * @param templateClassReference the reference to the template provider class
     * @return the available template providers for the passed space, as {@link Document}s
     */
    private List<Document> loadAvailableTemplateProviders(SpaceReference spaceReference,
        DocumentReference templateClassReference, XWikiContext context)
    {
        List<Document> templates = new ArrayList<>();
        try {
            QueryManager queryManager = Utils.getComponent((Type) QueryManager.class, "secure");
            Query query =
                queryManager.createQuery("from doc.object(XWiki.TemplateProviderClass) as template "
                    + "where doc.fullName not like 'XWiki.TemplateProviderTemplate' " + "order by template.name",
                    Query.XWQL);

            // TODO: Extend the above query to include a filter on the type and allowed spaces properties so we can
            // remove the java code below, thus improving performance by not loading all the documents, but only the
            // documents we need.

            List<XWikiDocument> recommendedTemplates = new ArrayList<>();

            List<String> templateProviderDocNames = query.execute();
            for (String templateProviderName : templateProviderDocNames) {
                // get the document and template provider object
                DocumentReference reference = getCurrentMixedResolver().resolve(templateProviderName);
                
                // Verify that the current user has the view right on the Template document
                if (!getAuthorizationManager().hasAccess(Right.VIEW, context.getUserReference(), reference)) {
                    continue;
                }
                    
                XWikiDocument templateDoc = context.getWiki().getDocument(reference, context);
                BaseObject templateObject = templateDoc.getXObject(templateClassReference);

                // Check the template provider's visibility restrictions.
                if (isTemplateProviderAllowedInSpace(templateObject, spaceReference,
                    TP_VISIBILITY_RESTRICTIONS_PROPERTY)) {

                    List<String> creationRestrictions =
                        getTemplateProviderRestrictions(templateObject, TP_CREATION_RESTRICTIONS_PROPERTY);
                    if (creationRestrictions.size() > 0 && isTemplateProviderAllowedInSpace(templateObject,
                        spaceReference, TP_CREATION_RESTRICTIONS_PROPERTY)) {
                        // Consider providers that have creations restrictions matching the current space as
                        // "recommended" and handle them separately.
                        recommendedTemplates.add(templateDoc);
                    } else {
                        // Other visible providers.
                        templates.add(new Document(templateDoc, context));
                    }
                }
            }

            EntityReferenceSerializer<String> localSerializer =
                Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, LOCAL_SERIALIZER_HINT);
            String spaceStringReference = localSerializer.serialize(spaceReference);

            // Sort the recommended providers and promote the most specific ones.
            recommendedTemplates.sort(Comparator.comparing((XWikiDocument templateProviderDocument) -> {
                BaseObject templateProviderObject = templateProviderDocument.getXObject(templateClassReference);

                // Look at any set creation restrictions.
                List<String> restrictions =
                    getTemplateProviderRestrictions(templateProviderObject, TP_CREATION_RESTRICTIONS_PROPERTY);

                // Return the longest (max) matching restriction reference size as being the most specific.
                return restrictions.stream()
                    // Filter only restrictions matching the current space.
                    .filter(restriction -> matchesRestriction(spaceStringReference, restriction))
                    // Score them based on which has the most specific restriction.
                    .mapToInt(restriction -> {
                        SpaceReferenceResolver<String> spaceResolver =
                            Utils.getComponent(SpaceReferenceResolver.TYPE_STRING);
                        SpaceReference restrictionSpaceReference = spaceResolver.resolve(restriction);
                        // The specificity score.
                        int specificity = restrictionSpaceReference.getReversedReferenceChain().size();
                        return specificity;
                    }).max()
                    // Or 0 if no restrictions are set or if none match the current space.
                    .orElse(0);
            }).reversed());

            this.recommendedTemplateProviders = recommendedTemplates.stream()
                .map(recommendedTemplate -> new Document(recommendedTemplate, context)).collect(Collectors.toList());

            // Give priority to the providers that that specify creation restrictions
            templates.addAll(0, recommendedTemplateProviders);
        } catch (Exception e) {
            LOGGER.warn("There was an error getting the available templates for space {0}", spaceReference, e);
        }

        return templates;
    }

    private DocumentReferenceResolver<String> getCurrentMixedResolver()
    {
        // resolver to use to resolve references received in request parameters
        return Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, CURRENT_MIXED_RESOLVER_HINT);
    }

    private boolean isTemplateProviderAllowedInSpace(BaseObject templateObject, SpaceReference spaceReference,
        String restrictionsProperty)
    {
        // Check the allowed spaces list.
        List<String> restrictions = getTemplateProviderRestrictions(templateObject, restrictionsProperty);
        if (restrictions.size() > 0) {
            EntityReferenceSerializer<String> localSerializer =
                Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, LOCAL_SERIALIZER_HINT);
            String spaceStringReference = localSerializer.serialize(spaceReference);

            for (String allowedSpace : restrictions) {
                // Exact match or parent space (i.e. prefix) match.
                if (matchesRestriction(spaceStringReference, allowedSpace)) {
                    return true;
                }
            }

            // No match, not allowed.
            return false;
        }

        // No restrictions exist, allowed by default.
        return true;
    }

    private boolean matchesRestriction(String spaceStringReferenceToTest, String allowedSpaceRestriction)
    {
        return allowedSpaceRestriction.equals(spaceStringReferenceToTest)
            || StringUtils.startsWith(spaceStringReferenceToTest, String.format("%s.", allowedSpaceRestriction));
    }

    private List<String> getTemplateProviderRestrictions(BaseObject templateObject, String restrictionsProperty)
    {
        List<String> creationRestrictions = templateObject.getListValue(restrictionsProperty);
        if (creationRestrictions.size() == 0) {
            // Backwards compatibility for template providers created before 8.3M2, where the "spaces" property handled
            // both visibility and creation.
            creationRestrictions = templateObject.getListValue(SPACES_PROPERTY);
        }
        return creationRestrictions;
    }

    /**
     * @return the document reference of the new document to be created, {@code null} if a no document can be created
     *         (because the conditions are not met)
     */
    public DocumentReference getNewDocumentReference()
    {
        DocumentReference result = null;

        if (StringUtils.isEmpty(name)) {
            // Can`t do anything without a name.
            return null;
        }

        // The new values, after the processing needed for ND below, to be used when creating the document reference.
        SpaceReference newSpaceReference = spaceReference;
        String newName = name;

        // Special handling for old spaces or new Nested Documents.
        if (isSpace) {
            EntityReference parentSpaceReference = spaceReference;
            if (parentSpaceReference == null) {
                parentSpaceReference = context.getDoc().getDocumentReference().getWikiReference();
            }

            // The new space's reference.
            newSpaceReference = new SpaceReference(name, parentSpaceReference);

            // The new document's name set to the new space's homepage. In Nested Documents, this leads to the new ND's
            // reference name.
            newName = WEBHOME;
        }

        // Proceed with creating the document...

        if (newSpaceReference == null) {
            // No space specified, nothing to do. This can be the case for terminal documents, since non-terminal
            // documents can be top-level.
            return null;
        }

        // Check whether there is a template parameter set, be it an empty one. If a page should be created and there is
        // no template parameter, it means the create action is not supposed to be executed, but only display the
        // available templates and let the user choose
        // If there's no passed template, check if there are any available templates. If none available, then the fact
        // that there is no template is ok.
        if (hasTemplate() || availableTemplateProviders.isEmpty()) {
            result = new DocumentReference(newName, newSpaceReference);
        }

        return result;
    }

    /**
     * @return if a template or a template provider have been set (it can be empty however)
     */
    public boolean hasTemplate()
    {
        return request.getParameter(TEMPLATE_PROVIDER) != null || request.getParameter(TEMPLATE) != null;
    }

    /**
     * Verifies if the creation inside the specified spaceReference is allowed by the current template provider. If the
     * creation is not allowed, an exception will be set on the context.
     *
     * @return {@code true} if the creation is allowed, {@code false} otherwise
     */
    public boolean isTemplateProviderAllowedToCreateInCurrentSpace()
    {
        // Check that the chosen space is allowed with the given template, if not:
        // - Cancel the redirect
        // - Set an error on the context, to be read by the create.vm
        if (templateProvider != null) {
            // Check if the creation restrictions are enforced.
            boolean creationRestrictionsEnforced =
                templateProvider.getIntValue(TP_CREATION_RESTRICTIONS_ARE_SUGGESTIONS_PROPERTY, 0) == 0;

            // Check using the template provider's creation restrictions, only when they are enforced.
            if (creationRestrictionsEnforced && !isTemplateProviderAllowedInSpace(templateProvider, spaceReference,
                TP_CREATION_RESTRICTIONS_PROPERTY)) {
                // put an exception on the context, for create.vm to know to display an error
                Object[] args = {templateProvider.getStringValue(TEMPLATE), spaceReference, name};
                XWikiException exception =
                    new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                        XWikiException.ERROR_XWIKI_APP_TEMPLATE_NOT_AVAILABLE,
                        "Template {0} cannot be used in space {1} when creating page {2}", null, args);

                ScriptContext scontext = getCurrentScriptContext();
                scontext.setAttribute(EXCEPTION, exception, ScriptContext.ENGINE_SCOPE);
                scontext.setAttribute("createAllowedSpaces",
                    getTemplateProviderRestrictions(templateProvider, TP_CREATION_RESTRICTIONS_PROPERTY),
                    ScriptContext.ENGINE_SCOPE);

                return false;
            }
        }

        // For all other cases, creation is allowed.
        return true;
    }

    /**
     * @param newDocument the new document to check if it already exists
     * @return true if the document already exists (i.e. is not usable) and set an exception in the velocity context;
     *         false otherwise.
     */
    public boolean isDocumentAlreadyExisting(XWikiDocument newDocument)
    {
        // if the document exists don't create it, put the exception on the context so that the template gets it and
        // re-requests the page and space, else create the document and redirect to edit
        if (!isEmptyDocument(newDocument)) {
            ScriptContext scontext = getCurrentScriptContext();

            // Expose to the template reference of the document that already exist so that it can propose to view or
            // edit it.
            scontext.setAttribute("existingDocumentReference", newDocument.getDocumentReference(),
                ScriptContext.ENGINE_SCOPE);

            // Throw an exception.
            Object[] args = {newDocument.getDocumentReference()};
            XWikiException documentAlreadyExists =
                new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY,
                    "Cannot create document {0} because it already has content", null, args);
            scontext.setAttribute(EXCEPTION, documentAlreadyExists, ScriptContext.ENGINE_SCOPE);

            return true;
        }

        return false;
    }

    /**
     * Checks if a document is empty, that is, if a document with that name could be created from a template. <br>
     * TODO: move this function to a more accessible place, to be used by the readFromTemplate method as well, so that
     * we have consistency.
     *
     * @param document the document to check
     * @return {@code true} if the document is empty (i.e. a document with the same name can be created (from
     *         template)), {@code false} otherwise
     */
    private boolean isEmptyDocument(XWikiDocument document)
    {
        // If it's a new document or a redirect placeholder, it's fine.
        if (document.isNew() || document.getXObject(REDIRECT_CLASS) != null) {
            return true;
        }

        // FIXME: the code below is not really what users might expect. Overriding an existing document (even if no
        // content or objects) is not really nice to do. Should be removed.

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
     * @return the {@link VelocityContext} for the context we are handling
     * @deprecated since 8.3M1, use {@link #getCurrentScriptContext()} instead
     */
    @Deprecated
    public VelocityContext getVelocityContext()
    {
        return Utils.getComponent(VelocityManager.class).getVelocityContext();
    }
    
    /**
     * @return the authorization manager
     * @since 9.11
     */
    protected AuthorizationManager getAuthorizationManager()
    {
        return Utils.getComponent(AuthorizationManager.class);
    }

    /**
     * @return the current script context
     * @since 8.3M1
     */
    protected ScriptContext getCurrentScriptContext()
    {
        if (this.scriptContextManager == null) {
            this.scriptContextManager = Utils.getComponent(ScriptContextManager.class);
        }

        return this.scriptContextManager.getCurrentScriptContext();
    }

    /**
     * @return the space reference where the new document will be created
     */
    public SpaceReference getSpaceReference()
    {
        return spaceReference;
    }

    /**
     * @return the name of the new document. See {@link #isSpace()}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return true if the new document is a space (i.e. Nested Document and the name means space name) or false if it's
     *         a terminal regular document (i.e. Nested Spaces document and the name means document name)
     */
    public boolean isSpace()
    {
        return isSpace;
    }

    /**
     * @return the available template providers for the space from where we are creating the new document
     */
    public List<Document> getAvailableTemplateProviders()
    {
        return availableTemplateProviders;
    }

    /**
     * @return the recommended template providers, from the list of available ones
     */
    public List<Document> getRecommendedTemplateProviders()
    {
        return recommendedTemplateProviders;
    }

    /**
     * @return the currently used template provider read from the request, or {@code null} if none was set
     */
    public BaseObject getTemplateProvider()
    {
        return templateProvider;
    }

    /**
     * @return the type of document to create, read from the request, or {@code null} if none was set
     * @since 7.2
     */
    public String getType()
    {
        return type;
    }
}
