package com.xpn.xwiki.user.impl.LDAP;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Helper to manager LDAP profile XClass and XObject.
 * 
 * @version $Id$
 */
public class LDAPProfileXClass
{
    public static final String LDAP_XCLASS = "XWiki.LDAPProfileClass";

    public static final String LDAP_XFIELD_DN = "dn";

    public static final String LDAP_XFIELDPN_DN = "LDAP DN";

    public static final String LDAP_XFIELD_UID = "uid";

    public static final String LDAP_XFIELDPN_UID = "LDAP user unique identifier";

    /**
     * The XWiki space where users are stored.
     */
    private static final String XWIKI_USER_SPACE = "XWiki";

    /**
     * Logging tool.
     */
    private static final Log LOG = LogFactory.getLog(XWikiLDAPAuthServiceImpl.class);

    private XWikiContext context;

    private BaseClass ldapClass;

    public LDAPProfileXClass(XWikiContext context) throws XWikiException
    {
        this.context = context;

        XWikiDocument ldapClassDoc = context.getWiki().getDocument(LDAP_XCLASS, context);

        this.ldapClass = ldapClassDoc.getxWikiClass();

        boolean needsUpdate = this.ldapClass.addTextField(LDAP_XFIELD_DN, LDAP_XFIELDPN_DN, 80);
        needsUpdate |= this.ldapClass.addTextField(LDAP_XFIELD_UID, LDAP_XFIELDPN_UID, 80);

        if (needsUpdate) {
            context.getWiki().saveDocument(ldapClassDoc, "Update LDAP user profile class", context);
        }
    }

    /**
     * @param userDocument the user profile page.
     * @return the dn store in the user profile. Null if it can't find any or if it's empty.
     */
    public String getDn(XWikiDocument userDocument)
    {
        BaseObject ldapObject = userDocument.getObject(this.ldapClass.getName());

        return ldapObject == null ? null : getDn(ldapObject);
    }

    /**
     * @param ldapObject the ldap profile object.
     * @return the dn store in the user profile. Null if it can't find any or if it's empty.
     */
    public String getDn(BaseObject ldapObject)
    {
        String dn = ldapObject.getStringValue(LDAP_XFIELD_DN);

        return dn.length() == 0 ? null : dn;
    }

    /**
     * @param userDocument the user profile page.
     * @return the uid store in the user profile. Null if it can't find any or if it's empty.
     */
    public String getUid(XWikiDocument userDocument)
    {
        BaseObject ldapObject = userDocument.getObject(this.ldapClass.getName());

        return ldapObject == null ? null : getUid(ldapObject);
    }

    /**
     * @param ldapObject the ldap profile object.
     * @return the uid store in the user profile. Null if it can't find any or if it's empty.
     */
    public String getUid(BaseObject ldapObject)
    {
        String uid = ldapObject.getStringValue(LDAP_XFIELD_UID);

        return uid.length() == 0 ? null : uid;
    }

    /**
     * Update or create LDAP profile of an existing user profile with provided LDAP user informations.
     * 
     * @param xwikiUserName the name of the XWiki user to update LDAP profile.
     * @param dn the dn to store in the LDAP profile.
     * @param uid the uid to store in the LDAP profile.
     * @throws XWikiException error when storing information in user profile.
     */
    public void updateLDAPObject(String xwikiUserName, String dn, String uid) throws XWikiException
    {
        XWikiDocument userDocument =
            this.context.getWiki().getDocument(XWIKI_USER_SPACE + "." + xwikiUserName, this.context);

        boolean needsUpdate = updateLDAPObject(userDocument, dn, uid);

        if (needsUpdate) {
            this.context.getWiki().saveDocument(userDocument, "Update LDAP user profile", this.context);
        }
    }

    /**
     * Update LDAP profile object with provided LDAP user informations.
     * 
     * @param userDocument the user profile page to update.
     * @param dn the dn to store in the LDAP profile.
     * @param uid the uid to store in the LDAP profile.
     * @return true if modifications has been made to provided user profile, false otherwise.
     */
    public boolean updateLDAPObject(XWikiDocument userDocument, String dn, String uid)
    {
        BaseObject ldapObject = userDocument.getObject(this.ldapClass.getName(), true, this.context);

        Map<String, String> map = new HashMap<String, String>();

        boolean needsUpdate = false;

        String objDn = getDn(ldapObject);
        if (!dn.equalsIgnoreCase(objDn)) {
            map.put(LDAP_XFIELD_DN, dn);
            needsUpdate = true;
        }

        String objUid = getUid(ldapObject);
        if (!uid.equalsIgnoreCase(objUid)) {
            map.put(LDAP_XFIELD_UID, uid);
            needsUpdate = true;
        }

        if (needsUpdate) {
            this.ldapClass.fromMap(map, ldapObject);
        }

        return needsUpdate;
    }

    /**
     * Search the XWiki storage for a existing user profile with provided LDAP user uid stored.
     * <p>
     * If more than one profile is found the first one in returned and an error is logged.
     * 
     * @param uid the LDAP unique id.
     * @return the user profile containing LDAP uid.
     */
    public XWikiDocument searchDocumentByUid(String uid)
    {
        XWikiDocument doc = null;

        List<XWikiDocument> documentList;
        try {
            // Search for uid in database
            String sql =
                ", BaseObject as obj, StringProperty as prop where doc.fullName=obj.name and obj.className=? and obj.id=prop.id.id and prop.name=? and lower(prop.value)=?";

            documentList =
                this.context.getWiki().getStore().searchDocuments(sql, false, false, false, 0, 0,
                    Arrays.asList(ldapClass.getName(), LDAP_XFIELD_UID, uid.toLowerCase()), this.context);
        } catch (XWikiException e) {
            LOG.error("Fail to search for document containing ldap uid [" + uid + "]", e);

            documentList = Collections.emptyList();
        }

        if (documentList.size() > 1) {
            LOG.error("There is more than one user profile for LDAP uid [" + uid + "]");
        }

        if (!documentList.isEmpty()) {
            doc = documentList.get(0);
        }

        return doc;
    }

    /**
     * Search the LDAP user DN stored in an existing user profile with provided LDAP user uid stored.
     * <p>
     * If more than one profile is found the first one in returned and an error is logged.
     * 
     * @param uid the LDAP unique id.
     * @return the found LDAP DN, null if it can't find one or if it's empty.
     */
    public String searchDn(String uid)
    {
        XWikiDocument document = searchDocumentByUid(uid);

        return document == null ? null : getDn(document);
    }
}
