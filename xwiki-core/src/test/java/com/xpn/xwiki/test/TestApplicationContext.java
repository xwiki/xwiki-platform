package com.xpn.xwiki.test;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.xwiki.container.ApplicationContext;

/**
 * Simple {@link ApplicationContext} implementation that uses the classloader's <code>getResource</code> and
 * <code>getResourceAsStream</code> methods to access resources. Useful for running tests without a real live container.
 * 
 * @version $Id$
 */
public class TestApplicationContext implements ApplicationContext
{
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.container.ApplicationContext#getResource(String)
     */
    public URL getResource(String resourceName) throws MalformedURLException
    {
        return getClass().getClassLoader().getResource(StringUtils.removeStart(resourceName, "/"));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.container.ApplicationContext#getResourceAsStream(String)
     */
    public InputStream getResourceAsStream(String resourceName)
    {
        return getClass().getResourceAsStream(StringUtils.removeStart(resourceName, "/"));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.container.ApplicationContext#getTemporaryDirectory()
     */
    public File getTemporaryDirectory()
    {
        try {
            // The system temporary directory is a good place for temporary test data.
            return new File(System.getProperty("java.io.tmpdir"));
        } catch (SecurityException e) {
            return new File(".");
        }
    }
}
