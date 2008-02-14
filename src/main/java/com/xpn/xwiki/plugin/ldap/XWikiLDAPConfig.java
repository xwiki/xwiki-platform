package com.xpn.xwiki.plugin.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;

/**
 * Access to LDAP configurations.
 * 
 * @version $Id: $
 */
public final class XWikiLDAPConfig
{
    /**
     * LDAP properties names suffix in xwiki.cfg.
     */
    public static final String CFG_LDAP_SUFFIX = "ldap_";

    /**
     * LDAP port property name in xwiki.cfg.
     */
    public static final String CFG_LDAP_PORT = CFG_LDAP_SUFFIX + "port";

    /**
     * LDAP group mapping properties names suffix in xwiki.cfg.
     */
    public static final String CFG_LDAP_GROUPMAPPING_SUFFIX = CFG_LDAP_PORT + "group_mapping_";

    /**
     * LDAP properties names suffix in XWikiPreferences.
     */
    public static final String PREF_LDAP_SUFFIX = "xwiki.authentication.ldap.";

    /**
     * LDAP port property name in XWikiPreferences.
     */
    public static final String PREF_LDAP_PORT = "ldap_port";

    /**
     * LDAP port property name in XWikiPreferences.
     */
    public static final String PREF_LDAP_UID = "ldap_UID_attr";

    /**
     * Mapping fields separator.
     */
    public static final String USERMAPPING_SEP = ",";

    /**
     * Character user to link XWiki field name and LDAP field name in user mappings property.
     */
    public static final String USERMAPPING_XWIKI_LDAP_LINK = "=";

    /**
     * Logging tool.
     */
    private static final Log LOG = LogFactory.getLog(XWikiLDAPConfig.class);

    /**
     * Unique instance of {@link XWikiLDAPConfig}.
     */
    private static XWikiLDAPConfig instance;

    /**
     * The mapping between XWiki users and LDAP users.
     */
    private Map groupMappings;

    /**
     * The mapping between XWiki users attributes and LDAP users attributes.
     */
    private Map userMappings;

    /**
     * The name of users attributes.
     */
    private List userAttributeList = new ArrayList();

    /**
     * Protected constructor. Use {@link #getInstance()}.
     */
    private XWikiLDAPConfig()
    {
        
    }

    /**
     * @return unique instance of {@link XWikiLDAPConfig}.
     */
    public static XWikiLDAPConfig getInstance()
    {
        if (instance == null) {
            instance = new XWikiLDAPConfig();
        }

        return instance;
    }

    /**
     * First try to retrieve value from XWiki Preferences and then from xwiki.cfg Syntax ldap_*name*
     * (for XWiki Preferences) will be changed to ldap.*name* for xwiki.cfg.
     * 
     * @param prefName the name of the property in XWikiPreferences.
     * @param cfgName the name of the property in xwiki.cfg.
     * @param def default value.
     * @param context the XWiki context.
     * @return the value of the property.
     */
    public String getLDAPParam(String prefName, String cfgName, String def, XWikiContext context)
    {
        String param = def;

        try {
            param = context.getWiki().getXWikiPreference(prefName, context);
        } catch (Exception e) {
            // ignore
        }

        if (param == null || "".equals(param)) {
            try {
                param =
                    context.getWiki().Param(cfgName);
            } catch (Exception e) {
                // ignore
            }
        }

        if (param == null) {
            param = def;
        }

        return param;
    }
    
    /**
     * First try to retrieve value from XWiki Preferences and then from xwiki.cfg Syntax ldap_*name*
     * (for XWiki Preferences) will be changed to ldap.*name* for xwiki.cfg.
     * 
     * @param name the name of the property in XWikiPreferences.
     * @param def default value.
     * @param context the XWiki context.
     * @return the value of the property.
     */
    public String getLDAPParam(String name, String def, XWikiContext context)
    {
        return getLDAPParam(name, name.replace(PREF_LDAP_SUFFIX, CFG_LDAP_SUFFIX), def, context);
    }

