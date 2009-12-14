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
package org.xwiki.bridge;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.internal.DefaultAttachmentNameSerializer;
import org.xwiki.component.util.ReflectionUtils;

/**
 * Unit tests for {@link DefaultAttachmentNameSerializer}.
 * 
 * @version $Id$
 * @since 2.0.1
 */
@Deprecated
public class DefaultAttachmentNameSerializerTest
{
    private Mockery context;
    
    private DefaultAttachmentNameSerializer attachmentNameSerializer;

    private DocumentNameSerializer dns;
    
    @Before
    public void setUp()
    {
        context = new Mockery();
        attachmentNameSerializer = new DefaultAttachmentNameSerializer();
        
        dns = context.mock(DocumentNameSerializer.class);
        ReflectionUtils.setFieldValue(attachmentNameSerializer, "documentNameSerializer", dns);
    }
    
    @Test
    public void testSerializeAttachmentName()
    {
        final DocumentName documentName = new DocumentName("wiki", "space", "page");
        AttachmentName attachmentName = new AttachmentName(documentName, "filename");
        
        context.checking(new Expectations() {{
            allowing(dns).serialize(documentName); will(returnValue("wiki:space.page"));
        }});

        Assert.assertEquals("wiki:space.page@filename", attachmentNameSerializer.serialize(attachmentName));
    }
}
