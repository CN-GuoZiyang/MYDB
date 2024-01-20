package top.guoziyang.mydb.backend.parser;

import top.guoziyang.mydb.common.Error;
//将一个语句切割成多个tokenizer，来解析
public class Tokenizer {
    private byte[] stat;

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    private int pos;
    private String currentToken;
    private boolean flushToken;
    private Exception err;

    public Tokenizer(byte[] stat) {
        this.stat = stat;
        this.pos = 0;
        this.currentToken = "";
        this.flushToken = true;//用来控制当前token是否需要刷新
    }
    //获取下一个token
    public String peek() throws Exception {
        if(err != null) {
            throw err;
        }
        if(flushToken) {
            String token = null;
            try {
                token = next();
            } catch(Exception e) {
                err = e;
                throw e;
            }
            currentToken = token;
            flushToken = false;
        }
        return currentToken;
    }
    //刷新标记
    public void pop() {
        flushToken = true;
    }
    //生成一个错误标记数组
    //错误位置之前的<< 错误位置之后
    public byte[] errStat() {
        byte[] res = new byte[stat.length+3];
        System.arraycopy(stat, 0, res, 0, pos);
        System.arraycopy("<< ".getBytes(), 0, res, pos, 3);
        System.arraycopy(stat, pos, res, pos+3, stat.length-pos);
        return res;
    }
    //移动byte数组里面的位置指针
    private void popByte() {
        pos ++;
        if(pos > stat.length) {
            pos = stat.length;
        }
    }
    //返回数组当前位置的字节
    private Byte peekByte() {
        if(pos == stat.length) {
            return null;
        }
        return stat[pos];
    }
    //读取下一个语句的数据
    private String next() throws Exception {
        if(err != null) {
            throw err;
        }
        return nextMetaState();
    }
    //解析下一个元数据状态内容
    private String nextMetaState() throws Exception {
        while(true) {
            Byte b = peekByte();
            if(b == null) {
                return "";
            }
            if(!isBlank(b)) {
                break;
            }
            popByte();
        }
        byte b = peekByte();
        //如果是符号，则直接返回该符号，字符串表示
        if(isSymbol(b)) {
            popByte();
            return new String(new byte[]{b});
            //如果是单引号或双引号，继续解析引号内容
        } else if(b == '"' || b == '\'') {
            return nextQuoteState();
            //如果是字母或者数字，则去返回完整的字母和数字
        } else if(isAlphaBeta(b) || isDigit(b)) {
            return nextTokenState();
        } else {
            err = Error.InvalidCommandException;
            throw err;
        }
    }
    //将字母或数字返回
    private String nextTokenState() throws Exception {
        StringBuilder sb = new StringBuilder();
        while(true) {
            Byte b = peekByte();
            //如果b不满足条件了，就跳出循环，返回值
            if(b == null || !(isAlphaBeta(b) || isDigit(b) || b == '_')) {
                if(b != null && isBlank(b)) {
                    popByte();
                }
                return sb.toString();
            }
            sb.append(new String(new byte[]{b}));
            popByte();
        }
    }
    //判断b是不是数字
    static boolean isDigit(byte b) {
        return (b >= '0' && b <= '9');
    }
    //判断b是不是字母
    static boolean isAlphaBeta(byte b) {
        return ((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z'));
    }
    //解析括号里的内容
    private String nextQuoteState() throws Exception {
        byte quote = peekByte();
        popByte();
        StringBuilder sb = new StringBuilder();
        while(true) {
            Byte b = peekByte();
            if(b == null) {
                err = Error.InvalidCommandException;
                throw err;
            }
            if(b == quote) {
                popByte();
                break;
            }
            sb.append(new String(new byte[]{b}));
            popByte();
        }
        return sb.toString();
    }
    //检查b是否是符号
    static boolean isSymbol(byte b) {
        return (b == '>' || b == '<' || b == '=' || b == '*' ||
		b == ',' || b == '(' || b == ')');
    }
    //检查b是否为空
    static boolean isBlank(byte b) {
        return (b == '\n' || b == ' ' || b == '\t');
    }
}
