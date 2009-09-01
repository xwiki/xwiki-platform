package org.xwiki.rendering.macro.script;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.xwiki.bridge.AttachmentNameFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentNameFactory;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;

public class MockSetup
{
    public Mockery mockery;
    
    public DocumentAccessBridge bridge;
    
    public AttachmentNameFactory attachmentNameFactory;
    
    public DocumentNameFactory documentNameFactory;

    public MockSetup(ComponentManager componentManager) throws Exception
    {
        mockery = new Mockery();

        // Document Access Bridge Mock setup
        bridge = mockery.mock(DocumentAccessBridge.class);
        mockery.checking(new Expectations() {{
            allowing(bridge).hasProgrammingRights(); will(returnValue(true));
        }});

        DefaultComponentDescriptor<DocumentAccessBridge> descriptorDAB =
            new DefaultComponentDescriptor<DocumentAccessBridge>();
        descriptorDAB.setRole(DocumentAccessBridge.class);
        componentManager.registerComponent(descriptorDAB, bridge);

        // Use a mock for the AttachmentName Factory
        attachmentNameFactory = mockery.mock(AttachmentNameFactory.class);
        DefaultComponentDescriptor<AttachmentNameFactory> descriptorANF =
            new DefaultComponentDescriptor<AttachmentNameFactory>();
        descriptorANF.setRole(AttachmentNameFactory.class);
        componentManager.registerComponent(descriptorANF, attachmentNameFactory);

        // Use a mock for the DocumentName Factory
        documentNameFactory = mockery.mock(DocumentNameFactory.class);
        DefaultComponentDescriptor<DocumentNameFactory> descriptorDNF =
            new DefaultComponentDescriptor<DocumentNameFactory>();
        descriptorDNF.setRole(DocumentNameFactory.class);
        descriptorDNF.setRoleHint("current");
        componentManager.registerComponent(descriptorDNF, documentNameFactory);        
    }
}
