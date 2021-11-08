package top.guoziyang.mydb.backend.vm;

import top.guoziyang.mydb.backend.dm.DataManager;

public class VersionManagerImpl implements VersionManager {

    DataManager dm;

    @Override
    public byte[] read(long xid, long uid) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long insert(long xid, byte[] data) throws Exception {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean delete(long xid, long uid) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long begin(int level) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void commit(long xid) throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void abort(long xid) {
        // TODO Auto-generated method stub
        
    }

    public void releaseEntry(Entry entry) {
    }
    
}
