package top.guoziyang.mydb.backend.tbm;

import java.util.*;

import com.google.common.primitives.Bytes;

import top.guoziyang.mydb.backend.dm.DataManager;
import top.guoziyang.mydb.backend.parser.statement.Create;
import top.guoziyang.mydb.backend.parser.statement.Delete;
import top.guoziyang.mydb.backend.parser.statement.Insert;
import top.guoziyang.mydb.backend.parser.statement.Select;
import top.guoziyang.mydb.backend.parser.statement.Update;
import top.guoziyang.mydb.backend.parser.statement.Where;
import top.guoziyang.mydb.backend.tbm.Field.ParseValueRes;
import top.guoziyang.mydb.backend.tm.TransactionManagerImpl;
import top.guoziyang.mydb.backend.utils.Panic;
import top.guoziyang.mydb.backend.utils.ParseStringRes;
import top.guoziyang.mydb.backend.utils.Parser;
import top.guoziyang.mydb.backend.vm.VersionManager;
import top.guoziyang.mydb.common.Error;

/**
 * Table 维护了表结构
 * 二进制结构如下：
 * [TableName][NextTable]
 * [Field1Uid][Field2Uid]...[FieldNUid]
 */
public class Table {
    TableManager tbm;
    long uid;
    String name;
    byte status;
    long nextUid;
    List<Field> fields = new ArrayList<>();

    //读取表信息
    public static Table loadTable(TableManager tbm, long uid) {
        byte[] raw = null;
        try {
            raw = ((TableManagerImpl) tbm).vm.read(TransactionManagerImpl.SUPER_XID, uid);
        } catch (Exception e) {
            Panic.panic(e);
        }
        assert raw != null;
        Table tb = new Table(tbm, uid);
        return tb.parseSelf(raw);
    }

    //创建一个表
    public static Table createTable(TableManager tbm, long nextUid, long xid, Create create) throws Exception {
        Table tb = new Table(tbm, create.tableName, nextUid);
        for (int i = 0; i < create.fieldName.length; i++) {
            String fieldName = create.fieldName[i];
            String fieldType = create.fieldType[i];
            boolean indexed = false;
            //判断是否要对相应的字段创建索引
            for (int j = 0; j < create.index.length; j++) {
                if (fieldName.equals(create.index[j])) {
                    indexed = true;
                    break;
                }
            }
            //创建字段
            tb.fields.add(Field.createField(tb, xid, fieldName, fieldType, indexed));
        }

        return tb.persistSelf(xid);
    }

    public Table(TableManager tbm, long uid) {
        this.tbm = tbm;
        this.uid = uid;
    }

    public Table(TableManager tbm, String tableName, long nextUid) {
        this.tbm = tbm;
        this.name = tableName;
        this.nextUid = nextUid;
    }

    //从字节数组中读取table的信息
    private Table parseSelf(byte[] raw) {
        //先读取table的基本信息，name，nextuid，下张表的uid
        int position = 0;
        ParseStringRes res = Parser.parseString(raw);
        name = res.str;
        position += res.next;
        nextUid = Parser.parseLong(Arrays.copyOfRange(raw, position, position + 8));
        position += 8;
        //先读取字段的uid
        //然后根据uid读取对应的字段值
        while (position < raw.length) {
            long uid = Parser.parseLong(Arrays.copyOfRange(raw, position, position + 8));
            position += 8;
            fields.add(Field.loadField(this, uid));
        }
        return this;
    }

    //将表的信息保存到数据库文件当中
    //都转化成byte数组
    private Table persistSelf(long xid) throws Exception {
        byte[] nameRaw = Parser.string2Byte(name);
        byte[] nextRaw = Parser.long2Byte(nextUid);
        byte[] fieldRaw = new byte[0];
        for (Field field : fields) {
            fieldRaw = Bytes.concat(fieldRaw, Parser.long2Byte(field.uid));
        }
        //插入数据文件中
        uid = ((TableManagerImpl) tbm).vm.insert(xid, Bytes.concat(nameRaw, nextRaw, fieldRaw));
        return this;
    }

    //删除表信息
    public int delete(long xid, Delete delete) throws Exception {
        List<Long> uids = parseWhere(delete.where, xid);
        int count = 0;
        for (Long uid : uids) {
            if (((TableManagerImpl) tbm).vm.delete(xid, uid)) {
                count++;
            }
        }
        return count;
    }

    //更新表信息
    public int update(long xid, Update update) throws Exception {
        //获得where对应的数据uid
        List<Long> uids = parseWhere(update.where, xid);
        Field fd = null;
        //找到要更新的表字段
        for (Field f : fields) {
            if (f.fieldName.equals(update.fieldName)) {
                fd = f;
                break;
            }
        }
        if (fd == null) {
            throw Error.FieldNotFoundException;
        }
        //将value转化为字段对应的字段类型
        Object value = fd.string2Value(update.value);
        int count = 0;
        //读取数据
        for (Long uid : uids) {
            //读取对应uid的字段值
            byte[] raw = ((TableManagerImpl) tbm).vm.read(xid, uid);
            if (raw == null) continue;
            //删除原有的数据
            ((TableManagerImpl) tbm).vm.delete(xid, uid);
            //将要更改的数据放进去
            Map<String, Object> entry = parseEntry(raw);
            entry.put(fd.fieldName, value);
            raw = entry2Raw(entry);
            //添加数据
            long uuid = ((TableManagerImpl) tbm).vm.insert(xid, raw);

            count++;

            for (Field field : fields) {
                if (field.isIndexed()) {
                    field.insert(entry.get(field.fieldName), uuid);
                }
            }
        }
        //修改的数据条数
        return count;
    }

