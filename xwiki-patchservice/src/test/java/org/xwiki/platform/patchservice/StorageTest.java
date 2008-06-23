package org.xwiki.platform.patchservice;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.jmock.Mock;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.Patch;
import org.xwiki.platform.patchservice.api.RWOperation;
import org.xwiki.platform.patchservice.api.RWPatch;
import org.xwiki.platform.patchservice.api.RWPatchId;
import org.xwiki.platform.patchservice.impl.LogicalTimeImpl;
import org.xwiki.platform.patchservice.impl.OperationFactoryImpl;
import org.xwiki.platform.patchservice.impl.PatchIdImpl;
import org.xwiki.platform.patchservice.impl.PatchImpl;
import org.xwiki.platform.patchservice.impl.PositionImpl;
import org.xwiki.platform.patchservice.storage.PatchStorage;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.test.AbstractXWikiComponentTestCase;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiServletContext;

public class StorageTest extends AbstractXWikiComponentTestCase
{
    private XWikiContext context;

    private Mock mockXWiki;

    private Mock mockEngineContext;

    private PatchStorage storage;

    private XWikiHibernateStore store;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        this.context = new XWikiContext();
        this.mockXWiki =
            mock(XWiki.class, new Class[] {XWikiConfig.class, XWikiContext.class}, new Object[] {new XWikiConfig(),
            this.context});
        this.mockXWiki.stubs().method("Param").withAnyArguments().will(returnValue(null));
        this.mockXWiki.stubs().method("isMySQL").will(returnValue(false));
        this.mockXWiki.stubs().method("isVirtualMode").will(returnValue(false));
        this.mockXWiki.stubs().method("getPlugin").will(returnValue(null));
        this.context.setWiki((XWiki) this.mockXWiki.proxy());
        this.mockEngineContext =
            mock(XWikiServletContext.class, new Class[] {ServletContext.class}, new Object[] {null});
        this.mockEngineContext.stubs().method("getResource").withAnyArguments().will(
            returnValue(getClass().getResource("/hibernate.cfg.xml")));
        this.context.setEngineContext((XWikiEngineContext) this.mockEngineContext.proxy());
        // TODO This should be "xwiki", and XWikiHibernateBaseStore should transform it to "PUBLIC"
        this.context.setDatabase("public");
        this.store = new XWikiHibernateStore(this.context);
        this.store.checkHibernate(this.context);
        this.mockXWiki.stubs().method("getNotCacheStore").will(returnValue(this.store));

