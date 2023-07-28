package top.guoziyang.mydb.backend.vm;

import top.guoziyang.mydb.backend.tm.TransactionManager;
//确定事务的可行性
public class Visibility {
    //版本t是否跳过了版本e
    //就是看版本t什么时候会跳过版本e
    //数据已经被删除并且xmax大于本事务或者事务在活跃当中。
    //这时候版本t不会知道数据被删除，就会跳过这个版本。
    public static boolean isVersionSkip(TransactionManager tm, Transaction t, Entry e) {
        long xmax = e.getXmax();
        if(t.level == 0) {
            return false;
        } else {
            return tm.isCommitted(xmax) && (xmax > t.xid || t.isInSnapshot(xmax));
        }
    }
    //检查这条记录对本事务是否有可见性
    public static boolean isVisible(TransactionManager tm, Transaction t, Entry e) {
        //读已提交
        if(t.level == 0) {
            return readCommitted(tm, t, e);
        } else {
            //可重复读
            return repeatableRead(tm, t, e);
        }
    }
    //读已提交
    private static boolean readCommitted(TransactionManager tm, Transaction t, Entry e) {
        long xid = t.xid;
        long xmin = e.getXmin();
        long xmax = e.getXmax();
        //这条记录是当前事务创建，并且没有删除，就可以读
        if(xmin == xid && xmax == 0) return true;
        //这条记录不是本事务创建，但是已提交
        //情况一：没有被删除，可以读
        //情况二：有删除记录，删除者不是本事务，并且删除的事务没有提交，就可以读
        if(tm.isCommitted(xmin)) {
            if(xmax == 0) return true;
            if(xmax != xid) {
                if(!tm.isCommitted(xmax)) {
                    return true;
                }
            }
        }
        return false;
    }
    //可重复读
    private static boolean repeatableRead(TransactionManager tm, Transaction t, Entry e) {
        long xid = t.xid;
        long xmin = e.getXmin();
        long xmax = e.getXmax();
        //记录是本事务创建的，并且没被删除，就可以读
        if(xmin == xid && xmax == 0) return true;
        //创建该记录的xin已经提交并且xin<本事务并且在本事务开始之前，xmin不是活跃的
        if(tm.isCommitted(xmin) && xmin < xid && !t.isInSnapshot(xmin)) {
            //没人删除，就可以读
            if(xmax == 0) return true;
            //有人删除，在删除者不是本事务的情况下
            //xmax没有提交，或者xmax大于xid，或者xmax在本事务开始之前是活跃的
            if(xmax != xid) {
                if(!tm.isCommitted(xmax) || xmax > xid || t.isInSnapshot(xmax)) {
                    return true;
                }
            }
        }
        return false;
    }

}
