package top.guoziyang.mydb.backend.vm;

import java.util.Arrays;

import com.google.common.primitives.Bytes;

import top.guoziyang.mydb.backend.common.SubArray;
import top.guoziyang.mydb.backend.dm.dataItem.DataItem;
import top.guoziyang.mydb.backend.utils.Parser;

/**
 * VM向上层抽象出entry
 * entry结构：
 * [XMIN] [XMAX] [data]
 */
public class Entry {

    private static final int OF_XMIN = 0;//创建该记录的事务编号
    private static final int OF_XMAX = OF_XMIN+8;//删除该记录的事务编号
    private static final int OF_DATA = OF_XMAX+8;//该条记录持有的数据

    private long uid;
    private DataItem dataItem;
    private VersionManager vm;

    //根据uid，dataitem创建一个entry对象
    public static Entry newEntry(VersionManager vm, DataItem dataItem, long uid) {
        if (dataItem == null) {
            return null;
        }
        Entry entry = new Entry();
        entry.uid = uid;
        entry.dataItem = dataItem;
        entry.vm = vm;
        return entry;
    }
    //根据uid，获得dataitem，然后创建一个entry对象
    public static Entry loadEntry(VersionManager vm, long uid) throws Exception {
        DataItem di = ((VersionManagerImpl)vm).dm.read(uid);
        return newEntry(vm, di, uid);
    }
    //将数据打包成entry格式
    public static byte[] wrapEntryRaw(long xid, byte[] data) {
        byte[] xmin = Parser.long2Byte(xid);
        byte[] xmax = new byte[8];
        return Bytes.concat(xmin, xmax, data);
    }
    //从内存中释放该条记录
    public void release() {
        ((VersionManagerImpl)vm).releaseEntry(this);
    }
    //释放该条dataitem数据
    public void remove() {
        dataItem.release();
    }

    // 以拷贝的形式返回内容
    public byte[] data() {
        dataItem.rLock();
        try {
            SubArray sa = dataItem.data();
            byte[] data = new byte[sa.end - sa.start - OF_DATA];
            System.arraycopy(sa.raw, sa.start+OF_DATA, data, 0, data.length);
            return data;
        } finally {
            dataItem.rUnLock();
        }
    }
    //得到该条记录的创建数据
    public long getXmin() {
        dataItem.rLock();
        try {
            SubArray sa = dataItem.data();
            return Parser.parseLong(Arrays.copyOfRange(sa.raw, sa.start+OF_XMIN, sa.start+OF_XMAX));
        } finally {
            dataItem.rUnLock();
        }
    }
    //得到该条记录的删除数据
    public long getXmax() {
        dataItem.rLock();
        try {
            SubArray sa = dataItem.data();
            return Parser.parseLong(Arrays.copyOfRange(sa.raw, sa.start+OF_XMAX, sa.start+OF_DATA));
        } finally {
            dataItem.rUnLock();
        }
    }
    //设置该条数据的删除
    //删除之前对dataitm的数据进行保存，把dataitem的raw数据先保存到old-raw数组里面
    //生成一个更新日志，把更新的内容写进日志里面
    public void setXmax(long xid) {
        dataItem.before();
        try {
            SubArray sa = dataItem.data();
            System.arraycopy(Parser.long2Byte(xid), 0, sa.raw, sa.start+OF_XMAX, 8);
        } finally {
            dataItem.after(xid);
        }
    }
    //获取当前dataitem的uid
    public long getUid() {
        return uid;
    }
}
