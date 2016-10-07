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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link com.xpn.xwiki.web.CreateAction}.
 *
 * @version $Id$
 * @since 7.2M1
 */
@ComponentList
@ReferenceComponentList
public class CreateActionTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    XWikiURLFactory mockURLFactory;

    CreateAction action;

    XWikiContext context;

    XWikiRequest mockRequest;

    XWikiResponse mockResponse;

    Query mockTemplateProvidersQuery;

    @Before
    public void setUp() throws Exception
    {
        context = oldcore.getXWikiContext();

        Utils.setComponentManager(oldcore.getMocker());

        QueryManager mockSecureQueryManager =
            oldcore.getMocker().registerMockComponent((Type) QueryManager.class, "secure");

        mockTemplateProvidersQuery = mock(Query.class);
        when(mockSecureQueryManager.createQuery(anyString(), anyString())).thenReturn(mockTemplateProvidersQuery);
        when(mockTemplateProvidersQuery.execute()).thenReturn(Collections.emptyList());

        when(oldcore.getMockContextualAuthorizationManager().hasAccess(any(Right.class), any(EntityReference.class)))
            .thenReturn(true);

        Provider<DocumentReference> mockDocumentReferenceProvider =
            oldcore.getMocker().registerMockComponent(DocumentReference.TYPE_PROVIDER);
        when(mockDocumentReferenceProvider.get())
            .thenReturn(new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome"));

        mockURLFactory = mock(XWikiURLFactory.class);
        context.setURLFactory(mockURLFactory);

        action = new CreateAction();

        mockRequest = mock(XWikiRequest.class);
        context.setRequest(mockRequest);

        mockResponse = mock(XWikiResponse.class);
        context.setResponse(mockResponse);

        when(mockRequest.get("type")).thenReturn("plain");
    }

    @Test
    public void newDocumentFromURL() throws Exception
    {
        // new document = xwiki:X.Y
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("X"), "Y");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(true);

        context.setDoc(document);

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        verify(mockURLFactory).createURL("X", "Y", "edit", "template=&parent=Main.WebHome&title=Y", null, "xwiki",
            context);
    }

    @Test
    public void newDocumentButNonTerminalFromURL() throws Exception
    {
        // new document = xwiki:X.Y
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("X"), "Y");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(true);

        context.setDoc(document);

        // Pass the tocreate=nonterminal request parameter
        when(mockRequest.getParameter("tocreate")).thenReturn("nonterminal");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        verify(mockURLFactory).createURL("X.Y", "WebHome", "edit", "template=&parent=Main.WebHome&title=Y", null,
            "xwiki", context);
    }

    @Test
    public void newDocumentFromURLWhenNoType() throws Exception
    {
        // No type has been set by the user
        when(mockRequest.get("type")).thenReturn(null);

        // new document = xwiki:X.Y
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("X"), "Y");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(true);

        context.setDoc(document);

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertEquals("create", result);
    }

    @Test
    public void newDocumentWebHomeTopLevelFromURL() throws Exception
    {
        // new document = xwiki:X.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("X"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(true);

        context.setDoc(document);

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: The title is not "WebHome", but "X" (the space's name) to avoid exposing "WebHome" in the UI.
        verify(mockURLFactory).createURL("X", "WebHome", "edit", "template=&parent=Main.WebHome&title=X", null, "xwiki",
            context);
    }

    @Test
    public void newDocumentWebHomeFromURL() throws Exception
    {
        // new document = xwiki:X.Y.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("X", "Y"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(true);

        context.setDoc(document);

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note1: The bebavior is the same for both a top level space and a child space WebHome.
        // Note2: The title is not "WebHome", but "Y" (the space's name) to avoid exposing "WebHome" in the UI.
        verify(mockURLFactory).createURL("X.Y", "WebHome", "edit", "template=&parent=Main.WebHome&title=Y", null,
            "xwiki", context);
    }

    @Test
    public void newDocumentWebHomeButTerminalFromURL() throws Exception
    {
        // new document = xwiki:X.Y.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("X", "Y"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(true);

        context.setDoc(document);

        // Pass the tocreate=terminal request parameter
        when(mockRequest.getParameter("tocreate")).thenReturn("terminal");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating X.Y instead of X.Y.WebHome because the tocreate parameter says "terminal".
        verify(mockURLFactory).createURL("X", "Y", "edit", "template=&parent=Main.WebHome&title=Y", null, "xwiki",
            context);
    }

    @Test
    public void newDocumentWebHomeTopLevelSpaceButTerminalFromURL() throws Exception
    {
        // new document = xwiki:X.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("X"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(true);

        context.setDoc(document);

        // Pass the tocreate=terminal request parameter
        when(mockRequest.getParameter("tocreate")).thenReturn("terminal");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify that the create template is rendered, so the UI is displayed for the user to enter the missing values.
        assertEquals("create", result);

        // Note: We can not create the "X" terminal document, since it is already at the top level of the hierarchy and
        // none was able to be deducted from the given information. The user needs to specify more info in order to
        // continue.
        // We should not get this far so no redirect should be done, just the template will be rendered.
        verify(mockURLFactory, never()).createURL(anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), any(XWikiContext.class));
    }

    @Test
    public void existingDocumentFromUINoName() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Just landed on the create page or submitted with no values (no name) specified.

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify that the create template is rendered, so the UI is displayed for the user to enter the missing values.
        assertEquals("create", result);

        // We should not get this far so no redirect should be done, just the template will be rendered.
        verify(mockURLFactory, never()).createURL(anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), any(XWikiContext.class));
    }

    @Test
    public void existingDocumentFromUI() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI spaceReference=X&name=Y
        when(mockRequest.getParameter("spaceReference")).thenReturn("X");
        when(mockRequest.getParameter("name")).thenReturn("Y");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating X.Y.WebHome since we default to non-terminal documents.
        verify(mockURLFactory).createURL("X.Y", "WebHome", "edit", "template=&parent=Main.WebHome&title=Y", null,
            "xwiki", context);
    }

    @Test
    public void existingDocumentFromUICheckEscaping() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI spaceReference=X.Y&name=Z
        when(mockRequest.getParameter("spaceReference")).thenReturn("X.Y");
        when(mockRequest.getParameter("name")).thenReturn("Z");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating X.Y.Z.WebHome since we default to non-terminal documents.
        verify(mockURLFactory).createURL("X.Y.Z", "WebHome", "edit", "template=&parent=Main.WebHome&title=Z", null,
            "xwiki", context);
    }

    @Test
    public void existingDocumentTerminalFromUI() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI spaceReference=X&name=Y&tocreate=terminal
        when(mockRequest.getParameter("spaceReference")).thenReturn("X");
        when(mockRequest.getParameter("name")).thenReturn("Y");
        when(mockRequest.getParameter("tocreate")).thenReturn("terminal");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating X.Y instead of X.Y.WebHome because the tocreate parameter says "terminal".
        verify(mockURLFactory).createURL("X", "Y", "edit", "template=&parent=Main.WebHome&title=Y", null, "xwiki",
            context);
    }

    @Test
    public void existingDocumentTerminalFromUICheckEscaping() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI spaceReference=X.Y&name=Z&tocreate=termina
        when(mockRequest.getParameter("spaceReference")).thenReturn("X.Y");
        when(mockRequest.getParameter("name")).thenReturn("Z");
        when(mockRequest.getParameter("tocreate")).thenReturn("terminal");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating X.Y.Z instead of X.Y.Z.WebHome because the tocreate parameter says "terminal".
        verify(mockURLFactory).createURL("X.Y", "Z", "edit", "template=&parent=Main.WebHome&title=Z", null, "xwiki",
            context);
    }

    @Test
    public void existingDocumentTerminalFromUIButAlreadyExisting() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);
        // Mock it as existing in the DB as well with non-empty content
        oldcore.getDocuments().put(new DocumentReference(documentReference, Locale.ROOT), document);
        when(document.getContent()).thenReturn("Some non-empty content");

        // Submit from the UI spaceReference=X&name=Y&tocreate=terminal
        // No diference if it was a non-terminal document, just easier to mock since we already have Main.WebHome set
        // up.
        when(mockRequest.getParameter("spaceReference")).thenReturn("Main");
        when(mockRequest.getParameter("name")).thenReturn("WebHome");
        when(mockRequest.getParameter("tocreate")).thenReturn("terminal");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify that the create template is rendered, so the UI is displayed for the user to see the error.
        assertEquals("create", result);

        // Check that the exception is properly set in the context for the UI to display.
        XWikiException exception = (XWikiException) this.oldcore.getScriptContext().getAttribute("createException");
        assertNotNull(exception);
        assertEquals(XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY, exception.getCode());

        // We should not get this far so no redirect should be done, just the template will be rendered.
        verify(mockURLFactory, never()).createURL(anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), any(XWikiContext.class));
    }

    @Test
    public void existingDocumentFromUITopLevelDocument() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI name=Y
        when(mockRequest.getParameter("name")).thenReturn("Y");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating X.Y.WebHome since we default to non-terminal documents.
        verify(mockURLFactory).createURL("Y", "WebHome", "edit", "template=&parent=Main.WebHome&title=Y", null, "xwiki",
            context);
    }

    /*
     * Deprecated parameters
     */

    @Test
    public void existingDocumentFromUIDeprecated() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI space=X&page=Y
        when(mockRequest.getParameter("space")).thenReturn("X");
        when(mockRequest.getParameter("page")).thenReturn("Y");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating X.Y since the deprecated parameters were creating terminal documents by default.
        verify(mockURLFactory).createURL("X", "Y", "edit", "template=&parent=Main.WebHome&title=Y", null, "xwiki",
            context);
    }

    @Test
    public void existingDocumentFromUIDeprecatedCheckEscaping() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI space=X.Y&page=Z
        when(mockRequest.getParameter("space")).thenReturn("X.Y");
        when(mockRequest.getParameter("page")).thenReturn("Z");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note1: The space parameter was previously considered as space name, not space reference, so it is escaped.
        // Note2: We are creating X\.Y.Z since the deprecated parameters were creating terminal documents by default.
        verify(mockURLFactory).createURL("X\\.Y", "Z", "edit", "template=&parent=Main.WebHome&title=Z", null, "xwiki",
            context);
    }

    @Test
    public void existingDocumentNonTerminalFromUIDeprecated() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI space=X&tocreate=space
        when(mockRequest.getParameter("space")).thenReturn("X");
        when(mockRequest.getParameter("tocreate")).thenReturn("space");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating X.WebHome because the tocreate parameter says "space".
        verify(mockURLFactory).createURL("X", "WebHome", "edit", "template=&parent=Main.WebHome&title=X", null, "xwiki",
            context);
    }

    @Test
    public void existingDocumentNonTerminalFromUIDeprecatedIgnoringPage() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI space=X&page=Y&tocreate=space
        when(mockRequest.getParameter("space")).thenReturn("X");
        when(mockRequest.getParameter("page")).thenReturn("Y");
        when(mockRequest.getParameter("tocreate")).thenReturn("space");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating X.WebHome instead of X.Y because the tocreate parameter says "space" and the page
        // parameter is ignored.
        verify(mockURLFactory).createURL("X", "WebHome", "edit", "template=&parent=Main.WebHome&title=X", null, "xwiki",
            context);
    }

    @Test
    public void existingDocumentNonTerminalFromUIDeprecatedCheckEscaping() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI space=X.Y&tocreate=space
        when(mockRequest.getParameter("space")).thenReturn("X.Y");
        when(mockRequest.getParameter("tocreate")).thenReturn("space");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note1: The space parameter was previously considered as space name, not space reference, so it is escaped.
        // Note2: We are creating X\.Y.WebHome because the tocreate parameter says "space".
        verify(mockURLFactory).createURL("X\\.Y", "WebHome", "edit", "template=&parent=Main.WebHome&title=X.Y", null,
            "xwiki", context);
    }

    /*
     * Template providers
     */

    @Test
    public void existingDocumentFromUITemplateProviderExistingButNoneSelected() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI spaceReference=X&name=Y
        when(mockRequest.getParameter("spaceReference")).thenReturn("X");
        when(mockRequest.getParameter("name")).thenReturn("Y");

        // Mock 1 existing template provider
        mockExistingTemplateProviders("XWiki.MyTemplateProvider",
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Collections.EMPTY_LIST);

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify that the create template is rendered, so the UI is displayed for the user to enter the missing values.
        assertEquals("create", result);

        // We should not get this far so no redirect should be done, just the template will be rendered.
        verify(mockURLFactory, never()).createURL(anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), any(XWikiContext.class));
    }

    /**
     * Mocks 1 existing template provider.
     * <p>
     * Note: Calling it multiple times does not add multiple providers.
     */
    private void mockExistingTemplateProviders(String fullName, DocumentReference resolvedDocumentReference,
        List<String> allowedSpaces) throws Exception
    {
        mockExistingTemplateProviders(fullName, resolvedDocumentReference, allowedSpaces, false);
    }

    /**
     * Mocks 1 existing template provider.
     * <p>
     * Note: Calling it multiple times does not add multiple providers.
     */
    private void mockExistingTemplateProviders(String fullName, DocumentReference resolvedDocumentReference,
        List<String> allowedSpaces, Boolean terminal) throws Exception
    {
        mockExistingTemplateProviders(fullName, resolvedDocumentReference, allowedSpaces, terminal, null);
    }

    /**
     * Mocks 1 existing template provider.
     * <p>
     * Note: Calling it multiple times does not add multiple providers.
     */
    private void mockExistingTemplateProviders(String fullName, DocumentReference resolvedDocumentReference,
        List<String> allowedSpaces, Boolean terminal, String type) throws Exception
    {
        DocumentReference templateProviderClassReference =
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "TemplateProviderClass");

        // Mock to return at least 1 existing template provider
        when(mockTemplateProvidersQuery.execute()).thenReturn(new ArrayList<Object>(Arrays.asList(fullName)));

        // Mock the template document as existing.
        XWikiDocument templateProviderDocument = mock(XWikiDocument.class);
        when(templateProviderDocument.getDocumentReference()).thenReturn(resolvedDocumentReference);
        oldcore.getDocuments().put(new DocumentReference(resolvedDocumentReference, Locale.ROOT),
            templateProviderDocument);
        // Mock the provider object (template + spaces properties)
        BaseObject templateProviderObject = mock(BaseObject.class);
        when(templateProviderObject.getListValue("creationRestrictions")).thenReturn(allowedSpaces);
        String templateDocumentFullName = fullName.substring(0, fullName.indexOf("Provider"));
        when(templateProviderObject.getStringValue("template")).thenReturn(templateDocumentFullName);
        if (terminal != null) {
            when(templateProviderObject.getIntValue("terminal", -1)).thenReturn(terminal ? 1 : 0);
        } else {
            when(templateProviderObject.getIntValue("terminal", -1)).thenReturn(-1);
        }
        if (type != null) {
            when(templateProviderObject.getStringValue("type")).thenReturn(type);
        }
        when(templateProviderDocument.getXObject(templateProviderClassReference)).thenReturn(templateProviderObject);

        // Mock the template document as existing
        String templateDocumentName =
            resolvedDocumentReference.getName().substring(0, resolvedDocumentReference.getName().indexOf("Provider"));
        DocumentReference templateDocumentReference =
            new DocumentReference(templateDocumentName, new SpaceReference(resolvedDocumentReference.getParent()));
        mockTemplateDocumentExisting(templateDocumentFullName, templateDocumentReference);
    }

    /**
     * @param templateDocumentFullName
     * @param templateDocumentReference
     * @throws XWikiException
     */
    private void mockTemplateDocumentExisting(String templateDocumentFullName,
        DocumentReference templateDocumentReference) throws XWikiException
    {
        XWikiDocument templateDocument = mock(XWikiDocument.class);
        when(templateDocument.getDocumentReference()).thenReturn(templateDocumentReference);
        when(templateDocument.getDefaultEditMode(context)).thenReturn("edit");
        oldcore.getDocuments().put(new DocumentReference(templateDocumentReference, Locale.ROOT), templateDocument);
    }

    @Test
    public void existingDocumentFromUITemplateProviderSpecified() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI spaceReference=X&name=Y&templateProvider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        when(mockRequest.getParameter("spaceReference")).thenReturn("X");
        when(mockRequest.getParameter("name")).thenReturn("Y");
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);

        // Mock 1 existing template provider
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Collections.EMPTY_LIST);

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating X.Y and using the template extracted from the template provider.
        verify(mockURLFactory).createURL("X.Y", "WebHome", "edit",
            "template=XWiki.MyTemplate&parent=Main.WebHome&title=Y", null, "xwiki", context);
    }

    @Test
    public void existingDocumentFromUITemplateProviderSpecifiedRestrictionExists() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI spaceReference=X&name=Y&templateProvider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        String spaceReferenceString = "X";
        when(mockRequest.getParameter("spaceReference")).thenReturn(spaceReferenceString);
        when(mockRequest.getParameter("name")).thenReturn("Y");
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);

        // Mock 1 existing template provider that allows usage in target space.
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Arrays.asList("X"));

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note1: We are allowed to create anything under space X, be it a terminal or a non-terminal document.
        // Note2: We are creating X.Y and using the template extracted from the template provider.
        verify(mockURLFactory).createURL("X.Y", "WebHome", "edit",
            "template=XWiki.MyTemplate&parent=Main.WebHome&title=Y", null, "xwiki", context);
    }

    @Test
    public void existingDocumentFromUITemplateProviderSpecifiedRestrictionExistsOnParentSpace() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI spaceReference=X.Y.Z&name=W&templateProvider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        String spaceReferenceString = "X.Y.Z";
        when(mockRequest.getParameter("spaceReference")).thenReturn(spaceReferenceString);
        when(mockRequest.getParameter("name")).thenReturn("W");
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);

        // Mock 1 existing template provider that allows usage in one of the target space's parents (top level in this
        // case).
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Arrays.asList("X"));

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note1: We are allowed to create anything under space X or its children, be it a terminal or a non-terminal
        // document
        // Note2: We are creating X.Y.Z.W and using the template extracted from the template provider.
        verify(mockURLFactory).createURL("X.Y.Z.W", "WebHome", "edit",
            "template=XWiki.MyTemplate&parent=Main.WebHome&title=W", null, "xwiki", context);
    }

    @Test
    public void existingDocumentFromUITemplateProviderSpecifiedButNotAllowed() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI spaceReference=X&name=Y&templateProvider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        when(mockRequest.getParameter("spaceReference")).thenReturn("X");
        when(mockRequest.getParameter("name")).thenReturn("Y");
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);

        // Mock 1 existing template provider
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"),
            Arrays.asList("AnythingButX"));

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify that the create template is rendered, so the UI is displayed for the user to see the error.
        assertEquals("create", result);

        // Check that the exception is properly set in the context for the UI to display.
        XWikiException exception = (XWikiException) this.oldcore.getScriptContext().getAttribute("createException");
        assertNotNull(exception);
        assertEquals(XWikiException.ERROR_XWIKI_APP_TEMPLATE_NOT_AVAILABLE, exception.getCode());

        // We should not get this far so no redirect should be done, just the template will be rendered.
        verify(mockURLFactory, never()).createURL(anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), any(XWikiContext.class));
    }

    @Test
    public void newDocumentFromURLTemplateProviderSpecifiedButNotAllowed() throws Exception
    {
        // new document = xwiki:X.Y
        DocumentReference documentReference =
            new DocumentReference("Y", new SpaceReference("X", new WikiReference("xwiki")));
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(true);

        context.setDoc(document);

        // Specifying a template provider in the URL: templateprovider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);

        // Mock 1 existing template provider
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"),
            Arrays.asList("AnythingButX"));

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify that the create template is rendered, so the UI is displayed for the user to see the error.
        assertEquals("create", result);

        // Check that the exception is properly set in the context for the UI to display.
        XWikiException exception = (XWikiException) this.oldcore.getScriptContext().getAttribute("createException");
        assertNotNull(exception);
        assertEquals(XWikiException.ERROR_XWIKI_APP_TEMPLATE_NOT_AVAILABLE, exception.getCode());

        // We should not get this far so no redirect should be done, just the template will be rendered.
        verify(mockURLFactory, never()).createURL(anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), any(XWikiContext.class));
    }

    @Test
    public void newDocumentWebHomeFromURLTemplateProviderSpecifiedButNotAllowed() throws Exception
    {
        // new document = xwiki:X.Y.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("X", "Y"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(true);

        context.setDoc(document);

        // Specifying a template provider in the URL: templateprovider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);

        // Mock 1 existing template provider
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"),
            Arrays.asList("AnythingButX"));

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify that the create template is rendered, so the UI is displayed for the user to see the error.
        assertEquals("create", result);

        // Check that the exception is properly set in the context for the UI to display.
        XWikiException exception = (XWikiException) this.oldcore.getScriptContext().getAttribute("createException");
        assertNotNull(exception);
        assertEquals(XWikiException.ERROR_XWIKI_APP_TEMPLATE_NOT_AVAILABLE, exception.getCode());

        // We should not get this far so no redirect should be done, just the template will be rendered.
        verify(mockURLFactory, never()).createURL(anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), any(XWikiContext.class));
    }

    @Test
    public void newDocumentWebHomeFromURLTemplateProviderSpecifiedTerminal() throws Exception
    {
        // new document = xwiki:X.Y.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("X", "Y"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(true);

        context.setDoc(document);

        // Specifying a template provider in the URL: templateprovider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);

        // Mock 1 existing template provider
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Collections.EMPTY_LIST, true);

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating the document X.Y as terminal and using a template, as specified in the template
        // provider.
        verify(mockURLFactory).createURL("X", "Y", "edit", "template=XWiki.MyTemplate&parent=Main.WebHome&title=Y",
            null, "xwiki", context);
    }

    @Test
    public void newDocumentWebHomeFromURLTemplateProviderSpecifiedTerminalOverriddenFromUIToNonTerminal()
        throws Exception
    {
        // new document = xwiki:X.Y.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("X", "Y"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(true);

        context.setDoc(document);

        // Specifying a template provider in the URL: templateprovider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);
        when(mockRequest.getParameter("tocreate")).thenReturn("nonterminal");

        // Mock 1 existing template provider
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Collections.EMPTY_LIST, true);

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating the document X.Y.WebHome as non-terminal even if the template provider says otherwise.
        // Also using a template, as specified in the template provider.
        verify(mockURLFactory).createURL("X.Y", "WebHome", "edit",
            "template=XWiki.MyTemplate&parent=Main.WebHome&title=Y", null, "xwiki", context);
    }

    @Test
    public void newDocumentFromURLTemplateProviderSpecifiedNonTerminal() throws Exception
    {
        // new document = xwiki:X.Y
        DocumentReference documentReference = new DocumentReference("xwiki", "X", "Y");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(true);

        context.setDoc(document);

        // Specifying a template provider in the URL: templateprovider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);

        // Mock 1 existing template provider
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Collections.EMPTY_LIST,
            false);

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating the document X.Y as terminal and using a template, as specified in the template
        // provider.
        verify(mockURLFactory).createURL("X.Y", "WebHome", "edit",
            "template=XWiki.MyTemplate&parent=Main.WebHome&title=Y", null, "xwiki", context);
    }

    @Test
    public void newDocumentFromURLTemplateProviderSpecifiedNonTerminalButOverriddenFromUITerminal() throws Exception
    {
        // new document = xwiki:X.Y
        DocumentReference documentReference = new DocumentReference("xwiki", "X", "Y");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(true);

        context.setDoc(document);

        // Specifying a template provider in the URL: templateprovider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);
        when(mockRequest.getParameter("tocreate")).thenReturn("terminal");

        // Mock 1 existing template provider
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Collections.EMPTY_LIST,
            false);

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating the document X.Y as terminal and using a template, as specified in the template
        // provider.
        verify(mockURLFactory).createURL("X", "Y", "edit", "template=XWiki.MyTemplate&parent=Main.WebHome&title=Y",
            null, "xwiki", context);
    }

    @Test
    public void existingDocumentFromUITemplateSpecified() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);

        context.setDoc(document);

        // Submit from the UI spaceReference=X&name=Y&template=XWiki.MyTemplate
        String templateDocumentFullName = "XWiki.MyTemplate";
        DocumentReference templateDocumentReference =
            new DocumentReference("MyTemplate", Arrays.asList("XWiki"), "xwiki");
        when(mockRequest.getParameter("spaceReference")).thenReturn("X");
        when(mockRequest.getParameter("name")).thenReturn("Y");
        when(mockRequest.getParameter("template")).thenReturn("XWiki.MyTemplate");

        // Mock the passed template document as existing.
        mockTemplateDocumentExisting(templateDocumentFullName, templateDocumentReference);

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating X.Y.WebHome and using the template specified in the request.
        verify(mockURLFactory).createURL("X.Y", "WebHome", "edit",
            "template=XWiki.MyTemplate&parent=Main.WebHome&title=Y", null, "xwiki", context);
    }

    @Test
    public void existingDocumentFromUITemplateProviderSpecifiedTerminal() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI spaceReference=X&name=Y&templateProvider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        String spaceReferenceString = "X";
        when(mockRequest.getParameter("spaceReference")).thenReturn(spaceReferenceString);
        when(mockRequest.getParameter("name")).thenReturn("Y");
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);

        // Mock 1 existing template provider that creates terminal documents.
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Collections.EMPTY_LIST, true);

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating the document X.Y as terminal and using a template, as specified in the template
        // provider.
        verify(mockURLFactory).createURL("X", "Y", "edit", "template=XWiki.MyTemplate&parent=Main.WebHome&title=Y",
            null, "xwiki", context);
    }

    @Test
    public void existingDocumentFromUITemplateProviderSpecifiedTerminalOverridenFromUIToNonTerminal() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI spaceReference=X&name=Y&templateProvider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        String spaceReferenceString = "X";
        when(mockRequest.getParameter("spaceReference")).thenReturn(spaceReferenceString);
        when(mockRequest.getParameter("name")).thenReturn("Y");
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);
        when(mockRequest.getParameter("tocreate")).thenReturn("nonterminal");

        // Mock 1 existing template provider that creates terminal documents.
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Collections.EMPTY_LIST, true);

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating the document X.Y.WebHome as non-terminal, even if the template provider says otherwise.
        // Also using a template, as specified in the template provider.
        verify(mockURLFactory).createURL("X.Y", "WebHome", "edit",
            "template=XWiki.MyTemplate&parent=Main.WebHome&title=Y", null, "xwiki", context);
    }

    @Test
    public void existingDocumentFromUITemplateProviderSpecifiedNonTerminal() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI spaceReference=X&name=Y&templateProvider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        String spaceReferenceString = "X";
        when(mockRequest.getParameter("spaceReference")).thenReturn(spaceReferenceString);
        when(mockRequest.getParameter("name")).thenReturn("Y");
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);

        // Mock 1 existing template provider that creates terminal documents.
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Collections.EMPTY_LIST,
            false);

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating the document X.Y.WebHome as non-terminal and using a template, as specified in the
        // template provider.
        verify(mockURLFactory).createURL("X.Y", "WebHome", "edit",
            "template=XWiki.MyTemplate&parent=Main.WebHome&title=Y", null, "xwiki", context);
    }

    @Test
    public void existingDocumentFromUITemplateProviderSpecifiedNonTerminalOverridenFromUIToTerminal() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI spaceReference=X&name=Y&templateProvider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        String spaceReferenceString = "X";
        when(mockRequest.getParameter("spaceReference")).thenReturn(spaceReferenceString);
        when(mockRequest.getParameter("name")).thenReturn("Y");
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);
        when(mockRequest.getParameter("tocreate")).thenReturn("terminal");

        // Mock 1 existing template provider that creates terminal documents.
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Collections.EMPTY_LIST,
            false);

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating the document X.Y as terminal, even if the template provider says otherwise.
        // Also using a template, as specified in the template provider.
        verify(mockURLFactory).createURL("X", "Y", "edit", "template=XWiki.MyTemplate&parent=Main.WebHome&title=Y",
            null, "xwiki", context);
    }

    @Test
    public void newDocumentWebHomeFromURLTemplateProviderSpecifiedButOldPageType() throws Exception
    {
        // new document = xwiki:X.Y.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("X", "Y"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(true);

        context.setDoc(document);

        // Specifying a template provider in the URL: templateprovider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);

        // Mock 1 existing template provider
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Collections.EMPTY_LIST, null,
            "page");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating the document X.Y as terminal, since the template provider did not specify a "terminal"
        // property and it used the old "page" type instead. Also using a template, as specified in the template
        // provider.
        verify(mockURLFactory).createURL("X", "Y", "edit", "template=XWiki.MyTemplate&parent=Main.WebHome&title=Y",
            null, "xwiki", context);
    }

    @Test
    public void newDocumentWebHomeFromURLTemplateProviderSpecifiedButOldPageTypeButOverriddenFromUIToNonTerminal()
        throws Exception
    {
        // new document = xwiki:X.Y.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("X", "Y"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(true);

        context.setDoc(document);

        // Specifying a template provider in the URL: templateprovider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);
        when(mockRequest.getParameter("tocreate")).thenReturn("nonterminal");

        // Mock 1 existing template provider
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Collections.EMPTY_LIST, null,
            "page");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating the document X.Y.WebHome as non-terminal, since even if the template provider did not
        // specify a "terminal" property and it used the old "page" type, the UI explicitly asked for a non-terminal
        // document. Also using a template, as specified in the template provider.
        verify(mockURLFactory).createURL("X.Y", "WebHome", "edit",
            "template=XWiki.MyTemplate&parent=Main.WebHome&title=Y", null, "xwiki", context);
    }

    @Test
    public void existingDocumentFromUITemplateProviderSpecifiedButOldSpaceType() throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI spaceReference=X&name=Y&templateProvider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        when(mockRequest.getParameter("spaceReference")).thenReturn("X");
        when(mockRequest.getParameter("name")).thenReturn("Y");
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);

        // Mock 1 existing template provider
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Collections.EMPTY_LIST, null,
            "space");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating X.Y.WebHome as non-terminal, since the template provider does not specify a "terminal"
        // property and we fallback on the "type" property's value. Also using the template extracted from the template
        // provider.
        verify(mockURLFactory).createURL("X.Y", "WebHome", "edit",
            "template=XWiki.MyTemplate&parent=Main.WebHome&title=Y", null, "xwiki", context);
    }

    @Test
    public void existingDocumentFromUITemplateProviderSpecifiedButOldSpaceTypeButOverridenFromUIToTerminal()
        throws Exception
    {
        // current document = xwiki:Main.WebHome
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Main"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.isNew()).thenReturn(false);
        context.setDoc(document);

        // Submit from the UI spaceReference=X&name=Y&templateProvider=XWiki.MyTemplateProvider
        String templateProviderFullName = "XWiki.MyTemplateProvider";
        when(mockRequest.getParameter("spaceReference")).thenReturn("X");
        when(mockRequest.getParameter("name")).thenReturn("Y");
        when(mockRequest.getParameter("templateprovider")).thenReturn(templateProviderFullName);
        when(mockRequest.getParameter("tocreate")).thenReturn("terminal");

        // Mock 1 existing template provider
        mockExistingTemplateProviders(templateProviderFullName,
            new DocumentReference("xwiki", Arrays.asList("XWiki"), "MyTemplateProvider"), Collections.EMPTY_LIST, null,
            "space");

        // Run the action
        String result = action.render(context);

        // The tests are below this line!

        // Verify null is returned (this means the response has been returned)
        assertNull(result);

        // Note: We are creating X.Y as terminal, since it is overriden from the UI, regardless of any backwards
        // compatibility resolutions. Also using the template extracted from the template provider.
        verify(mockURLFactory).createURL("X", "Y", "edit", "template=XWiki.MyTemplate&parent=Main.WebHome&title=Y",
            null, "xwiki", context);
    }
}
