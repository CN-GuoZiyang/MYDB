package top.guoziyang.mydb.backend.dm.page;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import top.guoziyang.mydb.backend.dm.pageCache.PageCache;
//对页面的数据进行管理
//页号啊，字节数据，脏页标志位
public class PageImpl implements Page {
    private int pageNumber;//该页面的页号
    private byte[] data;//页面实际包含的字节数据
    private boolean dirty;//脏页，需要被写会磁盘当中
    private Lock lock;
    
    private PageCache pc;//方便在拿到page引用时，可以快速对页面进行释放操作

    public PageImpl(int pageNumber, byte[] data, PageCache pc) {
        this.pageNumber = pageNumber;
        this.data = data;
        this.pc = pc;
        lock = new ReentrantLock();
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public void release() {
        pc.release(this);
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return dirty;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public byte[] getData() {
        return data;
    }

}
