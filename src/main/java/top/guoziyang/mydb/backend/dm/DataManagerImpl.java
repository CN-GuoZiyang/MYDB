package top.guoziyang.mydb.backend.dm;

import top.guoziyang.mydb.backend.dm.dataItem.DataItem;

public class DataManagerImpl implements DataManager {

    @Override
    public DataItem read(long uid) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long insert(long xid, byte[] data) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }

    public void logDataItem(long xid, DataItem di) {

    }

    public void releaseDataItem(DataItem di) {
        
    }
    
}
