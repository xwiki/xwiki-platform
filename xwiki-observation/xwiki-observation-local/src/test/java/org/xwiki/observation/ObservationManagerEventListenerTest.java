package org.xwiki.observation;

import java.util.Arrays;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.internal.StackingComponentEventManager;
import org.xwiki.observation.event.Event;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link ObservationManager}.
 * 
 * @version $Id$
 */
public class ObservationManagerEventListenerTest extends AbstractComponentTestCase
{
    private ObservationManager manager;
    
    private EventListener eventListenerMock;
    
    private Event eventMock;
    
    private DefaultComponentDescriptor<EventListener> componentDescriptor;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.manager = getComponentManager().lookup(ObservationManager.class);
        StackingComponentEventManager componentEventManager = new StackingComponentEventManager();
        componentEventManager.shouldStack(false);
        componentEventManager.setObservationManager(this.manager);
        getComponentManager().setComponentEventManager(componentEventManager);

        this.eventListenerMock = getMockery().mock(EventListener.class);
        this.eventMock = getMockery().mock(Event.class);

        this.componentDescriptor = new DefaultComponentDescriptor<EventListener>();
        this.componentDescriptor.setImplementation(eventListenerMock.getClass());
        this.componentDescriptor.setRole(EventListener.class);
        this.componentDescriptor.setRoleHint("mylistener");
        
        getMockery().checking(new Expectations() {{
            allowing(eventMock).matches(with(same(eventMock))); will(returnValue(true));
            allowing(eventListenerMock).getName(); will(returnValue("mylistener"));
            allowing(eventListenerMock).getEvents(); will(returnValue(Arrays.asList(eventMock)));
        }});
    }

    @Test
    public void testNewListenerComponent() throws Exception
    {
        getComponentManager().registerComponent(this.componentDescriptor, this.eventListenerMock);
        
        Assert.assertSame(this.eventListenerMock, this.manager.getListener("mylistener"));
    }
    
    @Test
    public void testRemovedListenerComponent() throws Exception
    {
        getComponentManager().registerComponent(this.componentDescriptor, this.eventListenerMock);        
        getComponentManager().unregisterComponent(this.componentDescriptor.getRole(), this.componentDescriptor.getRoleHint());
        
        Assert.assertNull(this.manager.getListener("mylistener"));
    }
}
