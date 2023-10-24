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

/**
 * Test of {@link ModelValidationScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class ModelValidationScriptServiceTest
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
        when(this.nameStrategy.transform(TEST_NAME)).thenReturn(TRANSFORMED_NAME);
        when(this.nameStrategy.transform(this.entityReferenceSource)).thenReturn(this.entityReferenceTarget);
        when(this.nameStrategy.isValid(TEST_NAME)).thenReturn(false);
        when(this.nameStrategy.isValid(this.entityReferenceSource)).thenReturn(false);
    }

    @Test
    void getManager()
    {
        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(false);
        assertNull(this.scriptService.getManager());

        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);
        assertSame(this.nameStrategyManager, this.scriptService.getManager());
    }

    @Test
    void getConfiguration()
    {
        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(false);
        assertNull(this.scriptService.getConfiguration());

        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);
        assertSame(this.configuration, this.scriptService.getConfiguration());
    }

    @Test
    void getReplaceCharacterNameStrategyConfiguration()
    {
        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(false);
        assertNull(this.scriptService.getReplaceCharacterEntityNameValidationConfiguration());

        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);
        assertSame(this.replaceCharacterEntityNameValidationConfiguration,
            this.scriptService.getReplaceCharacterEntityNameValidationConfiguration());
    }

    @Test
    void transformName()
    {
        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(this.nameStrategy);
        when(this.configuration.useTransformation()).thenReturn(true);
        assertEquals(TRANSFORMED_NAME, this.scriptService.transformName(TEST_NAME));
        verify(this.nameStrategy, times(1)).transform(TEST_NAME);

        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        assertEquals(TEST_NAME, this.scriptService.transformName(TEST_NAME));
        verify(this.nameStrategy, times(1)).transform(TEST_NAME);

        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(this.nameStrategy);
        when(this.configuration.useTransformation()).thenReturn(false);
        assertEquals(TEST_NAME, this.scriptService.transformName(TEST_NAME));
        verify(this.nameStrategy, times(1)).transform(TEST_NAME);

        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        when(this.configuration.useTransformation()).thenReturn(false);
        assertEquals(TEST_NAME, this.scriptService.transformName(TEST_NAME));
        verify(this.nameStrategy, times(1)).transform(TEST_NAME);
    }

    @Test
    void transformEntityReference()
    {
        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(this.nameStrategy);
        when(this.configuration.useTransformation()).thenReturn(true);
        assertSame(this.entityReferenceTarget, this.scriptService.transformEntityReference(this.entityReferenceSource));
        verify(this.nameStrategy, times(1)).transform(this.entityReferenceSource);

        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        assertSame(this.entityReferenceSource, this.scriptService.transformEntityReference(this.entityReferenceSource));
        verify(this.nameStrategy, times(1)).transform(this.entityReferenceSource);

        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(this.nameStrategy);
        when(this.configuration.useTransformation()).thenReturn(false);
        assertSame(this.entityReferenceSource, this.scriptService.transformEntityReference(this.entityReferenceSource));
        verify(this.nameStrategy, times(1)).transform(this.entityReferenceSource);

        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        when(this.configuration.useTransformation()).thenReturn(false);
        assertSame(this.entityReferenceSource, this.scriptService.transformEntityReference(this.entityReferenceSource));
        verify(this.nameStrategy, times(1)).transform(this.entityReferenceSource);
    }

    @Test
    void transformNameWithHint()
    {
        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(this.nameStrategy);
        when(this.configuration.useTransformation()).thenReturn(false);
        assertEquals(TRANSFORMED_NAME, this.scriptService.transformName(TEST_NAME, HINT));
        verify(this.nameStrategy, times(1)).transform(TEST_NAME);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(this.configuration.useTransformation()).thenReturn(false);
        assertEquals(TEST_NAME, this.scriptService.transformName(TEST_NAME, HINT));
        verify(this.nameStrategy, times(1)).transform(TEST_NAME);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(this.configuration.useTransformation()).thenReturn(true);
        assertEquals(TEST_NAME, this.scriptService.transformName(TEST_NAME, HINT));
        verify(this.nameStrategy, times(1)).transform(TEST_NAME);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(this.nameStrategy);
        when(this.configuration.useTransformation()).thenReturn(true);
        assertEquals(TRANSFORMED_NAME, this.scriptService.transformName(TEST_NAME, HINT));
        verify(this.nameStrategy, times(2)).transform(TEST_NAME);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();
    }

    @Test
    void transformSourceEntityWithHint()
    {
        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(this.nameStrategy);
        when(this.configuration.useTransformation()).thenReturn(false);
        assertSame(
            this.entityReferenceTarget, this.scriptService.transformEntityReference(this.entityReferenceSource, HINT));
        verify(this.nameStrategy, times(1)).transform(this.entityReferenceSource);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(this.configuration.useTransformation()).thenReturn(false);
        assertSame(
            this.entityReferenceSource, this.scriptService.transformEntityReference(this.entityReferenceSource, HINT));
        verify(this.nameStrategy, times(1)).transform(this.entityReferenceSource);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(this.configuration.useTransformation()).thenReturn(true);
        assertSame(
            this.entityReferenceSource, this.scriptService.transformEntityReference(this.entityReferenceSource, HINT));
        verify(this.nameStrategy, times(1)).transform(this.entityReferenceSource);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(this.nameStrategy);
        when(this.configuration.useTransformation()).thenReturn(true);
        assertSame(
            this.entityReferenceTarget, this.scriptService.transformEntityReference(this.entityReferenceSource, HINT));
        verify(this.nameStrategy, times(2)).transform(this.entityReferenceSource);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();
    }

    @Test
    void isValidName()
    {
        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(this.nameStrategy);
        when(this.configuration.useValidation()).thenReturn(true);
        assertFalse(this.scriptService.isValid(TEST_NAME));
        verify(this.nameStrategy, times(1)).isValid(TEST_NAME);

        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        assertTrue(this.scriptService.isValid(TEST_NAME));
        verify(this.nameStrategy, times(1)).isValid(TEST_NAME);

        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(this.nameStrategy);
        when(this.configuration.useValidation()).thenReturn(false);
        assertTrue(this.scriptService.isValid(TEST_NAME));
        verify(this.nameStrategy, times(1)).isValid(TEST_NAME);

        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        when(this.configuration.useValidation()).thenReturn(false);
        assertTrue(this.scriptService.isValid(TEST_NAME));
        verify(this.nameStrategy, times(1)).isValid(TEST_NAME);
    }

    @Test
    void isValidEntityReference()
    {
        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(this.nameStrategy);
        when(this.configuration.useValidation()).thenReturn(true);
        assertFalse(this.scriptService.isValid(this.entityReferenceSource));
        verify(this.nameStrategy, times(1)).isValid(this.entityReferenceSource);

        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        assertTrue(this.scriptService.isValid(this.entityReferenceSource));
        verify(this.nameStrategy, times(1)).isValid(this.entityReferenceSource);

        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(this.nameStrategy);
        when(this.configuration.useValidation()).thenReturn(false);
        assertTrue(this.scriptService.isValid(this.entityReferenceSource));
        verify(this.nameStrategy, times(1)).isValid(this.entityReferenceSource);

        when(this.nameStrategyManager.getEntityReferenceNameStrategy()).thenReturn(null);
        when(this.configuration.useValidation()).thenReturn(false);
        assertTrue(this.scriptService.isValid(this.entityReferenceSource));
        verify(this.nameStrategy, times(1)).isValid(this.entityReferenceSource);
    }

    @Test
    void isValidNameWithHint()
    {
        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(this.nameStrategy);
        when(this.configuration.useValidation()).thenReturn(false);
        assertFalse(this.scriptService.isValid(TEST_NAME, HINT));
        verify(this.nameStrategy, times(1)).isValid(TEST_NAME);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(this.configuration.useValidation()).thenReturn(false);
        assertTrue(this.scriptService.isValid(TEST_NAME, HINT));
        verify(this.nameStrategy, times(1)).isValid(TEST_NAME);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(this.nameStrategy);
        when(this.configuration.useValidation()).thenReturn(true);
        assertFalse(this.scriptService.isValid(TEST_NAME, HINT));
        verify(this.nameStrategy, times(2)).isValid(TEST_NAME);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(this.configuration.useValidation()).thenReturn(true);
        assertTrue(this.scriptService.isValid(TEST_NAME, HINT));
        verify(this.nameStrategy, times(2)).isValid(TEST_NAME);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();
    }

    @Test
    void isValidEntityReferenceWithHint()
    {
        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(this.nameStrategy);
        when(this.configuration.useValidation()).thenReturn(false);
        assertFalse(this.scriptService.isValid(this.entityReferenceSource, HINT));
        verify(this.nameStrategy, times(1)).isValid(this.entityReferenceSource);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(this.configuration.useValidation()).thenReturn(false);
        assertTrue(this.scriptService.isValid(this.entityReferenceSource, HINT));
        verify(this.nameStrategy, times(1)).isValid(this.entityReferenceSource);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(this.nameStrategy);
        when(this.configuration.useValidation()).thenReturn(true);
        assertFalse(this.scriptService.isValid(this.entityReferenceSource, HINT));
        verify(this.nameStrategy, times(2)).isValid(this.entityReferenceSource);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();

        when(this.nameStrategyManager.getEntityReferenceNameStrategy(HINT)).thenReturn(null);
        when(this.configuration.useValidation()).thenReturn(true);
        assertTrue(this.scriptService.isValid(this.entityReferenceSource, HINT));
        verify(this.nameStrategy, times(2)).isValid(this.entityReferenceSource);
        verify(this.nameStrategyManager, never()).getEntityReferenceNameStrategy();
    }
}
