package top.guoziyang.mydb.backend.common;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AbstractCache 实现了一个执行LRU驱逐策略的缓存
 */
public abstract class AbstractCache<T> {
    private HashMap<Long, T> cache;                     // 实际缓存的数据
    private LinkedList<Long> cacheKeysList;             // 表述使用时间的链表
    private ConcurrentHashMap<Long, Boolean> getting;   // 正在获取某资源的线程

    private int maxResource;        // 缓存的最大缓存资源数
    private Lock lock;

    public AbstractCache(int maxResource) {
        this.maxResource = maxResource;
        cache = new HashMap<>();
        cacheKeysList = new LinkedList<>();
        getting = new ConcurrentHashMap<>();
        lock = new ReentrantLock();
    }

    public T get(long key) throws Exception {
        while(true) {
            lock.lock();
            if(getting.containsKey(key)) {
                // 请求的资源正在被其他线程获取
                lock.unlock();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }
                continue;
            }

            if(cache.containsKey(key)) {
                // 资源在缓存中，直接返回
                T obj = cache.get(key);
                cacheKeysList.remove(key);
                cacheKeysList.addFirst(key);
                lock.unlock();
                return obj;
            }

            // 尝试获取该资源
            getting.put(key, true);
            lock.unlock();
            break;
        }

        T obj = null;
        try {
            obj = getForCache(key);
        } catch(Exception e) {
            lock.lock();
            getting.remove(key);
            lock.lock();
            throw e;
        }

        lock.lock();
        getting.remove(key);
        if(cache.size() == maxResource) {
            // 缓存已满，则驱逐一个资源
            release(cacheKeysList.getLast());
        }
        cache.put(key, obj);
        cacheKeysList.addFirst(key);
        lock.unlock();
        
        return obj;
    }

    /**
     * 强行释放一个缓存
     */
    public void release(long key) {
        lock.lock();
        try {
            T obj = cache.get(key);
            if(obj == null) return;
            releaseForCache(obj);
            cache.remove(key);
            cacheKeysList.remove(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 关闭缓存，写回所有资源
     */
    public void close() {
        lock.lock();
        try {
            Set<Long> keys = cache.keySet();
            for (long key : keys) {
                release(key);
                cache.remove(key);
                cacheKeysList.remove(key);
            }
        } finally {
            lock.unlock();
        }
    }


    /**
     * 当资源不在缓存时的获取行为
     */
    protected abstract T getForCache(long key) throws Exception;
    /**
     * 当资源被驱逐时的写回行为
     */
    protected abstract void releaseForCache(T obj);
}
