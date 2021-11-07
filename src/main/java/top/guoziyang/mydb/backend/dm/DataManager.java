package top.guoziyang.mydb.backend.dm;

import top.guoziyang.mydb.backend.dm.dataItem.DataItem;

public interface DataManager {
    DataItem read(long uid) throws Exception;
    long insert(long xid, byte[] data) throws Exception;
    void close();
}
