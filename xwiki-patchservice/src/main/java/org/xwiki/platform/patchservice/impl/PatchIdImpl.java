package org.xwiki.platform.patchservice.impl;

import java.util.Date;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.LogicalTime;
import org.xwiki.platform.patchservice.api.RWPatchId;

import com.xpn.xwiki.XWikiException;

public class PatchIdImpl implements RWPatchId
{
    public static final String NODE_NAME = "id";

    public static final String DOCID_ATTRIBUTE_NAME = "doc";

    public static final String HOSTID_ATTRIBUTE_NAME = "host";

    public static final String TIME_ATTRIBUTE_NAME = "time";

    public static final String[] FORMATS =
        new String[] {DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern()};

    private String documentId;

    private String hostId;

    private Date time;

    private LogicalTime logicalTime;

    public void setDocumentId(String documentId)
    {
        this.documentId = documentId;
    }

    public void setHostId(String hostId)
    {
        this.hostId = hostId;
    }

    public void setLogicalTime(LogicalTime logicalTime)
    {
        this.logicalTime = logicalTime;
    }

    public void setTime(Date time)
    {
        this.time = time;
    }

    public void fromXml(Element e) throws XWikiException
    {
        try {
            this.documentId = e.getAttribute(DOCID_ATTRIBUTE_NAME);
            this.hostId = e.getAttribute(HOSTID_ATTRIBUTE_NAME);
            String timeText = e.getAttribute(TIME_ATTRIBUTE_NAME);
            this.time = DateUtils.parseDate(timeText, FORMATS);
            this.logicalTime =
                new LogicalTimeImpl((Element) e.getElementsByTagName(LogicalTimeImpl.NODE_NAME)
                    .item(0));
        } catch (Exception ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_DOC_XML_PARSING,
                "Invalid Patch XML",
                ex);
        }
    }

    public String getDocumentId()
    {
        return this.documentId;
    }

    public String getHostId()
    {
        return this.hostId;
    }

    public LogicalTime getLogicalTime()
    {
        return this.logicalTime;
    }

    public Date getTime()
    {
        return this.time;
    }

    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = doc.createElement(NODE_NAME);
        xmlNode.setAttribute(DOCID_ATTRIBUTE_NAME, this.documentId);
        xmlNode.setAttribute(HOSTID_ATTRIBUTE_NAME, this.hostId);
        xmlNode.setAttribute(TIME_ATTRIBUTE_NAME, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT
            .format(this.time));
        xmlNode.appendChild(this.logicalTime.toXml(doc));
        return xmlNode;
    }
}
