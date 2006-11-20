/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author ludovic
 * @author sdumitriu
 */


package com.xpn.xwiki.test;

import java.net.URL;

import junit.framework.TestCase;

import org.apache.velocity.app.Velocity;
import org.hibernate.HibernateException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.XWikiServletURLFactory;

public class MacroMappingRenderTest extends TestCase {

    private XWiki xwiki;
    private XWikiContext context;

    public XWikiHibernateStore getHibStore() {
        XWikiStoreInterface store = xwiki.getStore();
        if (store instanceof XWikiCacheStoreInterface)
            return (XWikiHibernateStore)((XWikiCacheStoreInterface)store).getStore();
        else
            return (XWikiHibernateStore) store;
    }

    public XWikiStoreInterface getStore() {
        return xwiki.getStore();
    }

    public void setUp() throws Exception {
        context = new XWikiContext();
        XWikiConfig config = new XWikiConfig("./xwiki.cfg");
        xwiki = new XWiki(config, context, null, false);
        xwiki.setDatabase("xwikitest");
        context.setDatabase("xwikitest");
        context.setWiki(xwiki);
        context.setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
        xwiki.getConfig().setProperty("xwiki.render.macromapping", "1");
        Velocity.init("velocity.properties");
    }

    public void tearDown() throws HibernateException {
        getHibStore().shutdownHibernate(context);
        xwiki = null;
        context = null;
        System.gc();
    }

    public void testMacroMappingsVelocitySimple() throws XWikiException {
        XWikiStoreInterface store = getStore();

        Utils.setStringValue("XWiki.XWikiPreferences", "macros_velocity", "XWiki.VelocityMacros", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "macros_groovy", "XWiki.GroovyMacros", context);
        Utils.setLargeStringValue("XWiki.XWikiPreferences", "macros_mapping", "hello=velocity:hello", context);

        // Reset the Rendering Engine
        xwiki.resetRenderingEngine(context);
        XWikiRenderingEngine wikiengine = xwiki.getRenderingEngine();

        XWikiDocument doc1 = Utils.createDoc("Test.MacroMappingVelocity", "{hello}", store, context);
        XWikiDocument doc2 = Utils.createDoc("XWiki.VelocityMacros", "#macro(hello)\nHello World\n#end\n", store, context);

        AbstractRenderTest.renderTest(wikiengine, doc1, "Hello World", false, context);
    }

    public void testMacroMappingsVelocityParams() throws XWikiException {
        XWikiStoreInterface store = getStore();

        Utils.setStringValue("XWiki.XWikiPreferences", "macros_velocity", "XWiki.VelocityMacros", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "macros_groovy", "XWiki.GroovyMacros", context);
        Utils.setLargeStringValue("XWiki.XWikiPreferences", "macros_mapping", "hello=velocity:hello2:name", context);

        // Reset the Rendering Engine
        xwiki.resetRenderingEngine(context);
        XWikiRenderingEngine wikiengine = xwiki.getRenderingEngine();

        XWikiDocument doc1 = Utils.createDoc("Test.MacroMappingVelocity", "{hello:ludovic}", store, context);
        XWikiDocument doc2 = Utils.createDoc("XWiki.VelocityMacros", "#macro(hello2 $name)\nHello $name\n#end\n", store, context);

        AbstractRenderTest.renderTest(wikiengine, doc1, "Hello ludovic", false, context);
    }

    public void testMacroMappingsVelocityParamsWithQuotes() throws XWikiException {
        XWikiStoreInterface store = getStore();

        Utils.setStringValue("XWiki.XWikiPreferences", "macros_velocity", "XWiki.VelocityMacros", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "macros_groovy", "XWiki.GroovyMacros", context);
        Utils.setLargeStringValue("XWiki.XWikiPreferences", "macros_mapping", "hello=velocity:hello3:name", context);

        // Reset the Rendering Engine
        xwiki.resetRenderingEngine(context);
        XWikiRenderingEngine wikiengine = xwiki.getRenderingEngine();

        XWikiDocument doc1 = Utils.createDoc("Test.MacroMappingVelocity", "{hello:I like \"sports\"}", store, context);
        XWikiDocument doc2 = Utils.createDoc("XWiki.VelocityMacros", "#macro(hello3 $name)\nHello $name\n#end\n", store, context);

        AbstractRenderTest.renderTest(wikiengine, doc1, "Hello I like", false, context);
        AbstractRenderTest.renderTest(wikiengine, doc1, "sports", false, context);
    }

