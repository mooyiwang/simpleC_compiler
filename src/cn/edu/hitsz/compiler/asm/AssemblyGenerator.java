package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


/**
 * 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    /**
     * 需要的数据结构：
     *
     *     <li>中间代码 instructions : list (预处理后)</li>
     *     <li>汇编代码 assembly : list</li>
     *     <li>寄存器分配表 allocMap : BMap</li>
     *     <li>变量最后出现位置表 lastOccur: HashMap<IRVariable, Integer></li>
     *     <li>寄存器 regs : list</li>
     *
     */

    private List<Instruction> instructions;
    private HashMap<IRVariable, Integer> lastOccur = new HashMap<>();
    private List<String>  assembly = new LinkedList<>();
    private BMap<ASMReg, IRVariable> allocMap = new BMap<>();
    private List<ASMReg> regs = new ArrayList<>();

    /**
     * 在lastOccur表中添加记录，更新某个变量出现的位置
     * @param var
     * @param position
     */
    public void addEntry( IRVariable var, int position){
        if(!(lastOccur.containsKey(var))){
            lastOccur.put(var, position);
        }
        else{
            if(lastOccur.get(var) < position){
                lastOccur.put(var, position);
            }
        }
    }

    /**
     * 扫描中间代码，生成lastOccur表
     * 只有变量才能加入lastOccur表
     */
    public void makeLastOccurTable(){
        for(Instruction ins : instructions){
            InstructionKind kind = ins.getKind();
            if(kind == InstructionKind.RET){
                IRValue ret = ins.getReturnValue();
                if(ret.isIRVariable()){
                    addEntry((IRVariable) ret, instructions.indexOf(ins));
                }
            }
            else if(kind == InstructionKind.MOV){
                IRVariable ret = ins.getResult();
                IRValue from = ins.getFrom();
                if(ret.isIRVariable()){
                    addEntry(ret, instructions.indexOf(ins));
                }
                if(from.isIRVariable()){
                    addEntry((IRVariable)from, instructions.indexOf(ins));
                }
            }
            else{
                IRVariable ret = ins.getResult();
                IRValue lhs = ins.getLHS();
                IRValue rhs = ins.getRHS();
                if(ret.isIRVariable()){
                    addEntry(ret, instructions.indexOf(ins));
                }
                if(lhs.isIRVariable()){
                    addEntry((IRVariable)lhs, instructions.indexOf(ins));
                }
                if(rhs.isIRVariable()){
                    addEntry((IRVariable)rhs, instructions.indexOf(ins));
                }
            }
        }
    }

    /**
     * lhs和rhs都为立即数的情况下，直接计算出值，将其变为mv指令
     * @param inst
     * @param kind
     * @param lhs_imm
     * @param rhs_imm
     * @return
     */
    public Instruction bin2Mov(Instruction inst, InstructionKind kind, int lhs_imm, int rhs_imm){
        switch (kind){
            case ADD -> {
                return Instruction.createMov(inst.getResult(), IRImmediate.of(lhs_imm + rhs_imm));
            }
            case MUL -> {
                return Instruction.createMov(inst.getResult(), IRImmediate.of(lhs_imm * rhs_imm));
            }
            case SUB -> {
                return Instruction.createMov(inst.getResult(), IRImmediate.of(lhs_imm - rhs_imm));
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * 中间代码预处理
     * @param instructions
     * @return
     */
    public List<Instruction> preprocessing(List<Instruction> instructions){
        List<Instruction> ret = new LinkedList<>();
        for(int idx = 0; idx < instructions.size(); idx++){
            ret.add(instructions.get(idx));
        }
        for(int idx = 0; idx < instructions.size(); idx++){
            Instruction ins = instructions.get(idx);
            InstructionKind kind = ins.getKind();
            if(kind.isBinary()){
                IRValue lhs = ins.getLHS();
                IRValue rhs = ins.getRHS();
                if(lhs.isImmediate() && rhs.isImmediate()){
                    // op z imm imm -> op z immximm
                    Instruction mov = bin2Mov(ins, kind, ((IRImmediate)lhs).getValue(), ((IRImmediate)rhs).getValue());
                    ret.add(ret.indexOf(ins), mov);
                    ret.remove(ins);
                }
                else if(lhs.isImmediate() && rhs.isIRVariable()) {
                    if(kind == InstructionKind.ADD) {
                        // add z imm x -> add z x imm
                        Instruction add = Instruction.createAdd(ins.getResult(), rhs, lhs);
                        ret.add(ret.indexOf(ins), add);
                        ret.remove(ins);
                    }
                    else if(kind == InstructionKind.SUB){
                        // sub z imm x -> mv z imm; sub z z x;
                        Instruction mov = Instruction.createMov(ins.getResult(), lhs);
                        ret.add(ret.indexOf(ins), mov);
                        Instruction sub = Instruction.createSub(ins.getResult(), ins.getResult(), rhs);
                        ret.add(ret.indexOf(ins), sub);
                        ret.remove(ins);
                    }
                    else if(kind == InstructionKind.MUL){
                        // mul z imm x -> mv z imm; mul z z x;
                        Instruction mov = Instruction.createMov(ins.getResult(), lhs);
                        ret.add(ret.indexOf(ins), mov);
                        Instruction mul = Instruction.createMul(ins.getResult(), ins.getResult(), rhs);
                        ret.add(ret.indexOf(ins), mul);
                        ret.remove(ins);
                    }
                }
                else if(lhs.isIRVariable() && rhs.isImmediate()){
                    if(kind == InstructionKind.MUL){
                        // mul z x imm -> mv z imm; mul z x z
                        Instruction mov = Instruction.createMov(ins.getResult(), rhs);
                        ret.add(ret.indexOf(ins), mov);
                        Instruction mul = Instruction.createMul(ins.getResult(), lhs, ins.getResult());
                        ret.add(ret.indexOf(ins), mul);
                        ret.remove(ins);
                    }
                }
            }
            else if(kind.isUnary()){
                if(kind == InstructionKind.RET){
                    if((instructions.indexOf(ins)) != instructions.size()-1){
                        List<Instruction> ret_1 = new LinkedList<>();
                        for(int idx_1 = 0; idx_1 <= ret.indexOf(ins); idx_1++){
                            ret_1.add(ret.get(idx_1));
                        }
                        return ret_1;
                    }

                }
            }
        }
        return ret;
    }
    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
//        读入前端提供的中间代码并生成所需要的信息
        instructions = new LinkedList<>(originInstructions);
        instructions = preprocessing(instructions);
        FileUtils.writeLines("data/out/intermediate_code_preprocessed.txt", instructions.stream().map(Instruction::toString).toList());
//        throw new NotImplementedException();

    }

    /**
     * 初始化寄存器分配表
     */
    public void bMapInit(){
        ArrayList<String> regStrings = new ArrayList<>();
        for(int i=0; i<7; i++){
            regStrings.add("t%d".formatted(i));
        }
        for(String resString: regStrings){
            regs.add(ASMReg.Reg(resString));
        }
        for(ASMReg reg : regs){
            allocMap.put(reg, null);
        }
    }

    /**
     * 寄存器分配
     * @param irVariable 变量
     * @param position 变量的位置
     * @return 寄存器
     */
    public ASMReg allocReg(IRVariable irVariable, int position){
        if(allocMap.containsValue(irVariable)){
            return allocMap.getByValue(irVariable);
        }
        for(ASMReg reg: regs){
            if(allocMap.getByKey(reg) == null){
                allocMap.put(reg, irVariable);
                return reg;
            }
        }
        for(ASMReg reg: regs){
            if(lastOccur.get(allocMap.getByKey(reg)) < position){
                allocMap.put(reg, irVariable);
                return reg;
            }
        }
        return null;
    }
    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        //  执行寄存器分配与代码生成
        makeLastOccurTable();
        bMapInit();
        assembly.add(".text");
        for(Instruction ins: instructions){
            int position = instructions.indexOf(ins);
            switch (ins.getKind()){
                case ADD -> {
                    IRValue rhs = ins.getRHS();
                    if(rhs.isImmediate()){
                        ASMReg lhsReg = allocReg((IRVariable) ins.getLHS(), position);
                        ASMReg resReg = allocReg(ins.getResult(), position);
                        assembly.add("    addi %s, %s, %s".formatted(resReg.toString(), lhsReg.toString(), rhs.toString()));
                    }
                    else{
                        ASMReg resReg = allocReg(ins.getResult(), position);
                        ASMReg lhsReg = allocReg((IRVariable) ins.getLHS(), position);
                        ASMReg rhsReg = allocReg((IRVariable) rhs, position);
                        assembly.add("    add %s, %s, %s".formatted(resReg.toString(), lhsReg.toString(), rhsReg.toString()));
                    }
                    break;
                }
                case SUB -> {
                    IRValue rhs = ins.getRHS();
                    if(rhs.isImmediate()){
                        ASMReg resReg = allocReg(ins.getResult(), position);
                        ASMReg lhsReg = allocReg((IRVariable) ins.getLHS(), position);
                        assembly.add("    subi %s, %s, %s".formatted(resReg.toString(), lhsReg.toString(), rhs.toString()));
                    }
                    else{
                        ASMReg resReg = allocReg(ins.getResult(), position);
                        ASMReg lhsReg = allocReg((IRVariable) ins.getLHS(), position);
                        ASMReg rhsReg = allocReg((IRVariable) rhs, position);
                        assembly.add("    sub %s, %s, %s".formatted(resReg.toString(), lhsReg.toString(), rhsReg.toString()));
                    }
                    break;
                }
                case MUL -> {
                    ASMReg resReg = allocReg(ins.getResult(), position);
                    ASMReg lhsReg = allocReg((IRVariable) ins.getLHS(), position);
                    ASMReg rhsReg = allocReg((IRVariable) ins.getRHS(), position);
                    assembly.add("    mul %s, %s, %s".formatted(resReg.toString(), lhsReg.toString(), rhsReg.toString()));
                    break;
                }
                case MOV -> {
                    IRValue from = ins.getFrom();
                    if(from.isImmediate()){
                        ASMReg resReg = allocReg(ins.getResult(), position);
                        assembly.add("    li %s, %s".formatted(resReg.toString(), from.toString()));
                    }
                    else{
                        ASMReg resReg = allocReg(ins.getResult(), position);
                        ASMReg fromReg = allocReg((IRVariable) from, position);
                        assembly.add("    mv %s, %s".formatted(resReg.toString(), fromReg.toString()));
                    }
                }
                case RET -> {
                    IRValue ret = ins.getReturnValue();
                    if(ret.isImmediate()){
                        assembly.add("    mv a0, %s".formatted(ret.toString()));
                    }
                    else{
                        ASMReg retReg = allocReg((IRVariable) ret, position);
                        assembly.add("    mv a0, %s".formatted(retReg.toString()));
                    }
                    break;
                }
                default -> {
                    break;
                }
            }
        }
//        throw new NotImplementedException();
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        //  输出汇编代码到文件
        FileUtils.writeLines(path, assembly.stream().map(String::toString).toList());
//        throw new NotImplementedException();
    }
}

