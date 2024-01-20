package top.guoziyang.mydb.backend.dm.pageCache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import top.guoziyang.mydb.backend.dm.page.Page;
import top.guoziyang.mydb.backend.utils.Panic;
import top.guoziyang.mydb.common.Error;
//用来管理page
public interface PageCache {
    
    public static final int PAGE_SIZE = 1 << 13;

    int newPage(byte[] initData);
    Page getPage(int pgno) throws Exception;
    void close();
    void release(Page page);

    void truncateByBgno(int maxPgno);
    int getPageNumber();
    void flushPage(Page pg);
    //使用创建数据库文件的方式打开pagecache
    //读取数据库文件
    //获取对应的文件流，通道和缓存页面的张数
    public static PageCacheImpl create(String path, long memory) {
        File f = new File(path+PageCacheImpl.DB_SUFFIX);
        try {
            if(!f.createNewFile()) {
                Panic.panic(Error.FileExistsException);
            }
        } catch (Exception e) {
            Panic.panic(e);
        }
        if(!f.canRead() || !f.canWrite()) {
            Panic.panic(Error.FileCannotRWException);
        }

        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "rw");
            fc = raf.getChannel();
        } catch (FileNotFoundException e) {
           Panic.panic(e);
        }
        //根据传入的可用内存大小，来确定内存中可以缓存几张页面
        return new PageCacheImpl(raf, fc, (int)memory/PAGE_SIZE);
    }
    //使用打开数据库文件的方式，来创建一个pagecache对象
    public static PageCacheImpl open(String path, long memory) {
        File f = new File(path+PageCacheImpl.DB_SUFFIX);
        if(!f.exists()) {
            Panic.panic(Error.FileNotExistsException);
        }
        if(!f.canRead() || !f.canWrite()) {
            Panic.panic(Error.FileCannotRWException);
        }

        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "rw");
            fc = raf.getChannel();
        } catch (FileNotFoundException e) {
           Panic.panic(e);
        }
        return new PageCacheImpl(raf, fc, (int)memory/PAGE_SIZE);
    }
}
