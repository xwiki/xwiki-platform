/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 23 nov. 2003
 * Time: 23:58:27
 */
package com.xpn.xwiki.doc;

import org.apache.commons.jrcs.rcs.*;
import com.xpn.xwiki.*;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.*;
import java.util.Date;
import java.util.ArrayList;
import java.util.Map;
import java.io.FileNotFoundException;

public interface XWikiDocInterface {
    public long getId();
    public void setId(long id);
    public String getName();
    public void setName(String name);
    public String getWeb();
    public void setWeb(String web);
    public String getFullName();
    public void setFullName(String name);
    public Version getRCSVersion();
    public void setRCSVersion(Version version);
    public String getVersion();
    public void setVersion(String version);
    public String getParent();
    public void setParent(String parent);
    public String getContent();
    public void setContent(String content);
    public String getFormat();
    public void setFormat(String format);
    public String getAuthor();
    public void setAuthor(String author);
    public Date getDate();
    public void setDate(Date date);
    public String getMeta();
    public void setMeta(String meta);
    public void appendMeta(String meta);

    public boolean isMetaDataDirty();
    public boolean isContentDirty();

    public void incrementVersion();
    public Archive getRCSArchive();
    public void setRCSArchive(Archive archive);
    public String getArchive() throws XWikiException;
    public void setArchive(String text) throws FileNotFoundException, ParseException, XWikiException;
    public void updateArchive(String text) throws XWikiException;
    public boolean isMostRecent();
    public void setMostRecent(boolean mostRecent);

    public boolean isNew();
    public void setNew(boolean aNew);

    public String getTemplate();
    public void setTemplate(String template);

    public XWikiStoreInterface getStore();
    public void setStore(XWikiStoreInterface store);

    public BaseClass getxWikiClass();
    public void setxWikiClass(BaseClass xWikiClass);
    public BaseObject getxWikiObject();
    public void setxWikiObject(BaseObject xWikiObject);

    public Map getxWikiObjects();
    public void setxWikiObjects(Map xWikiObject);
    public void createNewObject(String classname, XWikiContext context) throws XWikiException;

    public XWikiDocCacheInterface getDocCache();
    public void setDocCache(XWikiDocCacheInterface doccache);

    public String getActionUrl(String action, XWikiContext context);

    public void mergexWikiClass(XWikiDocInterface templatedoc);
    public void mergexWikiObject(XWikiDocInterface templatedoc);
    public void mergexWikiObjects(XWikiDocInterface templatedoc);

}
