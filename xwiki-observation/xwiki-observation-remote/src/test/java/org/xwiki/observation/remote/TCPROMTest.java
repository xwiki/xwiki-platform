package org.xwiki.observation.remote;

import java.util.Arrays;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.test.AbstractROMTestCase;
import org.xwiki.observation.remote.test.TestEvent;

public class TCPROMTest extends AbstractROMTestCase
{
    private Mockery context = new Mockery();

    private ObservationManager manager1;

    private ObservationManager manager2;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        getComponentManager1().lookup(RemoteObservationManager.class).startChannel("tcp1");
        getComponentManager2().lookup(RemoteObservationManager.class).startChannel("tcp2");

        this.manager1 = getComponentManager1().lookup(ObservationManager.class);
        this.manager2 = getComponentManager2().lookup(ObservationManager.class);
    }

    @After
    public void tearDown() throws Exception
    {
        this.context.assertIsSatisfied();
        
        getComponentManager1().lookup(RemoteObservationManager.class).stopChannel("tcp1");
        getComponentManager2().lookup(RemoteObservationManager.class).stopChannel("tcp2");
    }

    /**
     * Validate sharing a simple Serializable event between two instances of {@link RemoteObservationManager}.
     */
    @Test
    public void testSerializableEvent() throws InterruptedException
    {
        final EventListener listener = this.context.mock(EventListener.class);
        final TestEvent event = new TestEvent();

        this.context.checking(new Expectations() {{
                allowing(listener).getName(); will(returnValue("mylistener"));
                allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
                oneOf(listener).onEvent(with(equal(event)), with(equal("some source")), with(equal("some data")));
            }});

        this.manager2.addListener(listener);

        this.manager1.notify(event, "some source", "some data");

        // Make sure JGroups has enough time to send the message
        Thread.sleep(1000);
    }
}