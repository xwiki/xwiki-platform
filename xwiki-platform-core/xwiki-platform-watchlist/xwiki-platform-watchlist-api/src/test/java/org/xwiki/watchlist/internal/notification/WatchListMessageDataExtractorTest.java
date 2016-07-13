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
package org.xwiki.watchlist.internal.notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentManagerRule;
import org.xwiki.watchlist.internal.WatchListEventMatcher;
import org.xwiki.watchlist.internal.api.WatchListEvent;
import org.xwiki.watchlist.internal.job.WatchListJob;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WatchListMessageDataExtractor}.
 *
 * @version $Id$
 * @since 7.1M1
 */
public class WatchListMessageDataExtractorTest
{
    @Rule
    public final MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    List<WatchListEvent> events;

    List<DocumentReference> subscribers;

    Map<String, Object> parameters;

    Map<String, Object> factoryParameters;

    WatchListEventMatcher mockEventMatcher;

    Execution mockExecution;

    DocumentReferenceResolver<String> mockExplicitDocumentReferenceResolver;

    WatchListMessageDataExtractor extractor;

    XWikiDocument mockDocument;

    BaseObject mockUserObject;

    XWiki mockWiki;

    XWikiContext mockContext;

    ExecutionContext mockExecutionContext;

    DocumentReference testSubscriberReference;

    String testSubscriberStringReference;

    List<WatchListEvent> testMatchingEvents;

    String testFirstName;

    String testLastName;

    String testEmail;

    DocumentReference testTemplateReference;

    @Before
    public void setup() throws Exception
    {
        events = new ArrayList<>();
        subscribers = new ArrayList<>();
        EventsAndSubscribersSource source = new EventsAndSubscribersSource(events, subscribers);

        parameters = new HashMap<>();
        factoryParameters = new HashMap<>();
        parameters.put(WatchListEventMimeMessageFactory.PARAMETERS_PARAMETER, factoryParameters);

        mockEventMatcher = mocker.registerMockComponent(WatchListEventMatcher.class);

        mockExecution = mocker.registerMockComponent(Execution.class);

        mockExplicitDocumentReferenceResolver =
            mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "explicit");

        extractor =
            new WatchListMessageDataExtractor(source, parameters, mockEventMatcher, mockExecution,
                mockExplicitDocumentReferenceResolver);

        // Document and object
        mockDocument = mock(XWikiDocument.class);
        mockUserObject = mock(BaseObject.class);

        // Context and wiki
        mockWiki = mock(XWiki.class);

        mockContext = mock(XWikiContext.class);
        when(mockContext.getWiki()).thenReturn(mockWiki);

