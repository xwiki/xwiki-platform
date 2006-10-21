/*
 * Copyright 2005 Jens Krämer
 * 
 * Created on 10.04.2005
 * Version: $Id$
 */
package net.jkraemer.xwiki.plugins.emailnotify;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import junit.framework.TestCase;

/**
 * @author <a href="jk@jkraemer.net">Jens Kraemer </a>
 */
public class ConfigTest extends TestCase
{
    Config out;

    Map    configData;

    public void testGetThreadConfig ()
    {
        List cfg = out.getThreadConfig ();
        assertNotNull (cfg);
        assertEquals (3, cfg.size ());
        for (Iterator iter = cfg.iterator (); iter.hasNext ();)
        {
            NotifierThreadConfig element = (NotifierThreadConfig) iter.next ();
            assertEquals (configData.get (element.getName ()), element.getCrontab ());
        }
    }

    protected void setUp () throws Exception
    {
        super.setUp ();
        configData = new HashMap ();
        configData.put ("hourly", "0 0 0/1 * * ?");
        configData.put ("daily", "0 0 9 * * ?");
        configData.put ("weekly", "0 0 9 ? * MON");
        assertNotNull(ResourceBundle.getBundle("configtest"));
        out = new Config ("configtest");
    }

    protected void tearDown () throws Exception
    {
        out = null;
        super.tearDown ();
    }

}
