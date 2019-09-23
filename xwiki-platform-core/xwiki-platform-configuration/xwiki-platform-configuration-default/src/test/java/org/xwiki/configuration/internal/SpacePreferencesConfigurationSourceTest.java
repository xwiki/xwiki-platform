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

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.internal.test.AbstractTestDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Unit tests for {@link SpacePreferencesConfigurationSource}.
 *
 * @version $Id$
 */
public class SpacePreferencesConfigurationSourceTest extends AbstractTestDocumentConfigurationSource
{
    private static final DocumentReference SPACE_DOCUMENT =
        new DocumentReference(CURRENT_WIKI, "currentspace", SpacePreferencesConfigurationSource.DOCUMENT_NAME);

    public SpacePreferencesConfigurationSourceTest()
    {
        super(SpacePreferencesConfigurationSource.class);
    }

    @Override
    public void before() throws Exception
    {
        super.before();

        DocumentAccessBridge documentAccessBridge = this.componentManager.getInstance(DocumentAccessBridge.class);
        when(documentAccessBridge.getCurrentDocumentReference()).thenReturn(SPACE_DOCUMENT);
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return SpacePreferencesConfigurationSource.CLASS_REFERENCE;
    }

    @Test
    public void getPropertyForStringWhenExists() throws Exception
    {
        setStringProperty(SPACE_DOCUMENT, "key", "value");

        Assert.assertEquals("value", this.componentManager.getComponentUnderTest().getProperty("key", String.class));
    }
}
