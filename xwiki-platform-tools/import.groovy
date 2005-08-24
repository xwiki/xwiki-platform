package xwiki.export;

import com.xpn.xwiki.*;
import com.xpn.xwiki.plugin.packaging.*;

println "Starting import"
context = new XWikiContext();

context.setUser("XWiki.superadmin");
config = new XWikiConfig();
config.put("xwiki.store.class", "com.xpn.xwiki.store.XWikiHibernateStore");
config.put("xwiki.store.hibernate.path", "./hibernate.cfg.xml");
config.put("xwiki.store.hibernate.updateschema", "1");
config.put("xwiki.virtual", "1");

println "Starting xwiki"
xwiki = new XWiki(config, context)
context.setWiki(xwiki);

db = "xwiki"
// Export a different database
// db = "xwiki"

println("Import database " + db)
context.setDatabase(db);

pack = new Package()
pack.setWithVersions(false)
pack.setBackupPack(true)
println "Reading documents"
pack.readFromDir(new File("./db/"), context)
println "Installing documents"
pack.install(context)
println "Finished."

