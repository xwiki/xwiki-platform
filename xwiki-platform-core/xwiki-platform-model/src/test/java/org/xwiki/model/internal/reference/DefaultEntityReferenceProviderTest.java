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
package org.xwiki.model.internal.reference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.test.TestConstants;

/**
 * Unit tests for the deprecated {@link DefaultEntityReferenceProvider}.
 *
 * @version $Id$
 */
public class DefaultEntityReferenceProviderTest implements TestConstants
{
    private Mockery mockery = new Mockery();

    private EntityReferenceProvider provider;

    @Before
    public void setUp()
    {
        this.provider = new DefaultEntityReferenceProvider();
        final ModelConfiguration mockConfiguration = this.mockery.mock(ModelConfiguration.class);
        ReflectionUtils.setFieldValue(this.provider, "configuration", mockConfiguration);

        this.mockery.checking(new Expectations()
        {
            {
                allowing(mockConfiguration).getDefaultReferenceValue(EntityType.SPACE);
                will(returnValue(DEFAULT_SPACE));
                allowing(mockConfiguration).getDefaultReferenceValue(EntityType.WIKI);
                will(returnValue(DEFAULT_WIKI));
                allowing(mockConfiguration).getDefaultReferenceValue(EntityType.DOCUMENT);
                will(returnValue(DEFAULT_PAGE));
                allowing(mockConfiguration).getDefaultReferenceValue(EntityType.ATTACHMENT);
                will(returnValue(DEFAULT_ATTACHMENT));
            }
        });
    }

    @Test
    public void testGetDefaultValue()
    {
        Assert.assertEquals(DEFAULT_PAGE_REFERENCE, this.provider.getDefaultReference(EntityType.DOCUMENT));
        Assert.assertEquals(DEFAULT_SPACE_REFERENCE, this.provider.getDefaultReference(EntityType.SPACE));
        Assert.assertEquals(DEFAULT_ATTACHMENT_REFERENCE, this.provider.getDefaultReference(EntityType.ATTACHMENT));
        Assert.assertEquals(DEFAULT_WIKI_REFERENCE, this.provider.getDefaultReference(EntityType.WIKI));
    }
}
