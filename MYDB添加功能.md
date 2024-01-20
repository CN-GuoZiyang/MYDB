# MYDB添加功能

## 增加Where查找不等于的功能

在当前项目中， where只能查找等于，小于，大于，不能查找不等于，我在此基础上添加了不等于的功能。

希望达到的目标是：select  * from table where not id = 10;这条语句能够成立。

核心思路是

1. 先检查where语句中是否有not，没有的话就按原样执行，有的话执行2
2. where语句的思路是求出where语句中的范围，比如b>10,最后得出的范围就是11-Long.Max_value；如果where语句有not的话，就对这个范围取反。也就是求出id所有的值放入集合当中，然后把集合中对应的范围值取掉，最后返回这个集合

### 具体操作

在Where类当中添加一个flag，用于判断Where是否进行查找不等于的字段

```
public class Where {
    public SingleExpression singleExp1;
    public String logicOp;
    public SingleExpression singleExp2;
    public boolean Notflag=false;
}
```

在parser类当中的parser.Where方法中，添加判断是否有NOT

```
 int notPos = tokenizer.getPos();
 				//保存tokenizer里的pos，方便回溯
        System.out.println(notPos);
        String not = tokenizer.peek();
        if (not.equals("not"))
        {
        		//如果有not，就更新where的flag为true
        		//并且将tokenizer刷新
            where.Notflag=true;
            tokenizer.pop();
        }
        else
        {
        		//没有的话，将将pos设置回原来的值
        		//刷新tokenizer
        		//如果不刷新的话，tokenizer就永远不变了
            tokenizer.setPos(notPos);
            tokenizer.pop();
        }
```

接着在table类的parseWhere方法里面

在查找完字段的范围后，判断where.notFlag是否为true，如果是的话，就要对该范围进行取反。

```
//判断是否有notfLAG
        if(where != null && where.Notflag)
        {
            List<Long> alluids = fd.search(0,Long.MAX_VALUE);
            for(Long uid:uids)
            {
                alluids.remove(uid);
            }
            return alluids;
        }
```

### 遇到的问题

忘记对tokenizer进行pop的操作，导致tokenizer不能刷新，一直是一个值。

还有就是更改代码的时候，忘记先停止运行了...

## 修改Select功能

目前Select语句存在缺陷，体现在

```
Select * from table 
Select id from table 
```

这两个Sql语句是没有区别的，最后得到的结果都是第一个Sql语句的结果

​		核心思路：

1. 先弄清楚字段插入文件时，是以什么样的形式插入的
2. 在Table类的insert方法中可以看到，它是把值转化成map的形式，以key-10，value-10的方式进行插入，再将这个map转化成byte数组的格式插入文件中
3. 在Table类的insert方法中可以看到，它的读取也是这样的，先把byte数组转化成map，再把里面的数据读出来
4. 我们只需要按照提供的field，来读取对应的数据就可以了。

### 具体操作

在Table类的read方法里，我们在获取了范围内的UID后就可以从数据文件中根据UID把数据读出来了。

我们在这里添加一个判断逻辑，如果Select.field里面是*的话，就按照原来的逻辑就行，即输出列的全部字段信息。

如果不是列的话，就根据Select.field一个个读取出来。

```
if(read.fields[0].equals("*"))sb.append(printEntry(entry)).append("\n");
            else
            {
                sb.append("[");
                for(String field:read.fields)
                {
                    sb.append(entry.get(field)).append(",");
                }
                sb.deleteCharAt(sb.length()-1);
                sb.append("]");
                sb.append("\n");
            }
```

### 遇到的问题

忘记了String字符串判断是否相等的时候，不是用==，而是使用equals

## 全表扫描

在使用Select语句查询字段的时候呢，目前只支持基于索引去查询，如果使用不是索引的字段去查询的话呢，就会报错，现在我要增加一个全表扫描的功能

### 核心思路

1. 通过该表的索引，获取对应B+树上的所有数据
2. 然后再去这些数据里进行查询即可。

### 具体操作

1. 修改了Field类里的

   ```
    public FieldCalRes calExp(SingleExpression exp) throws Exception {
           Object v = null;
           FieldCalRes res = new FieldCalRes();
           res.field = exp.field;
   ```

   目的是为了获取单个表达式里属性的值，即id>10里的id。

2.修改FieldCalRes类

```
public class FieldCalRes {
    public long left;
    public long right;
    public  String field;
}
```

增加了一个filed属性，用来记录单个表达式的值

3.修改Table中的parseWhere方法

1. 首先获取当前表所有的数据，通过索引树获取

   ```
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
   ```

2. 接着开始解析Where，如果表达式的属性有索引的话，就正常处理

3. 如果不是的话，就进行全表扫描，实现方式为

   ```
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
   ```

   在前面获取的该表所有数据里，一个个查找符合条件的数据。

4. 判断是否是单一表达式，如果是的话，直接返回uid就行了

5. 不是的话，就和前面一样，根据表达式的属性来求范围，求完之后要处理左右两边范围的合并，这里需要判断where.logicop是什么，如果是and就取交集，如果是or的话就取交集。

   ```
   if(where.logicOp.equals("and"))uids.retainAll(right_res);
                           else
                           {
                               Set<Long> mergedSet = new HashSet<>(uids);
                               mergedSet.addAll(right_res);
                               // 将 Set 转换回列表得到最终结果
                               uids = new ArrayList<>(mergedSet);
                           }
   ```

### 遇到的问题

1. 第一个遇到的问题是：获取到当前表的所有数据的alluid后，要读取当前这些uid将其具体数据放入list，在后面的全表扫描中，我们需要在这个list当中查找符合条件的数据，并返回对应的uid；在当前使用的数据结构，没办法把uid和list放在一起，所以我选择创建一个flag数组，用来记录对应的位置是否满足条件，最后根据flag的值来返回对应的uid。
2. 第二个问题：由于在本项目中，删除数据是设置dataitem的flag，而不是将其从数据文件中清除，所以对应数据的uid其实还是存在索引树当中，我们获取当前表所有数据时，就会获得已删除的数据的uid，只不过对应的具体数据是读取不到的；这样情况下，后面进行全表扫描时，会进入map来查找数据，有可能会触发空指针异常，所以我在这里添加了一个判断，判断map.size是否为空来规避。