    /**
     * @param context the XWiki context.
     * @return true if LDAP is enabled.
     */
    public boolean isLDAPEnabled(XWikiContext context)
    {
        String param = getLDAPParam("ldap", "xwiki.authentication.ldap", "0", context);

        return param != null && param.equals("1");
    }

    /**
     * Get LDAP port from configuration.
     * 
     * @param context the XWiki context.
     * @return the LDAP port.
     */
    public int getLDAPPort(XWikiContext context)
    {
        int port;

        try {
            port = context.getWiki().getXWikiPreferenceAsInt(CFG_LDAP_PORT, context);
        } catch (Exception e) {
            port = (int) context.getWiki().ParamAsLong(PREF_LDAP_PORT, 0);
        }

        return port;
    }

    /**
     * Get mapping between XWiki groups names and LDAP groups names.
     * 
     * @param context the XWiki context.
     * @return the mapping between XWiki users and LDAP users.
     */
    public Map getGroupMappings(XWikiContext context)
    {
        if (this.groupMappings == null) {
            this.groupMappings = new HashMap();

            int pos = 1;
            String grouplistmapping =
                getLDAPParam(CFG_LDAP_GROUPMAPPING_SUFFIX + pos, "", context);
            while (grouplistmapping != null && grouplistmapping.length() > 0) {
                int splitt = grouplistmapping.indexOf('=');

                if (splitt < 1) {
                    LOG.error("Error parsing ldap_group_mapping attribute in xwiki.cfg: "
                        + grouplistmapping);
                } else {
                    String xwikigroup = grouplistmapping.substring(0, splitt);
                    String ldapgroup = grouplistmapping.substring(splitt + 1);

                    this.groupMappings.put(ldapgroup, xwikigroup);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Groupmapping found: " + xwikigroup + " " + ldapgroup);
                    }
                }

                ++pos;
                grouplistmapping =
                    getLDAPParam(CFG_LDAP_GROUPMAPPING_SUFFIX + pos, null, context);
            }
        }

        return this.groupMappings;
    }

    /**
     * Get mapping between XWiki users attributes and LDAP users attributes.
     * 
     * @param attrListToFill the list to fill with extracted LDAP fields to use in LDAP search.
     * @param context the XWiki context.
     * @return the mapping between XWiki groups and LDAP groups.
     */
    public Map getUserMappings(List attrListToFill, XWikiContext context)
    {
        if (this.userMappings == null) {
            this.userMappings = new HashMap();

            String ldapFieldMapping = getLDAPParam("ldap_fields_mapping", null, context);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Ready to create user from LDAP with fields " + ldapFieldMapping);
            }

            if (ldapFieldMapping != null && ldapFieldMapping.length() > 0) {
                String[] fields = ldapFieldMapping.split(USERMAPPING_SEP);

                for (int j = 0; j < fields.length; j++) {
                    String[] field = fields[j].split(USERMAPPING_XWIKI_LDAP_LINK);
                    if (2 == field.length) {
                        String xwikiattr = field[0].replace(" ", "");
                        String ldapattr = field[1].replace(" ", "");

                        this.userMappings.put(ldapattr, xwikiattr);

                        this.userAttributeList.add(ldapattr);
                    } else {
                        LOG.error("Error parsing ldap_fields_mapping attribute in xwiki.cfg: "
                            + fields[j]);
                    }
                }
            }
        }

        if (attrListToFill != null) {
            attrListToFill.addAll(this.userAttributeList);
        }

        return this.userMappings;
    }

    /**
     * @param context the XWiki context.
     * @return the time in ms until a entry in the cache is to expire.
     */
    public int getCacheExpiration(XWikiContext context)
    {
        try {
            return context.getWiki().getXWikiPreferenceAsInt("ldap_groupcache_expiration",
                context);
        } catch (Exception e) {
            return (int) context.getWiki().ParamAsLong(
                "xwiki.authentication.ldap.groupcache_expiration", 21800);
        }
    }
}
