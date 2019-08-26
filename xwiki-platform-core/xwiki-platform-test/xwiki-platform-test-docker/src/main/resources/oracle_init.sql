create user xwiki identified by xwiki;
-- This is the path for tablespaces on oracleinanutshell/oracle-xe-11g docker image.
alter system set db_create_file_dest = "/u01/app/oracle/oradata/XE";
-- Fixing the size and maxsize seems to lead to an error when triggering the inilizing script.
-- Besides we don't really need it here: by default the tablespace will use 100M and is autoextensible.
create tablespace xwiki datafile;
alter user xwiki quota unlimited on xwiki;
grant connect to xwiki;
grant resource to xwiki;
grant dba to xwiki;