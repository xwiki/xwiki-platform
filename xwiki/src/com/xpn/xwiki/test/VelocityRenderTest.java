package com.xpn.xwiki.test;

import junit.framework.TestCase;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiCacheInterface;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.render.XWikiWikiBaseRenderer;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import org.apache.velocity.app.Velocity;
import net.sf.hibernate.HibernateException;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 8 mars 2004
 * Time: 09:19:35
 * To change this template use File | Settings | File Templates.
 */
public class VelocityRenderTest extends TestCase {

        private XWiki xwiki;
        private XWikiContext context;

        public XWikiHibernateStore getHibStore() {
            XWikiStoreInterface store = xwiki.getStore();
            if (store instanceof XWikiCacheInterface)
                return (XWikiHibernateStore)((XWikiCacheInterface)store).getStore();
            else
                return (XWikiHibernateStore) store;
        }

        public XWikiStoreInterface getStore() {
            return xwiki.getStore();
        }

        public void setUp() throws Exception {
            context = new XWikiContext();
            xwiki = new XWiki("./xwiki.cfg", context);
            context.setWiki(xwiki);
            Velocity.init("velocity.properties");
        }

        public void tearDown() throws HibernateException {
            getHibStore().shutdownHibernate(context);
            xwiki = null;
            context = null;
            System.gc();
        }




        public void testVelocityRenderer() throws XWikiException {
            XWikiRenderer wikibase = new XWikiVelocityRenderer();

            RenderTest.renderTest(wikibase, "#set( $foo = \"Velocity\" )\nHello $foo World!",
                    "Hello Velocity World!", true, context);
            RenderTest.renderTest(wikibase, "Test: #include( \"view.pm\" )",
                    "Test: #include", false, context);
            RenderTest.renderTest(wikibase, "Test: #INCLUDE( \"view.pm\" )",
                    "Test: #INCLUDE", false, context);

            RenderTest.renderTest(wikibase, "#set( $count = 0 )\n#if ( $count == 1)\nHello1\n#else\nHello2\n#end\n",
                    "Hello2", true, context);
        }

        public void testRenderingEngine() throws XWikiException {
            XWikiRenderingEngine wikiengine = new XWikiRenderingEngine(xwiki);
            RenderTest.renderTest(wikiengine, "#set( $count = 0 )\n#if ( $count == 1)\n *Hello1* \n#else\n *Hello2* \n#end\n",
                    "Hello2", false, context);
        }


        public void testInclude(String text, String result) throws XWikiException {
            XWikiRenderingEngine wikiengine = xwiki.getRenderingEngine();
            XWikiStoreInterface store = getStore();

            XWikiSimpleDoc doc1 = new XWikiSimpleDoc("Test", "WebHome");
            doc1.setContent("This is the topic name: $doc.name");
            doc1.setAuthor(Utils.author);
            doc1.setParent(Utils.parent);
            store.saveXWikiDoc(doc1, context);

            XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Test", "IncludeTest");
            context.put("doc", doc2);
            RenderTest.renderTest(wikiengine, text, result, false, context);
        }

        public void testIncludeTopic() throws XWikiException {
            testInclude("#includeTopic(\"Test.WebHome\")", "This is the topic name");
            testInclude("#includeTopic(\"Test.WebHome\")", "WebHome");
        }

        public void testIncludeForm() throws XWikiException {
            testInclude( "#includeForm(\"Test.WebHome\")", "IncludeTest");
        }

        public void testIncludeFromOtherDatabase() throws XWikiException {
            testInclude( "#includeTopic(\"xwiki:XWiki.XWikiUsers\")", "XWiki Users");
        }

    }
