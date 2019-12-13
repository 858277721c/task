package com.sd.lib.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class FTaskManager
{
    private static FTaskManager sInstance;

    private final ExecutorService DEFAULT_EXECUTOR = Executors.newCachedThreadPool();
    private final ExecutorService SINGLE_EXECUTOR = Executors.newSingleThreadExecutor();

    private final Map<Runnable, FTaskInfo> mMapTaskInfo = new ConcurrentHashMap<>();
    private final Map<String, Map<FTaskInfo, String>> mMapTaskTag = new ConcurrentHashMap<>();

    private FTaskManager()
    {
    }

    public static FTaskManager getInstance()
    {
        if (sInstance == null)
        {
            synchronized (FTaskManager.class)
            {
                if (sInstance == null)
                    sInstance = new FTaskManager();
            }
        }
        return sInstance;
    }

    public FTaskInfo submit(Runnable runnable)
    {
        return submit(runnable, null);
    }

    public FTaskInfo submit(Runnable runnable, TaskCallback callback)
    {
        return submit(runnable, null, callback);
    }

    public FTaskInfo submit(Runnable runnable, String tag, TaskCallback callback)
    {
        return submitTo(runnable, tag, DEFAULT_EXECUTOR, callback);
    }

    public FTaskInfo submitSequence(Runnable runnable)
    {
        return submitSequence(runnable, null);
    }

    public FTaskInfo submitSequence(Runnable runnable, TaskCallback callback)
    {
        return submitSequence(runnable, null, callback);
    }

    public FTaskInfo submitSequence(Runnable runnable, String tag, TaskCallback callback)
    {
        return submitTo(runnable, tag, SINGLE_EXECUTOR, callback);
    }

    /**
     * 提交要执行的Runnable
     *
     * @param runnable        要执行的Runnable
     * @param executorService 要执行Runnable的线程池
     * @param tag             对应的tag，可用于取消
     * @param callback        任务执行回调
     * @return
     */
    public synchronized FTaskInfo submitTo(Runnable runnable, String tag, ExecutorService executorService, TaskCallback callback)
    {
        cancel(runnable, true);

        final RunnableWrapper wrapper = new RunnableWrapper(runnable, callback);
        final Future<?> future = executorService.submit(wrapper);
        final FTaskInfo info = new FTaskInfo(tag, future);

        mMapTaskInfo.put(runnable, info);

        Map<FTaskInfo, String> mapTagTask = mMapTaskTag.get(tag);
        if (mapTagTask == null)
        {
            mapTagTask = new ConcurrentHashMap<>();
            mMapTaskTag.put(tag, mapTagTask);
        }
        mapTagTask.put(info, "");

        return info;
    }

    /**
     * 返回Runnable对应的任务信息
     *
     * @param runnable
     * @return
     */
    public synchronized FTaskInfo getTaskInfo(Runnable runnable)
    {
        return mMapTaskInfo.get(runnable);
    }

    /**
     * 返回tag对应的任务信息列表
     *
     * @param tag
     * @return
     */
    public synchronized List<FTaskInfo> getTaskInfo(String tag)
    {
        final List<FTaskInfo> listInfo = new ArrayList<>();

        if (tag != null && mMapTaskTag.size() > 0)
        {
            final Map<FTaskInfo, String> map = mMapTaskTag.get(tag);
            if (map != null && map.size() > 0)
            {
                listInfo.addAll(map.keySet());
            }
        }

        return listInfo;
    }

    /**
     * 取消Runnable
     *
     * @param runnable
     * @param mayInterruptIfRunning true-如果线程已经执行有可能被打断
     * @return true-申请取消成功
     */
    public synchronized boolean cancel(Runnable runnable, boolean mayInterruptIfRunning)
    {
        final FTaskInfo info = getTaskInfo(runnable);
        if (info == null)
            return false;

        return info.cancel(mayInterruptIfRunning);
    }

    /**
     * 根据tag取消Runnable
     *
     * @param tag
     * @param mayInterruptIfRunning true-如果线程已经执行有可能被打断
     * @return 申请取消成功的数量
     */
    public synchronized int cancelTag(String tag, boolean mayInterruptIfRunning)
    {
        int count = 0;

        final List<FTaskInfo> listInfo = getTaskInfo(tag);
        for (FTaskInfo item : listInfo)
        {
            if (item.cancel(mayInterruptIfRunning))
                count++;
        }

        return count;
    }

    private synchronized boolean removeTask(Runnable runnable)
    {
        if (runnable == null)
            throw new IllegalArgumentException("runnable is null");

        final FTaskInfo info = mMapTaskInfo.remove(runnable);
        if (info != null)
        {
            final Map<FTaskInfo, String> mapTagTask = mMapTaskTag.get(info.getTag());
            if (mapTagTask != null)
                return mapTagTask.remove(info) != null;
        }

        return false;
    }

    private final class RunnableWrapper extends FutureTask<String>
    {
        private final Runnable mRunnable;
        private final TaskCallback mCallback;

        public RunnableWrapper(Runnable runnable, TaskCallback callback)
        {
            super(new CallableRunnable(runnable));
            mRunnable = runnable;
            mCallback = callback;
        }

        @Override
        protected void done()
        {
            super.done();
            try
            {
                get();
            } catch (InterruptedException e)
            {
                onError(e);
            } catch (CancellationException e)
            {
                onCancel();
            } catch (ExecutionException e)
            {
                onError(e.getCause());
            }


            final boolean remove = removeTask(mRunnable);
            if (!remove)
                throw new RuntimeException("remove task error, runnable was not found:" + mRunnable);

            onFinish();
        }

        protected void onError(Throwable throwable)
        {
            if (mCallback != null)
                mCallback.onError(throwable);
        }

        protected void onCancel()
        {
            if (mCallback != null)
                mCallback.onCancel();
        }

        protected void onFinish()
        {
            if (mCallback != null)
                mCallback.onFinish();
        }
    }

    private static final class CallableRunnable implements Callable<String>
    {
        private final Runnable mRunnable;

        public CallableRunnable(Runnable runnable)
        {
            if (runnable == null)
                throw new IllegalArgumentException("runnable is null");
            mRunnable = runnable;
        }

        @Override
        public String call() throws Exception
        {
            mRunnable.run();
            return null;
        }
    }

    public interface TaskCallback
    {
        void onError(Throwable e);

        void onCancel();

        void onFinish();
    }
}