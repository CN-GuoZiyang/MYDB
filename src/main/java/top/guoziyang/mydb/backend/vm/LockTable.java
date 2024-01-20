package top.guoziyang.mydb.backend.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import top.guoziyang.mydb.common.Error;

/**
 * 维护了一个依赖等待图，以进行死锁检测
 */
public class LockTable {
    
    private Map<Long, List<Long>> x2u;  // 某个XID已经获得的资源的UID列表
    private Map<Long, Long> u2x;        // UID被某个XID持有
    private Map<Long, List<Long>> wait; // 正在等待UID的XID列表
    private Map<Long, Lock> waitLock;   // 正在等待资源的XID的锁
    private Map<Long, Long> waitU;      // XID正在等待的UID
    private Lock lock;

    public LockTable() {
        x2u = new HashMap<>();
        u2x = new HashMap<>();
        wait = new HashMap<>();
        waitLock = new HashMap<>();
        waitU = new HashMap<>();
        lock = new ReentrantLock();
    }

    // 不需要等待则返回null，否则返回锁对象
    // 会造成死锁则抛出异常
    public Lock add(long xid, long uid) throws Exception {
        lock.lock();
        try {
            //看xid是否拥有uid这个资源
            if(isInList(x2u, xid, uid)) {
                return null;
            }
            //如果uid这个资源还没被其他xid持有
            //把uid这个资源交给xid
            //加入map中
            if(!u2x.containsKey(uid)) {
                u2x.put(uid, xid);
                putIntoList(x2u, xid, uid);
                return null;
            }
            //如果uid被其他xid持有了
            //将xid加入等待资源队列
            waitU.put(xid, uid);
            //putIntoList(wait, xid, uid);
            //加入等待uid的xid列表
            putIntoList(wait, uid, xid);
            //检查是否死锁
            if(hasDeadLock()) {
                //检测到了死锁
                //将xid从等待资源移除
                //将xid从等待uid资源队列移除
                //抛出异常
                waitU.remove(xid);
                removeFromList(wait, uid, xid);
                throw Error.DeadlockException;
            }
            //将该xid加入真正的锁的队列
            Lock l = new ReentrantLock();
            l.lock();
            waitLock.put(xid, l);
            return l;

        } finally {
            lock.unlock();
        }
    }
    //在一个事务commit或者aborted的时候，释放锁
    public void remove(long xid) {
        lock.lock();
        try {
            //获取xid持有的全部uid资源
            List<Long> l = x2u.get(xid);
            if(l != null) {
                while(l.size() > 0) {
                    Long uid = l.remove(0);
                    selectNewXID(uid);
                }
            }
            waitU.remove(xid);
            x2u.remove(xid);
            waitLock.remove(xid);

        } finally {
            lock.unlock();
        }
    }

    // 从等待队列中选择一个xid来占用uid
    private void selectNewXID(long uid) {
        u2x.remove(uid);
        //获取等待uid资源的xid队列
        List<Long> l = wait.get(uid);
        if(l == null) return;
        assert l.size() > 0;

        while(l.size() > 0) {
            //将xid从等待uid队列去除
            long xid = l.remove(0);
            //查看等待锁队列里是否有该事务
            if(!waitLock.containsKey(xid)) {
                continue;
            } else {
                //如果有的话
                //将xid放入map，说明xid持有uid
                //把xid从等待锁中去除
                u2x.put(uid, xid);
                Lock lo = waitLock.remove(xid);
                waitU.remove(xid);
                lo.unlock();
                break;
            }
        }
        //如果列表里面没有等待的xid了，就去除掉该uid的key
        if(l.size() == 0) wait.remove(uid);
    }

    private Map<Long, Integer> xidStamp;
    private int stamp;

    private boolean hasDeadLock() {
        xidStamp = new HashMap<>();
        stamp = 1;
        //遍历每个节点，为每个节点设置访问戳
        //不同的图的访问戳是不同的
        //对每个图进行深度搜索
        for(long xid : x2u.keySet()) {
            Integer s = xidStamp.get(xid);
            if(s != null && s > 0) {
                continue;
            }
            stamp ++;
            if(dfs(xid)) {
                return true;
            }
        }
        return false;
    }
    //核心就是检查该事务是否缺少资源uid
    //然后找持有该uid的资源xid
    //再进行深度搜索
    private boolean dfs(long xid) {
        Integer stp = xidStamp.get(xid);
        if(stp != null && stp == stamp) {
            return true;
        }
        if(stp != null && stp < stamp) {
            return false;
        }
        xidStamp.put(xid, stamp);

        Long uid = waitU.get(xid);
        if(uid == null) return false;
        Long x = u2x.get(uid);
        assert x != null;
        return dfs(x);
    }
    //将xid从对应的列表中删除
    private void removeFromList(Map<Long, List<Long>> listMap, long uid0, long uid1) {
        List<Long> l = listMap.get(uid0);
        if(l == null) return;
        Iterator<Long> i = l.iterator();
        while(i.hasNext()) {
            long e = i.next();
            if(e == uid1) {
                i.remove();
                break;
            }
        }
        if(l.size() == 0) {
            listMap.remove(uid0);
        }
    }
    //将xid加入对应的列表
    private void putIntoList(Map<Long, List<Long>> listMap, long uid0, long uid1) {
        if(!listMap.containsKey(uid0)) {
            listMap.put(uid0, new ArrayList<>());
        }
        listMap.get(uid0).add(0, uid1);
    }
    //查看事务是否拥有uid该资源
    private boolean isInList(Map<Long, List<Long>> listMap, long uid0, long uid1) {
        List<Long> l = listMap.get(uid0);
        if(l == null) return false;
        Iterator<Long> i = l.iterator();
        while(i.hasNext()) {
            long e = i.next();
            if(e == uid1) {
                return true;
            }
        }
        return false;
    }

}
