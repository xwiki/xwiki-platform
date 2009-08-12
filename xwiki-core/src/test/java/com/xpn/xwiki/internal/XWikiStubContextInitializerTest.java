package com.xpn.xwiki.internal;

import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

public class XWikiStubContextInitializerTest extends AbstractBridgedXWikiComponentTestCase
{
    private ExecutionContextManager executionContextManager;

    @Override
    protected void setUp() throws Exception
    {
        // TODO Auto-generated method stub
        super.setUp();

        this.executionContextManager = getComponentManager().lookup(ExecutionContextManager.class);
    }

    public void testWithAndWithoutXWikiContext() throws ExecutionContextException, InterruptedException
    {
        XWikiContext xcontext = new XWikiContext();
        xcontext.put("key", "value");

        ExecutionContext context = new ExecutionContext();
        context.setProperty("xwikicontext", xcontext);

        this.executionContextManager.initialize(context);

        assertSame(xcontext, context.getProperty("xwikicontext"));

        Thread thread = new Thread(new Runnable()
        {
            public void run()
            {
                ExecutionContext context = new ExecutionContext();

                try {
                    executionContextManager.initialize(context);
                } catch (ExecutionContextException e) {
                    fail("Failed to initialize execution context: " + e.getStackTrace());
                }

                XWikiContext xcontext = (XWikiContext) context.getProperty("xwikicontext");
                assertNotNull(xcontext);
                assertEquals("value", xcontext.get("key"));
            }
        });

        thread.join();
    }
}
