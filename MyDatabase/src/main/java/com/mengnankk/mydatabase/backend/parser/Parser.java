package com.mengnankk.mydatabase.backend.parser;

import java.util.ArrayList;
import java.util.List;

import com.mengnankk.mydatabase.backend.parser.statement.Abort;
import com.mengnankk.mydatabase.backend.parser.statement.Begin;
import com.mengnankk.mydatabase.backend.parser.statement.Commit;
import com.mengnankk.mydatabase.backend.parser.statement.Create;
import com.mengnankk.mydatabase.backend.parser.statement.Delete;
import com.mengnankk.mydatabase.backend.parser.statement.Drop;
import com.mengnankk.mydatabase.backend.parser.statement.Insert;
import com.mengnankk.mydatabase.backend.parser.statement.Select;
import com.mengnankk.mydatabase.backend.parser.statement.Show;
import com.mengnankk.mydatabase.backend.parser.statement.SingleExpression;
import com.mengnankk.mydatabase.backend.parser.statement.Update;
import com.mengnankk.mydatabase.backend.parser.statement.Where;
import com.mengnankk.mydatabase.common.Error;

public class Parser {
    public static Object Parse(byte[] statement) throws Exception {
        Tokenizer tokenizer = new Tokenizer(statement);
        String token = tokenizer.peek().toLowerCase();  // Convert to lowercase
        tokenizer.pop();

        Object stat = null;
        Exception statErr = null;
        try {
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
                    String nextToken = tokenizer.peek().toLowerCase();
                    if(!"table".equals(nextToken)) {
                        throw new RuntimeException("Syntax error: 'create' must be followed by 'table'");
                    }
                    tokenizer.pop(); // consume 'table'
                    stat = parseCreate(tokenizer);
                    break;
                case "drop":
                    nextToken = tokenizer.peek().toLowerCase();
                    if(!"table".equals(nextToken)) {
                        throw new RuntimeException("Syntax error: 'drop' must be followed by 'table'");
                    }
                    tokenizer.pop(); // consume 'table'
                    stat = parseDrop(tokenizer);
                    break;
                case "select":
                    stat = parseSelect(tokenizer);
                    break;
                case "insert":
                    nextToken = tokenizer.peek().toLowerCase();
                    if(!"into".equals(nextToken)) {
                        throw new RuntimeException("Syntax error: 'insert' must be followed by 'into'");
                    }
                    tokenizer.pop(); // consume 'into'
                    stat = parseInsert(tokenizer);
                    break;
                case "delete":
                    nextToken = tokenizer.peek().toLowerCase();
                    if(!"from".equals(nextToken)) {
                        throw new RuntimeException("Syntax error: 'delete' must be followed by 'from'");
                    }
                    tokenizer.pop(); // consume 'from'
                    stat = parseDelete(tokenizer);
                    break;
                case "update":
                    stat = parseUpdate(tokenizer);
                    break;
                case "show":
                    stat = parseShow(tokenizer);
                    break;
                default:
                    String[] validCommands = {"begin", "commit", "abort", "create table", "drop table", "select", "insert into", "delete from", "update", "show"};
                    StringBuilder sb = new StringBuilder();
                    sb.append("Invalid command: '").append(token).append("'\n");
                    sb.append("Valid commands are:\n");
                    for (String cmd : validCommands) {
                        sb.append("  - ").append(cmd).append("\n");
                    }
                    throw new RuntimeException(sb.toString());
            }
        } catch(Exception e) {
            statErr = e;
        }
        try {
            String next = tokenizer.peek();
            if(!"".equals(next)) {
                byte[] errStat = tokenizer.errStat();
                statErr = new RuntimeException("Syntax error: Statement not properly terminated\n" +
                    "Current position: " + new String(errStat, "UTF-8") + "\n" +
                    "Unexpected token: '" + next + "'");
            }
        } catch(Exception e) {
            e.printStackTrace();
            byte[] errStat = tokenizer.errStat();
            statErr = new RuntimeException("Syntax error: " + new String(errStat, "UTF-8"));
        }
        if(statErr != null) {
            throw statErr;
        }
        return stat;
    }

    private static Show parseShow(Tokenizer tokenizer) throws Exception {
        String tmp = tokenizer.peek();
        if("".equals(tmp)) {
            return new Show();
        }
        throw Error.InvalidCommandException;
    }

    private static Update parseUpdate(Tokenizer tokenizer) throws Exception {
        Update update = new Update();
        update.tableName = tokenizer.peek();
        tokenizer.pop();

        if(!"set".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();

        update.fieldName = tokenizer.peek();
        tokenizer.pop();

        if(!"=".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();

        update.value = tokenizer.peek();
        tokenizer.pop();

        String tmp = tokenizer.peek();
        if("".equals(tmp)) {
            update.where = null;
            return update;
        }

        update.where = parseWhere(tokenizer);
        return update;
    }

    private static Delete parseDelete(Tokenizer tokenizer) throws Exception {
        Delete delete = new Delete();

        if(!"from".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();

        String tableName = tokenizer.peek();
        if(!isName(tableName)) {
            throw Error.InvalidCommandException;
        }
        delete.tableName = tableName;
        tokenizer.pop();

        delete.where = parseWhere(tokenizer);
        return delete;
    }

    private static Insert parseInsert(Tokenizer tokenizer) throws Exception {
        Insert insert = new Insert();

        // Get table name
        String tableName = tokenizer.peek();
        if(!isName(tableName)) {
            throw Error.InvalidTableNameException;
        }
        insert.tableName = tableName;
        tokenizer.pop();

        // Check 'values' keyword
        String next = tokenizer.peek().toLowerCase();
        if(!"values".equals(next)) {
            throw new RuntimeException("Syntax error: 'insert into' must be followed by 'values'");
        }
        tokenizer.pop();

        // Parse values
        List<String> values = new ArrayList<>();
        while(true) {
            String value = tokenizer.peek();
            if("".equals(value)) {
                if(values.isEmpty()) {
                    throw new RuntimeException("Syntax error: 'values' must be followed by at least one value");
                }
                break;
            }
            values.add(value);
            tokenizer.pop();
        }
        insert.values = values.toArray(new String[values.size()]);

        return insert;
    }

    private static Select parseSelect(Tokenizer tokenizer) throws Exception {
        Select read = new Select();

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

        if(!"from".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();

        String tableName = tokenizer.peek();
        if(!isName(tableName)) {
            throw Error.InvalidCommandException;
        }
        read.tableName = tableName;
        tokenizer.pop();

        String tmp = tokenizer.peek();
        if("".equals(tmp)) {
            read.where = null;
            return read;
        }

        read.where = parseWhere(tokenizer);
        return read;
    }

    private static Where parseWhere(Tokenizer tokenizer) throws Exception {
        Where where = new Where();

        if(!"where".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();

        SingleExpression exp1 = parseSingleExp(tokenizer);
        where.singleExp1 = exp1;

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

        SingleExpression exp2 = parseSingleExp(tokenizer);
        where.singleExp2 = exp2;

        if(!"".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        return where;
    }

    private static SingleExpression parseSingleExp(Tokenizer tokenizer) throws Exception {
        SingleExpression exp = new SingleExpression();
        
        String field = tokenizer.peek();
        if(!isName(field)) {
            throw Error.InvalidCommandException;
        }
        exp.field = field;
        tokenizer.pop();

        String op = tokenizer.peek();
        if(!isCmpOp(op)) {
            throw Error.InvalidCommandException;
        }
        exp.compareOp = op;
        tokenizer.pop();

        exp.value = tokenizer.peek();
        tokenizer.pop();
        return exp;
    }

    private static boolean isCmpOp(String op) {
        return ("=".equals(op) || ">".equals(op) || "<".equals(op));
    }

    private static boolean isLogicOp(String op) {
        return ("and".equals(op) || "or".equals(op));
    }

    private static Drop parseDrop(Tokenizer tokenizer) throws Exception {
        if(!"table".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();

        String tableName = tokenizer.peek();
        if(!isName(tableName)) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();

        if(!"".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        
        Drop drop = new Drop();
        drop.tableName = tableName;
        return drop;
    }

    private static Create parseCreate(Tokenizer tokenizer) throws Exception {
        Create create = new Create();
        
        // Get table name
        String name = tokenizer.peek();
        if(!isName(name)) {
            throw Error.InvalidTableNameException;
        }
        create.tableName = name;
        tokenizer.pop();

        // Check left parenthesis
        String next = tokenizer.peek();
        if(!"(".equals(next)) {
            throw new RuntimeException("Syntax error: Field definitions must be enclosed in parentheses after table name");
        }
        tokenizer.pop();

        // Parse field definitions
        List<String> fNames = new ArrayList<>();
        List<String> fTypes = new ArrayList<>();
        boolean hasFields = false;
        
        while(true) {
            String field = tokenizer.peek();
            if(")".equals(field)) {
                if(!hasFields) {
                    throw Error.NoFieldsException;
                }
                break;
            }

            if(!isName(field)) {
                throw Error.InvalidFieldNameException;
            }
            tokenizer.pop();

            String fieldType = tokenizer.peek();
            if(!isType(fieldType)) {
                throw new RuntimeException("Invalid field type: '" + fieldType + "'. Supported types are: int32, int64, string");
            }
            fNames.add(field);
            fTypes.add(fieldType);
            hasFields = true;
            tokenizer.pop();
            
            next = tokenizer.peek();
            if(",".equals(next)) {
                tokenizer.pop();
                continue;
            } else if(")".equals(next)) {
                break;
            } else {
                throw new RuntimeException("Syntax error: Field definition must be followed by comma or closing parenthesis");
            }
        }
        tokenizer.pop(); // Consume right parenthesis

        if(fNames.isEmpty()) {
            throw Error.NoFieldsException;
        }
        create.fieldName = fNames.toArray(new String[fNames.size()]);
        create.fieldType = fTypes.toArray(new String[fTypes.size()]);

        // Check index definition
        next = tokenizer.peek();
        if(!"(".equals(next)) {
            throw Error.TableNoIndexException;
        }
        tokenizer.pop();

        next = tokenizer.peek();
        if(!"index".equals(next)) {
            throw new RuntimeException("Syntax error: Index fields must be specified after 'index' keyword");
        }
        tokenizer.pop();

        List<String> indexes = new ArrayList<>();
        while(true) {
            String field = tokenizer.peek();
            if(")".equals(field)) {
                if(indexes.isEmpty()) {
                    throw Error.NoIndexFieldsException;
                }
                break;
            }
            if(!isName(field)) {
                throw Error.InvalidIndexException;
            }
            // Check if index field exists in field list
            boolean found = false;
            for(String fName : fNames) {
                if(fName.equals(field)) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                throw Error.IndexFieldNotFoundException;
            }
            indexes.add(field);
            tokenizer.pop();
            
            next = tokenizer.peek();
            if(",".equals(next)) {
                tokenizer.pop();
                continue;
            } else if(")".equals(next)) {
                break;
            } else {
                throw new RuntimeException("Syntax error: Index field must be followed by comma or closing parenthesis");
            }
        }
        tokenizer.pop(); // Consume right parenthesis

        create.index = indexes.toArray(new String[indexes.size()]);

        // Check if statement is properly terminated
        if(!"".equals(tokenizer.peek())) {
            throw Error.InvalidStatementException;
        }
        return create;
    }

    private static boolean isType(String tp) {
        return ("int32".equals(tp) || "int64".equals(tp) ||
        "string".equals(tp));
    }

    private static Abort parseAbort(Tokenizer tokenizer) throws Exception {
        if(!"".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        return new Abort();
    }

    private static Commit parseCommit(Tokenizer tokenizer) throws Exception {
        if(!"".equals(tokenizer.peek())) {
            throw Error.InvalidCommandException;
        }
        return new Commit();
    }

    private static Begin parseBegin(Tokenizer tokenizer) throws Exception {
        String isolation = tokenizer.peek();
        Begin begin = new Begin();
        if("".equals(isolation)) {
            return begin;
        }
        if(!"isolation".equals(isolation)) {
            throw Error.InvalidCommandException;
        }

        tokenizer.pop();
        String level = tokenizer.peek();
        if(!"level".equals(level)) {
            throw Error.InvalidCommandException;
        }
        tokenizer.pop();

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

    private static boolean isName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        
        // 检查第一个字符是否为字母
        if (!Tokenizer.isAlphaBeta(name.getBytes()[0])) {
            return false;
        }
        
        // 检查其余字符是否为字母、数字或下划线
        for (int i = 1; i < name.length(); i++) {
            byte b = name.getBytes()[i];
            if (!Tokenizer.isAlphaBeta(b) && !Tokenizer.isDigit(b) && b != '_') {
                return false;
            }
        }
        
        return true;
    }
}
