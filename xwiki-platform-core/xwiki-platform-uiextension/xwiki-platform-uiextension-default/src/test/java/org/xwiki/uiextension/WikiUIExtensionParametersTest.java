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
package org.xwiki.uiextension;

import java.io.Reader;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.logging.LoggerConfiguration;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.uiextension.internal.WikiUIExtensionConstants;
import org.xwiki.uiextension.internal.WikiUIExtensionParameters;
import org.xwiki.uiextension.internal.parameters.UIXParameterValueFactory;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.VelocityTemplate;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WikiUIExtensionParameters}.
 *
 * @version $Id$
 * @since 5.0M1
 */
@ComponentTest
@ComponentList({ UIXParameterValueFactory.class, VelocityDetector.class })
class WikiUIExtensionParametersTest
{
    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "XWikiAdmin");

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "UIX");

    private static final String VELOCITY_VALUE = "$value";

    private static final String LOCALIZATION_VALUE = "$services.localization.render('my.key')";

    private static final String TRANSLATION = "My translation";

    private static final String KEY = "key";

    private static final String NAMESPACE = "id:key";

    @Mock
    private VelocityEngine velocityEngine;

    @MockComponent
    private ModelContext modelContext;

    @MockComponent
    private Execution execution;

    @MockComponent
    private VelocityManager velocityManager;

    @MockComponent
    private LoggerConfiguration loggerConfiguration;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    private DocumentAuthorizationManager authorizationManager;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @BeforeEach
    void setUp() throws Exception
    {
        VelocityContext velocityContext = new VelocityContext();
        ExecutionContext executionContext = mock(ExecutionContext.class);

        when(this.execution.getContext()).thenReturn(executionContext);
        when(this.velocityManager.getVelocityContext()).thenReturn(velocityContext);
        when(this.velocityManager.getVelocityEngine()).thenReturn(this.velocityEngine);
        when(this.velocityManager.compile(anyString(), any(Reader.class)))
            .thenAnswer(invocation -> mock(VelocityTemplate.class));
        // Output the namespace of the evaluated template to identify the value.
        doAnswer(invocation -> {
            invocation.<StringWriter>getArgument(1).write("evaluated " + invocation.getArgument(2));
            return null;
        }).when(this.velocityEngine).evaluate(any(), any(), anyString(), any(VelocityTemplate.class));

        when(this.authorExecutor.call(any(), any(), any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            return callable.call();
        });

        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, AUTHOR_REFERENCE,
            DOCUMENT_REFERENCE)).thenReturn(true);

        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));

        when(this.localizationManager.getTranslationPlain("my.key")).thenReturn(TRANSLATION);
    }

    @Test
    void getParametersWithAnEmptyParametersProperty() throws Exception
    {
        WikiUIExtensionParameters parameters = createParameters("");

        assertEquals(Map.of(), parameters.get());
    }

    @Test
    void getParametersWithAnEqualSignInAValue() throws Exception
    {
        WikiUIExtensionParameters parameters = createParameters("key=value=foo");

        assertEquals("value=foo", parameters.get().get(KEY));
    }

    @Test
    void getParametersWithCommentAloneOnLine() throws Exception
    {
        String paramsStr = """
            # a = 1
            x=1
            y=2
            # ...
            z=3""";
        WikiUIExtensionParameters parameters = createParameters(paramsStr);

        assertEquals(Map.of("x", "1", "y", "2", "z", "3"), parameters.get());
    }

    @Test
    void getParametersWithCommentEndOfLine() throws Exception
    {
        String paramsStr = """
            x=1##b
            y=2####x
            z=3 ## xyz
            """;
        WikiUIExtensionParameters parameters = createParameters(paramsStr);

        assertEquals(Map.of("x", "evaluated id:x", "y", "evaluated id:y", "z", "evaluated id:z"), parameters.get());
        verify(this.velocityManager).compile(eq("id:x"), any(Reader.class));
        verify(this.velocityManager).compile(eq("id:y"), any(Reader.class));
        verify(this.velocityManager).compile(eq("id:z"), any(Reader.class));
    }

    @Test
    void getParametersWithoutVelocity() throws Exception
    {
        WikiUIExtensionParameters parameters = createParameters("key=value\nother=other value");

        Map<String, String> result = parameters.get();
        assertEquals(Map.of(KEY, "value", "other", "other value"), result);
        assertSame(result, parameters.get());

        // Nothing is evaluated, so there is no need to check rights, to set up Velocity or to cache anything.
        verifyNoInteractions(this.authorizationManager, this.authorExecutor, this.velocityManager, this.execution,
            this.modelContext);
    }

    @Test
    void getParametersWithShortcutOnly() throws Exception
    {
        WikiUIExtensionParameters parameters = createParameters("key=" + LOCALIZATION_VALUE + "\nother=value");

        assertEquals(Map.of(KEY, TRANSLATION, "other", "value"), parameters.get());

        verify(this.authorizationManager).hasAccess(Right.SCRIPT, EntityType.DOCUMENT, AUTHOR_REFERENCE,
            DOCUMENT_REFERENCE);
        verifyNoInteractions(this.authorExecutor, this.velocityManager);
    }

    @Test
    void getParametersWithShortcutAndVelocity() throws Exception
    {
        WikiUIExtensionParameters parameters =
            createParameters("key=" + VELOCITY_VALUE + "\nlabel=" + LOCALIZATION_VALUE + "\nother=value");

        assertEquals(Map.of(KEY, "evaluated id:key", "label", TRANSLATION, "other", "value"), parameters.get());

        verify(this.authorExecutor).call(any(), eq(AUTHOR_REFERENCE), eq(DOCUMENT_REFERENCE));
        verify(this.velocityManager).getVelocityContext();
        verify(this.velocityManager).compile(eq(NAMESPACE), any(Reader.class));
        verify(this.velocityManager).getVelocityEngine();
    }

    @Test
    void getParametersReturnsUnmodifiableMap() throws Exception
    {
        Map<String, String> staticParameters = createParameters("key=value").get();
        Map<String, String> evaluatedParameters = createParameters("key=" + VELOCITY_VALUE).get();

        assertThrows(UnsupportedOperationException.class, () -> staticParameters.put(KEY, "other"));
        assertThrows(UnsupportedOperationException.class, () -> evaluatedParameters.put(KEY, "other"));
    }

    @Test
    void getParametersWhenVelocityFails() throws Exception
    {
        doThrow(new XWikiVelocityException("evaluation failure")).when(this.velocityEngine)
            .evaluate(any(VelocityContext.class), any(StringWriter.class), eq(NAMESPACE), any(VelocityTemplate.class));
        WikiUIExtensionParameters parameters = createParameters("key=" + VELOCITY_VALUE);

        // It put a warning in the logs and return the not-evaluated value.
        assertEquals(VELOCITY_VALUE, parameters.get().get(KEY));
        assertEquals("Failed to evaluate UI extension data value, key [key], value [$value]. Reason: "
            + "[XWikiVelocityException: evaluation failure]", this.logCapture.getMessage(0));
    }

    @Test
    void getParametersWhenVelocityCompilationFails() throws Exception
    {
        when(this.velocityManager.compile(eq(NAMESPACE), any(Reader.class)))
            .thenThrow(new XWikiVelocityException("compilation failure"));
        ExecutionContext ec1 = mock(ExecutionContext.class, "ec1");
        ExecutionContext ec2 = mock(ExecutionContext.class, "ec2");
        when(this.execution.getContext()).thenReturn(ec1, ec2);
        WikiUIExtensionParameters parameters = createParameters("key=" + VELOCITY_VALUE);

        assertEquals(VELOCITY_VALUE, parameters.get().get(KEY));
        assertEquals(VELOCITY_VALUE, parameters.get().get(KEY));

        assertEquals("Failed to evaluate UI extension data value, key [key], value [$value]. Reason: "
            + "[XWikiVelocityException: compilation failure]", this.logCapture.getMessage(0));
        assertEquals("Failed to evaluate UI extension data value, key [key], value [$value]. Reason: "
            + "[XWikiVelocityException: compilation failure]", this.logCapture.getMessage(1));
        // The value isn't compiled again for the second request.
        verify(this.velocityManager).compile(eq(NAMESPACE), any(Reader.class));
        verifyNoInteractions(this.velocityEngine);
    }

    @Test
    void getParametersWhenVelocityEngineFails() throws Exception
    {
        when(this.velocityManager.getVelocityEngine()).thenThrow(new XWikiVelocityException("engine failure"));
        WikiUIExtensionParameters parameters = createParameters("key=" + VELOCITY_VALUE + "\nlabel=" + LOCALIZATION_VALUE);

        assertEquals(Map.of(KEY, VELOCITY_VALUE, "label", LOCALIZATION_VALUE), parameters.get());
        assertEquals("Failed to get velocity engine. Reason: [XWikiVelocityException: engine failure]",
            this.logCapture.getMessage(0));
    }

    @Test
    void getParametersFromTheSameRequestAndForTheSameWiki() throws Exception
    {
        WikiUIExtensionParameters parameters = createParameters("key=" + VELOCITY_VALUE);

        assertEquals("evaluated id:key", parameters.get().get(KEY));
        assertEquals("evaluated id:key", parameters.get().get(KEY));

        // Verify the evaluate is done only once
        verify(this.velocityEngine).evaluate(any(), any(), eq(NAMESPACE), any(VelocityTemplate.class));
    }

    @Test
    void getParametersFromTheSameRequestButForDifferentWikis() throws Exception
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("wiki1"))
            .thenReturn(new WikiReference("wiki2"));
        WikiUIExtensionParameters parameters = createParameters("key=" + VELOCITY_VALUE);

        assertEquals("evaluated id:key", parameters.get().get(KEY));
        assertEquals("evaluated id:key", parameters.get().get(KEY));

        // Verify the velocity evaluation has been done for both wikis, but the value compiled only once.
        verify(this.velocityEngine, times(2)).evaluate(any(), any(), eq(NAMESPACE), any(VelocityTemplate.class));
        verify(this.velocityManager).compile(eq(NAMESPACE), any(Reader.class));
    }

    @Test
    void getParametersWithoutCurrentWiki() throws Exception
    {
        // No current wiki, e.g. when no XWiki context is initialized for the current thread, then the second call is
        // made once a wiki has been set.
        when(this.modelContext.getCurrentEntityReference()).thenReturn(null, null, new WikiReference("wiki"));
        WikiUIExtensionParameters parameters = createParameters("key=" + VELOCITY_VALUE);

        assertEquals("evaluated id:key", parameters.get().get(KEY));
        assertEquals("evaluated id:key", parameters.get().get(KEY));
        assertEquals("evaluated id:key", parameters.get().get(KEY));

        // Evaluated once without a wiki (and cached for the second call), then again once a wiki is set.
        verify(this.velocityEngine, times(2)).evaluate(any(), any(), eq(NAMESPACE), any(VelocityTemplate.class));
    }

    @Test
    void getParametersFromDifferentRequests() throws Exception
    {
        WikiUIExtensionParameters parameters = createParameters("key=" + VELOCITY_VALUE);

        ExecutionContext ec1 = mock(ExecutionContext.class, "ec1");
        ExecutionContext ec2 = mock(ExecutionContext.class, "ec2");
        when(this.execution.getContext()).thenReturn(ec1, ec2, ec1);

        assertEquals("evaluated id:key", parameters.get().get(KEY));
        assertEquals("evaluated id:key", parameters.get().get(KEY));
        // Only the parameters of the last request are cached.
        assertEquals("evaluated id:key", parameters.get().get(KEY));

        // Verify the velocity evaluation has been done for each request change, but the value compiled only once.
        verify(this.velocityEngine, times(3)).evaluate(any(), any(), eq(NAMESPACE), any(VelocityTemplate.class));
        verify(this.velocityManager).compile(eq(NAMESPACE), any(Reader.class));
    }

    @Test
    void getParametersWithoutScriptRight() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, AUTHOR_REFERENCE,
            DOCUMENT_REFERENCE)).thenReturn(false);
        WikiUIExtensionParameters parameters =
            createParameters("key=" + VELOCITY_VALUE + "\nlabel=" + LOCALIZATION_VALUE + "\nother=value");

        // Without script right, not even the calls that are executed without Velocity are executed.
        assertEquals(Map.of(KEY, VELOCITY_VALUE, "label", LOCALIZATION_VALUE, "other", "value"), parameters.get());
        verifyNoInteractions(this.velocityEngine, this.velocityManager, this.localizationManager,
            this.authorExecutor);
    }

    @Test
    void getParametersWithAuthorExecutor() throws Exception
    {
        // Do not call the callable to check that the call to the Velocity engine is inside the author executor.
        doReturn(null).when(this.authorExecutor).call(any(), any(), any());

        WikiUIExtensionParameters parameters = createParameters("key=" + VELOCITY_VALUE);

        assertEquals(VELOCITY_VALUE, parameters.get().get(KEY));
        verify(this.authorExecutor).call(any(), eq(AUTHOR_REFERENCE), eq(DOCUMENT_REFERENCE));
        verifyNoInteractions(this.velocityEngine);
    }

    @Test
    void getParametersWithEmptyKey() throws Exception
    {
        WikiUIExtensionParameters parameters = createParameters("=" + VELOCITY_VALUE);

        assertTrue(parameters.get().isEmpty());
    }

    private WikiUIExtensionParameters createParameters(String parameters) throws Exception
    {
        return new WikiUIExtensionParameters(constructMockUIXObject(parameters), this.componentManager);
    }

    private BaseObject constructMockUIXObject(String parameters)
    {
        BaseObject result = mock();

        when(result.getStringValue(WikiUIExtensionConstants.ID_PROPERTY)).thenReturn("id");
        when(result.getStringValue(WikiUIExtensionConstants.PARAMETERS_PROPERTY)).thenReturn(parameters);
        XWikiDocument xWikiDocumentMock = mock();
        when(result.getOwnerDocument()).thenReturn(xWikiDocumentMock);
        when(result.getOwnerDocument().getAuthorReference()).thenReturn(WikiUIExtensionParametersTest.AUTHOR_REFERENCE);
        when(result.getDocumentReference()).thenReturn(WikiUIExtensionParametersTest.DOCUMENT_REFERENCE);

        return result;
    }
}
