package top.guoziyang.mydb.backend.parser;

import java.util.ArrayList;
import java.util.List;

import top.guoziyang.mydb.backend.parser.statement.Abort;
import top.guoziyang.mydb.backend.parser.statement.Begin;
import top.guoziyang.mydb.backend.parser.statement.Commit;
import top.guoziyang.mydb.backend.parser.statement.Create;
import top.guoziyang.mydb.backend.parser.statement.Delete;
import top.guoziyang.mydb.backend.parser.statement.Drop;
import top.guoziyang.mydb.backend.parser.statement.Insert;
import top.guoziyang.mydb.backend.parser.statement.Select;
import top.guoziyang.mydb.backend.parser.statement.Show;
import top.guoziyang.mydb.backend.parser.statement.SingleExpression;
import top.guoziyang.mydb.backend.parser.statement.Update;
import top.guoziyang.mydb.backend.parser.statement.Where;
import top.guoziyang.mydb.common.Error;

public class Parser {
    public static Object Parse(byte[] statement) throws Exception {
        Tokenizer tokenizer = new Tokenizer(statement);
        //获取第一个token，并刷新flushtoken
        String token = tokenizer.peek();
        tokenizer.pop();

        Object stat = null;
        Exception statErr = null;
        try {
            //根据不同的token来分析剩下的语句
            switch(token) {
                case "begin":
                    stat = parseBegin(tokenizer);
                    break;
                case "commit":
                    stat = parseCommit(tokenizer);
                    break;
                case "abort":
                    stat = parseAbort(tokenizer);
                    break;
                case "create":
                    stat = parseCreate(tokenizer);
                    break;
                case "drop":
                    stat = parseDrop(tokenizer);
                    break;
                case "select":
                    stat = parseSelect(tokenizer);
                    break;
                case "insert":
                    stat = parseInsert(tokenizer);
                    break;
                case "delete":
                    stat = parseDelete(tokenizer);
                    break;
                case "update":
                    stat = parseUpdate(tokenizer);
                    break;
                case "show":
                    stat = parseShow(tokenizer);
                    break;
                default:
                    throw Error.InvalidCommandException;
            }
        } catch(Exception e) {
            statErr = e;
        }
        //检查语法错误
        try {
            String next = tokenizer.peek();
            if(!"".equals(next)) {
                byte[] errStat = tokenizer.errStat();
                statErr = new RuntimeException("Invalid statement: " + new String(errStat));
            }
        } catch(Exception e) {
            e.printStackTrace();
            byte[] errStat = tokenizer.errStat();
            statErr = new RuntimeException("Invalid statement: " + new String(errStat));
        }
        if(statErr != null) {
            throw statErr;
        }
        return stat;
    }
    //解析show语句
    private static Show parseShow(Tokenizer tokenizer) throws Exception {
        String tmp = tokenizer.peek();
        if("".equals(tmp)) {
            return new Show();
        }
        throw Error.InvalidCommandException;
    }
    //解析update语句
    private static Update parseUpdate(Tokenizer tokenizer) throws Exception {
        Update update = new Update();
        update.tableName = tokenizer.peek();
        tokenizer.pop();
        //检查set是否存在
        if(!"set".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();
        //获取字段名
        update.fieldName = tokenizer.peek();
        tokenizer.pop();
        //检测=是否存在
        if(!"=".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();
        //获取数值
        update.value = tokenizer.peek();
        tokenizer.pop();
        //检测后面有没有where
        String tmp = tokenizer.peek();
        if("".equals(tmp)) {
            update.where = null;
            return update;
        }
        //获取where
        update.where = parseWhere(tokenizer);
        return update;
    }
    //解析删除语句
    private static Delete parseDelete(Tokenizer tokenizer) throws Exception {
        Delete delete = new Delete();
        //检查是否有from
        if(!"from".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();
        //获取表名
        String tableName = tokenizer.peek();
        if(!isName(tableName)) {
            throw Error.InvalidCommandException;
        }
        delete.tableName = tableName;
        tokenizer.pop();
        //解析后面的where语句
        delete.where = parseWhere(tokenizer);
        return delete;
    }
    //解析insert语句
    private static Insert parseInsert(Tokenizer tokenizer) throws Exception {
        Insert insert = new Insert();
        //检查是否有into
        if(!"into".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();
        //获取表名
        String tableName = tokenizer.peek();
        if(!isName(tableName)) {
            throw Error.InvalidCommandException;
        }
        insert.tableName = tableName;
        tokenizer.pop();
        //检查是否有values
        if(!"values".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        //获取插入的数值
        List<String> values = new ArrayList<>();
        while(true) {
            tokenizer.pop();
            String value = tokenizer.peek();
            if("".equals(value)) {
                break;
            } else {
                values.add(value);
            }
        }
        insert.values = values.toArray(new String[values.size()]);

        return insert;
    }
    //解析select语句
    private static Select parseSelect(Tokenizer tokenizer) throws Exception {
        Select read = new Select();
        //获取要读取的属性
        List<String> fields = new ArrayList<>();
        String asterisk = tokenizer.peek();
        if("*".equals(asterisk)) {
            fields.add(asterisk);
            tokenizer.pop();
        } else {
            while(true) {
                String field = tokenizer.peek();
                if(!isName(field)) {
                    throw Error.InvalidCommandException;
                }
                fields.add(field);
                tokenizer.pop();
                if(",".equals(tokenizer.peek())) {
                    tokenizer.pop();
                } else {
                    break;
                }
            }
        }
        read.fields = fields.toArray(new String[fields.size()]);
        //检查from是否存在
        if(!"from".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();
        //获取要读的表名
        String tableName = tokenizer.peek();
        if(!isName(tableName)) {
            throw Error.InvalidCommandException;
        }
        read.tableName = tableName;
        tokenizer.pop();
        //检查是否有where语句
        String tmp = tokenizer.peek();
        if("".equals(tmp)) {
            read.where = null;
            return read;
        }
        //解析where语句
        read.where = parseWhere(tokenizer);
        return read;
    }
    //解析where语句
    private static Where parseWhere(Tokenizer tokenizer) throws Exception {
        Where where = new Where();
        //查看是否有where的存在
        if(!"where".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();
        //判断是否有Not
        int notPos = tokenizer.getPos();
        System.out.println(notPos);
        String not = tokenizer.peek();
        if (not.equals("not"))
        {
            where.Notflag=true;
            tokenizer.pop();
        }
        else
        {
            tokenizer.setPos(notPos);
            tokenizer.pop();
        }
        //获取第一个表达式
        SingleExpression exp1 = parseSingleExp(tokenizer);
        where.singleExp1 = exp1;
        //获取逻辑运算符
        String logicOp = tokenizer.peek();
        if("".equals(logicOp)) {
            where.logicOp = logicOp;
            return where;
        }
        if(!isLogicOp(logicOp)) {
            throw Error.InvalidCommandException;
        }
        where.logicOp = logicOp;
        tokenizer.pop();
        //获取第二个表达式
        SingleExpression exp2 = parseSingleExp(tokenizer);
        where.singleExp2 = exp2;

        if(!"".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        return where;
    }
    //解析大于，小于这类语句
    private static SingleExpression parseSingleExp(Tokenizer tokenizer) throws Exception {
        SingleExpression exp = new SingleExpression();
        //先获取字段名
        String field = tokenizer.peek();
        if(!isName(field)) {
            throw Error.InvalidCommandException;
        }
        exp.field = field;
        tokenizer.pop();
        //获取操作符
        String op = tokenizer.peek();
        if(!isCmpOp(op)) {
            throw Error.InvalidCommandException;
        }
        exp.compareOp = op;
        tokenizer.pop();
        //获取值
        exp.value = tokenizer.peek();
        tokenizer.pop();
        return exp;
    }
    //判断是否是符号
    private static boolean isCmpOp(String op) {
        return ("=".equals(op) || ">".equals(op) || "<".equals(op) || "!=".equals(op));
    }
    //判断是否是逻辑运算法
    private static boolean isLogicOp(String op) {
        return ("and".equals(op) || "or".equals(op));
    }
    //解析drop语句
    private static Drop parseDrop(Tokenizer tokenizer) throws Exception {
        //检查table是否存在
        if(!"table".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();
        //获取表名
        String tableName = tokenizer.peek();
        if(!isName(tableName)) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();
        //检查语法错误
        if(!"".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        
        Drop drop = new Drop();
        drop.tableName = tableName;
        return drop;
    }
    //解析create语句
    private static Create parseCreate(Tokenizer tokenizer) throws Exception {
        //先检查语法
        if(!"table".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();

        Create create = new Create();
        String name = tokenizer.peek();
        //检查table name 是否合法
        if(!isName(name)) {
            throw Error.InvalidCommandException;
        }
        create.tableName = name;

        List<String> fNames = new ArrayList<>();
        List<String> fTypes = new ArrayList<>();
        //获取属性名和属性类型
        while(true) {
            tokenizer.pop();
            String field = tokenizer.peek();
            if("(".equals(field)) {
                break;
            }
            //检查是否符合命名规则
            if(!isName(field)) {
                throw Error.InvalidCommandException;
            }

            tokenizer.pop();
            String fieldType = tokenizer.peek();
            if(!isType(fieldType)) {
                throw Error.InvalidCommandException;
            }
            fNames.add(field);
            fTypes.add(fieldType);
            tokenizer.pop();
            //检查语法
            String next = tokenizer.peek();
            if(",".equals(next)) {
                continue;
            } else if("".equals(next)) {
                throw Error.TableNoIndexException;
            } else if("(".equals(next)) {
                break;
            } else {
                throw Error.InvalidCommandException;
            }
        }
        create.fieldName = fNames.toArray(new String[fNames.size()]);
        create.fieldType = fTypes.toArray(new String[fTypes.size()]);
        //检查是否有语法错误
        tokenizer.pop();
        if(!"index".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        //获取索引
        List<String> indexes = new ArrayList<>();
        while(true) {
            tokenizer.pop();
            String field = tokenizer.peek();
            if(")".equals(field)) {
                break;
            }
            if(!isName(field)) {
                throw Error.InvalidCommandException;
            } else {
                indexes.add(field);
            }
        }
        create.index = indexes.toArray(new String[indexes.size()]);
        tokenizer.pop();
        //检测语法错误
        if(!"".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        return create;
    }

    private static boolean isType(String tp) {
        return ("int32".equals(tp) || "int64".equals(tp) ||
        "string".equals(tp));
    }
    //解析abort语句
    //检查后续语法是否正常
    private static Abort parseAbort(Tokenizer tokenizer) throws Exception {
        if(!"".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        return new Abort();
    }
    //解析commit语句
    //检查语法是否合格
    private static Commit parseCommit(Tokenizer tokenizer) throws Exception {
        if(!"".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        return new Commit();
    }
    //分析begin语句
    private static Begin parseBegin(Tokenizer tokenizer) throws Exception {
        //判断隔离级别
        //先检查语法有无错误，跟着的是不是isolation
        String isolation = tokenizer.peek();
        Begin begin = new Begin();
        if("".equals(isolation)) {
            return begin;
        }
        if(!"isolation".equals(isolation)) {
            throw Error.InvalidCommandException;
        }
        //检查跟着的是不是level
        tokenizer.pop();
        String level = tokenizer.peek();
        if(!"level".equals(level)) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();
        //判断是什么隔离级别
        String tmp1 = tokenizer.peek();
        if("read".equals(tmp1)) {
            tokenizer.pop();
            String tmp2 = tokenizer.peek();
            if("committed".equals(tmp2)) {
                tokenizer.pop();
                if(!"".equals(tokenizer.peek())) {
                    throw Error.InvalidCommandException;
                }
                return begin;
            } else {
                throw Error.InvalidCommandException;
            }
            //如果是可重复读级别，还有给begin里的属性赋值true
        } else if("repeatable".equals(tmp1)) {
            tokenizer.pop();
            String tmp2 = tokenizer.peek();
            if("read".equals(tmp2)) {
                begin.isRepeatableRead = true;
                tokenizer.pop();
                if(!"".equals(tokenizer.peek())) {
                    throw Error.InvalidCommandException;
                }
                return begin;
            } else {
                throw Error.InvalidCommandException;
            }
        } else {
            throw Error.InvalidCommandException;
        }
    }
    //检查这个name是否合法
    private static boolean isName(String name) {
        return !(name.length() == 1 && !Tokenizer.isAlphaBeta(name.getBytes()[0]));
    }
}
