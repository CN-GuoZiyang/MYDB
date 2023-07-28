package top.guoziyang.mydb.backend.parser.statement;
//标记create 语句里面的属性
public class Create {
    public String tableName;
    public String[] fieldName;
    public String[] fieldType;
    public String[] index;
}
