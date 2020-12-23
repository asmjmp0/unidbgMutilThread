package com.github.unidbg.arm;

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.unix.UnixThread;
import unicorn.ArmConst;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ThreadDispatcher {
    public final Map<Integer, UnixThread> threadMap = new HashMap<>(5);
    public static volatile int thread_count_index=0;
    public int current_thread_id=0;
    private final Emulator<?> emulator;
    private long hit_count=0;

    public ThreadDispatcher(Emulator<?> emulator){
        this.emulator=emulator;
    }

    public long CurrentThreadContext(){
        return threadMap.get(current_thread_id).context;
    }

    private int GetNextThreadId(){
         Object[] key =  threadMap.keySet().toArray();
         int index=-1;
         for(int i=0;i<key.length;i++){
             if((Integer) key[i]==current_thread_id){
                 index=i;
                 break;
             }
         }
         index=(index+1)%key.length;
         return (Integer) key[index];
    }

    public void ExitCurrentThread(){
        long in_pc=0;
        Backend backend=emulator.getBackend();
        int prepare_to_delect_id=current_thread_id;
        System.out.println("thread " + prepare_to_delect_id+" exit");
        /*获取下个需要调度的线程*/
        current_thread_id=GetNextThreadId();
        threadMap.remove(prepare_to_delect_id);

        backend.context_restore(threadMap.get(current_thread_id).context);

        backend.emu_stop();

        UnidbgPointer pc= UnidbgPointer.pointer(emulator,backend.reg_read(ArmConst.UC_ARM_REG_PC));

        Cpsr cpsr=Cpsr.getArm(backend);
        in_pc=pc.peer;
        if(cpsr.isThumb()){
            in_pc=pc.peer+1;
        }
        System.out.println(this);
        backend.reg_write(ArmConst.UC_ARM_REG_PC,in_pc);
    }

    public void ExchangeThreadImmediately(){
        long in_pc=0;
        Backend backend=emulator.getBackend();
        /*保存当前上下文*/
        backend.context_save(CurrentThreadContext());
        /*选择下个线程*/
        current_thread_id=GetNextThreadId();
        /*恢复下个线程的上下文*/
        backend.context_restore(threadMap.get(current_thread_id).context);

        backend.emu_stop();

        UnidbgPointer pc= UnidbgPointer.pointer(emulator,backend.reg_read(ArmConst.UC_ARM_REG_PC));

        Cpsr cpsr=Cpsr.getArm(backend);
        in_pc=pc.peer;
        if(cpsr.isThumb()){
            in_pc=pc.peer+1;
        }
//        System.out.println(this);
        backend.reg_write(ArmConst.UC_ARM_REG_PC,in_pc);
    }

    public void ExchangeThread(){
        hit_count++;
        if(hit_count%1000==0){
           ExchangeThreadImmediately();
        }
    }

    @Override
    public String toString() {
        return "threadID："+current_thread_id+",is thumb:"+",running......";
    }

}
