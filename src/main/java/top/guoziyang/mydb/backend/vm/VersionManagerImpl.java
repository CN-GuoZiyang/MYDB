package top.guoziyang.mydb.backend.vm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import top.guoziyang.mydb.backend.common.AbstractCache;
import top.guoziyang.mydb.backend.dm.DataManager;
import top.guoziyang.mydb.backend.tm.TransactionManager;
import top.guoziyang.mydb.backend.tm.TransactionManagerImpl;
import top.guoziyang.mydb.backend.utils.Panic;
import top.guoziyang.mydb.common.Error;
//获取entry需要从缓存中获取，需要继承抽象缓存类
public class VersionManagerImpl extends AbstractCache<Entry> implements VersionManager {

    TransactionManager tm;
    DataManager dm;
    Map<Long, Transaction> activeTransaction;
    Lock lock;
    LockTable lt;

    public VersionManagerImpl(TransactionManager tm, DataManager dm) {
        super(0);
        this.tm = tm;
        this.dm = dm;
        this.activeTransaction = new HashMap<>();
        //先把super-xid加进去，它的事务级别就是最低的，权限最高
        activeTransaction.put(TransactionManagerImpl.SUPER_XID, Transaction.newTransaction(TransactionManagerImpl.SUPER_XID, 0, null));
        this.lock = new ReentrantLock();
        this.lt = new LockTable();
    }
    //开启读行为
    //检查该事务开启时，系统中活跃的事务
    @Override
    public byte[] read(long xid, long uid) throws Exception {
        lock.lock();
        Transaction t = activeTransaction.get(xid);
        lock.unlock();

        if(t.err != null) {
            throw t.err;
        }
        //获取对应的entry
        Entry entry = null;
        try {
            entry = super.get(uid);
        } catch(Exception e) {
            if(e == Error.NullEntryException) {
                return null;
            } else {
                throw e;
            }
        }
        //检查该事务是否有读取该entry的资格
        //读完后就释放该entry
        try {
            if(Visibility.isVisible(tm, t, entry)) {
                return entry.data();
            } else {
                return null;
            }
        } finally {
            entry.release();
        }
    }
    //插入数据
    //同样是先获取该事务开启时，系统中活跃事务列表
    @Override
    public long insert(long xid, byte[] data) throws Exception {
        lock.lock();
        Transaction t = activeTransaction.get(xid);
        lock.unlock();

        if(t.err != null) {
            throw t.err;
        }
        //数据包装成entry
        //调用dataitem的方法插入文件中
        byte[] raw = Entry.wrapEntryRaw(xid, data);
        return dm.insert(xid, raw);
    }
    //删除数据
    //获取事务开启时，系统中活跃事务列表
    @Override
    public boolean delete(long xid, long uid) throws Exception {
        lock.lock();
        Transaction t = activeTransaction.get(xid);
        lock.unlock();

        if(t.err != null) {
            throw t.err;
        }
        //获取entry
        Entry entry = null;
        try {
            entry = super.get(uid);
        } catch(Exception e) {
            if(e == Error.NullEntryException) {
                return false;
            } else {
                throw e;
            }
        }
        //检查该事务有没有对该数据修改的权限
        try {
            if(!Visibility.isVisible(tm, t, entry)) {
                return false;
            }
            //去锁管理中，请求资源
            //有资源就继续
            //没有资源就进入等待队列，死锁就报错，撤销该事务
            Lock l = null;
            try {
                l = lt.add(xid, uid);
            } catch(Exception e) {
                t.err = Error.ConcurrentUpdateException;
                internAbort(xid, true);
                t.autoAborted = true;
                throw t.err;
            }
            //假如进入了等待队列，就自旋一下，等待锁的释放
            if(l != null) {
                l.lock();
                l.unlock();
            }
            //检查该资源是否被自己删除
            if(entry.getXmax() == xid) {
                return false;
            }
            //查看事务t是否跳过了版本
            //如果跳过了，就撤销该事务
            if(Visibility.isVersionSkip(tm, t, entry)) {
                t.err = Error.ConcurrentUpdateException;
                internAbort(xid, true);
                t.autoAborted = true;
                throw t.err;
            }
            //删除该数据，生成update日志
            entry.setXmax(xid);
            return true;

        } finally {
            entry.release();
        }
    }
    //开启一个事务
    @Override
    public long begin(int level) {
        lock.lock();
        try {
            long xid = tm.begin();
            //里面的构造函数根据当前activeTransaction的key生成active set。
            Transaction t = Transaction.newTransaction(xid, level, activeTransaction);
            //将该事务放进活跃列表中
            activeTransaction.put(xid, t);
            return xid;
        } finally {
            lock.unlock();
        }
    }
    //提交一个事务
    @Override
    public void commit(long xid) throws Exception {
        lock.lock();
        Transaction t = activeTransaction.get(xid);
        lock.unlock();

        try {
            if(t.err != null) {
                throw t.err;
            }
        } catch(NullPointerException n) {
            System.out.println(xid);
            System.out.println(activeTransaction.keySet());
            Panic.panic(n);
        }
        //将该事务从活跃事务移除
        //释放该事务所持有的全部资源
        //修改该事务的状态
        lock.lock();
        activeTransaction.remove(xid);
        lock.unlock();

        lt.remove(xid);
        tm.commit(xid);
    }
    //将该事务给撤销
    @Override
    public void abort(long xid) {
        internAbort(xid, false);
    }
    //同样的将该事务从活跃列表去除
    //释放掉该事务持有的资源
    //将该事务的标志设为撤销
    private void internAbort(long xid, boolean autoAborted) {
        lock.lock();
        Transaction t = activeTransaction.get(xid);
        if(!autoAborted) {
            activeTransaction.remove(xid);
        }
        lock.unlock();

        if(t.autoAborted) return;
        lt.remove(xid);
        tm.abort(xid);
    }

    public void releaseEntry(Entry entry) {
        super.release(entry.getUid());
    }

    //实际上是调用dataitem的方法
    //然后将dataitem包装成entry返回
    @Override
    protected Entry getForCache(long uid) throws Exception {
        Entry entry = Entry.loadEntry(this, uid);
        if(entry == null) {
            throw Error.NullEntryException;
        }
        return entry;
    }
    //实际上是调用dataitem的release
    @Override
    protected void releaseForCache(Entry entry) {
        entry.remove();
    }
    
}
