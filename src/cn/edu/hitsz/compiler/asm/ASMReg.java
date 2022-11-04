package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.IRVariable;

public class ASMReg{

    public String getName() {
        return name;
    }

    public static ASMReg Reg(String name) {
        return new ASMReg(name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ASMReg reg && name.equals(reg.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    private ASMReg(String name) {
        this.name = name;
    }

    private final String name;
}
