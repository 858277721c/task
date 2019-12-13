package com.sd.lib.task;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;

public abstract class FTask implements Runnable, FTaskManager.TaskCallback
{
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private final String mTag;

    public FTask()
    {
        this(null);
    }

    public FTask(String tag)
    {
        mTag = tag;
    }

    /**
     * 返回任务对应的tag
     *
     * @return
     */
    public final String getTag()
    {
        return mTag;
    }

    /**
     * 提交任务
     *
     * @return
     */
    public final FTaskInfo submit()
    {
        return FTaskManager.getInstance().submit(this, getTag(), this);
    }

    /**
     * 提交任务，按提交的顺序一个个执行
     *
     * @return
     */
    public final FTaskInfo submitSequence()
    {
        return FTaskManager.getInstance().submitSequence(this, getTag(), this);
    }

    /**
     * 提交要执行的任务
     *
     * @param executorService 要执行任务的线程池
     * @return
     */
    public final FTaskInfo submitTo(ExecutorService executorService)
    {
        return FTaskManager.getInstance().submitTo(this, getTag(), executorService, this);
    }

    /**
     * 取消任务
     *
     * @param mayInterruptIfRunning true-如果线程已经执行有可能被打断
     * @return
     */
    public final boolean cancel(boolean mayInterruptIfRunning)
    {
        return FTaskManager.getInstance().cancel(this, mayInterruptIfRunning);
    }

    /**
     * 任务是否还在进行中
     *
     * @return true-已提交未执行或者执行中
     */
    public final boolean isRunning()
    {
        final FTaskInfo taskInfo = FTaskManager.getInstance().getTaskInfo(this);
        return taskInfo != null && !taskInfo.isDone();
    }

    @Override
    public final void run()
    {
        onRun();
    }

    /**
     * 执行回调（执行线程）
     */
    protected abstract void onRun();

    /**
     * 错误回调（执行线程）
     *
     * @param e
     */
    @Override
    public void onError(Throwable e)
    {

    }

    /**
     * 取消回调（执行线程）
     */
    @Override
    public void onCancel()
    {

    }

    /**
     * 结束回调（执行线程）
     */
    @Override
    public void onFinish()
    {

    }

    public static void runOnUiThread(Runnable runnable)
    {
        if (runnable == null)
            return;

        if (Looper.myLooper() == Looper.getMainLooper())
            runnable.run();
        else
            MAIN_HANDLER.post(runnable);
    }

    public static void removeCallbacks(Runnable runnable)
    {
        if (runnable == null)
            return;

        MAIN_HANDLER.removeCallbacks(runnable);
    }
}