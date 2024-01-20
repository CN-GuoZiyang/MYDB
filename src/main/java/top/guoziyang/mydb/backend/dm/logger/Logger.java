package top.guoziyang.mydb.backend.dm.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import top.guoziyang.mydb.backend.utils.Panic;
import top.guoziyang.mydb.backend.utils.Parser;
import top.guoziyang.mydb.common.Error;

public interface Logger {
    void log(byte[] data);
    void truncate(long x) throws Exception;
    byte[] next();
    void rewind();
    void close();
    //创建一个log文件
    //生成文件流和文件通道去获取这个文件
    //
    public static Logger create(String path) {
        File f = new File(path+LoggerImpl.LOG_SUFFIX);
        try {
            if(!f.createNewFile()) {
                Panic.panic(Error.FileExistsException);
            }
        } catch (Exception e) {
            Panic.panic(e);
        }
        if(!f.canRead() || !f.canWrite()) {
            Panic.panic(Error.FileCannotRWException);
        }

        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "rw");
            fc = raf.getChannel();
        } catch (FileNotFoundException e) {
           Panic.panic(e);
        }
        //把0写进去，是为了标志日志的初始状态
        //因为日志的格式包括xchecksum，所以初始化的时候
        //要将xchecksum初始化为0
        ByteBuffer buf = ByteBuffer.wrap(Parser.int2Byte(0));
        try {
            fc.position(0);
            fc.write(buf);
            //metadata代表文件的权限，创建时间，修改时间。
            fc.force(false);
        } catch (IOException e) {
            Panic.panic(e);
        }

        return new LoggerImpl(raf, fc, 0);
    }
    //通过打开日志文件的方式，来创建一个logger
    //这里就不需要给日志文件赋初值了
    //直接创建文件流，通道读取文件就行了。
    public static Logger open(String path) {
        File f = new File(path+LoggerImpl.LOG_SUFFIX);
        if(!f.exists()) {
            Panic.panic(Error.FileNotExistsException);
        }
        if(!f.canRead() || !f.canWrite()) {
            Panic.panic(Error.FileCannotRWException);
        }

        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "rw");
            fc = raf.getChannel();
        } catch (FileNotFoundException e) {
           Panic.panic(e);
        }

        LoggerImpl lg = new LoggerImpl(raf, fc);
        lg.init();

        return lg;
    }
}
