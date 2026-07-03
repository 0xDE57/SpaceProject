package com.spaceproject.utility;

import java.nio.IntBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

public class GPUUtil {
    private static final IntBuffer intBuffer = BufferUtils.newIntBuffer(16);

    //https://developer.download.nvidia.com/opengl/specs/GL_NVX_gpu_memory_info.txt
    public static final String GL_NVX_gpu_memory_info_ext = "GL_NVX_gpu_memory_info";
    public static final int GL_GPU_MEMORY_INFO_DEDICATED_VIDMEM_NVX = 0x9047; // - dedicated video memory, total size (in kb) of the GPU memory
    public static final int GL_GPU_MEM_INFO_TOTAL_AVAILABLE_MEM_NVX = 0x9048; // - total available memory, total size (in Kb) of the memory available for allocations
    public static final int GL_GPU_MEM_INFO_CURRENT_AVAILABLE_MEM_NVX = 0x9049; // - current available dedicated video memory (in kb), currently unused GPU memory
    public static final int GL_GPU_MEMORY_INFO_EVICTION_COUNT_NVX = 0x904A; // - count of total evictions seen by system
    public static final int GL_GPU_MEMORY_INFO_EVICTED_MEMORY_NVX = 0x904B; // - size of total video memory evicted (in kb)

    public static int getMaxMemoryKB(){
        intBuffer.clear();
        Gdx.gl.glGetIntegerv(GL_GPU_MEM_INFO_TOTAL_AVAILABLE_MEM_NVX, intBuffer);
        return intBuffer.get();
    }

    public static int getAvailableMemoryKB(){
        intBuffer.clear();
        Gdx.gl.glGetIntegerv(GL_GPU_MEM_INFO_CURRENT_AVAILABLE_MEM_NVX, intBuffer);
        return intBuffer.get();
    }

    public static boolean hasMemoryInfo(){
        return Gdx.graphics.supportsExtension(GL_NVX_gpu_memory_info_ext);
    }

}