package org.xwiki.lesscss.internal;

import java.nio.file.Path;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.lesscss.LESSCompiler;
import org.xwiki.lesscss.LESSCompilerException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.jgroups.util.Util.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link DefaultLESSCompiler}.
 *
 * @since 6.3M1
 * @version $Id$
 */
public class DefaultLESSCompilerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultLESSCompiler> mocker =
            new MockitoComponentMockingRule<>(DefaultLESSCompiler.class);

    private ConfigurationSource configurationSource;

    private ComponentManager componentManager;

    @Before
    public void setUp() throws Exception
    {
        configurationSource = mocker.getInstance(ConfigurationSource.class);
        componentManager = mocker.getInstance(ComponentManager.class);
    }

    @Test
    public void compile() throws Exception
    {
        // Mocks
        when(configurationSource.getProperty(eq("less.compiler"), anyString())).thenReturn("elvis");

        // Mocked compiler
        LESSCompiler mockedCompiler = mock(LESSCompiler.class);
        when(mockedCompiler.compile(anyString())).thenReturn("It's ok");
        when(mockedCompiler.compile(anyString(), any(Path[].class))).thenReturn("Good");
        mocker.registerComponent(LESSCompiler.class, "elvis", mockedCompiler);

        // Test 1
        String result = mocker.getComponentUnderTest().compile("Blah Blah");
        assertEquals("It's ok", result);

        // Test 2
        Path[] paths = new Path[1];
        String result2 = mocker.getComponentUnderTest().compile("Blah Blah", paths);
        assertEquals("Good", result2);
    }

    @Test
    public void compileWithException() throws Exception
    {
        // Mocks
        when(configurationSource.getProperty(eq("less.compiler"), anyString())).thenReturn("elvis");

        // Test
        Exception exception = null;
        try {
            mocker.getComponentUnderTest().compile("Blah");
        } catch (LESSCompilerException e) {
            exception = e;
        }

        // Verify
        assertNotNull(exception);
        assertTrue(exception instanceof  LESSCompilerException);
        assertEquals("Unable to get the LESS Compiler component [elvis].", exception.getMessage());
    }

    @Test
    public void compilePathsWithException() throws Exception
    {
        // Mocks
        when(configurationSource.getProperty(eq("less.compiler"), anyString())).thenReturn("elvis");
        Path[] paths = new Path[1];

        // Test
        Exception exception = null;
        try {
            mocker.getComponentUnderTest().compile("Blah", paths);
        } catch (LESSCompilerException e) {
            exception = e;
        }

        // Verify
        assertNotNull(exception);
        assertTrue(exception instanceof  LESSCompilerException);
        assertEquals("Unable to get the LESS Compiler component [elvis].", exception.getMessage());
    }

    @Test
    public void ensureLESS4JisTheDefault() throws Exception
    {
        when(configurationSource.getProperty(eq("less.compiler"), eq("less4j"))).thenReturn("less4j");

        // Mocked compiler
        LESSCompiler mockedCompiler = mock(LESSCompiler.class);
        when(mockedCompiler.compile(anyString())).thenReturn("LESS4J did it");
        mocker.registerComponent(LESSCompiler.class, "less4j", mockedCompiler);

        // Test
        String result = mocker.getComponentUnderTest().compile("Blah Blah");

        // Verify
        assertEquals("LESS4J did it", result);
    }
}
