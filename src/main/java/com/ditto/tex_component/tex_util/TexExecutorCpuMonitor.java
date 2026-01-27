package com.ditto.tex_component.tex_util;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * CPU监控工具类：TTL包装 + CPU监控 + 高CPU自动睡眠（仅改此类，业务代码不变）
 */
public class TexExecutorCpuMonitor {
    // 日志框架（替换System.out，建议项目统一使用）
    private static final Logger log = LoggerFactory.getLogger(TexExecutorCpuMonitor.class);

    // ========== 可配置常量：根据业务需求调整 ==========
    public static final double CPU_HIGH_THRESHOLD = 70.0; // CPU高负载阈值（百分比），超过则睡眠
    public static final long SLEEP_TIME_MS = 1000; // 高CPU时的睡眠时长（毫秒），可根据需要调大/调小
    public static final int MAX_SLEEP_TIMES = 30; // 单任务最大睡眠次数，避免无限睡眠


    private static final double RANDOM_FACTOR_MIN = 1.0D;
    private static final double RANDOM_FACTOR_MAX = 3.0D;

    // ========== 原有CPU监控核心方法（保持不变，确保getCpuUsage返回百分比值） ==========

    /**
     * 优化后的 getCpuUsage 方法
     * 可以直接替换原有的 getCpuUsage() 方法
     *
     * 主要优化：
     * 1. 多次采样取平均值（3次），提高准确性
     * 2. 初始化等待机制，处理首次调用返回 -1.0 的情况
     * 3. 进程 CPU 负载作为备选方案
     * 4. 完善的异常处理和日志记录
     */
    public static double getCpuUsage() {
        try {
            // 获取JDK扩展的系统MXBean
            OperatingSystemMXBean osMxBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

            // 采样配置
            int sampleCount = 3;           // 采样次数（3次取平均值）
            long sampleInterval = 100;     // 采样间隔（100毫秒）
            long initWaitTime = 200;       // 初始化等待时间（200毫秒）

            // 步骤1：初始化等待，确保系统有足够时间收集 CPU 数据
            double initialSample = osMxBean.getSystemCpuLoad();
            if (initialSample < 0) {
                // 如果首次采样返回 -1.0，等待一段时间后重试
                try {
                    Thread.sleep(initWaitTime);
                    initialSample = osMxBean.getSystemCpuLoad();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("[CPU监控] 等待初始化被中断", e);
                }
            }

            // 步骤2：如果系统 CPU 负载不可用，尝试使用进程 CPU 负载作为备选
            if (initialSample < 0) {
                try {
                    double processCpuLoad = osMxBean.getProcessCpuLoad();
                    if (processCpuLoad >= 0) {
                        log.debug("[CPU监控] 系统CPU负载不可用，使用进程CPU负载: {}%", processCpuLoad * 100);
                        return Math.max(0.0, Math.min(100.0, processCpuLoad * 100.0));
                    }
                } catch (Exception e) {
                    log.warn("[CPU监控] 获取进程CPU负载失败", e);
                }
                log.warn("[CPU监控] 无法获取有效的CPU负载数据，返回0.0");
                return 0.0;
            }

            // 步骤3：多次采样取平均值，提高准确性
            double totalCpuLoad = initialSample;
            int validSampleCount = 1;

            for (int i = 1; i < sampleCount; i++) {
                try {
                    Thread.sleep(sampleInterval);
                    double sample = osMxBean.getSystemCpuLoad();

                    // 只累计有效采样（>= 0）
                    if (sample >= 0) {
                        totalCpuLoad += sample;
                        validSampleCount++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.debug("[CPU监控] 采样被中断，使用已收集的{}个有效采样", validSampleCount);
                    break;
                }
            }

            // 步骤4：计算平均值并转换为百分比
            if (validSampleCount > 0) {
                double avgCpuLoad = totalCpuLoad / validSampleCount;
                double cpuUsage = avgCpuLoad * 100.0;

                // 边界值防护：确保数值在 0~100 之间
                return Math.max(0.0, Math.min(100.0, cpuUsage));
            } else {
                log.warn("[CPU监控] 所有采样均无效，返回0.0");
                return 0.0;
            }

        } catch (ClassCastException e) {
            // 兼容少数定制化JDK（未包含扩展MXBean）
            log.error("[CPU监控] JDK环境未支持扩展OperatingSystemMXBean，无法获取CPU使用率", e);
            return 0.0;
        } catch (Exception e) {
            // 捕获其他未知异常，避免CPU监控异常导致业务崩溃
            log.error("[CPU监控] 获取系统CPU使用率异常", e);
            return 0.0;
        }
    }



    /**
     * 获取当前CPU使用率（核心：返回【百分比格式】，如80.0表示80%，不是0.8）
     */
/*    public static double getCpuUsage() {
        try {
            // 获取JDK 1.8扩展的系统MXBean（JDK1.8原生支持，主流版本均兼容）
            OperatingSystemMXBean osMxBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

            // JDK1.8核心方法：getSystemCpuLoad()，直接返回系统CPU负载（0.0 ~ 1.0）
            // 0.0表示0%负载，1.0表示100%负载，完美契合系统整体CPU监控需求
            double systemCpuLoad = osMxBean.getSystemCpuLoad();

            // 转换为百分比格式（0.0~100.0），遵循原有方法约定，直接对接CPU_HIGH_THRESHOLD判断
            double cpuUsage = systemCpuLoad * 100.0;

            // 边界值防护：避免JVM初始化阶段返回-1.0（无有效数据）或计算误差，确保数值在0~100之间
            return Math.max(0.0, Math.min(100.0, cpuUsage));

        } catch (ClassCastException e) {
            // 兼容少数定制化JDK1.8（未包含扩展MXBean），打日志并返回0.0，不影响主业务
            log.error("[CPU监控] JDK1.8环境未支持扩展OperatingSystemMXBean，无法获取CPU使用率", e);
            return 0.0;
        } catch (Exception e) {
            // 捕获其他未知异常，避免CPU监控异常导致线程池任务崩溃
            log.error("[CPU监控] JDK1.8获取系统CPU使用率异常", e);
            return 0.0;
        }
    }*/
/*
    public static double getCpuUsage() {
        // 此处保留你昨天实现的CPU使用率计算逻辑
        // 示例模拟（实际替换为你的真实实现）：
        return Math.random() * 100;
    }*/

    /**
     * 启动CPU定时监控（原有逻辑不变）
     */
    public static void startCpuMonitor() {
        // 此处保留你昨天实现的定时监控逻辑
    }

    // ========== 核心：TTL+CPU监控+高CPU睡眠 三重包装线程池 ==========
    public static ExecutorService getMonitoredTtlExecutorService(ExecutorService executor) {
        // 1. 先做TTL包装，保留原有上下文传递核心需求
        ExecutorService ttlExecutor = TtlExecutors.getTtlExecutorService(executor);

        // 2. 包装监控+睡眠逻辑，仅重写execute（业务唯一使用的方法），其余方法委托给TTL线程池
        return new ExecutorService() {
            @Override
            public void execute(Runnable command) {
                ttlExecutor.execute(() -> {
                    long startNanos = System.nanoTime();
                    double startCpu = TexExecutorCpuMonitor.getCpuUsage();
                    int sleepCount = 0; // 记录当前任务睡眠次数，防止无限睡眠

                    try {
                        // 生成随机间隔 = 基础间隔 * 0.5~1.5倍随机数
                        long randomInterval = (long) (SLEEP_TIME_MS * ThreadLocalRandom.current().nextDouble(RANDOM_FACTOR_MIN, RANDOM_FACTOR_MAX));
                        Thread.sleep(randomInterval);
                        // ========== 新增：执行任务前检测CPU，高负载则睡眠 ==========
                        while (startCpu > CPU_HIGH_THRESHOLD && sleepCount < MAX_SLEEP_TIMES) {
                            // 修正：SLF4J纯{}占位符，格式通过String.format控制
                            log.warn("[CPU高负载触发睡眠] 当前CPU使用率：{}%，阈值：{}%，即将睡眠{}ms，已睡眠{}次",
                                    String.format("%.2f", startCpu),
                                    String.format("%.2f", CPU_HIGH_THRESHOLD),
                                    SLEEP_TIME_MS,
                                    sleepCount);

                            Thread.sleep(SLEEP_TIME_MS); // 线程睡眠，释放CPU资源
                            sleepCount++;
                            startCpu = TexExecutorCpuMonitor.getCpuUsage(); // 重新检测CPU使用率
                        }

                        // 睡眠后CPU恢复正常，或达到最大睡眠次数，执行原有业务任务
                        if (sleepCount >= MAX_SLEEP_TIMES) {
                            log.error("[CPU高负载达到最大睡眠次数] 已睡眠{}次，强制执行任务，当前CPU：{}%",
                                    sleepCount,
                                    String.format("%.2f", startCpu));
                        } else if (sleepCount > 0) {
                            log.info("[CPU恢复正常] 睡眠{}次后CPU降至{}%，开始执行业务任务",
                                    sleepCount,
                                    String.format("%.2f", startCpu));
                        }

                        command.run(); // 执行你的分页查询、Excel写入核心业务

                    } catch (InterruptedException e) {
                        log.error("[任务睡眠被中断]", e);
                        Thread.currentThread().interrupt(); // 恢复线程中断状态，避免业务异常
                    } catch (Exception e) {
                        log.error("[业务任务执行异常]", e);
                    } finally {
                        // ========== 原有监控逻辑：任务完成后打印耗时和CPU ==========
                        long costNanos = System.nanoTime() - startNanos;
                        double endCpu = TexExecutorCpuMonitor.getCpuUsage();
                        double avgCpu = (startCpu + endCpu) / 2;
                        long costMs = TimeUnit.NANOSECONDS.toMillis(costNanos);

                        // 修正：SLF4J纯{}占位符，多参数格式分别控制
                        log.info("[线程池CPU监控] 任务耗时：{}ms | 开始CPU：{}% | 结束CPU：{}% | 平均CPU：{}% | 睡眠次数：{}",
                                costMs,
                                String.format("%.2f", startCpu),
                                String.format("%.2f", endCpu),
                                String.format("%.2f", avgCpu),
                                sleepCount);
                    }
                });
            }

            // ========== 以下方法全部委托给TTL线程池，无需任何修改 ==========
            @Override
            public void shutdown() {ttlExecutor.shutdown();}
            @Override
            public List<Runnable> shutdownNow() {return ttlExecutor.shutdownNow();}
            @Override
            public boolean isShutdown() {return ttlExecutor.isShutdown();}
            @Override
            public boolean isTerminated() {return ttlExecutor.isTerminated();}
            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return ttlExecutor.awaitTermination(timeout, unit);
            }
            @Override
            public <T> Future<T> submit(Callable<T> task) {return ttlExecutor.submit(task);}
            @Override
            public <T> Future<T> submit(Runnable task, T result) {return ttlExecutor.submit(task, result);}
            @Override
            public Future<?> submit(Runnable task) {return ttlExecutor.submit(task);}
            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
                return ttlExecutor.invokeAll(tasks);
            }
            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
                return ttlExecutor.invokeAll(tasks, timeout, unit);
            }
            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
                return ttlExecutor.invokeAny(tasks);
            }
            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return ttlExecutor.invokeAny(tasks, timeout, unit);
            }
        };
    }
}