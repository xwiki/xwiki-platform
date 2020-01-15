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
package org.xwiki.model.validation.script;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.validation.EntityNameValidation;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.model.validation.EntityNameValidationConfiguration;
import org.xwiki.model.validation.internal.ReplaceCharacterEntityNameValidationConfiguration;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class ModelValidationScriptServiceTest
{
    private static final String TEST_NAME = "foo";
    private static final String HINT = TEST_NAME;
    private static final String TRANSFORMED_NAME = "bar";

    @InjectMockComponents
    private ModelValidationScriptService scriptService;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @MockComponent
    private EntityNameValidationManager nameStrategyManager;

    @MockComponent
    private EntityNameValidationConfiguration configuration;

    @MockComponent
    private ReplaceCharacterEntityNameValidationConfiguration replaceCharacterEntityNameValidationConfiguration;

    @MockComponent
    private EntityNameValidation nameStrategy;

    @Mock
    private EntityReference entityReferenceSource;

    @Mock
    private EntityReference entityReferenceTarget;

    @BeforeEach
    public void setup()
    {
        when(nameStrategy.transform(TEST_NAME)).thenReturn(TRANSFORMED_NAME);
        when(nameStrategy.transform(entityReferenceSource)).thenReturn(entityReferenceTarget);
        when(nameStrategy.isValid(TEST_NAME)).thenReturn(false);
        when(nameStrategy.isValid(entityReferenceSource)).thenReturn(false);
    }

    @Test
    public void getManager()
    {
        when(authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(false);
        assertNull(scriptService.getManager());

        when(authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);
        assertSame(nameStrategyManager, scriptService.getManager());
    }

    @Test
    public void getConfiguration()
    {
        when(authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(false);
        assertNull(scriptService.getConfiguration());

        when(authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);
        assertSame(configuration, scriptService.getConfiguration());
    }

    @Test
    public void getReplaceCharacterNameStrategyConfiguration()
    {
        when(authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(false);
        assertNull(scriptService.getReplaceCharacterEntityNameValidationConfiguration());

        when(authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);
        assertSame(replaceCharacterEntityNameValidationConfiguration,
            scriptService.getReplaceCharacterEntityNameValidationConfiguration());
    }

    @Test
    public void transformName()
    {
        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(nameStrategy);
        when(configuration.useTransformation()).thenReturn(true);
        assertEquals(TRANSFORMED_NAME, scriptService.transformName(TEST_NAME));
        verify(nameStrategy, times(1)).transform(TEST_NAME);

        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        assertEquals(TEST_NAME, scriptService.transformName(TEST_NAME));
        verify(nameStrategy, times(1)).transform(TEST_NAME);

        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(nameStrategy);
        when(configuration.useTransformation()).thenReturn(false);
        assertEquals(TEST_NAME, scriptService.transformName(TEST_NAME));
        verify(nameStrategy, times(1)).transform(TEST_NAME);

        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        when(configuration.useTransformation()).thenReturn(false);
        assertEquals(TEST_NAME, scriptService.transformName(TEST_NAME));
        verify(nameStrategy, times(1)).transform(TEST_NAME);
    }

    @Test
    public void transformEntityReference()
    {
        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(nameStrategy);
        when(configuration.useTransformation()).thenReturn(true);
        assertSame(entityReferenceTarget, scriptService.transformEntityReference(entityReferenceSource));
        verify(nameStrategy, times(1)).transform(entityReferenceSource);

        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        assertSame(entityReferenceSource, scriptService.transformEntityReference(entityReferenceSource));
        verify(nameStrategy, times(1)).transform(entityReferenceSource);

        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(nameStrategy);
        when(configuration.useTransformation()).thenReturn(false);
        assertSame(entityReferenceSource, scriptService.transformEntityReference(entityReferenceSource));
        verify(nameStrategy, times(1)).transform(entityReferenceSource);

        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        when(configuration.useTransformation()).thenReturn(false);
        assertSame(entityReferenceSource, scriptService.transformEntityReference(entityReferenceSource));
        verify(nameStrategy, times(1)).transform(entityReferenceSource);
    }

    @Test
    public void transformNameWithHint()
    {
        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(nameStrategy);
        when(configuration.useTransformation()).thenReturn(false);
        assertEquals(TRANSFORMED_NAME, scriptService.transformName(TEST_NAME, HINT));
        verify(nameStrategy, times(1)).transform(TEST_NAME);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(configuration.useTransformation()).thenReturn(false);
        assertEquals(TEST_NAME, scriptService.transformName(TEST_NAME, HINT));
        verify(nameStrategy, times(1)).transform(TEST_NAME);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(configuration.useTransformation()).thenReturn(true);
        assertEquals(TEST_NAME, scriptService.transformName(TEST_NAME, HINT));
        verify(nameStrategy, times(1)).transform(TEST_NAME);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(nameStrategy);
        when(configuration.useTransformation()).thenReturn(true);
        assertEquals(TRANSFORMED_NAME, scriptService.transformName(TEST_NAME, HINT));
        verify(nameStrategy, times(2)).transform(TEST_NAME);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();
    }

    @Test
    public void transformSourceEntityWithHint()
    {
        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(nameStrategy);
        when(configuration.useTransformation()).thenReturn(false);
        assertSame(entityReferenceTarget, scriptService.transformEntityReference(entityReferenceSource, HINT));
        verify(nameStrategy, times(1)).transform(entityReferenceSource);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(configuration.useTransformation()).thenReturn(false);
        assertSame(entityReferenceSource, scriptService.transformEntityReference(entityReferenceSource, HINT));
        verify(nameStrategy, times(1)).transform(entityReferenceSource);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(configuration.useTransformation()).thenReturn(true);
        assertSame(entityReferenceSource, scriptService.transformEntityReference(entityReferenceSource, HINT));
        verify(nameStrategy, times(1)).transform(entityReferenceSource);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(nameStrategy);
        when(configuration.useTransformation()).thenReturn(true);
        assertSame(entityReferenceTarget, scriptService.transformEntityReference(entityReferenceSource, HINT));
        verify(nameStrategy, times(2)).transform(entityReferenceSource);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();
    }

    @Test
    public void isValidName()
    {
        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(nameStrategy);
        when(configuration.useValidation()).thenReturn(true);
        assertFalse(scriptService.isValid(TEST_NAME));
        verify(nameStrategy, times(1)).isValid(TEST_NAME);

        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        assertTrue(scriptService.isValid(TEST_NAME));
        verify(nameStrategy, times(1)).isValid(TEST_NAME);

        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(nameStrategy);
        when(configuration.useValidation()).thenReturn(false);
        assertTrue(scriptService.isValid(TEST_NAME));
        verify(nameStrategy, times(1)).isValid(TEST_NAME);

        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        when(configuration.useValidation()).thenReturn(false);
        assertTrue(scriptService.isValid(TEST_NAME));
        verify(nameStrategy, times(1)).isValid(TEST_NAME);
    }

    @Test
    public void isValidEntityReference()
    {
        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(nameStrategy);
        when(configuration.useValidation()).thenReturn(true);
        assertFalse(scriptService.isValid(entityReferenceSource));
        verify(nameStrategy, times(1)).isValid(entityReferenceSource);

        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        assertTrue(scriptService.isValid(entityReferenceSource));
        verify(nameStrategy, times(1)).isValid(entityReferenceSource);

        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(nameStrategy);
        when(configuration.useValidation()).thenReturn(false);
        assertTrue(scriptService.isValid(entityReferenceSource));
        verify(nameStrategy, times(1)).isValid(entityReferenceSource);

        when(nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        when(configuration.useValidation()).thenReturn(false);
        assertTrue(scriptService.isValid(entityReferenceSource));
        verify(nameStrategy, times(1)).isValid(entityReferenceSource);
    }

    @Test
    public void isValidNameWithHint()
    {
        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(nameStrategy);
        when(configuration.useValidation()).thenReturn(false);
        assertFalse(scriptService.isValid(TEST_NAME, HINT));
        verify(nameStrategy, times(1)).isValid(TEST_NAME);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(configuration.useValidation()).thenReturn(false);
        assertTrue(scriptService.isValid(TEST_NAME, HINT));
        verify(nameStrategy, times(1)).isValid(TEST_NAME);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(nameStrategy);
        when(configuration.useValidation()).thenReturn(true);
        assertFalse(scriptService.isValid(TEST_NAME, HINT));
        verify(nameStrategy, times(2)).isValid(TEST_NAME);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(configuration.useValidation()).thenReturn(true);
        assertTrue(scriptService.isValid(TEST_NAME, HINT));
        verify(nameStrategy, times(2)).isValid(TEST_NAME);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();
    }

    @Test
    public void isValidEntityReferenceWithHint()
    {
        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(nameStrategy);
        when(configuration.useValidation()).thenReturn(false);
        assertFalse(scriptService.isValid(entityReferenceSource, HINT));
        verify(nameStrategy, times(1)).isValid(entityReferenceSource);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(configuration.useValidation()).thenReturn(false);
        assertTrue(scriptService.isValid(entityReferenceSource, HINT));
        verify(nameStrategy, times(1)).isValid(entityReferenceSource);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(nameStrategy);
        when(configuration.useValidation()).thenReturn(true);
        assertFalse(scriptService.isValid(entityReferenceSource, HINT));
        verify(nameStrategy, times(2)).isValid(entityReferenceSource);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(configuration.useValidation()).thenReturn(true);
        assertTrue(scriptService.isValid(entityReferenceSource, HINT));
        verify(nameStrategy, times(2)).isValid(entityReferenceSource);
        verify(nameStrategyManager, never()).getEntityReferenceNameStrategy();
    }
}
