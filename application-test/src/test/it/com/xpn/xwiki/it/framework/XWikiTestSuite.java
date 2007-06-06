package com.xpn.xwiki.it.framework;

import junit.framework.TestSuite;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class XWikiTestSuite extends TestSuite
{
    public XWikiTestSuite(String name)
    {
        super(name);
    }

    public void addTestSuite(Class testClass, Class skinExecutorClass)
    {
        if (!AbstractXWikiTestCase.class.isAssignableFrom(testClass)) {
            throw new RuntimeException("Test case must extend ["
                + AbstractXWikiTestCase.class.getName() + "]");
        }

         try {
             addSkinExecutorToSuite(testClass, skinExecutorClass);
         } catch (Exception e) {
            throw new RuntimeException("Failed to add skin executor class ["
                + skinExecutorClass.getName() + "] for test case class ["
                + testClass.getName() + "]", e);
         }
    }

    private void addSkinExecutorToSuite(Class testClass, Class skinExecutorClass)
        throws Exception
    {
        // Find all methods starting with "test" and add them
        Method[] methods = testClass.getMethods();
        for (int i = 0; i < methods.length; i++)
        {
            if (methods[i].getName().startsWith("test"))
            {
                addSkinExecutorToTest(methods[i].getName(), testClass, skinExecutorClass);
            }
        }
    }

    private void addSkinExecutorToTest(String testName, Class testClass, Class skinExecutorClass)
        throws Exception
    {
        Constructor constructor = testClass.getConstructor(null);
        AbstractXWikiTestCase test = (AbstractXWikiTestCase) constructor.newInstance(null);

        Constructor skinExecutorConstructor = skinExecutorClass.getConstructor(
            new Class[] { AbstractXWikiTestCase.class });
        SkinExecutor skinExecutor = (SkinExecutor) skinExecutorConstructor.newInstance(
            new Object[] { (AbstractXWikiTestCase) test });

        test.setSkinExecutor(skinExecutor);
        test.setName(testName);

        addTest(test);
    }
}
