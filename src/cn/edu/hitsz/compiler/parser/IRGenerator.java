package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;

//实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    private List<Instruction> instructions;
    private SymbolTable table;
    private ArrayList<IRValue>  valStack;

    public IRGenerator(){
        instructions = new ArrayList<>();
        valStack = new ArrayList<>();
        valStack.add(null);
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        //
        String name = currentToken.getText();
        String type = currentToken.getKind().getIdentifier();
        if("IntConst".equals(type)){
            valStack.add(IRImmediate.of(Integer.parseInt(name)));
        }
        else if("id".equals(type)){
            valStack.add(IRVariable.named(name));
        }
        else{
            valStack.add(null);
        }
//        throw new NotImplementedException();
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        //
        switch (production.index()){
            case 6 ->{ //S -> id = E;
                IRValue from = valStack.get(valStack.size()-1);
                IRValue to = valStack.get(valStack.size()-3);
                instructions.add(Instruction.createMov((IRVariable) to, from));
                for(int i=0; i<production.body().size(); i++){
                    valStack.remove(valStack.size()-1);
                }
                valStack.add(null);
                break;
            }
            case 7 ->{ //S -> return E;
                instructions.add(Instruction.createRet(valStack.get(valStack.size()-1)));
                for(int i=0; i<production.body().size(); i++){
                    valStack.remove(valStack.size()-1);
                }
                valStack.add(null);
                break;
            }
            case 8 ->{//E -> E + A;
                IRVariable result = IRVariable.temp();
                IRValue left = valStack.get(valStack.size()-3);
                IRValue right = valStack.get(valStack.size()-1);
                instructions.add(Instruction.createAdd(result, left, right));
                for(int i=0; i<production.body().size(); i++){
                    valStack.remove(valStack.size()-1);
                }
                valStack.add(result);
                break;
            }
            case 9 ->{//E -> E - A;
                IRVariable result = IRVariable.temp();
                IRValue left = valStack.get(valStack.size()-3);
                IRValue right = valStack.get(valStack.size()-1);
                instructions.add(Instruction.createSub(result, left, right));
                for(int i=0; i<production.body().size(); i++){
                    valStack.remove(valStack.size()-1);
                }
                valStack.add(result);
                break;
            }
            case 10->{//E -> A;
                //不处理
                break;
            }
            case 11->{//A -> A * B;
                IRVariable result = IRVariable.temp();
                IRValue left = valStack.get(valStack.size()-3);
                IRValue right = valStack.get(valStack.size()-1);
                instructions.add(Instruction.createMul(result, left, right));
                for(int i=0; i<production.body().size(); i++){
                    valStack.remove(valStack.size()-1);
                }
                valStack.add(result);
                break;
            }
            case 12->{//A -> B;
                //不处理
                break;
            }
            case 13->{//B -> ( E );
                //不处理
                IRValue val = valStack.get(valStack.size()-2);
                for(int i=0; i<production.body().size(); i++){
                    valStack.remove(valStack.size()-1);
                }
                valStack.add(val);
                break;
            }
            case 14->{//B -> id;
                //不处理
                break;
            }
            case 15->{//B -> IntConst;
                //不处理
                break;
            }
            default -> {
                for(int i=0; i<production.body().size(); i++){
                    valStack.remove(valStack.size()-1);
                }
                valStack.add(null);
            }
        }
//        throw new NotImplementedException();
    }


    @Override
    public void whenAccept(Status currentStatus) {
        //
//        throw new NotImplementedException();
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        //
        this.table = table;
//        throw new NotImplementedException();
    }

    public List<Instruction> getIR() {
        //
        return instructions;
//        throw new NotImplementedException();
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

