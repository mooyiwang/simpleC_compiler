package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;
import com.sun.jdi.event.StepEvent;

import java.time.format.SignStyle;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * DFA状态
     */
    private enum State{
        S0, S1, S2, S3, S4, S5
    }


    /**
     * 读入文件的缓冲和输出的tokens列表
     */
    private List<String> lines;
    private List<Token> tokens;


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        lines = FileUtils.readLines(path);
        //throw new NotImplementedException();
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        tokens = new ArrayList<>();
        int start, current;  //两个指针
        State curState = State.S0;
        for(String line : lines){
            start = 0;
            current = 0;
            for(; start < line.length() && current < line.length(); ){
                    char ch = line.charAt(current);
                    switch (curState) {
                        case S0 -> {
                            if (Character.isSpaceChar(ch)) {
                                curState = State.S0;
                                start++;
                                current++;
                            } else if (Character.isAlphabetic(ch)) {
                                curState = State.S1;
                                current++;
                            } else if (Character.isDigit(ch) && ch != '0') {
                                curState = State.S3;
                                current++;
                            } else {
                                curState = State.S5;
                            }
                        }
                        case S1 -> {
                            if (Character.isDigit(ch) || Character.isAlphabetic(ch)) {
                                curState = State.S1;
                                current++;
                            } else {
                                curState = State.S2;
                            }

                        }
                        case S3 -> {
                            if (Character.isDigit(ch)) {
                                curState = State.S3;
                                current++;
                            } else {
                                curState = State.S4;
                            }

                        }
                        //关键字或者标识符的接受态
                        case S2 -> {
                            String sub = line.substring(start, current);
                            if ("int".equals(sub) || "return".equals(sub)) {
                                tokens.add(Token.simple(sub));
                            } else {
                                tokens.add(Token.normal("id", sub));
                                symbolTable.add(sub);
                            }
                            start = current;
                            curState = State.S0;
                        }
                        //整型常数接受态
                        case S4 -> {
                            String sub = line.substring(start, current);
                            tokens.add(Token.normal("IntConst", sub));
                            start = current;
                            curState = State.S0;
                        }
                        //运算符（不含==， &&等）， 分界符的接受态
                        case S5 -> {
                            String sub = line.substring(start, ++current);
                            if (";".equals(sub)) {
                                tokens.add(Token.simple("Semicolon"));
                            } else {
                                tokens.add(Token.simple(sub));
                            }
                            start = current;
                            curState = State.S0;
                        }
                        default -> {
                            curState = State.S0;
                        }
                    }
            }
        }
        tokens.add(Token.eof());

//        throw new NotImplementedException();
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokens;
//        throw new NotImplementedException();
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
