package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.symtab.SymbolTableEntry;

import java.util.ArrayList;

// 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {

    private SymbolTable table;
    private ArrayList<String> nameStack;
    private ArrayList<SourceCodeType> typeStack;

    public SemanticAnalyzer(){
        this.nameStack = new ArrayList<>();
        this.typeStack = new ArrayList<>();
        nameStack.add("NULL");
        typeStack.add(SourceCodeType.NULL);
    }

    @Override
    public void whenAccept(Status currentStatus) {
        // 该过程在遇到 Accept 时要采取的代码动作
//        throw new NotImplementedException();
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        //该过程在遇到 reduce production 时要采取的代码动作
        switch (production.index()){
            case 4 ->{ // S -> D id
                String name = nameStack.get(nameStack.size()-1);
                if(table.has(name)){
                    SymbolTableEntry entry = table.get(name);
                    entry.setType(typeStack.get(typeStack.size()-2));
                    for(int i=0; i<production.body().size(); i++){
                        nameStack.remove(nameStack.size()-1);
                        typeStack.remove(typeStack.size()-1);
                    }
                    nameStack.add("NULL");
                    typeStack.add(SourceCodeType.NULL);
                    break;
                }
            }
            case 5 ->{ // D -> int
                //name type栈都不变
                break;
            }
            default -> {
                for(int i=0; i<production.body().size(); i++){
                    nameStack.remove(nameStack.size()-1);
                    typeStack.remove(typeStack.size()-1);
                }
                nameStack.add("NULL");
                typeStack.add(SourceCodeType.NULL);
                break;
            }
        }
//        throw new NotImplementedException();
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // 该过程在遇到 shift 时要采取的代码动作
        String name = currentToken.getText();
        String type = currentToken.getKind().getIdentifier();
        if("".equals(name)){
            nameStack.add("NULL");
        }
        else{
            nameStack.add(name);
        }
        if("int".equals(type)){
            typeStack.add(SourceCodeType.Int);
        }
        else{
            typeStack.add(SourceCodeType.NULL);
        }
//        throw new NotImplementedException();
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        this.table = table;
//        throw new NotImplementedException();
    }
}

