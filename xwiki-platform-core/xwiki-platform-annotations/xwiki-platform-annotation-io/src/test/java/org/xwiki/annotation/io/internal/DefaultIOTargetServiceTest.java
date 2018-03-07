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
package org.xwiki.annotation.io.internal;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.annotation.io.IOTargetService;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.test.jmock.AbstractComponentTestCase;

import com.xpn.xwiki.web.Utils;

import static org.junit.Assert.assertEquals;

/**
 * Tests the default implementation of {@link IOTargetService}, and integration with target resolvers, up to the
 * document access bridge access.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class DefaultIOTargetServiceTest extends AbstractComponentTestCase
{
    /**
     * Tested io target service.
     */
    private IOTargetService ioTargetService;

    /**
     * Mock for the document access bridge.
     */
    private DocumentAccessBridge dabMock;

    /**
     * Mock for DocumentReferenceResolver<String> used by BaseObjectReference
     */
    private DocumentReferenceResolver<String> classResolver;

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        // register the dab
        this.dabMock = registerMockComponent(DocumentAccessBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(dabMock).getCurrentUserReference();
            }
        });
        this.classResolver = registerMockComponent(DocumentReferenceResolver.TYPE_STRING);

        // We don't care about multi CM
        DefaultComponentDescriptor<ComponentManager> componentDescriptor = new DefaultComponentDescriptor<>();
        componentDescriptor.setRoleType(ComponentManager.class);
        componentDescriptor.setRoleHint("context");
        getComponentManager().registerComponent(componentDescriptor, getComponentManager());
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        // get the default io target service
        ioTargetService = getComponentManager().getInstance(IOTargetService.class);
        Utils.setComponentManager(getComponentManager());
    }

    @Test
    public void testGettersWhenTargetIsTypedDocument() throws Exception
    {
        final DocumentModelBridge dmb = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(dabMock).getTranslatedDocumentInstance(new DocumentReference("wiki", "Space", "Page"));
                will(returnValue(dmb));
                oneOf(dmb).getContent();
                will(returnValue("defcontent"));
                oneOf(dmb).getSyntax();
                will(returnValue(new Syntax(SyntaxType.XWIKI,"2.0")));
            }
        });

        String reference = "DOCUMENT://wiki:Space.Page";
        assertEquals("defcontent", ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", ioTargetService.getSourceSyntax(reference));
    }

    @Test
    public void testGettersWhenTargetIsNonTypedDocument() throws Exception
    {
        final DocumentModelBridge dmb = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(dabMock).getTranslatedDocumentInstance(new DocumentReference("wiki", "Space", "Page"));
                will(returnValue(dmb));
                oneOf(dmb).getContent();
                will(returnValue("defcontent"));
                oneOf(dmb).getSyntax();
                will(returnValue(new Syntax(SyntaxType.XWIKI,"2.0")));
            }
        });

        String reference = "wiki:Space.Page";
        assertEquals("defcontent", ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", ioTargetService.getSourceSyntax(reference));
    }

    @Test
    public void testGettersWhenTargetIsNonTypedRelativeDocument() throws Exception
    {
        final DocumentModelBridge dmb = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                // default resolver should be used
                allowing(dabMock).getTranslatedDocumentInstance(new DocumentReference("xwiki", "Space", "Page"));
                will(returnValue(dmb));
                oneOf(dmb).getContent();
                will(returnValue("defcontent"));
                oneOf(dmb).getSyntax();
                will(returnValue(new Syntax(SyntaxType.XWIKI,"2.0")));
            }
        });

        String reference = "Space.Page";
        assertEquals("defcontent", ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", ioTargetService.getSourceSyntax(reference));
    }

    @Test
    public void testGettersWhenTargetIsTypedRelativeDocument() throws Exception
    {
        final DocumentModelBridge dmb = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                // default resolver should be used
                allowing(dabMock).getTranslatedDocumentInstance(new DocumentReference("xwiki", "Space", "Page"));
                will(returnValue(dmb));
                oneOf(dmb).getContent();
                will(returnValue("defcontent"));
                oneOf(dmb).getSyntax();
                will(returnValue(new Syntax(SyntaxType.XWIKI,"2.0")));
            }
        });

        String reference = "DOCUMENT://Space.Page";
        assertEquals("defcontent", ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", ioTargetService.getSourceSyntax(reference));
    }

    @Test
    public void testGettersWhenTargetIsTypedSpace() throws Exception
    {
        final DocumentModelBridge dmb = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                // default resolver should be used
                oneOf(dabMock).getDocumentContent("SPACE://wiki:Space");
                will(returnValue("defcontent"));
                oneOf(dabMock).getDocumentSyntaxId("SPACE://wiki:Space");
                will(returnValue("xwiki/2.0"));
            }
        });

        // expect source ref to be used as is, as it doesn't parse to something acceptable
        String reference = "SPACE://wiki:Space";
        assertEquals("defcontent", ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", ioTargetService.getSourceSyntax(reference));
    }

    @Test
    public void testGettersWhenTargetIsEmptyString() throws Exception
    {
        final DocumentModelBridge dmb = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                // default resolver should be used. Note that this will fail if default values change, not very well
                // isolated
                allowing(dabMock).getTranslatedDocumentInstance(new DocumentReference("xwiki", "Main", "WebHome"));
                will(returnValue(dmb));
                oneOf(dmb).getContent();
                will(returnValue("defcontent"));
                oneOf(dmb).getSyntax();
                will(returnValue(new Syntax(SyntaxType.XWIKI,"2.0")));
            }
        });

        // expect source ref to be used as is, as it doesn't parse to something acceptable
        String reference = "";
        assertEquals("defcontent", ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", ioTargetService.getSourceSyntax(reference));
    }

    @Test
    public void testGetterWhenTargetIsTypedIndexedObjectProperty() throws Exception
    {
        final DocumentModelBridge dmb = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(classResolver).resolve("XWiki.Class", new DocumentReference("wiki", "Space", "Page"));
                will(returnValue(new DocumentReference("wiki", "XWiki", "Class")));
                oneOf(dabMock).getProperty(new DocumentReference("wiki", "Space", "Page"),
                                           new DocumentReference("wiki", "XWiki", "Class"), 1, "property");
                will(returnValue("defcontent"));
                oneOf(dabMock).getTranslatedDocumentInstance(new DocumentReference("wiki", "Space", "Page"));
                will(returnValue(dmb));
                oneOf(dmb).getSyntax();
                will(returnValue(new Syntax(SyntaxType.XWIKI,"2.0")));
            }
        });

        String reference = "OBJECT_PROPERTY://wiki:Space.Page^XWiki.Class[1].property";
        assertEquals("defcontent", ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", ioTargetService.getSourceSyntax(reference));
    }

    @Test
    public void testGetterWhenTargetIsTypedDefaultObjectProperty() throws Exception
    {
        final DocumentModelBridge dmb = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(classResolver).resolve("XWiki.Class", new DocumentReference("wiki", "Space", "Page"));
                will(returnValue(new DocumentReference("wiki", "XWiki", "Class")));
                oneOf(dabMock).getProperty(new DocumentReference("wiki", "Space", "Page"),
                                           new DocumentReference("wiki", "XWiki", "Class"), "property");
                will(returnValue("defcontent"));
                oneOf(dabMock).getTranslatedDocumentInstance(new DocumentReference("wiki", "Space", "Page"));
                will(returnValue(dmb));
                oneOf(dmb).getSyntax();
                will(returnValue(new Syntax(SyntaxType.XWIKI,"2.0")));
            }
        });

        String reference = "OBJECT_PROPERTY://wiki:Space.Page^XWiki.Class.property";
        assertEquals("defcontent", ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", ioTargetService.getSourceSyntax(reference));
    }

    @Test
    public void testGetterWhenTargetIsTypedObjectPropertyInRelativeDocument() throws Exception
    {
        final DocumentModelBridge dmb = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(classResolver).resolve("XWiki.Class", new DocumentReference("xwiki", "Main", "Page"));
                will(returnValue(new DocumentReference("xwiki", "XWiki", "Class")));
                oneOf(dabMock).getProperty(new DocumentReference("xwiki", "Main", "Page"),
                    new DocumentReference("xwiki", "XWiki", "Class"), "property");
                will(returnValue("defcontent"));
                oneOf(dabMock).getTranslatedDocumentInstance(new DocumentReference("xwiki", "Main", "Page"));
                will(returnValue(dmb));
                oneOf(dmb).getSyntax();
                will(returnValue(new Syntax(SyntaxType.XWIKI,"2.0")));
            }
        });

        String reference = "OBJECT_PROPERTY://Page^XWiki.Class.property";
        assertEquals("defcontent", ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", ioTargetService.getSourceSyntax(reference));
    }

    @Test
    public void testGetterWhenTargetIsNonTypedObjectProperty() throws Exception
    {
        final DocumentModelBridge dmb = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                // target will be parsed as document, because document is the default
                allowing(dabMock).getTranslatedDocumentInstance(new DocumentReference("wiki", "Space.Page^XWiki.Class", "property"));
                will(returnValue(dmb));
                oneOf(dmb).getContent();
                will(returnValue("defcontent"));
                oneOf(dmb).getSyntax();
                will(returnValue(new Syntax(SyntaxType.XWIKI,"2.0")));
            }
        });

        String reference = "wiki:Space\\.Page^XWiki\\.Class.property";
        assertEquals("defcontent", ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", ioTargetService.getSourceSyntax(reference));
    }

    @Test
    public void testGetterWhenTargetIsTypedIndexedRelativeObjectProperty() throws Exception
    {
        final DocumentModelBridge dmb = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                // this will fail if defaults fail, not very well isolated
                allowing(classResolver).resolve("Classes.Class", new DocumentReference("xwiki", "Main", "WebHome"));
                will(returnValue(new DocumentReference("xwiki", "Classes", "Class")));
                oneOf(dabMock).getProperty(new DocumentReference("xwiki", "Main", "WebHome"),
                    new DocumentReference("xwiki", "Classes", "Class"), 3, "property");
                will(returnValue("defcontent"));
                oneOf(dabMock).getTranslatedDocumentInstance(new DocumentReference("xwiki", "Main", "WebHome"));
                will(returnValue(dmb));
                oneOf(dmb).getSyntax();
                will(returnValue(new Syntax(SyntaxType.XWIKI,"2.0")));
            }
        });

        String reference = "OBJECT_PROPERTY://Classes.Class[3].property";
        assertEquals("defcontent", ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", ioTargetService.getSourceSyntax(reference));
    }
}