    public void testMacroMappingsVelocityTwoParams() throws XWikiException {
        XWikiStoreInterface store = getStore();

        Utils.setStringValue("XWiki.XWikiPreferences", "macros_velocity", "XWiki.VelocityMacros", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "macros_groovy", "XWiki.GroovyMacros", context);
        Utils.setLargeStringValue("XWiki.XWikiPreferences", "macros_mapping", "hello=velocity:hello4:first_name,last_name", context);

        // Reset the Rendering Engine
        xwiki.resetRenderingEngine(context);
        XWikiRenderingEngine wikiengine = xwiki.getRenderingEngine();

        XWikiDocument doc1 = Utils.createDoc("Test.MacroMappingVelocity", "{hello:ludovic|dubost}", store, context);
        XWikiDocument doc2 = Utils.createDoc("XWiki.VelocityMacros", "#macro(hello4 $first_name $last_name)\nHello $first_name $last_name\n#end\n", store, context);

        AbstractRenderTest.renderTest(wikiengine, doc1, "Hello ludovic dubost", false, context);
    }

    public void testMacroMappingsVelocityTwoParamsNamed() throws XWikiException {
        XWikiStoreInterface store = getStore();

        Utils.setStringValue("XWiki.XWikiPreferences", "macros_velocity", "XWiki.VelocityMacros", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "macros_groovy", "XWiki.GroovyMacros", context);
        Utils.setLargeStringValue("XWiki.XWikiPreferences", "macros_mapping", "hello=velocity:hello5:first_name,last_name", context);

        // Reset the Rendering Engine
        xwiki.resetRenderingEngine(context);
        XWikiRenderingEngine wikiengine = xwiki.getRenderingEngine();

        XWikiDocument doc1 = Utils.createDoc("Test.MacroMappingVelocity", "{hello:first_name=ludovic|last_name=dubost}", store, context);
        XWikiDocument doc2 = Utils.createDoc("XWiki.VelocityMacros", "#macro(hello5 $first_name $last_name)\nHello $first_name $last_name\n#end\n", store, context);

        AbstractRenderTest.renderTest(wikiengine, doc1, "Hello ludovic dubost", false, context);
    }

    public void testMacroMappingsVelocityTwoParamsNamedInverted() throws XWikiException {
        XWikiStoreInterface store = getStore();

        Utils.setStringValue("XWiki.XWikiPreferences", "macros_velocity", "XWiki.VelocityMacros", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "macros_groovy", "XWiki.GroovyMacros", context);
        Utils.setLargeStringValue("XWiki.XWikiPreferences", "macros_mapping", "hello=velocity:hello6:first_name,last_name", context);

        // Reset the Rendering Engine
        xwiki.resetRenderingEngine(context);
        XWikiRenderingEngine wikiengine = xwiki.getRenderingEngine();

        XWikiDocument doc1 = Utils.createDoc("Test.MacroMappingVelocity", "{hello:last_name=dubost|first_name=ludovic}", store, context);
        XWikiDocument doc2 = Utils.createDoc("XWiki.VelocityMacros", "#macro(hello6 $first_name $last_name)\nHello $first_name $last_name\n#end\n", store, context);

        AbstractRenderTest.renderTest(wikiengine, doc1, "Hello ludovic dubost", false, context);
    }


    public void testMacroMappingsVelocityMultiline() throws XWikiException {
        XWikiStoreInterface store = getStore();

        Utils.setStringValue("XWiki.XWikiPreferences", "macros_velocity", "XWiki.VelocityMacros", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "macros_groovy", "XWiki.GroovyMacros", context);
        Utils.setLargeStringValue("XWiki.XWikiPreferences", "macros_mapping", "hello=velocity:hello7:first_name,last_name:content", context);

        // Reset the Rendering Engine
        xwiki.resetRenderingEngine(context);
        XWikiRenderingEngine wikiengine = xwiki.getRenderingEngine();

        XWikiDocument doc1 = Utils.createDoc("Test.MacroMappingVelocity", "{hello:last_name=dubost|first_name=ludovic}This is my profile{hello}", store, context);
        XWikiDocument doc2 = Utils.createDoc("XWiki.VelocityMacros", "#macro(hello7 $first_name $last_name $content)\nHello $first_name $last_name\nProfile: $content\n#end", store, context);

        AbstractRenderTest.renderTest(wikiengine, doc1, "Hello ludovic dubost\nProfile: This is my profile", false, context);
    }


    public void testMacroMappingsGroovySimple() throws XWikiException {
        XWikiStoreInterface store = getStore();

        Utils.setStringValue("XWiki.XWikiPreferences", "macros_velocity", "XWiki.VelocityMacros", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "macros_groovy", "XWiki.GroovyMacros", context);
        Utils.setLargeStringValue("XWiki.XWikiPreferences", "macros_mapping", "hello=groovy:hellogroovy", context);

        // Reset the Rendering Engine
        xwiki.resetRenderingEngine(context);
        XWikiRenderingEngine wikiengine = xwiki.getRenderingEngine();

        XWikiDocument doc1 = Utils.createDoc("Test.MacroMappingGroovy", "{hello}", store, context);
        XWikiDocument doc2 = Utils.createDoc("XWiki.GroovyMacros", "<% def hellogroovy() {\n println \"Hello World from Groovy\" \n}\n %>\n", store, context);

        AbstractRenderTest.renderTest(wikiengine, doc1, "Hello World from Groovy", false, context);
    }


}
