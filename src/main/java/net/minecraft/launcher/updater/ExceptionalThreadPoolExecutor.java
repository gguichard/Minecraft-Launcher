package net.minecraft.launcher.updater;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.minecraft.launcher.Launcher;

public class ExceptionalThreadPoolExecutor extends ThreadPoolExecutor {
    public class ExceptionalFutureTask<T> extends FutureTask<T> {

        public ExceptionalFutureTask(final Callable<T> callable) {
            super(callable);
        }

        public ExceptionalFutureTask(final Runnable runnable, final T value) {
            super(runnable, value);
        }

        @Override
        protected void done() {
            try {
                get();
            }
            catch(final Throwable t) {
                Launcher.getInstance().println("Unhandled exception in executor " + this, t);
            }
        }
    }

    public ExceptionalThreadPoolExecutor(final int threadCount) {
        super(threadCount, threadCount, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    }

    @Override
    protected void afterExecute(final Runnable r, Throwable t) {
        super.afterExecute(r, t);

        if(t == null && r instanceof Future)
            try {
                final Future<Runnable> future = (Future) r;
                if(future.isDone())
                    future.get();
            }
            catch(final CancellationException ce) {
                t = ce;
            }
            catch(final ExecutionException ee) {
                t = ee.getCause();
            }
            catch(final InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(final Callable<T> callable) {
        return new ExceptionalFutureTask<T>(callable);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value) {
        return new ExceptionalFutureTask<T>(runnable, value);
    }
}