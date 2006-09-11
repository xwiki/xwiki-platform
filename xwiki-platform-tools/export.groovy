package xwiki.export;

import com.xpn.xwiki.*;
import com.xpn.xwiki.plugin.packaging.*;

println "Starting export"
context = new XWikiContext();

context.setUser("XWiki.superadmin");
config = new XWikiConfig();
config.put("xwiki.store.class", "com.xpn.xwiki.store.XWikiHibernateStore");
config.put("xwiki.store.hibernate.path", "./hibernate.cfg.xml");
config.put("xwiki.store.hibernate.updateschema", "1");
config.put("xwiki.virtual", "1");

println "Starting xwiki"
xwiki = new XWiki(config, context);
context.setWiki(xwiki);

db = "xwiki"
// Change this to export a different database
// db = "mydb"

println("Export database " + db)
context.setDatabase(db);

println "Creating package"
pack = new Package()
pack.setWithVersions(false)
println "Adding documents"
pack.addAllWikiDocuments(context)
println "Exporting documents"
pack.exportToDir(new File(args[0]), context)

println "Finished."
System.exit(0)