    //读取对应字段
    public String read(long xid, Select read) throws Exception {
        //先获得范围内的uid
        List<Long> uids = parseWhere(read.where, xid);
        StringBuilder sb = new StringBuilder();
        //将范围内的数据读取出来
        //然后加入到StringBuilder里面
        for (Long uid : uids) {
            byte[] raw = ((TableManagerImpl) tbm).vm.read(xid, uid);
            if (raw == null) continue;
            Map<String, Object> entry = parseEntry(raw);
            //根据Select语句中field的值，来读取对应的数据。
            if (read.fields[0].equals("*")) sb.append(printEntry(entry)).append("\n");
            else {
                sb.append("[");
                for (String field : read.fields) {
                    sb.append(entry.get(field)).append(",");
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.append("]");
                sb.append("\n");
            }
            // sb.append(printEntry(entry)).append("\n");
        }
        return sb.toString();
    }

    //插入字段值
    public void insert(long xid, Insert insert) throws Exception {
        //将insert里面的值，转化为字段名-值这样的map
        Map<String, Object> entry = string2Entry(insert.values);
        //将map转化为二进制数组
        byte[] raw = entry2Raw(entry);
        //把数据插入数据文件中
        long uid = ((TableManagerImpl) tbm).vm.insert(xid, raw);
        for (Field field : fields) {
            if (field.isIndexed()) {
                //将对应的值插入索引树当中
                field.insert(entry.get(field.fieldName), uid);
            }
        }
    }

    //将value放入map当中，key是对应的filedname
    private Map<String, Object> string2Entry(String[] values) throws Exception {
        if (values.length != fields.size()) {
            throw Error.InvalidValuesException;
        }
        Map<String, Object> entry = new HashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            Field f = fields.get(i);
            Object v = f.string2Value(values[i]);
            entry.put(f.fieldName, v);
        }
        return entry;
    }
    //根据给定的条件where，在字段中fields进行查找
    private List<Long> parseWhere(Where where, long xid) throws Exception {
        long l0 = 0, r0 = 0, l1 = 0, r1 = 0;
        String fieldLeft=null, fieldRight=null;
        boolean single = false;
        Field fd = null;
        List<Long> alluids=null;
        boolean[] flag=null;
        boolean[] right_flag=null;
        ArrayList<Map<String, Object>> list=null;
        List<Long> left_res = null;
        List<Long> right_res=null;
        List<Long> uids=null;
        //如果where为空
        //则在所有的字段查找是否有索引字段
        //选择第一个索引字段作为查询字段条件
        if (where == null) {
            for (Field field : fields) {
                if (field.isIndexed()) {
                    fd = field;
                    break;
                }
            }
            l0 = 0;
            r0 = Long.MAX_VALUE;
            single = true;
        } else {
            //查找与查询条件匹配的字段
            //并检查该字段是否有索引
            for (Field field : fields) {
                    if (field.isIndexed())
                    {
                        fd = field;
                        break;
                    }
            }
            if (fd == null) {
                throw Error.FieldNotFoundException;
            }
            alluids = fd.search(0, Long.MAX_VALUE);
            flag = new boolean[alluids.size()];
            right_flag=new boolean[alluids.size()];
            list = new ArrayList<>();
            for (Long uid : alluids) {
                byte[] read = ((TableManagerImpl) tbm).vm.read(xid, uid);
                //因为删除数据时，只是把dataitem的标志位设置为0
                //它本身的数据还是会存在索引树、文件当中，所以在读取数据的时候就要注意树否为空
                //否则会出现空指针的情况
                if(read==null)
                {
                    list.add(new HashMap<>());
                    continue;
                }
                Map<String, Object> map = parseEntry(read);
                list.add(map);
            }
            CalWhereRes res = calWhere(fd, where);
            l0 = res.l0;
            r0 = res.r0;
            l1 = res.l1;
            r1 = res.r1;
            fieldLeft = res.fieldLeft;
            fieldRight = res.fieldRight;
            single = res.single;
        }
        //判断字段值是否是索引，不是的话就用全表扫描
        if(where!=null)
        {
            if(fieldLeft.equals(fd.fieldName))
            {
                uids = fd.search(l0, r0);
            }
            //在全部数据，list当中，找出符合条件的数据，并且将对应的flag设置为true
            //然后根据flag和alluid，给left-res赋值，代表where左边的范围
            else
            {
                for (int i = 0; i < list.size(); i++) {
                    Map<String, Object> map = list.get(i);
                    if (map.size() == 0) continue;
                    int temp = (int) map.get(fieldLeft);
                    if (temp >= l0 && temp <= r0) {
                        flag[i] = true;
                    }
                }
                left_res = new ArrayList<>();
                for (int j = 0; j < alluids.size(); j++) {
                    if (flag[j]) left_res.add(alluids.get(j));
                }
                uids = left_res;
            }
            if(!single)
                {
                    //右边的表达式是索引的情况
                    if(fieldRight.equals(fd.fieldName))
                    {
                        List<Long> tmp = fd.search(l1, r1);
                        //根据where的符号来决定是取交集还是并集
                        if(where.logicOp.equals("and")) uids.retainAll(tmp);
                        else
                        {
                            Set<Long> mergedSet = new HashSet<>(uids);
                            mergedSet.addAll(tmp);
                            // 将 Set 转换回列表得到最终结果
                            uids = new ArrayList<>(mergedSet);
                        }
                    }
                    //右边的表达式不是索引的情况
                    else
                    {
                        for(int i=0;i<list.size();i++)
                        {
                            Map<String, Object> map = list.get(i);
                            if(map.size()==0) continue;
                            int temp = (int) map.get(fieldRight);
                            if (temp >= l1 && temp <= r1) {
                                right_flag[i]=true;
                            }
                        }
                        right_res = new ArrayList<>();
                        for(int j=0;j<alluids.size();j++)
                        {
                            if(right_flag[j]) right_res.add(alluids.get(j));
                        }
                        //如果两边表达式相同取交集，不同的话就取并集。
                        if(where.logicOp.equals("and"))uids.retainAll(right_res);
                        else
                        {
                            Set<Long> mergedSet = new HashSet<>(uids);
                            mergedSet.addAll(right_res);
                            // 将 Set 转换回列表得到最终结果
                            uids = new ArrayList<>(mergedSet);
                        }

                    }
                }
            }
        else
        {
            //根据where获得的字段值范围，来查找字段的uid
            uids = fd.search(l0, r0);
            if (!single)
            {
            List<Long> tmp = fd.search(l1, r1);
            uids.addAll(tmp);
            }
        }


        //判断是否有notfLAG
        if (where != null && where.Notflag) {
            List<Long> all_uids = fd.search(0, Long.MAX_VALUE);
            for (Long uid : uids) {
                all_uids.remove(uid);
            }
            return all_uids;
        }
        return uids;
    }

