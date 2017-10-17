package com.fanwe.lib.task;

import java.util.concurrent.Future;

/**
 * Created by zhengjun on 2017/10/13.
 */

public class SDTaskInfo implements SDTaskFuture
{
    private Future future;
    private String tag;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        if (!isDone())
        {
            return getFuture().cancel(mayInterruptIfRunning);
        } else
        {
            return false;
        }
    }

    @Override
    public boolean isCancelled()
    {
        return getFuture().isCancelled();
    }

    @Override
    public boolean isDone()
    {
        return getFuture().isDone();
    }

    private Future getFuture()
    {
        return future;
    }

    void setFuture(Future future)
    {
        this.future = future;
    }

    public String getTag()
    {
        return tag;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }
}