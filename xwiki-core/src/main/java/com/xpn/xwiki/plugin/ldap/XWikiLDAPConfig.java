package com.xpn.xwiki.plugin.ldap;

import java.security.Provider;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
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
     * Mapping fields separator.
     */
    public static final String DEFAULT_SEPARATOR = ",";

    /**
     * LDAP properties names suffix in xwiki.cfg.
     */
    public static final String CFG_LDAP_SUFFIX = "xwiki.authentication.ldap.";

    /**
     * LDAP port property name in xwiki.cfg.
     */
    public static final String CFG_LDAP_PORT = CFG_LDAP_SUFFIX + "port";

    /**
     * LDAP properties names suffix in XWikiPreferences.
     */
    public static final String PREF_LDAP_SUFFIX = "ldap_";

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
    public static final String USERMAPPING_SEP = DEFAULT_SEPARATOR;

    /**
     * Character user to link XWiki field name and LDAP field name in user mappings property.
     */
    public static final String USERMAPPING_XWIKI_LDAP_LINK = "=";

    /**
     * Different LDAP implementations groups classes name.
     */
    public static final Set<String> DEFAULT_GROUP_CLASSES = new HashSet<String>();

    /**
     * Different LDAP implementations groups member property name.
     */
    public static final Set<String> DEFAULT_GROUP_MEMBERFIELDS = new HashSet<String>();
    
    /**
     * Logging tool.
     */
    private static final Log LOG = LogFactory.getLog(XWikiLDAPConfig.class);

    /**
     * The default secure provider to use for SSL.
     */
    private static final String DEFAULT_SECUREPROVIDER = "com.sun.net.ssl.internal.ssl.Provider";

    static {
        DEFAULT_GROUP_CLASSES.add("group".toLowerCase());
        DEFAULT_GROUP_CLASSES.add("groupOfNames".toLowerCase());
        DEFAULT_GROUP_CLASSES.add("groupOfUniqueNames".toLowerCase());
        DEFAULT_GROUP_CLASSES.add("dynamicGroup".toLowerCase());
        DEFAULT_GROUP_CLASSES.add("dynamicGroupAux".toLowerCase());
        DEFAULT_GROUP_CLASSES.add("groupWiseDistributionList".toLowerCase());

        DEFAULT_GROUP_MEMBERFIELDS.add("member".toLowerCase());
        DEFAULT_GROUP_MEMBERFIELDS.add("uniqueMember".toLowerCase());
    }

    /**
     * Unique instance of {@link XWikiLDAPConfig}.
     */
    private static XWikiLDAPConfig instance;

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
                param = context.getWiki().Param(cfgName);
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
     * @return the of the LDAP groups classes.
     */
    public Collection<String> getGroupClasses(XWikiContext context)
    {
        String param = getLDAPParam("ldap_group_classes", null, context);

        Collection<String> set;

        if (param != null) {
            String[] table = param.split(DEFAULT_SEPARATOR);

            set = new HashSet<String>();
            CollectionUtils.addAll(set, table);
        } else {
            set = DEFAULT_GROUP_CLASSES;
        }

        return set;
    }

    /**
     * @param context the XWiki context.
     * @return the names of the fields for members of groups.
     */
    public Collection<String> getGroupMemberFields(XWikiContext context)
    {
        String param = getLDAPParam("ldap_group_memberfields", null, context);

        Collection<String> set;

        if (param != null) {
            String[] table = param.split(DEFAULT_SEPARATOR);

            set = new HashSet<String>();
            CollectionUtils.addAll(set, table);
        } else {
            set = DEFAULT_GROUP_MEMBERFIELDS;
        }

        return set;
    }

    /**
     * @param context the XWiki context.
     * @return the secure provider to use for SSL.
     * @throws XWikiLDAPException error when trying to instantiate secure provider.
     */
    public Provider getSecureProvider(XWikiContext context) throws XWikiLDAPException
    {
        Provider provider;

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String className =
            getLDAPParam("ldap_ssl.secure_provider", DEFAULT_SECUREPROVIDER, context);

        try {
            provider = (java.security.Provider) cl.loadClass(className).newInstance();
        } catch (Exception e) {
            throw new XWikiLDAPException("Fail to load secure ssl provider.", e);
        }

        return provider;
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
            port = context.getWiki().getXWikiPreferenceAsInt(PREF_LDAP_PORT, context);
        } catch (Exception e) {
            port = (int) context.getWiki().ParamAsLong(CFG_LDAP_PORT, 0);
        }

        return port;
    }

    /**
     * Get mapping between XWiki groups names and LDAP groups names.
     * 
     * @param context the XWiki context.
     * @return the mapping between XWiki users and LDAP users.
     */
    public Map<String, Set<String>> getGroupMappings(XWikiContext context)
    {
        Map<String, Set<String>> groupMappings = new HashMap<String, Set<String>>();

        String param = getLDAPParam("ldap_group_mapping", "", context);

        if (param.trim().length() > 0) {
            String[] mappingTable = param.split("\\|");

            for (int i = 0; i < mappingTable.length; ++i) {
                String mapping = mappingTable[i].trim();

                int splitIndex = mapping.indexOf('=');

                if (splitIndex < 1) {
                    LOG.error("Error parsing ldap_group_mapping attribute: " + mapping);
                } else {
                    String xwikigroup = mapping.substring(0, splitIndex);
                    String ldapgroup = mapping.substring(splitIndex + 1);

                    Set<String> xwikigroups = groupMappings.get(ldapgroup);

                    if (xwikigroups == null) {
                        xwikigroups = new HashSet<String>();
                        groupMappings.put(ldapgroup, xwikigroups);
                    }

                    xwikigroups.add(xwikigroup);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Groupmapping found: " + xwikigroup + " " + ldapgroup);
                    }
                }
            }
        }

        return groupMappings;
    }

    /**
     * Get mapping between XWiki users attributes and LDAP users attributes.
     * 
     * @param attrListToFill the list to fill with extracted LDAP fields to use in LDAP search.
     * @param context the XWiki context.
     * @return the mapping between XWiki groups and LDAP groups.
     */
    public Map<String, String> getUserMappings(List<String> attrListToFill, XWikiContext context)
    {
        Map<String, String> userMappings = new HashMap<String, String>();

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

                    userMappings.put(ldapattr, xwikiattr);

                    if (attrListToFill != null) {
                        attrListToFill.add(ldapattr);
                    }
                } else {
                    LOG.error("Error parsing ldap_fields_mapping attribute in xwiki.cfg: "
                        + fields[j]);
                }
            }
        }

        return userMappings;
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
