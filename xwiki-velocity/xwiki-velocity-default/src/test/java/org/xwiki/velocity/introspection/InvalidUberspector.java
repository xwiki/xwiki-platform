package org.xwiki.velocity.introspection;

import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.VelMethod;

public class InvalidUberspector extends AbstractChainableUberspector
{
    public static int methodCalls = 0;

    private InvalidUberspector()
    {
        // This is private to ensure that it cannot be instatiated. Nothing to do here.
    }

    public VelMethod getMethod(Object obj, String methodName, Object[] args, Info i)
        throws Exception
    {
        ++methodCalls;
        return super.getMethod(obj, methodName, args, i);
    }
}
