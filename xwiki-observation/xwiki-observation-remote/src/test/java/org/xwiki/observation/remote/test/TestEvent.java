package org.xwiki.observation.remote.test;

import java.io.Serializable;

import org.junit.Ignore;
import org.xwiki.observation.event.Event;

@Ignore
public class TestEvent implements Event, Serializable
{
    public boolean matches(Object otherEvent)
    {
        return true;
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof TestEvent;
    }
}
