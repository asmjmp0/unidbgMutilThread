package com;

import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.Symbol;
import com.github.unidbg.arm.context.EditableArm64RegisterContext;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.android.AndroidARMEmulator;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.unix.UnixSyscallHandler;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.IOException;

public class ThreadTest extends AbstractJni {
    private final AndroidARMEmulator emulator;
    private final VM vm;
    private static final File APK_FILE = new File("./unidbg-android/src/test/java/com/app-debug.apk");
    private final Module mymd;
    private final DvmClass Native;

    private ThreadTest(){

        emulator = new AndroidARMEmulator("test");
        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));


        vm = emulator.createDalvikVM(APK_FILE);
        vm.setJni(this);
        vm.setVerbose(true);

        emulator.getSyscallHandler().setVerbose(true);

        DalvikModule dm = vm.loadLibrary(new File("./unidbg-android/src/libso/libnative-lib.so"), false);

        mymd=dm.getModule();
        dm.callJNI_OnLoad(emulator);
        Native = vm.resolveClass("com.mpt.jnithread.MainActivity".replace(".", "/"));
        DvmObject ret=Native.callStaticJniMethodObject(emulator,"stringFromJNI()Ljava/lang/String;");
        System.err.println(ret.toString());
    }
    private void destroy() throws IOException {
        emulator.close();
        System.out.println("destroy");
    }
    public static void main(String[] args) throws Exception {
        ThreadTest test = new ThreadTest();
        test.destroy();
    }
}
