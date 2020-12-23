package com.github.unidbg.linux;

import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.unix.UnixThread;
import com.sun.jna.Pointer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import unicorn.Arm64Const;
import unicorn.ArmConst;

public class LinuxThread extends UnixThread {

    private static final Log log = LogFactory.getLog(LinuxThread.class);

    // Our 'tls' and __pthread_clone's 'child_stack' are one and the same, just growing in
    // opposite directions.
    private final UnidbgPointer fn;
    private final Pointer arg;

    LinuxThread(Emulator<?> emulator,Pointer child_stack, Pointer fn, Pointer arg) {
        super(emulator.getBackend().context_alloc(),child_stack);
        Backend backend=emulator.getBackend();
        this.child_stack = child_stack;
        this.fn = (UnidbgPointer) arg.getPointer(48);
        this.arg = arg;

        //保存当前线程的上下文
        backend.context_save(emulator.getThreadDispatcher().CurrentThreadContext());
        //修改pc和栈指针
        int cpsr = backend.reg_read(ArmConst.UC_ARM_REG_CPSR).intValue();
        cpsr=cpsr|0b1000000;
        UnidbgPointer this_arg=((UnidbgPointer) this.arg).getPointer(52);
        backend.reg_write(ArmConst.UC_ARM_REG_CPSR,cpsr);
        backend.reg_write(ArmConst.UC_ARM_REG_R0,this_arg==null?0:this_arg.peer);
        backend.reg_write(ArmConst.UC_ARM_REG_PC,this.fn.peer);
        backend.reg_write(ArmConst.UC_ARM_REG_SP,((UnidbgPointer)this.child_stack).peer);
        backend.reg_write(ArmConst.UC_ARM_REG_LR,0xffffff00l);
        //修改指令类型为thumb

        //保存到子线程
        backend.context_save(context);
        //恢复当前线程上下文
        backend.context_restore(emulator.getThreadDispatcher().CurrentThreadContext());


//        Debugger debugger= emulator.attach();
//        debugger.addBreakPoint(this.fn.peer);
//        emulator.getBackend().emu_start(this.fn.peer, 0, 0, 0);

    }


}
