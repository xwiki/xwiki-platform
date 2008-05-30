package org.xwiki.platform.patchservice.storage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.xwiki.platform.patchservice.api.Patch;
import org.xwiki.platform.patchservice.api.PatchId;
import org.xwiki.platform.patchservice.impl.PatchImpl;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;

public class PatchStorage
{
    public static final String MAPPING_FILENAME = "/Patch.hbm.xml";

    private SessionFactory factory;

    private static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public PatchStorage(XWikiContext context) throws XWikiException
    {
        init(context);
    }

    private void init(XWikiContext context) throws XWikiException
    {
        System.err.println("storage initializing");
        XWikiHibernateStore storage = ((XWikiHibernateStore) context.getWiki().getNotCacheStore());
        Configuration config = storage.getConfiguration();
        try {
            // Make sure the schema is updated to include the Patch mapping
            config.setProperty("hibernate.hbm2ddl.auto", "update");
            factory =
                config.addXML(
                    IOUtils.toString(this.getClass().getResourceAsStream(MAPPING_FILENAME)))
                    .buildSessionFactory();
        } catch (MappingException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_INVALID_MAPPING,
                "Invalid Patch mapping file",
                e);
        } catch (HibernateException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Unknown error initializing the Patch storage",
                e);
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_INVALID_MAPPING,
                "Cannot load Patch mapping file",
                e);
        }
        System.err.println("storage initialized");
    }

    public boolean storePatch(Patch p)
    {
        try {
            Session s = factory.openSession();
            Transaction t = s.beginTransaction();
            s.save(p);
            t.commit();
            s.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Patch loadPatch(PatchId id)
    {
        List<Patch> patches =
            loadPatches("select patch.content from PatchImpl patch where patch.id.documentId = '"
                + id.getDocumentId() + "' and patch.id.time = '"
                + DATE_FORMAT.format(id.getTime() + "'"));
        if (patches.size() > 0) {
            return patches.get(0);
        }
        return null;
    }

    public List<Patch> loadAllPatches()
    {
        return loadPatches("select patch.content from PatchImpl patch");
    }

    public List<Patch> loadAllDocumentPatches(String documentId)
    {
        return loadPatches("select patch.content from PatchImpl patch where patch.id.documentId = '"
            + documentId + "'");
    }

    public List<Patch> loadAllDocumentPatchesSince(PatchId id)
    {
        return loadPatches("select patch.content from PatchImpl patch where patch.id.documentId = '"
            + StringEscapeUtils.escapeSql(id.getDocumentId())
            + "' and patch.id.time > '"
            + DATE_FORMAT.format(id.getTime()) + "'");
    }

    public List<Patch> loadAllPatchesSince(PatchId id)
    {
        return loadPatches("select patch.content from PatchImpl patch where patch.id.time > '"
            + DATE_FORMAT.format(id.getTime()) + "'");
    }

    @SuppressWarnings("unchecked")
    protected List<Patch> loadPatches(String query)
    {
        Session s = factory.openSession();
        List<String> contents = s.createQuery(query).list();
        List<Patch> patches = new ArrayList<Patch>();
        for (String patchXml : contents) {
            PatchImpl patch = new PatchImpl();
            patch.setContent(patchXml);
            patches.add(patch);
        }
        return patches;
    }
}
