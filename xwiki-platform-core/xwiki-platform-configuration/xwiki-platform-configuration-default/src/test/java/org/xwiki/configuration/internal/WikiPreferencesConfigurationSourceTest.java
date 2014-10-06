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

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.configuration.internal.test.AbstractTestDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Unit tests for {@link WikiPreferencesConfigurationSource}.
 * 
 * @version $Id: 31e2e0d488d6f5dbc1fcec1211d30dc30000b5eb
 */
public class WikiPreferencesConfigurationSourceTest extends AbstractTestDocumentConfigurationSource
{
    public WikiPreferencesConfigurationSourceTest()
    {
        super(WikiPreferencesConfigurationSource.class);
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return WikiPreferencesConfigurationSource.CLASS_REFERENCE;
    }

    @Test
    public void getPropertyForStringWhenExists() throws Exception
    {
        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.SPACE_NAME,
            WikiPreferencesConfigurationSource.PAGE_NAME), "key", "value");

        String result = this.componentManager.getComponentUnderTest().getProperty("key", String.class);

        Assert.assertEquals("value", result);
    }
}
