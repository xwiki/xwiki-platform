package org.xwiki.rendering.internal.renderer;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.listener.Link;

public class DefaultLinkLabelGeneratorTest extends MockObjectTestCase
{
    private DefaultLinkLabelGenerator generator;
    
    private Mock mockModelBridge;
    
    private Mock mockAccessBridge;
    
    protected void setUp()
    {
        this.generator = new DefaultLinkLabelGenerator();
    
        Mock mockConfiguration = mock(RenderingConfiguration.class);
        mockConfiguration.stubs().method("getLinkLabelFormat").will(returnValue("[%w:%s.%p] %P (%t)"));
        this.generator.setRenderingConfiguration((RenderingConfiguration) mockConfiguration.proxy());
        
        this.mockModelBridge = mock(DocumentModelBridge.class);
        
        this.mockAccessBridge = mock(DocumentAccessBridge.class);
        this.mockAccessBridge.stubs().method("getDocumentName").will(returnValue(
            new DocumentName("xwiki", "Main", "HelloWorld")));
        this.generator.setDocumentAccessBridge((DocumentAccessBridge) mockAccessBridge.proxy());
    }

    public void testGenerate()
    {
        Link link = new Link();
        link.setReference("HelloWorld");

        this.mockModelBridge.stubs().method("getTitle").will(returnValue("My title"));
        this.mockAccessBridge.stubs().method("getDocument").will(returnValue(this.mockModelBridge.proxy()));
        assertEquals("[xwiki:Main.HelloWorld] Hello World (My title)", this.generator.generate(link));
    }
    
    public void testGenerateWhenDocumentFailsToLoad()
    {
        Link link = new Link();
        link.setReference("HelloWorld");

        this.mockAccessBridge.stubs().method("getDocument").will(throwException(new Exception("error")));

        assertEquals("HelloWorld", this.generator.generate(new Link()));
    }

    public void testGenerateWhenDocumentTitleIsNull()
    {
        Link link = new Link();
        link.setReference("HelloWorld");

        this.mockModelBridge.stubs().method("getTitle").will(returnValue(null));
        this.mockAccessBridge.stubs().method("getDocument").will(returnValue(this.mockModelBridge.proxy()));

        assertEquals("HelloWorld", this.generator.generate(new Link()));
    }
}