        mockExecutionContext = mocker.registerMockComponent(ExecutionContext.class);
        when(mockExecution.getContext()).thenReturn(mockExecutionContext);
        when(mockExecutionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY)).thenReturn(mockContext);

        // Subscriber reference value
        testSubscriberReference = new DocumentReference("wiki", "XWiki", "User");

        testSubscriberStringReference = "wiki:XWiki.User";
        when(mockDocument.getPrefixedFullName()).thenReturn(testSubscriberStringReference);

        // Matched events
        testMatchingEvents = new ArrayList<>();
        WatchListEvent matchingEvent = mock(WatchListEvent.class);
        testMatchingEvents.add(matchingEvent);

        when(mockEventMatcher.getMatchingVisibleEvents(events, testSubscriberStringReference)).thenReturn(
            testMatchingEvents);

        // User object field values
        testFirstName = "U";
        testLastName = "ser";
        testEmail = "e@ma.il";

    }

    @Test
    public void noMatchingEvents() throws Exception
    {
        // Setup the matched events.
        testMatchingEvents.clear();

        // Run the extraction.
        WatchListMessageData result = extractor.extract(testSubscriberReference, mockDocument, mockUserObject);

        // Check the extracted data.
        assertNull(result);
    }

    @Test
    public void noEmailSet() throws Exception
    {
        // Setup the user profile data to extract.
        testEmail = null;
        setUpUserObject();

        // Run the extraction.
        WatchListMessageData result = extractor.extract(testSubscriberReference, mockDocument, mockUserObject);

        // Check the extracted data.
        assertNull(result);
    }

    @Test
    public void invalidEmailSet() throws Exception
    {
        // Setup the user profile data to extract.
        // TODO: Maybe we should pass the AddressUserDataExtractor as parameter and not end up testing its
        // functionality but mock it instead.
        testEmail = "@#$%^&*(";
        setUpUserObject();

        // Run the extraction.
        WatchListMessageData result = extractor.extract(testSubscriberReference, mockDocument, mockUserObject);

        // Check the extracted data.
        assertNull(result);
    }

    @Test
    public void duplicateEmail() throws Exception
    {
        // Setup the user profile data to extract.
        setUpUserObject();

        // Setup the template
        String templateStringReference = "xwiki:Some.Template";
        parameters.put(WatchListEventMimeMessageFactory.TEMPLATE_PARAMETER, templateStringReference);

        DocumentReference templateReference = new DocumentReference("xwiki", "Some", "Template");
        when(mockExplicitDocumentReferenceResolver.resolve(templateStringReference)).thenReturn(templateReference);

        testTemplateReference = templateReference;

        // Run the extraction.
        WatchListMessageData result1 = extractor.extract(testSubscriberReference, mockDocument, mockUserObject);
        WatchListMessageData result2 = extractor.extract(testSubscriberReference, mockDocument, mockUserObject);

        // Check the extracted data.
        assertResult(result1);
        assertNull(result2);
    }

    @Test
    public void absoluteTemplate() throws Exception
    {
        // Setup the user profile data to extract.
        setUpUserObject();

        // Setup the template
        String templateStringReference = "xwiki:Some.Template";
        parameters.put(WatchListEventMimeMessageFactory.TEMPLATE_PARAMETER, templateStringReference);

        DocumentReference templateReference = new DocumentReference("xwiki", "Some", "Template");
        when(mockExplicitDocumentReferenceResolver.resolve(templateStringReference)).thenReturn(templateReference);

        testTemplateReference = templateReference;

        // Run the extraction.
        WatchListMessageData result = extractor.extract(testSubscriberReference, mockDocument, mockUserObject);

        // Check the extracted data.
        assertResult(result);
    }

    @Test
    public void relativeTemplateUserWiki() throws Exception
    {
        // Setup the user profile data to extract.
        setUpUserObject();

        // Setup the template
        String templateStringReference = "Some.Template";
        parameters.put(WatchListEventMimeMessageFactory.TEMPLATE_PARAMETER, templateStringReference);

        DocumentReference userWikiTemplateReference = new DocumentReference("wiki", "Some", "Template");
        when(
            mockExplicitDocumentReferenceResolver.resolve(templateStringReference,
                testSubscriberReference.getWikiReference())).thenReturn(userWikiTemplateReference);

        when(mockWiki.exists(userWikiTemplateReference, mockContext)).thenReturn(true);

        testTemplateReference = userWikiTemplateReference;

        // Run the extraction.
        WatchListMessageData result = extractor.extract(testSubscriberReference, mockDocument, mockUserObject);

        // Check the extracted data.
        assertResult(result);
    }

    @Test
    public void relativeTemplateContextWiki() throws Exception
    {
        // Setup the user profile data to extract.
        setUpUserObject();

        // Setup the template
        String templateStringReference = "Some.Template";
        parameters.put(WatchListEventMimeMessageFactory.TEMPLATE_PARAMETER, templateStringReference);

        DocumentReference userWikiTemplateReference = new DocumentReference("wiki", "Some", "Template");
        when(
            mockExplicitDocumentReferenceResolver.resolve(templateStringReference,
                testSubscriberReference.getWikiReference())).thenReturn(userWikiTemplateReference);

        when(mockWiki.exists(userWikiTemplateReference, mockContext)).thenReturn(false);

        String currentWikiId = "xwiki";
        when(mockContext.getWikiId()).thenReturn(currentWikiId);
        WikiReference currentWikiReference = new WikiReference(currentWikiId);

        DocumentReference currentWikiTemplateReference = new DocumentReference(currentWikiId, "Some", "Template");
        when(mockExplicitDocumentReferenceResolver.resolve(templateStringReference, currentWikiReference)).thenReturn(
            currentWikiTemplateReference);

        when(mockWiki.exists(currentWikiTemplateReference, mockContext)).thenReturn(true);

        testTemplateReference = currentWikiTemplateReference;

        // Run the extraction.
        WatchListMessageData result = extractor.extract(testSubscriberReference, mockDocument, mockUserObject);

        // Check the extracted data.
        assertResult(result);
    }

    @Test
    public void relativeTemplateDefaultTemplateContextWiki() throws Exception
    {
        // Setup the user profile data to extract.
        setUpUserObject();

        // Setup the template
        String templateStringReference = "Some.Template";
        parameters.put(WatchListEventMimeMessageFactory.TEMPLATE_PARAMETER, templateStringReference);

        DocumentReference userWikiTemplateReference = new DocumentReference("wiki", "Some", "Template");
        when(
            mockExplicitDocumentReferenceResolver.resolve(templateStringReference,
                testSubscriberReference.getWikiReference())).thenReturn(userWikiTemplateReference);

        when(mockWiki.exists(userWikiTemplateReference, mockContext)).thenReturn(false);

        String currentWikiId = "xwiki";
        when(mockContext.getWikiId()).thenReturn(currentWikiId);
        WikiReference currentWikiReference = new WikiReference(currentWikiId);

        DocumentReference currentWikiTemplateReference = new DocumentReference(currentWikiId, "Some", "Template");
        when(mockExplicitDocumentReferenceResolver.resolve(templateStringReference, currentWikiReference)).thenReturn(
            currentWikiTemplateReference);

        when(mockWiki.exists(currentWikiTemplateReference, mockContext)).thenReturn(false);

        DocumentReference defaultCurrentWikiTemplateReference =
            new DocumentReference(currentWikiId, "Some", "Template");
        when(mockExplicitDocumentReferenceResolver.resolve(WatchListJob.DEFAULT_EMAIL_TEMPLATE, currentWikiReference))
            .thenReturn(defaultCurrentWikiTemplateReference);

        when(mockWiki.exists(defaultCurrentWikiTemplateReference, mockContext)).thenReturn(true);

        testTemplateReference = defaultCurrentWikiTemplateReference;

        // Run the extraction.
        WatchListMessageData result = extractor.extract(testSubscriberReference, mockDocument, mockUserObject);

        // Check the extracted data.
        assertResult(result);
    }

    @Test
    public void relativeTemplateDefaultTemplateMainWiki() throws Exception
    {
        // Setup the user profile data to extract.
        setUpUserObject();

        // Setup the template
        String templateStringReference = "Some.Template";
        parameters.put(WatchListEventMimeMessageFactory.TEMPLATE_PARAMETER, templateStringReference);

        DocumentReference userWikiTemplateReference =
            new DocumentReference(testSubscriberReference.getWikiReference().getName(), "Some", "Template");
        when(
            mockExplicitDocumentReferenceResolver.resolve(templateStringReference,
                testSubscriberReference.getWikiReference())).thenReturn(userWikiTemplateReference);

        when(mockWiki.exists(userWikiTemplateReference, mockContext)).thenReturn(false);

        String currentWikiId = "someWiki";
        when(mockContext.getWikiId()).thenReturn(currentWikiId);
        WikiReference currentWikiReference = new WikiReference(currentWikiId);

        DocumentReference currentWikiTemplateReference = new DocumentReference(currentWikiId, "Some", "Template");
        when(mockExplicitDocumentReferenceResolver.resolve(templateStringReference, currentWikiReference)).thenReturn(
            currentWikiTemplateReference);

        when(mockWiki.exists(currentWikiTemplateReference, mockContext)).thenReturn(false);

        DocumentReference defaultCurrentWikiTemplateReference =
            new DocumentReference(currentWikiId, "XWiki", "WatchListMessage");
        when(mockExplicitDocumentReferenceResolver.resolve(WatchListJob.DEFAULT_EMAIL_TEMPLATE, currentWikiReference))
            .thenReturn(defaultCurrentWikiTemplateReference);

        when(mockWiki.exists(defaultCurrentWikiTemplateReference, mockContext)).thenReturn(false);

        String mainWikiId = "xwiki";
        when(mockContext.getMainXWiki()).thenReturn(mainWikiId);
        WikiReference mainWikiReference = new WikiReference(mainWikiId);

        DocumentReference defaultMainWikiTemplateReference =
            new DocumentReference(mainWikiId, "XWiki", "WatchListMessage");
        when(mockExplicitDocumentReferenceResolver.resolve(WatchListJob.DEFAULT_EMAIL_TEMPLATE, mainWikiReference))
            .thenReturn(defaultMainWikiTemplateReference);

        testTemplateReference = defaultMainWikiTemplateReference;

        // Run the extraction.
        WatchListMessageData result = extractor.extract(testSubscriberReference, mockDocument, mockUserObject);

        // Check the extracted data.
        assertResult(result);
    }

    @Test
    public void skipContextUser() throws Exception
    {
        // Set the skipContextUser parameter and reinitialize the extractor.
        parameters.put(WatchListEventMimeMessageFactory.SKIP_CONTEXT_USER_PARAMETER, true);

        // Reinitialize the extractor to consider the new parameter.
        EventsAndSubscribersSource source = new EventsAndSubscribersSource(events, subscribers);
        extractor =
            new WatchListMessageDataExtractor(source, parameters, mockEventMatcher, mockExecution,
                mockExplicitDocumentReferenceResolver);

        // Setup the current context user.
        when(mockContext.getUserReference()).thenReturn(testSubscriberReference);

        // Run the extraction.
        WatchListMessageData result = extractor.extract(testSubscriberReference, mockDocument, mockUserObject);

        // Check the extracted data.
        assertNull(result);
    }

    /*
     * Helper methods
     */

    private void setUpUserObject()
    {
        when(mockUserObject.getStringValue("first_name")).thenReturn(testFirstName);
        when(mockUserObject.getStringValue("last_name")).thenReturn(testLastName);
        when(mockUserObject.getStringValue("email")).thenReturn(testEmail);
    }

    private void assertResult(WatchListMessageData result) throws Exception
    {
        assertEquals(testSubscriberReference, result.getUserReference());
        // We don`t care about validity exception since it should not get this far because the result will be null.
        Address address = InternetAddress.parse(testEmail)[0];
        assertEquals(address, result.getAddress());
        assertEquals(testFirstName, result.getFirstName());
        assertEquals(testLastName, result.getLastName());
        assertEquals(testMatchingEvents, result.getEvents());
        assertEquals(testTemplateReference, result.getTemplateReference());
    }
}
