package com.fanwe.lib.task;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Future;

/**
 * Created by zhengjun on 2017/9/12.
 */
public abstract class SDTask implements Runnable
{
    public static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private Future<?> mFuture;
    private boolean mIsCancelled;

    public static void runOnUiThread(Runnable runnable)
    {
        if (Looper.myLooper() == Looper.getMainLooper())
        {
            runnable.run();
        } else
        {
            MAIN_HANDLER.post(runnable);
        }
    }

    /**
     * 提交任务
     *
     * @param tag 任务对应的tag
     * @return
     */
    public synchronized final Future<?> submit(Object tag)
    {
        mFuture = SDTaskManager.getInstance().submit(this, tag);
        mIsCancelled = false;
        onSubmit();
        return mFuture;
    }

    /**
     * 取消任务
     *
     * @param mayInterruptIfRunning true-如果线程已经执行有可能被打断
     * @return
     */
    public synchronized boolean cancel(boolean mayInterruptIfRunning)
    {
        mIsCancelled = true;
        return SDTaskManager.getInstance().cancel(this, mayInterruptIfRunning);
    }

    /**
     * 是否被取消
     *
     * @return
     */
    public synchronized boolean isCancelled()
    {
        return mIsCancelled || (mFuture == null ? false : mFuture.isCancelled());
    }

    /**
     * 任务是否完成
     *
     * @return
     */
    public synchronized boolean isDone()
    {
        return mFuture == null ? false : mFuture.isDone();
    }

    /**
     * 根据tag取消Runnable
     *
     * @param tag
     * @param mayInterruptIfRunning true-如果线程已经执行有可能被打断
     * @return 取消成功的数量
     */
    public static int cancelTag(Object tag, boolean mayInterruptIfRunning)
    {
        return SDTaskManager.getInstance().cancelTag(tag, mayInterruptIfRunning);
    }

    @Override
    public final void run()
    {
        try
        {
            onRun();
        } catch (Exception e)
        {
            onError(e);
        } finally
        {
            onFinally();
        }
    }

    protected void onSubmit()
    {

    }

    protected abstract void onRun() throws Exception;

    protected void onError(Exception e)
    {

    }

    protected void onFinally()
    {
    }
}
