/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created by
 * Date: 30 nov. 2003
 * Time: 10:46:55
 */
package com.xpn.xwiki.render;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import org.apache.tools.ant.taskdefs.Execute;
import org.perl.inline.java.InlineJavaPerlCaller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;

public class XWikiPerlPluginRenderer implements XWikiRenderer {


    private String perlpath;
    private String pluginspath;
    private String classpath;
    private String javaserverport;
    private int debug = 0;
    private int javaserverdebug;
    private int launchcounter = 0;

    // Only way to get the right perlCaller it by the perl port
    private static Hashtable perlCallers = new Hashtable();
    private static Hashtable perlThreads = new Hashtable();
    private static Hashtable perlProcesses = new Hashtable();


    public XWikiPerlPluginRenderer(String perlpath, String pluginspath, String classpath, String port, int debug) throws XWikiException {
        setPerlpath(perlpath);
        setPluginspath(pluginspath);
        setJavaserverport(port);
        setClasspath(classpath);
        setDebug(debug);
    }

    public XWikiPerlPluginThread getPerlThread() {
        return (XWikiPerlPluginThread) perlThreads.get(javaserverport);
    }

    public void finalize() {
        stopServers();
    }
    public String getPerlpath() {
        return perlpath;
    }

    public void setPerlpath(String perlpath) {
        this.perlpath = perlpath;
    }

    public String getPluginspath() {
        return pluginspath;
    }

    public void setPluginspath(String pluginspath) {
        this.pluginspath = pluginspath;
    }

    public String getJavaserverport() {
        return javaserverport;
    }

    public void setJavaserverport(String javaserverport) {
        this.javaserverport = javaserverport;
    }

    public int getJavaserverdebug() {
        return javaserverdebug;
    }

    public void setJavaserverdebug(int javaserverdebug) {
        this.javaserverdebug = javaserverdebug;
    }


    public Process getPerlProcess() {
        return (Process) perlProcesses.get(javaserverport);
    }

    public void setPerlThread(XWikiPerlPluginThread thread) {
        perlThreads.put(javaserverport, thread);
    }

    public void setPerlProcess(Process process) {
        perlProcesses.put(javaserverport, process);
    }

    public InlineJavaPerlCaller getPerlCaller() throws XWikiException {
        InlineJavaPerlCaller perlCaller;
        while (true) {
            perlCaller = (InlineJavaPerlCaller) perlCallers.get(javaserverport);
            if (perlCaller!=null) {
                System.err.println("perlCaller found");
                break;
            }
            else {
                try {
                    int exitvalue = getPerlProcess().exitValue();
                    // The perl process is not running anymore.. let's stop the loop
                    throw new XWikiException(XWikiException.MODULE_XWIKI_PERLPLUGINS, XWikiException.ERROR_XWIKI_PERLPLUGIN_START,
                            "Error launching perl engine");
                }
                catch (IllegalThreadStateException e1) {
                    // Perl process is still running.. everything is fine
                    // Let's wait for the perlCaller to be assigned
                    try {
                        Thread.yield();
                    }
                    catch (Exception e) {
                    }
                }
            }
        }
        return perlCaller;
    }

    public static void setPerlCaller(String port, InlineJavaPerlCaller perlCaller) {
        XWikiPerlPluginRenderer.perlCallers.put(port, perlCaller);
    }



    public void startJavaPerlServer() throws XWikiException {
        try {
            if (getPerlThread()==null) {
                XWikiPerlPluginThread perlThread = new XWikiPerlPluginThread(this, javaserverport, getJavaserverdebug());
                setPerlThread(perlThread);
                perlThread.start();
                while (perlThread.checkServer()==false) {
                    Thread.yield();
                }
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PERLPLUGINS, XWikiException.ERROR_XWIKI_PERLPLUGIN_PERLSERVER_EXCEPTION,
                    "Exception while launching perl engine", e);
        }
    }


    public void startPerlEngine() throws XWikiException {

        Process perlProcess = getPerlProcess();
        if (perlProcess !=null) {
            try {
                perlProcess.exitValue();
                // TODO: Log info that the perl process is not running anymore
                System.err.println("Perl Process not running anymore.. restarting");
                try { readPerlOutput(); } catch (Exception e2) {
                };
            } catch (Exception e) {
                // Process is still running...
                return;
            }
        }

        if (launchcounter>10)
            throw new XWikiException(XWikiException.MODULE_XWIKI_PERLPLUGINS, XWikiException.ERROR_XWIKI_PERLPLUGIN_START_EXCEPTION,
                    "Exception while launching perl engine");


        try {
            launchcounter++;
            String[] command = { getPerlpath(), "perlplugins.pl", getClasspath(), getJavaserverport(),  "" + getDebug() };
            File dir = new File(getPluginspath());
            perlProcess = Execute.launch(null, command, null, dir, true);
            setPerlProcess(perlProcess);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PERLPLUGINS, XWikiException.ERROR_XWIKI_PERLPLUGIN_START_EXCEPTION,
                    "Exception while launching perl engine", e);
        }
        if (perlProcess==null)
            throw new XWikiException(XWikiException.MODULE_XWIKI_PERLPLUGINS, XWikiException.ERROR_XWIKI_PERLPLUGIN_START,
                    "Error launching perl engine");

        try {
            int exitvalue = perlProcess.exitValue();
            readPerlOutput();

            throw new XWikiException(XWikiException.MODULE_XWIKI_PERLPLUGINS, XWikiException.ERROR_XWIKI_PERLPLUGIN_START,
                    "Error launching perl engine");
        } catch (Exception e) {
            // The process has not terminated, which is what we want..
        }
    }
    public void startServers() throws XWikiException {
        startJavaPerlServer();
        startPerlEngine();
    }
    public void stopServers() {
        stopJavaPerlServer();
        stopJavaPerlEngine();
    }

    public void stopJavaPerlServer() {
        try {
            getPerlCaller().CallPerl("main", "stop", new Object[] {});
        }
        catch (Exception e2) {}

        for (int i=0;i<10;i++)  {
            try {
                int exitvalue = getPerlProcess().exitValue();
                readPerlOutput();
            } catch (Exception e) {
                try { Thread.yield(); }
                catch (Exception e2) {}
            }
        }
    }




    private void stopJavaPerlEngine()  {
        Thread perlThread = getPerlThread();
        if (perlThread!=null) {
            perlThreads.remove(javaserverport);
            perlThread.interrupt();
        }
    }


    public void readPerlOutput() throws IOException {
        String line;
        BufferedReader reader;

        reader = new BufferedReader(new InputStreamReader(getPerlProcess().getInputStream()));
        while ((line=reader.readLine()) != null)
            System.out.println(line);

        reader = new BufferedReader(new InputStreamReader(getPerlProcess().getErrorStream()));
        while ((line=reader.readLine()) != null)
            System.err.println(line);

    }




    public String render(String content, XWikiDocument contentdoc, XWikiDocument doc,  XWikiContext context) {
        try {
            startServers();
            String result = (String) getPerlCaller().CallPerl("main", "render",  new Object [] { content, doc });
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return content;
        }
    }

    public void flushCache() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getClasspath() {
        return classpath;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public int getDebug() {
        return debug;
    }

    public void setDebug(int debug) {
        this.debug = debug;
    }


}
