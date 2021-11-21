package top.guoziyang.mydb.transport;

public class Package {
    byte[] data;
    Exception err;

    Package(byte[] data, Exception err) {
        this.data = data;
        this.err = err;
    }

    public byte[] getData() {
        return data;
    }

    public Exception getErr() {
        return err;
    }
}
