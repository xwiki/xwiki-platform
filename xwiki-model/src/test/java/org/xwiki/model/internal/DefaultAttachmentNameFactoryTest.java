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
package org.xwiki.model.internal;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.AttachmentName;
import org.xwiki.model.DocumentName;
import org.xwiki.model.DocumentNameFactory;
import org.xwiki.model.Model;

/**
 * Unit tests for {@link DefaultAttachmentNameFactory}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
public class DefaultAttachmentNameFactoryTest
{
    private Mockery context;
    
    private DefaultAttachmentNameFactory attachmentNameFactory;
    
    private Model model;
    
    private DocumentNameFactory ddnf;

    private DocumentNameFactory cdnf;

    @Before
    public void setUp()
    {
        context = new Mockery();
        attachmentNameFactory = new DefaultAttachmentNameFactory();
        
        cdnf = context.mock(DocumentNameFactory.class, "current");
        ReflectionUtils.setFieldValue(attachmentNameFactory, "currentDocumentNameFactory", cdnf);

        ddnf = context.mock(DocumentNameFactory.class, "default");
        ReflectionUtils.setFieldValue(attachmentNameFactory, "defaultDocumentNameFactory", ddnf);

        model = context.mock(Model.class);
        ReflectionUtils.setFieldValue(attachmentNameFactory, "model", model);
    }

    @Test
    public void testCreateAttachmentNameWhenCurrentDocumentSetAndNoFilenameSeparator()
    {
        final DocumentName expectedDocumentName = new DocumentName("wiki", "space", "page");
        context.checking(new Expectations() {{
            allowing(model).getCurrentDocumentName(); will(returnValue(expectedDocumentName));
        }});
        
        // Test when there isn't the "@" separator.
        AttachmentName attachmentName = attachmentNameFactory.createAttachmentName("filename");
        Assert.assertEquals("filename", attachmentName.getFileName());
        Assert.assertEquals(expectedDocumentName, attachmentName.getDocumentName());
    }

    @Test
    public void testCreateAttachmentNameWhenCurrentDocumentSetAndFilenameSeparatorUsed()
    {
        final DocumentName expectedDocumentName = new DocumentName("somewiki", "somespace", "somepage");
        context.checking(new Expectations() {{
            allowing(cdnf).createDocumentName("somepage"); will(returnValue(expectedDocumentName));
        }});
        
        // Test with the "@" separator
        AttachmentName attachmentName = attachmentNameFactory.createAttachmentName("somepage@filename");
        Assert.assertEquals("filename", attachmentName.getFileName());
        Assert.assertEquals(expectedDocumentName, attachmentName.getDocumentName());
    }

    @Test
    public void testCreateAttachmentNameWhenNoCurrentDocumentSet()
    {
        final DocumentName expectedDocumentName = new DocumentName("xwiki", "XWiki", "WebHome");
        context.checking(new Expectations() {{
            allowing(model).getCurrentDocumentName(); will(returnValue(null));
            allowing(ddnf).createDocumentName(null); will(returnValue(expectedDocumentName));
        }});
        
        AttachmentName attachmentName = attachmentNameFactory.createAttachmentName("filename");
        Assert.assertEquals("filename", attachmentName.getFileName());
        Assert.assertEquals(expectedDocumentName, attachmentName.getDocumentName());
    }
}
