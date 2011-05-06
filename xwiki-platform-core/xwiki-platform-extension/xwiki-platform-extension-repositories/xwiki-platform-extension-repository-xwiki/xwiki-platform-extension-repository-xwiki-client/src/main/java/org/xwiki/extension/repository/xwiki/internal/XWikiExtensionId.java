package org.xwiki.extension.repository.xwiki.internal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ExtensionId")
public class XWikiExtensionId
{
    String id;
    String version;
}
