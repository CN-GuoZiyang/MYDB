package top.guoziyang.mydb.backend.utils;

public class Types {
    //通过位运算符，将页号左移32位，就是将页号设为32位的高位值
    //然后将偏移量的值低32位和左移后的页号进行合并，得到一个标识符
    public static long addressToUid(int pgno, short offset) {
        long u0 = (long)pgno;
        long u1 = (long)offset;
        return u0 << 32 | u1;
    }
}
