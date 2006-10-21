/*
 * Copyright 2005 Jens Krämer, All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * 
 * Created on 10.04.2005
 */
package net.jkraemer.xwiki.plugins.emailnotify;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

/**
 * @author <a href="mailto:jk@jkraemer.net">Jens Kraemer </a>
 */
public class Config
{
    private static final Logger LOG                       = Logger.getLogger (Config.class);
    private static final String THREAD_DECLARATION_PREFIX = "emailnotificationthread.";

    private final ResourceBundle    properties;
    private final List          threadConfigList;

    public Config (String filename)
    {
        this.threadConfigList = new ArrayList ();
//        this.properties = new Properties ();
            properties = ResourceBundle.getBundle(filename); //.load (this.getClass ().getResourceAsStream (filename));
        init ();
    }

    /**
     * 
     */
    protected void init ()
    {
        for (Enumeration en = properties.getKeys(); en.hasMoreElements ();)
        {
            String key = (String) en.nextElement ();
            if (key.startsWith (THREAD_DECLARATION_PREFIX))
            {
                addThread (key.substring (THREAD_DECLARATION_PREFIX.length ()), properties.getString(key));
            }
        }
    }

    /**
     * @param string
     */
    protected void addThread (String threadName, String crontab)
    {
        threadConfigList.add (new NotifierThreadConfig (threadName, crontab));
    }

    /**
     * @return List of Thread names read from configuration file
     */
    public List getThreadConfig ()
    {
        return threadConfigList;
    }
}

class NotifierThreadConfig
{
    private final String name;
    private final String crontab;

    public NotifierThreadConfig (String name, String crontab)
    {
        this.name = name;
        this.crontab = crontab;
    }

    /**
     * @return
     */
    public String getCrontab ()
    {
        return crontab;
    }

    /**
     * @return
     */
    public String getName ()
    {
        return name;
    }
}
