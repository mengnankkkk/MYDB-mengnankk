package com.mengnankk.mydatabase.common;

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
    public static final Exception FieldNotFoundException = new RuntimeException("Field not found!");
    public static final Exception FieldNotIndexedException = new RuntimeException("Field not indexed!");
    public static final Exception InvalidLogOpException = new RuntimeException("Invalid logic operation!");
    public static final Exception InvalidValuesException = new RuntimeException("Invalid values!");
    public static final Exception DuplicatedTableException = new RuntimeException("Table already exists!");
    public static final Exception TableNotFoundException = new RuntimeException("Table not found!");

    // parser
    public static final Exception InvalidCommandException = new RuntimeException("Invalid command!");
    public static final Exception TableNoIndexException = new RuntimeException("Table must have at least one index field!");
    public static final Exception InvalidTableNameException = new RuntimeException("Invalid table name!");
    public static final Exception InvalidFieldNameException = new RuntimeException("Invalid field name!");
    public static final Exception InvalidFieldTypeException = new RuntimeException("Invalid field type!");
    public static final Exception InvalidIndexException = new RuntimeException("Invalid index field!");
    public static final Exception InvalidSyntaxException = new RuntimeException("Syntax error!");
    public static final Exception NoFieldsException = new RuntimeException("Table must have at least one field!");
    public static final Exception NoIndexFieldsException = new RuntimeException("Table must have at least one index field!");
    public static final Exception IndexFieldNotFoundException = new RuntimeException("Index field not found in table fields!");
    public static final Exception InvalidStatementException = new RuntimeException("Invalid statement format!");

    // transport
    public static final Exception InvalidPkgDataException = new RuntimeException("Invalid package data!");

    // server
    public static final Exception NestedTransactionException = new RuntimeException("Nested transaction not supported!");
    public static final Exception NoTransactionException = new RuntimeException("Not in transaction!");

    // launcher
    public static final Exception InvalidMemException = new RuntimeException("Invalid memory!");

    // new additions
    public static final Exception TableAlreadyExistsException = new RuntimeException("表已存在");
    public static final Exception DataTooLongException = new RuntimeException("数据长度超出限制");
}
