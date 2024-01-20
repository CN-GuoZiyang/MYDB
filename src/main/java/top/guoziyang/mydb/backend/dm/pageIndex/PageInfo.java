package top.guoziyang.mydb.backend.dm.pageIndex;
//索引里包含的页面信息
//页面号
//页面空余空间
public class PageInfo {
    public int pgno;
    public int freeSpace;

    public PageInfo(int pgno, int freeSpace) {
        this.pgno = pgno;
        this.freeSpace = freeSpace;
    }
}
