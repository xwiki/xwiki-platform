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
package org.xwiki.configuration.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.internal.test.AbstractTestDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Unit tests for {@link UserPreferencesConfigurationSource}.
 *
 * @version $Id$
 */
public class UserPreferencesConfigurationSourceTest extends AbstractTestDocumentConfigurationSource
{
    public UserPreferencesConfigurationSourceTest()
    {
        super(UserPreferencesConfigurationSource.class);
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return UserPreferencesConfigurationSource.CLASS_REFERENCE;
    }

    @Test
    public void getPropertyForStringWhenExists() throws Exception
    {
        DocumentReference userReference = new DocumentReference(CURRENT_WIKI,
            UserPreferencesConfigurationSource.SPACE_NAME, "user");
        DocumentAccessBridge documentAccessBridge = this.componentManager.getInstance(DocumentAccessBridge.class);
        when(documentAccessBridge.getCurrentUserReference()).thenReturn(userReference);

        setStringProperty(userReference, "key", "value");

        String result = this.componentManager.getComponentUnderTest().getProperty("key", String.class);

        assertEquals("value", result);
    }

    /**
     * Verify that the superadmin user can always view hidden documents.
     */
    @Test
    public void getDisplayHiddenDocumentsPropertyForSuperadminUser() throws Exception
    {
        DocumentReference userReference = new DocumentReference(CURRENT_WIKI,
            UserPreferencesConfigurationSource.SPACE_NAME, "superadmin");
        DocumentAccessBridge documentAccessBridge = this.componentManager.getInstance(DocumentAccessBridge.class);
        when(documentAccessBridge.getCurrentUserReference()).thenReturn(userReference);

        int result =
            this.componentManager.getComponentUnderTest().getProperty("displayHiddenDocuments", Integer.class);
        assertEquals(1, result);
    }
}