        this.storage = new PatchStorage(this.context);
    }

    private void populate() throws XWikiException
    {
        try {
            this.store.beginTransaction(this.context);
            Session s = this.store.getSession(this.context);
            Connection c = s.connection();
            c.createStatement().execute("DELETE FROM xwikipatches");
            c.createStatement().execute(
                getQuery(Patch.DATE_FORMAT.parse("2008-01-01T12:00:00+0100"), "XWiki.Document", "www.host.org"));
            c.createStatement().execute(
                getQuery(Patch.DATE_FORMAT.parse("2008-01-02T12:00:00+0100"), "XWiki.Document", "www.host.org"));
            c.createStatement().execute(
                getQuery(Patch.DATE_FORMAT.parse("2008-01-02T12:01:00+0100"), "XWiki.Document", "www.anotherhost.org"));
            c.createStatement().execute(
                getQuery(Patch.DATE_FORMAT.parse("2008-01-02T12:00:00+0100"), "XWiki.OtherDocument", "www.host.org"));
            c.createStatement().execute(
                getQuery(Patch.DATE_FORMAT.parse("2008-01-02T12:00:00+0200"), "XWiki.OtherDocument",
                    "www.anotherhost.org"));
            this.store.endTransaction(this.context, true);
        } catch (HibernateException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getQuery(Date d, String doc, String host)
    {
        return "INSERT INTO xwikipatches(XWP_DOCID, XWP_TIME, XWP_CONTENT, XWP_HOSTID) VALUES ('" + doc + "', '"
            + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d)
            + "', '<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"
            + "<patch description=\"\" version=\"1.0\"><id doc=\"" + doc + "\" host=\"" + host + "\" time=\""
            + Patch.DATE_FORMAT.format(d) + "\"><logicalTime/></id>" + "<operation type=\"content-insert\">"
            + "<text>lorem ipsum</text><position column=\"0\" row=\"0\"/></operation></patch>', '" + host + "')";
    }

    public void testStore() throws XWikiException
    {
        RWPatch p = new PatchImpl();
        RWPatchId pid = new PatchIdImpl();
        pid.setDocumentId("XWiki.Document");
        pid.setHostId("www.sample.host");
        pid.setTime(new Date());
        pid.setLogicalTime(new LogicalTimeImpl());
        p.setId(pid);
        RWOperation o = OperationFactoryImpl.getInstance().newOperation(Operation.TYPE_CONTENT_INSERT);
        o.insert("asd", new PositionImpl());
        p.addOperation(o);
        this.storage.storePatch(p);
    }

    public void testLoadAllPatches() throws XWikiException
    {
        populate();
        List<Patch> storedPatches = this.storage.loadAllPatches();
        int i = 0;
        for (Iterator<Patch> it = storedPatches.iterator(); it.hasNext(); ++i) {
            assertTrue(it.next() instanceof Patch);
        }
        assertEquals(5, i);
    }

    public void testLoadAllPatchesForDocument() throws XWikiException
    {
        populate();
        List<Patch> storedPatches = this.storage.loadAllDocumentPatches("XWiki.Document");
        int i = 0;
        for (Iterator<Patch> it = storedPatches.iterator(); it.hasNext(); ++i) {
            assertTrue(it.next() instanceof Patch);
        }
        assertEquals(3, i);

        storedPatches = this.storage.loadAllDocumentPatches("XWiki.OtherDocument");
        i = 0;
        for (Iterator<Patch> it = storedPatches.iterator(); it.hasNext(); ++i) {
            assertTrue(it.next() instanceof Patch);
        }
        assertEquals(2, i);
    }

    public void testLoadAllDocumentPatchesSince() throws XWikiException, ParseException
    {
        populate();
        PatchIdImpl id = new PatchIdImpl();
        id.setDocumentId("XWiki.Document");
        id.setHostId("www.host.org");
        id.setTime(Patch.DATE_FORMAT.parse("2008-01-01T11:00:00-0400"));
        List<Patch> storedPatches = this.storage.loadAllDocumentPatchesSince(id);
        int i = 0;
        for (Iterator<Patch> it = storedPatches.iterator(); it.hasNext(); ++i) {
            assertTrue(it.next() instanceof Patch);
        }
        assertEquals(2, i);

        id.setTime(Patch.DATE_FORMAT.parse("2009-01-01T11:00:00-0400"));
        storedPatches = this.storage.loadAllDocumentPatchesSince(id);
        i = 0;
        for (Iterator<Patch> it = storedPatches.iterator(); it.hasNext(); ++i) {
            assertTrue(it.next() instanceof Patch);
        }
        assertEquals(0, i);
    }

    public void testLoadAllPatchesSince() throws XWikiException, ParseException
    {
        populate();
        PatchIdImpl id = new PatchIdImpl();
        id.setDocumentId("XWiki.Document");
        id.setHostId("www.host.org");
        id.setTime(Patch.DATE_FORMAT.parse("2008-01-01T11:00:00-0400"));
        List<Patch> storedPatches = this.storage.loadAllPatchesSince(id);
        int i = 0;
        for (Iterator<Patch> it = storedPatches.iterator(); it.hasNext(); ++i) {
            assertTrue(it.next() instanceof Patch);
        }
        assertEquals(4, i);

        id.setTime(Patch.DATE_FORMAT.parse("2009-01-01T11:00:00-0400"));
        storedPatches = this.storage.loadAllDocumentPatchesSince(id);
        i = 0;
        for (Iterator<Patch> it = storedPatches.iterator(); it.hasNext(); ++i) {
            assertTrue(it.next() instanceof Patch);
        }
        assertEquals(0, i);
    }
}