    class CalWhereRes {
        long l0, r0, l1, r1;
        boolean single;
        String fieldLeft, fieldRight;
    }

    //字段名
    private CalWhereRes calWhere(Field fd, Where where) throws Exception {
        CalWhereRes res = new CalWhereRes();
        switch (where.logicOp) {
            case "":
                res.single = true;
                FieldCalRes r = fd.calExp(where.singleExp1);
                res.l0 = r.left;
                res.r0 = r.right;
                res.fieldLeft = r.field;
                break;
            case "or":
                res.single = false;
                r = fd.calExp(where.singleExp1);
                res.l0 = r.left;
                res.r0 = r.right;
                res.fieldLeft = r.field;
                r = fd.calExp(where.singleExp2);
                res.l1 = r.left;
                res.r1 = r.right;
                res.fieldRight = r.field;
                break;
            case "and":
                res.single = false;
                r = fd.calExp(where.singleExp1);
                res.l0 = r.left;
                res.r0 = r.right;
                res.fieldLeft = r.field;
                r = fd.calExp(where.singleExp2);
                res.l1 = r.left;
                res.r1 = r.right;
                res.fieldRight = r.field;
                //合并范围
                if (res.l1 > res.l0) res.l0 = res.l1;
                if (res.r1 < res.r0) res.r0 = res.r1;
                break;
            default:
                throw Error.InvalidLogOpException;
        }
        return res;
    }

    //将filename-key这一map转化成string，加入到stringbuilder当中
    //为了打印出来
    private String printEntry(Map<String, Object> entry) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            sb.append(field.printValue(entry.get(field.fieldName)));
            if (i == fields.size() - 1) {
                sb.append("]");
            } else {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    //把要更改的数据传进来
    //
    private Map<String, Object> parseEntry(byte[] raw) {
        int pos = 0;
        Map<String, Object> entry = new HashMap<>();
        for (Field field : fields) {
            ParseValueRes r = field.parserValue(Arrays.copyOfRange(raw, pos, raw.length));
            entry.put(field.fieldName, r.v);
            pos += r.shift;
        }
        return entry;
    }

    //将map转化为二进制数组
    //只把里面的值存进字节数组
    private byte[] entry2Raw(Map<String, Object> entry) {
        byte[] raw = new byte[0];
        for (Field field : fields) {
            raw = Bytes.concat(raw, field.value2Raw(entry.get(field.fieldName)));
        }
        return raw;
    }

    //把表的基本信息转化成string
    //为了打印出来
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        sb.append(name).append(": ");
        for (Field field : fields) {
            sb.append(field.toString());
            if (field == fields.get(fields.size() - 1)) {
                sb.append("}");
            } else {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
