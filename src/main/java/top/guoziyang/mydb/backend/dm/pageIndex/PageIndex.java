package top.guoziyang.mydb.backend.dm.pageIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import top.guoziyang.mydb.backend.dm.pageCache.PageCache;

public class PageIndex {
    // 将一页划成40个区间
    private static final int INTERVALS_NO = 40;
    //一个区间的大小
    private static final int THRESHOLD = PageCache.PAGE_SIZE / INTERVALS_NO;

    private Lock lock;
    //每个区间里面的页面信息
    private List<PageInfo>[] lists;

    @SuppressWarnings("unchecked")
    //创建一个互斥锁
    //初始化每个区间里面的页面信息
    public PageIndex() {
        lock = new ReentrantLock();
        lists = new List[INTERVALS_NO+1];
        for (int i = 0; i < INTERVALS_NO+1; i ++) {
            lists[i] = new ArrayList<>();
        }
    }
    //将页面分配在索引区间的依据是：页面的空余空间
    //向页面索引当中添加页面信息
    //根据freespace，获取要添加到的区间
    //然后对应的区间添加页面信息
    public void add(int pgno, int freeSpace) {
        lock.lock();
        try {
            int number = freeSpace / THRESHOLD;
            lists[number].add(new PageInfo(pgno, freeSpace));
        } finally {
            lock.unlock();
        }
    }
    //首先用spacesize/区间大小，这个应该去哪个区间里面找
    //将number向上取整
    //然后看对应区间里的列表是否存有数据
    //没有的话就看下一个区间的，有的话就取出列表里的第一个，并且从列表中移除
    //被选择的页，会被pageindex移除，同一个页面是不允许并发写的
    //上层模块在添加完之后，要重新把这一个页面加回来
    public PageInfo select(int spaceSize) {
        lock.lock();
        try {
            int number = spaceSize / THRESHOLD;
            if(number < INTERVALS_NO) number ++;
            while(number <= INTERVALS_NO) {
                if(lists[number].size() == 0) {
                    number ++;
                    continue;
                }
                return lists[number].remove(0);
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

}
