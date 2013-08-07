package org.xwiki.wikistream.xar.internal;

public class XARModel
{
    public static final String PATH_PACKAGE = "package.xml";

    public static final String ELEMENT_PACKAGE = "package";

    public static final String ELEMENT_INFOS = "infos";

    public static final String ELEMENT_INFOS_NAME = "name";

    public static final String ELEMENT_INFOS_DESCRIPTION = "description";

    public static final String ELEMENT_INFOS_LICENSE = "licence";

    public static final String ELEMENT_INFOS_AUTHOR = "author";

    public static final String ELEMENT_INFOS_VERSION = "version";

    public static final String ELEMENT_INFOS_ISBACKUPPACK = "backupPack";

    public static final String ELEMENT_INFOS_ISPRESERVEVERSION = "preserveVersion";

    public static final String ELEMENT_FILES = "files";

    public static final String ELEMENT_FILES_FILES = "file";

    public static final String ATTRIBUTE_DEFAULTACTION = "defaultAction";

    public static final String ATTRIBUTE_LOCALE = "language";

    // action

    public final static int ACTION_NOT_DEFINED = -1;

    public final static int ACTION_OVERWRITE = 0;

    public final static int ACTION_SKIP = 1;

    public final static int ACTION_MERGE = 2;
}
