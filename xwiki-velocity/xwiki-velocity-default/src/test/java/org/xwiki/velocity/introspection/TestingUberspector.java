package org.xwiki.velocity.introspection;

import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.VelMethod;
import org.apache.velocity.util.introspection.VelPropertyGet;

public class TestingUberspector extends AbstractChainableUberspector
{
    public static int methodCalls = 0;

    public static int getterCalls = 0;

    public VelMethod getMethod(Object obj, String methodName, Object[] args, Info i)
        throws Exception
    {
        ++methodCalls;
        return super.getMethod(obj, methodName, args, i);
    }

    public VelPropertyGet getPropertyGet(Object obj, String identifier, Info i) throws Exception
    {
        ++getterCalls;
        return super.getPropertyGet(obj, identifier, i);
    }
}
