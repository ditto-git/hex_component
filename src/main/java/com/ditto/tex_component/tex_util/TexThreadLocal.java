package com.ditto.tex_component.tex_util;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.ditto.tex_component.tex_console.entity.TexTemplate;
import com.ditto.tex_component.tex_console.entity.TexTemplateCell;


import java.util.*;

/**
 * 线程本地存储工具类：存储模板相关上下文数据（模板、单元格、表头、公式）
 * 核心特性：
 * 1. 线程安全：每个线程独立持有一份数据，互不干扰
 * 2. 防空保护：未初始化时调用获取方法会抛出明确异常，避免空指针
 * 3. 数据安全：禁止外部直接修改内部集合/映射，保证数据一致性
 * 4. 使用规范：必须先调用 setExTemplate() 初始化，使用后调用 clear() 释放资源
 */
public class TexThreadLocal {

    // 静态常量：线程本地变量（全大写命名规范）
    private static final ThreadLocal<TexThread> THREAD_LOCAL = new TransmittableThreadLocal<>();

    /**
     * 初始化并设置模板（核心初始化方法）
     * 注意：同一线程多次调用会覆盖原有上下文数据
     * @param texTemplate 模板对象（不可为null）
     * @throws IllegalArgumentException 当texTemplate为null时抛出
     */
    public static void setExTemplate(TexTemplate texTemplate) {
        // 参数校验：禁止传入null
        if (texTemplate == null) {
            throw new IllegalArgumentException("模板TexTemplate不能为null，请传入有效模板对象");
        }
        // 初始化上下文并绑定到当前线程
        TexThread texThread = new TexThread();
        texThread.setEx(texTemplate);
        THREAD_LOCAL.set(texThread);
    }

    /**
     * 获取当前线程的模板对象
     * @return 已初始化的模板对象（非null）
     * @throws IllegalStateException 未调用setExTemplate()初始化时抛出
     */
    public static TexTemplate getExTemplate() {
        return getTexThread().getEx();
    }

    /**
     * 获取当前线程的模板单元格列表（不可直接修改）
     * @return 单元格列表（返回不可修改视图，避免外部篡改）
     * @throws IllegalStateException 未调用setExTemplate()初始化时抛出
     */
    public static List<TexTemplateCell> getExCells() {
        // 返回不可修改集合，防止外部直接add/remove
        return Collections.unmodifiableList(getTexThread().getCells());
    }

    /**
     * 向单元格列表添加元素（提供安全的修改入口）
     * @param cell 要添加的单元格对象（不可为null）
     * @throws IllegalStateException 未初始化时抛出
     * @throws IllegalArgumentException 传入null时抛出
     */
    public static void addExCell(TexTemplateCell cell) {
        if (cell == null) {
            throw new IllegalArgumentException("添加的单元格TexTemplateCell不能为null");
        }
        getTexThread().getCells().add(cell);
    }

    /**
     * 获取当前线程的模板表头（不可直接修改）
     * @return 表头映射（返回不可修改视图，避免外部篡改）
     * @throws IllegalStateException 未调用setExTemplate()初始化时抛出
     */
    public static Map<String, String> getExHead() {
        // 返回不可修改映射，防止外部直接put/remove
        return Collections.unmodifiableMap(getTexThread().getHead());
    }

    /**
     * 向表头添加键值对（提供安全的修改入口）
     * @param key 表头键（不可为null/空）
     * @param value 表头值（不可为null）
     * @throws IllegalStateException 未初始化时抛出
     * @throws IllegalArgumentException 键为空或值为null时抛出
     */
    public static void putExHead(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("表头键不能为null或空字符串");
        }
        if (value == null) {
            throw new IllegalArgumentException("表头值不能为null");
        }
        getTexThread().getHead().put(key, value);
    }


    public static List<TexFormula> getExFormulas() {

         return getTexThread().getFormulas();

    }

    /**
     * 清除当前线程的上下文数据（必须调用！避免内存泄漏）
     * 建议在 finally 块中调用，确保无论业务是否异常都能释放资源
     */
    public static void clear() {
        THREAD_LOCAL.remove();
    }

    /**
     * 内部工具方法：获取当前线程的上下文对象（带初始化校验）
     * @return 已初始化的TexThread对象
     * @throws IllegalStateException 未初始化时抛出
     */
    private static TexThread getTexThread() {
        TexThread texThread = THREAD_LOCAL.get();
        if (texThread == null) {
            throw new IllegalStateException("ExThreadLocal未初始化！请先调用setExTemplate()设置模板");
        }
        return texThread;
    }

    /**
     * 内部辅助类：存储线程本地的模板上下文数据（私有访问，禁止外部直接使用）
     */
    private static class TexThread {
        // 模板对象（核心数据）
        private TexTemplate texTemplate;
        // 模板单元格列表（修正命名：ex前缀对应业务语义）
        private final List<TexTemplateCell> exTemplateCells = new ArrayList<>();
        // 模板表头映射
        private final Map<String, String> exTemplateHead = new HashMap<>();
        // 模板公式（懒加载，首次使用时初始化）
        private List<TexFormula> formulas;

        /**
         * 设置模板（仅内部调用，确保模板非null）
         */
        public void setEx(TexTemplate texTemplate) {
            this.texTemplate = texTemplate;
        }

        /**
         * 获取模板（非null，因外部设置时已校验）
         */
        public TexTemplate getEx() {
            return texTemplate;
        }

        /**
         * 获取单元格列表（返回原始集合，供外部通过安全入口修改）
         */
        public List<TexTemplateCell> getCells() {
            return exTemplateCells;
        }

        /**
         * 获取表头映射（返回原始集合，供外部通过安全入口修改）
         */
        public Map<String, String> getHead() {
            return exTemplateHead;
        }

        /**
         * 懒加载公式：首次调用时读取，后续直接返回缓存
         */
        public List<TexFormula> getFormulas() {
            if (formulas == null) {
                // 此处依赖ExFormula.readFormula()，已确保texTemplate非null（外部设置时校验）
                formulas = TexFormulaUtil.readFormula(this.texTemplate);

                // 若ExFormula可能返回null，可添加兜底：formulas = Objects.requireNonNullElse(formulas, new HashMap<>());
            }
            return formulas;
        }
    }
}