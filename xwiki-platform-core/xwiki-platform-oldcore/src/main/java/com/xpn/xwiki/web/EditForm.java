/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */
package com.xpn.xwiki.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.stability.Unstable;
import org.xwiki.store.TemporaryAttachmentSessionsManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;

/**
 * An object containing the information sent in an action request to perform changes on a document.
 *
 * @version $Id$
 */
public class EditForm extends XWikiForm
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EditForm.class);

    /**
     * Format for passing xproperties references in URLs. General format:
     * {@code &lt;space&gt;.&lt;pageClass&gt;_&lt;number&gt;_<propertyName>} (e.g.
     * {@code XWiki.XWikiRights_0_member}).
     */
    private static final Pattern XPROPERTY_REFERENCE_PATTERN =
        Pattern.compile("^((?:[\\S ]+\\.)+[\\S ]+?)_([0-9]+)_(.+)$");

    /**
     * Format for passing xobjects references in URLs. General format:
     * {@code &lt;space&gt;.&lt;pageClass&gt;_<number>} (e.g.
     * {@code XWiki.XWikiRights_0}).
     */
    private static final Pattern XOBJECTS_REFERENCE_PATTERN = Pattern.compile("^((?:[\\S ]+\\.)+[\\S ]+?)_([0-9]+)$");

    private static final String OBJECTS_CLASS_DELIMITER = "_";

    // ---- Form fields -------------------------------------------------
    private String content;

    private String web;

    private String name;

    private String parent;

    private String creator;

    private String template;

    private String language;

    private String defaultLanguage;

    private String defaultTemplate;

    private String title;

    private String comment;

    private boolean isMinorEdit = false;

    private String tags;

    private boolean lockForce;

    private String syntaxId;

    private boolean convertSyntax;

    private String hidden;

    private String enforceRequiredRights;
    
    private ObjectPolicyType objectPolicy;

    private Map<String, List<Integer>> objectsToRemove;

    private Map<String, List<Integer>> objectsToAdd;

    private Map<String, SortedMap<Integer, Map<String, String[]>>> updateOrCreateMap;

    private List<String> temporaryUploadedFiles;

    @Override
    public void readRequest()
    {
        XWikiRequest request = getRequest();
        setContent(request.getParameter("content"));
        setWeb(request.getParameter("web"));
        setName(request.getParameter("name"));
        setParent(request.getParameter("parent"));
        setTemplate(request.getParameter("template"));
        setDefaultTemplate(request.getParameter("default_template"));
        setCreator(request.getParameter("creator"));
        setLanguage(request.getParameter("language"));
        setTitle(request.getParameter("title"));
        setComment(request.getParameter("comment"));
        setDefaultLanguage(request.getParameter("defaultLanguage"));
        setTags(request.getParameterValues("tags"));
        setLockForce("1".equals(request.getParameter("force")));
        setMinorEdit(request.getParameter("minorEdit") != null);
        setSyntaxId(request.getParameter("syntaxId"));
        setConvertSyntax(Boolean.valueOf(request.getParameter("convertSyntax")));
        setHidden(request.getParameter("xhidden"));
        setEnforceRequiredRights(request.getParameter("enforceRequiredRights"));
        setObjectPolicy(request.getParameter("objectPolicy"));
        setUpdateOrCreateMap(request);
        setObjectsToRemove(request.getParameterValues("deletedObjects"));
        setObjectsToAdd(request.getParameterValues("addedObjects"));
        setTemporaryUploadedFiles(request.getParameterValues("uploadedFiles"));
    }

    public void setTags(String[] parameter)
    {
        if (parameter == null) {
            this.tags = null;
            return;
        }
        StringBuilder tags = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < parameter.length; ++i) {
            if (!parameter[i].equals("")) {
                if (first) {
                    first = false;
                } else {
                    tags.append("|");
                }
                tags.append(parameter[i]);
            }
        }
        this.tags = tags.toString();
    }

    public String getTags()
    {
        return this.tags;
    }

    public String getContent()
    {
        return this.content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getWeb()
    {
        return this.web;
    }

    public void setWeb(String web)
    {
        this.web = web;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getLanguage()
    {
        return this.language;
    }

    public void setLanguage(String language)
    {
        this.language = Util.normalizeLanguage(language);
    }

    public int getObjectNumbers(String prefix)
    {
        String nb = getRequest().getParameter(prefix + "_nb");
        return NumberUtils.toInt(nb);
    }

    public Map<String, String[]> getObject(String prefix)
    {
        @SuppressWarnings("unchecked")
        Map<String, String[]> allParameters = getRequest().getParameterMap();
        Map<String, String[]> result = new HashMap<String, String[]>();
        for (String name : allParameters.keySet()) {
            if (name.startsWith(prefix + "_")) {
                String newname = name.substring(prefix.length() + 1);
                result.put(newname, allParameters.get(name));
            }
        }
        return result;
    }

    public String getParent()
    {
        return this.parent;
    }

    public void setParent(String parent)
    {
        this.parent = parent;
    }

    public String getCreator()
    {
        return this.creator;
    }

    public void setCreator(String creator)
    {
        this.creator = creator;
    }

    public String getTemplate()
    {
        return this.template;
    }

    public void setTemplate(String template)
    {
        this.template = template;
    }

    public String getDefaultTemplate()
    {
        return this.defaultTemplate;
    }

    public void setDefaultTemplate(String defaultTemplate)
    {
        this.defaultTemplate = defaultTemplate;
    }

    public String getDefaultLanguage()
    {
        return this.defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage)
    {
        this.defaultLanguage = Util.normalizeLanguage(defaultLanguage);
    }

    public String getTitle()
    {
        return this.title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getComment()
    {
        return this.comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public boolean isMinorEdit()
    {
        return this.isMinorEdit;
    }

    public void setMinorEdit(boolean isMinorEdit)
    {
        this.isMinorEdit = isMinorEdit;
    }

    public boolean isLockForce()
    {
        return this.lockForce;
    }

    public void setLockForce(boolean lockForce)
    {
        this.lockForce = lockForce;
    }

    public String getSyntaxId()
    {
        return this.syntaxId;
    }

    public void setSyntaxId(String syntaxId)
    {
        this.syntaxId = syntaxId;
    }

    /**
     * @return {@code true} if the document content and meta data should be converted to the new syntax specified on the
     *         edit form, {@code false} otherwise
     * @see #syntaxId
     * @since 12.6.3
     * @since 12.9RC1
     */
    public boolean isConvertSyntax()
    {
        return this.convertSyntax;
    }

    /**
     * Sets whether to convert the document content and meta data to the new syntax specified on the edit form.
     * 
     * @param convertSyntax {@code true} to convert the document content and meta data to the new syntax, {@code false}
     *            to only change the syntax identifier
     * @since 12.6.3
     * @since 12.9RC1
     */
    public void setConvertSyntax(boolean convertSyntax)
    {
        this.convertSyntax = convertSyntax;
    }

    public String getHidden()
    {
        return this.hidden;
    }

    public void setHidden(String hidden)
    {
        this.hidden = hidden;
    }

    /**
     * @return the enforce required rights flag, see {@link XWikiDocument#isEnforceRequiredRights()}
     * @since 16.10.0RC1
     */
    @Unstable
    public String getEnforceRequiredRights()
    {
        return this.enforceRequiredRights;
    }

    /**
     * @param enforceRequiredRights the enforce required rights flag, see {@link XWikiDocument#isEnforceRequiredRights()}
     * @since 16.10.0RC1
     */
    @Unstable
    public void setEnforceRequiredRights(String enforceRequiredRights)
    {
        this.enforceRequiredRights = enforceRequiredRights;
    }

    /**
     * Return the object policy given in the HTTP request. See {@link com.xpn.xwiki.web.ObjectPolicyType
     * ObjectPolicyType} for more information about what is an object policy.
     *
     * @return the Object Policy type
     * @since 7.0RC1
     */
    public ObjectPolicyType getObjectPolicy() {
        return this.objectPolicy;
    }

    /**
     * see {@link #getObjectPolicy}
     * 

     * @since 7.0RC1
     */
    private void setObjectPolicy(ObjectPolicyType objectPolicy)
    {
        this.objectPolicy = objectPolicy;
    }

    /**
     * see {@link #getObjectPolicy}
     *
     * @param objectPolicyName is a string converted to {@link com.xpn.xwiki.web.ObjectPolicyType ObjectPolicyType}
     * @since 7.0RC1
     */
    private void setObjectPolicy(String objectPolicyName)
    {
        this.objectPolicy = ObjectPolicyType.forName(objectPolicyName);
    }

    private void setObjectsToRemove(String[] objectsToRemove)
    {
        if (objectsToRemove == null) {
            this.objectsToRemove = Collections.emptyMap();
        } else {
            this.objectsToRemove = new HashMap<>();
            for (String objectAndId : objectsToRemove) {
                Matcher matcher = XOBJECTS_REFERENCE_PATTERN.matcher(objectAndId);
                if (matcher.matches()) {
                    String className = matcher.group(1);
                    String objectNumber = matcher.group(2);
                    try {
                        Integer objectId = Integer.parseInt(objectNumber);

                        if (!this.objectsToRemove.containsKey(className)) {
                            this.objectsToRemove.put(className, new ArrayList<>());
                        }
                        List<Integer> objectIds = this.objectsToRemove.get(className);
                        objectIds.add(objectId);
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Invalid xobject number [{}], ignoring removed xobject [{}].", objectNumber,
                            objectAndId);
                    }
                }
            }
        }
    }

    public Map<String, List<Integer>> getObjectsToRemove()
    {
        return objectsToRemove;
    }

    private void setObjectsToAdd(String[] objectsToAdd)
    {
        if (objectsToAdd == null) {
            this.objectsToAdd = Collections.emptyMap();
        } else {
            this.objectsToAdd = new HashMap<>();
            for (String objectAndId : objectsToAdd) {
                Matcher matcher = XOBJECTS_REFERENCE_PATTERN.matcher(objectAndId);
                if (matcher.matches()) {
                    String className = matcher.group(1);
                    String objectNumber = matcher.group(2);
                    try {
                        Integer objectId = Integer.parseInt(objectNumber);

                        if (!this.objectsToAdd.containsKey(className)) {
                            this.objectsToAdd.put(className, new ArrayList<>());
                        }
                        List<Integer> objectIds = this.objectsToAdd.get(className);
                        objectIds.add(objectId);
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Invalid xobject number [{}], ignoring added xobject [{}].", objectNumber,
                            objectAndId);
                    }
                }
            }
        }
    }

    private void setTemporaryUploadedFiles(String[] temporaryUploadedFiles)
    {
        if (temporaryUploadedFiles == null || temporaryUploadedFiles.length == 0) {
            this.temporaryUploadedFiles = Collections.emptyList();
        } else {
            this.temporaryUploadedFiles = Arrays.asList(temporaryUploadedFiles);
        }
    }

    /**
     * Retrieve the list of temporary uploaded files to add as attachment.
     *
     * @see TemporaryAttachmentSessionsManager
     * @return a list of filenames that should be attached.
     * @since 14.3RC1
     */
    public List<String> getTemporaryUploadedFiles()
    {
        return temporaryUploadedFiles;
    }

    public Map<String, List<Integer>> getObjectsToAdd()
    {
        return objectsToAdd;
    }

    /**
     * See {@link #getUpdateOrCreateMap()} for more information how the information are retrieved.
     */
    private void setUpdateOrCreateMap(XWikiRequest request)
    {
        if (ObjectPolicyType.UPDATE_OR_CREATE.equals(getObjectPolicy())) {
            this.updateOrCreateMap = new HashMap<>();
            @SuppressWarnings("unchecked")
            Map<String, String[]> allParameters = request.getParameterMap();
            for (Map.Entry<String, String[]> parameter : allParameters.entrySet()) {
                Matcher matcher = XPROPERTY_REFERENCE_PATTERN.matcher(parameter.getKey());
                if (!matcher.matches()) {
                    continue;
                }
                Integer classNumber;
                String className = matcher.group(1);
                String classNumberAsString = matcher.group(2);
                String classPropertyName = matcher.group(3);

                try {
                    classNumber = Integer.parseInt(classNumberAsString);
                } catch (NumberFormatException e) {
                    // If the numner isn't valid, skip the property update
                    LOGGER.warn("Invalid xobject number [{}], ignoring property update [{}].", classNumberAsString,
                        parameter.getKey());
                    continue;
                }
                SortedMap<Integer, Map<String, String[]>> objectMap = this.updateOrCreateMap.get(className);
                if (objectMap == null) {
                    objectMap = new TreeMap<>();
                    this.updateOrCreateMap.put(className, objectMap);
                }
                // Get the property from the right object #objectNumber of type 'objectName';
                // create it if they don't exist
                Map<String, String[]> object = objectMap.get(classNumber);
                if (object == null) {
                    object = new HashMap<>();
                    objectMap.put(classNumber, object);
                }
                object.put(classPropertyName, parameter.getValue());
            }
        } else {
            this.updateOrCreateMap = Collections.emptyMap();
        }
    }

    /**
     * If current objectPolicyType is {@link ObjectPolicyType#UPDATE_OR_CREATE}, retrieve a map from the request
     * parameters of the form {@code &lt;spacename&gt;.&lt;classname&gt;_&lt;number&gt;_<propertyname>'}
     * Keys of this map will be the reference {@code &lt;spacename&gt;.<classname>} to the Class
     * (for example, 'XWiki.XWikiRights'), the content is a list where each element describe property for the
     * object {@code &lt;number&gt;}. Element of the list is a map where key is {@code <propertyname>} and
     * content is the array of corresponding values.
     *
     * Example with a list of HTTP parameters:
     * <ul>
     * <li>XWiki.XWikiRights_0_users=XWiki.Admin</li>
     * <li>XWiki.XWikiRights_0_users=XWiki.Me</li>
     * <li>XWiki.XWikiRights_0_groups=XWiki.XWikiAllGroup</li>
     * <li>XWiki.XWikiRights_1_user=XWiki.Admin</li>
     * <li>XWiki.XWikiUsers_1_name=Spirou</li>
     * </ul>
     * will result in the following map <code>
     * {
     *   "XWiki.XWikiRights": {
     *     "0": {
     *       "users": ["XWiki.Admin", "XWiki.Me"],
     *       "groups": ["XWiki.XWikiAllGroup"]
     *     },
     *     "1": {
     *       "users": ["XWiki.Admin"]
     *     }
     *   ],
     *   "XWiki.XWikiUsers":
     *     "1": {
     *       "name": ["Spirou"]
     *     }
     *   ]
     * }
     * </code>
     * Note that the resulting map does not guarantee the consistency of the properties in regards with the actual
     * definition of the XClass. For example, the request could contain {@code XWiki.XWikiRights_0_foobar=value}: the
     * resulting map will always return a parameter "foobar" for XWiki.XWikiRights
     * even if the xclass does not define it or even if the xclass does not exist.
     *
     * If the current objectPolicyType is not {@link ObjectPolicyType#UPDATE_OR_CREATE}
     * it will return an empty map.
     *
     * @return a map containing ordered data or an empty map.
     */
    public Map<String, SortedMap<Integer, Map<String, String[]>>> getUpdateOrCreateMap()
    {
        return updateOrCreateMap;
    }
}
