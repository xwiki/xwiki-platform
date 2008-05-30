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
import org.jmock.cglib.MockObjectTestCase;
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
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiServletContext;

public class StorageTest extends MockObjectTestCase
{
    private XWikiContext context;

    private Mock mockXWiki;

    private Mock mockEngineContext;

    private PatchStorage storage;

    private XWikiHibernateStore store;

    protected void setUp() throws XWikiException
    {
        context = new XWikiContext();
        this.mockXWiki =
            mock(XWiki.class, new Class[] {XWikiConfig.class, XWikiContext.class}, new Object[] {
            new XWikiConfig(), this.context});
        this.mockXWiki.stubs().method("Param").withAnyArguments().will(returnValue(null));
        this.mockXWiki.stubs().method("isMySQL").will(returnValue(false));
        this.mockXWiki.stubs().method("isVirtualMode").will(returnValue(false));
        this.mockXWiki.stubs().method("getPlugin").will(returnValue(null));
        this.context.setWiki((XWiki) this.mockXWiki.proxy());
        this.mockEngineContext =
            mock(XWikiServletContext.class, new Class[] {ServletContext.class},
                new Object[] {null});
        this.mockEngineContext.stubs().method("getResource").withAnyArguments().will(
            returnValue(getClass().getResource("/hibernate.cfg.xml")));
        context.setEngineContext((XWikiEngineContext) mockEngineContext.proxy());
        // TODO This should be "xwiki", and XWikiHibernateBaseStore should transform it to "PUBLIC"
        context.setDatabase("public");
        store = new XWikiHibernateStore(context);
        store.checkHibernate(context);
        this.mockXWiki.stubs().method("getNotCacheStore").will(returnValue(store));

        storage = new PatchStorage(context);
    }

    private void populate() throws XWikiException
    {
        try {
            store.beginTransaction(context);
            Session s = store.getSession(context);
            Connection c = s.connection();
            c.createStatement().execute("DELETE FROM xwikipatches");
            c.createStatement().execute(
                getQuery(Patch.DATE_FORMAT.parse("2008-01-01T12:00:00+0100"),
                    "XWiki.Document", "www.host.org"));
            c.createStatement().execute(
                getQuery(Patch.DATE_FORMAT.parse("2008-01-02T12:00:00+0100"),
                    "XWiki.Document", "www.host.org"));
            c.createStatement().execute(
                getQuery(Patch.DATE_FORMAT.parse("2008-01-02T12:01:00+0100"),
                    "XWiki.Document", "www.anotherhost.org"));
            c.createStatement().execute(
                getQuery(Patch.DATE_FORMAT.parse("2008-01-02T12:00:00+0100"),
                    "XWiki.OtherDocument", "www.host.org"));
            c.createStatement().execute(
                getQuery(Patch.DATE_FORMAT.parse("2008-01-02T12:00:00+0200"),
                    "XWiki.OtherDocument", "www.anotherhost.org"));
            store.endTransaction(context, true);
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
        return "INSERT INTO xwikipatches(XWP_DOCID, XWP_TIME, XWP_CONTENT, XWP_HOSTID) VALUES ('"
            + doc
            + "', '"
            + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d)
            + "', '<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"
            + "<patch description=\"\" version=\"1.0\"><id doc=\""
            + doc
            + "\" host=\""
            + host
            + "\" time=\""
            + Patch.DATE_FORMAT.format(d)
            + "\"><logicalTime/></id>"
            + "<operation type=\"content-insert\">"
            + "<text>lorem ipsum</text><position column=\"0\" row=\"0\"/></operation></patch>', '"
            + host + "')";
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
        RWOperation o =
            OperationFactoryImpl.getInstance().newOperation(Operation.TYPE_CONTENT_INSERT);
        o.insert("asd", new PositionImpl());
        p.addOperation(o);
        storage.storePatch(p);
    }

    public void testLoadAllPatches() throws XWikiException
    {
        populate();
        List storedPatches = storage.loadAllPatches();
        int i = 0;
        for (Iterator it = storedPatches.iterator(); it.hasNext(); ++i) {
            assertTrue(it.next() instanceof Patch);
        }
        assertEquals(5, i);
    }

    public void testLoadAllPatchesForDocument() throws XWikiException
    {
        populate();
        List storedPatches = storage.loadAllDocumentPatches("XWiki.Document");
        int i = 0;
        for (Iterator it = storedPatches.iterator(); it.hasNext(); ++i) {
            assertTrue(it.next() instanceof Patch);
        }
        assertEquals(3, i);

        storedPatches = storage.loadAllDocumentPatches("XWiki.OtherDocument");
        i = 0;
        for (Iterator it = storedPatches.iterator(); it.hasNext(); ++i) {
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
        List storedPatches = storage.loadAllDocumentPatchesSince(id);
        int i = 0;
        for (Iterator it = storedPatches.iterator(); it.hasNext(); ++i) {
            assertTrue(it.next() instanceof Patch);
        }
        assertEquals(2, i);

        id.setTime(Patch.DATE_FORMAT.parse("2009-01-01T11:00:00-0400"));
        storedPatches = storage.loadAllDocumentPatchesSince(id);
        i = 0;
        for (Iterator it = storedPatches.iterator(); it.hasNext(); ++i) {
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
        List storedPatches = storage.loadAllPatchesSince(id);
        int i = 0;
        for (Iterator it = storedPatches.iterator(); it.hasNext(); ++i) {
            assertTrue(it.next() instanceof Patch);
        }
        assertEquals(4, i);

        id.setTime(Patch.DATE_FORMAT.parse("2009-01-01T11:00:00-0400"));
        storedPatches = storage.loadAllDocumentPatchesSince(id);
        i = 0;
        for (Iterator it = storedPatches.iterator(); it.hasNext(); ++i) {
            assertTrue(it.next() instanceof Patch);
        }
        assertEquals(0, i);
    }
}
