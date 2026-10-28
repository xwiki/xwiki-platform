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
package org.xwiki.uiextension.internal.parameters;

import java.io.Reader;
import java.io.StringWriter;

import jakarta.inject.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.VelocityTemplate;
import org.xwiki.velocity.XWikiVelocityContext;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.velocity.internal.util.VelocityDetector;
import org.xwiki.webjars.WebJarsUrlFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UIXParameterValueFactory} and the {@link UIXParameterValue} implementations it creates.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(VelocityDetector.class)
class UIXParameterValueFactoryTest
{
    private static final String NAMESPACE = "id:key";

    private static final String KEY = "my.key";

    private static final String TRANSLATION = "My translation";

    private static final String WEBJAR_ID = "org.xwiki.platform:xwiki-platform-test-webjar";

    private static final String WEBJAR_URL = "/xwiki/webjars/test.js";

    @InjectMockComponents
    private UIXParameterValueFactory factory;

    @MockComponent
    private VelocityManager velocityManager;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @MockComponent
    private WebJarsUrlFactory webJarsUrlFactory;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @BeforeEach
    void setUp()
    {
        when(this.localizationManager.getTranslationPlain(KEY)).thenReturn(TRANSLATION);
    }

    @Test
    void createWithoutVelocity()
    {
        UIXParameterValue value = this.factory.create(NAMESPACE, "Some value, (with) 'quotes'!");

        StaticParameterValue staticValue = assertInstanceOf(StaticParameterValue.class, value);
        assertTrue(staticValue.isStatic());
        assertFalse(staticValue.requiresVelocity());
        assertEquals("Some value, (with) 'quotes'!", staticValue.evaluate(null));
    }

    @Test
    void createWithLocalizationRender() throws Exception
    {
        UIXParameterValue value = this.factory.create(NAMESPACE, "$services.localization.render('my.key')");

        assertInstanceOf(ShortcutParameterValue.class, value);
        assertFalse(value.isStatic());
        assertFalse(value.requiresVelocity());
        assertEquals(TRANSLATION, value.evaluate(null));
        verifyNoInteractions(this.velocityManager);
    }

    @Test
    void createWithLocalizationRenderAndLiteralText() throws Exception
    {
        UIXParameterValue value =
            this.factory.create(NAMESPACE, " (prefix) $services.localization.render( \"my.key\" )! suffix ");

        assertInstanceOf(ShortcutParameterValue.class, value);
        assertEquals(" (prefix) " + TRANSLATION + "! suffix ", value.evaluate(null));
    }

    @Test
    void createWithLocalizationRenderOfMissingTranslation() throws Exception
    {
        UIXParameterValue value = this.factory.create(NAMESPACE, "$services.localization.render('missing.key')");

        assertEquals("missing.key", value.evaluate(null));
    }

    @Test
    void createWithWebJarURL() throws Exception
    {
        when(this.webJarsUrlFactory.url("test.js")).thenReturn("/one");
        when(this.webJarsUrlFactory.url(WEBJAR_ID, "test.js")).thenReturn(WEBJAR_URL);
        when(this.webJarsUrlFactory.url(WEBJAR_ID, "wiki:xwiki", "test.js")).thenReturn("/three");

        UIXParameterValue oneArgument = this.factory.create(NAMESPACE, "$services.webjars.url('test.js')");
        UIXParameterValue twoArguments = this.factory.create(NAMESPACE,
            "$services.webjars.url('org.xwiki.platform:xwiki-platform-test-webjar', 'test.js')");
        UIXParameterValue threeArguments = this.factory.create(NAMESPACE,
            "$services.webjars.url('org.xwiki.platform:xwiki-platform-test-webjar','wiki:xwiki','test.js')");

        assertEquals("/one", oneArgument.evaluate(null));
        assertEquals(WEBJAR_URL, twoArguments.evaluate(null));
        assertEquals("/three", threeArguments.evaluate(null));
        verifyNoInteractions(this.velocityManager);
    }

    @Test
    void createWithSkinFile() throws Exception
    {
        XWikiContext xcontext = mock();
        XWiki xwiki = mock();
        when(this.xcontextProvider.get()).thenReturn(xcontext);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xwiki.getSkinFile("js/test.js", xcontext)).thenReturn("/skins/flamingo/js/test.js");

        UIXParameterValue value = this.factory.create(NAMESPACE, "$xwiki.getSkinFile('js/test.js')");

