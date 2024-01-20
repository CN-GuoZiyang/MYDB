package top.guoziyang.mydb.backend.dm.page;
//管理page里面的具体
public interface Page {
    void lock();
    void unlock();
    void release();
    void setDirty(boolean dirty);
    boolean isDirty();//定义脏页
    int getPageNumber();//获得页号
    byte[] getData();//获得页面数据
}
