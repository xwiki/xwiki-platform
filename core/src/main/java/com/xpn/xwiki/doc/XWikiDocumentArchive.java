package com.xpn.xwiki.doc;

import com.xpn.xwiki.XWikiException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.filters.StringInputStream;
import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.util.ToString;

public class XWikiDocumentArchive {
    private static final Log log = LogFactory.getLog(XWikiDocumentArchive.class);

    private long id;
    private Archive archive;

    public XWikiDocumentArchive() {
    }

    public XWikiDocumentArchive(long id) {
        setId(id);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Archive getRCSArchive() {
        return archive;
    }

    public void setRCSArchive(Archive archive) {
        this.archive = archive;
    }

    public String getArchive() throws XWikiException {
        if (archive == null)
            return "";
        else {
            StringBuffer buffer = new StringBuffer();
            archive.toString(buffer);
            return buffer.toString();
        }
    }

    public void setArchive(String text) throws XWikiException {
        try {
            if ((text!=null)&&(!text.trim().equals(""))) {
                StringInputStream is = new StringInputStream(text);
                archive = new Archive("", is);
            } else
            if (text == null){
                Object[] lines = new Object[1];
                lines[0] = "";
                archive = new Archive(lines, "", "1.0");
            }
            else
            {
                Object[] lines = ToString.stringToArray(text);
                archive = new Archive(lines, "", "1.0");
            }
        }
        catch (Exception e) {
            Object[] args = { "" };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_ARCHIVEFORMAT,
                    "Exception while manipulating the archive for doc {0}", e, args);
        }
    }

    public void updateArchive(String docname, String text) throws XWikiException {

        // JRCS used the user.name System property to set the author of a change. However JRCS
        // has a bug when the user name has a space in the name
        // (see http://www.suigeneris.org/issues/browse/JRCS-22). The workaround is to set the
        // user.name System property to some user without a space in its name. In addition
        // we're not using that information anywhere so it won't matter. When JRCS bug is fixed
        // remove this hack.

        // Saving the property in case some other part of the code or some dependent framework
        // needs it.
        String originalUsername = System.getProperty("user.name");

        System.setProperty("user.name", "xwiki");

        try {
            Object[] lines = ToString.stringToArray(text);
            if (archive != null)
                archive.addRevision(lines, "");
            else
                archive = new Archive(lines, docname, "1.0");
        }
        catch (Exception e) {
            Object[] args = { docname };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_ARCHIVEFORMAT,
                    "Exception while manipulating the archive for doc {0}", e, args);
        } finally {
            // Restore the user name to its original value
            System.setProperty("user.name", originalUsername);
        }

    }

    public Object clone() {
        XWikiDocumentArchive docarchive = null;
        try {
            docarchive = (XWikiDocumentArchive) getClass().newInstance();
        } catch (Exception e) {
            // This should not happen
        }

        docarchive.setId(getId());
        docarchive.setRCSArchive(getRCSArchive());
        return docarchive;
    }


    public boolean equals(Object object) {
        XWikiDocumentArchive doc = (XWikiDocumentArchive) object;
        if (getId()!=doc.getId())
            return false;

        try {
            if (!getArchive().equals(doc.getArchive()))
                return false;
        } catch (XWikiException e) {
            return false;
        }

        return true;
    }

    public void resetArchive(String docname, String text, String version) throws XWikiException {
        Object[] lines = ToString.stringToArray(text);
        archive = new Archive(lines, docname, version);
    }

    /**
     * {@inheritDoc}
     * @see Object#toString() 
     */
    public String toString()
    {
        return "id = [" + getId() + "], archive = ["
            + (getRCSArchive() == null ? "null" : getRCSArchive().toString()) + "]"; 
    }
}