        assertEquals("/skins/flamingo/js/test.js", value.evaluate(null));
    }

    @Test
    void evaluateShortcutReturningNull() throws Exception
    {
        String raw = "prefix $services.webjars.url('test.js')";
        UIXParameterValue value = this.factory.create(NAMESPACE, raw);

        // Like Velocity, output the code as written when the call returns null.
        assertEquals(raw, value.evaluate(null));
    }

    @Test
    void evaluateShortcutFailing()
    {
        IllegalStateException exception = new IllegalStateException("Failure");
        when(this.webJarsUrlFactory.url("test.js")).thenThrow(exception);

        UIXParameterValue value = this.factory.create(NAMESPACE, "$services.webjars.url('test.js')");

        assertSame(exception, assertThrows(IllegalStateException.class, () -> value.evaluate(null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        // Not supported shapes of the supported calls.
        "$services.localization.render('my.key', 'other.key')",
        "$services.localization.render()",
        "$services.webjars.url('a', 'b', 'c', 'd')",
        "$xwiki.getSkinFile('js/test.js', true)",
        // Arguments that aren't simple string literals.
        "$services.localization.render(\"$key\")",
        "$services.localization.render(\"a\\b\")",
        "$services.localization.render('it''s')",
        "$services.localization.render('my.key',)",
        "$services.localization.render('my.key' 'other.key')",
        "$services.localization.render($key)",
        "$services.localization.render('(my.key)')",
        // The call is escaped, quiet, formal or continued.
        "\\$services.localization.render('my.key')",
        "$!services.localization.render('my.key')",
        "${services.localization.render('my.key')}",
        "$services.localization.render('my.key').toUpperCase()",
        "$services.localization.render('my.key')[0]",
        // Other Velocity code around the call.
        "$services.localization.render('my.key') $services.localization.render('other.key')",
        "$services.localization.render('my.key')#if (true) yes#end",
        "## comment $services.localization.render('my.key')",
        // Other calls.
        "$services.icon.render('my.key')",
        "$xwiki.getURL('Main.WebHome')"
    })
    void createWithOtherVelocity(String raw)
    {
        UIXParameterValue value = this.factory.create(NAMESPACE, raw);

        assertInstanceOf(VelocityParameterValue.class, value);
        assertTrue(value.requiresVelocity());
        assertEquals(raw, value.raw());
    }

    @Test
    void evaluateVelocityCompilesOnce() throws Exception
    {
        VelocityTemplate template = mock();
        when(this.velocityManager.compile(eq(NAMESPACE), any(Reader.class))).thenAnswer(invocation -> {
            assertEquals("#if (true)yes#end", IOUtils.toString(invocation.<Reader>getArgument(1)));
            return template;
        });
        VelocityEngine engine = mock();
        doAnswer(invocation -> {
            invocation.<StringWriter>getArgument(1).write("yes");
            return null;
        }).when(engine).evaluate(any(), any(), eq(NAMESPACE), eq(template));
        VelocityContext context = new VelocityContext();
        VelocityParameterSession session = new VelocityParameterSession(engine, context, false);

        UIXParameterValue value = this.factory.create(NAMESPACE, "#if (true)yes#end");
        // The value is compiled lazily.
        verifyNoInteractions(this.velocityManager);

        assertEquals("yes", value.evaluate(session));
        assertEquals("yes", value.evaluate(session));

        verify(this.velocityManager).compile(eq(NAMESPACE), any(Reader.class));
        // Each evaluation gets its own context so that variables don't leak.
        verify(engine, times(2)).evaluate(any(XWikiVelocityContext.class), any(),
            eq(NAMESPACE), eq(template));
    }

    @Test
    void evaluateVelocityWithSeparateContexts()
    {
        VelocityContext context = new VelocityContext();
        context.put("existing", "value");
        VelocityParameterSession session = new VelocityParameterSession(mock(), context, false);

        VelocityContext first = session.newContext();
        VelocityContext second = session.newContext();
        first.put("variable", "first");

        assertNotSame(first, second);
        assertEquals("value", second.get("existing"));
        assertFalse(second.containsKey("variable"));
        assertFalse(context.containsKey("variable"));
    }

    @Test
    void evaluateVelocityWithCompilationError() throws Exception
    {
        when(this.velocityManager.compile(eq(NAMESPACE), any(Reader.class)))
            .thenThrow(new RuntimeException("Parse error"));
        VelocityParameterSession session = new VelocityParameterSession(mock(), new VelocityContext(), false);

        UIXParameterValue value = this.factory.create(NAMESPACE, "#if (");

        XWikiVelocityException exception =
            assertThrows(XWikiVelocityException.class, () -> value.evaluate(session));
        assertEquals("Failed to compile the Velocity code with namespace [id:key]", exception.getMessage());
        assertSame(exception, assertThrows(XWikiVelocityException.class, () -> value.evaluate(session)));

        // The failure is remembered instead of compiling the value again.
        verify(this.velocityManager).compile(eq(NAMESPACE), any(Reader.class));
        verifyNoInteractions(session.engine());
    }
}
