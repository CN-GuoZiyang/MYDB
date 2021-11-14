package top.guoziyang.mydb.backend.common;

public class Error {
    // common
    public static final Exception CacheFullException = new RuntimeException("Cache is full!");
    public static final Exception FileExistsException = new RuntimeException("File already exists!");
    public static final Exception FileNotExistsException = new RuntimeException("File does not exists!");
    public static final Exception FileCannotRWException = new RuntimeException("File cannot read or write!");

    // dm
    public static final Exception BadLogFileException = new RuntimeException("Bad log file!");
    public static final Exception MemTooSmallException = new RuntimeException("Memory too small!");
    public static final Exception DataTooLargeException = new RuntimeException("Data too large!");
    public static final Exception DatabaseBusyException = new RuntimeException("Database is busy!");

    // tm
    public static final Exception BadXIDFileException = new RuntimeException("Bad XID file!");

    // vm
    public static final Exception DeadlockException = new RuntimeException("Deadlock!");
    public static final Exception ConcurrentUpdateException = new RuntimeException("Concurrent update issue!");
    public static final Exception NullEntryException = new RuntimeException("Null entry!");

    // tbm
    public static final Exception InvalidFieldException = new RuntimeException("Invalid field type!");
}
