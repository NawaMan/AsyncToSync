package nawaman.papercuts.concurrent.asynctosync;

import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This utility class make it easy to convert asynchronous operation to a synchronous one.
 * Of course, if you just need to make an asynchronous call synchronous, just call {@link Future#get()}.
 * There is no need for this class.
 * 
 * This class provides convenient functionality.
 * <ol>
 *   <li>Support both Future and CompletableFuture</li>
 *   <li>Default value when exception, cancelled, interrupted or timeout - via {@link AsyncToSync#orElse(...)}.</li>
 *   <li>Make it easy to do conditional cancellation - via {@link AsyncToSync#parallely(...)}</li>
 *   <li>Make it easy to notify progress - via {@link AsyncToSync#parallely(...)}</li>
 *   <li>Standard way of handling exception, cancellation, interrupted and timeout - via `onXXX` methods</li>
 *   <li>Declarative style API - For example timeout time is set on different method call instead of together with `get(...)`</li>
 * </ol>
 * 
 * @author NawaMan
 */
public class AsyncToSync<T> {

    private Optional<Function<RuntimeException, T>> onException = Optional.empty();
    
    private Optional<Supplier<T>> onCancelled = Optional.empty();
    
    private Optional<Supplier<T>> onInterrupted = Optional.empty();
    
    private Long waitTime = null;
    
    private Optional<Supplier<T>> onTimeout = Optional.empty();
    
    private Optional<Supplier<T>> orElse = Optional.empty();
    
    private Optional<Consumer<Future<T>>> parallely = Optional.empty();
    
    private Optional<Executor> parallelyExecutor = Optional.empty();
    
    /**
     * Add a consumer to be run in parallel with the future.
     * The Runnable will be cancelled once the future invocation is completed.
     * This method is useful for something like conditional cancellation.
     * 
     * @param parallely
     *            the runnable to be run in parallel.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> parallely(
            Consumer<Future<T>> parallely) {
        this.parallelyExecutor = Optional.of(ForkJoinPool.commonPool());
        this.parallely = Optional.ofNullable(parallely);
        return this;
    }
    
    /**
     * Add a runnable to be run in parallel with the future.
     * The Runnable will be cancelled once the future invocation is completed.
     * This method is useful for something like progress report.
     * 
     * @param parallely
     *            the runnable to be run in parallel.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> parallely(
            Runnable parallely) {
        this.parallelyExecutor = Optional.of(ForkJoinPool.commonPool());
        if (parallely != null) {
            this.parallely = Optional.ofNullable(future->
                    parallely.run());
        }
        return this;
    }
    
    /**
     * Add a consumer to be run in parallel with the future.
     * The Runnable will be cancelled once the future invocation is completed.
     * This method is useful for something like conditional cancellation.
     * 
     * @param parallely
     *            the runnable to be run in parallel.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> parallely(
            Executor executor, 
            Consumer<Future<T>> parallely) {
        this.parallelyExecutor = Optional.ofNullable(executor);
        this.parallely = Optional.ofNullable(parallely);
        return this;
    }
    
    /**
     * Add a runnable to be run in parallel with the future.
     * The Runnable will be cancelled once the future invocation is completed.
     * This method is useful for something like progress report.
     * 
     * @param executor
     *            the executor for the parallely
     * @param parallely
     *            the runnable to be run in parallel.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> parallely(
            Executor executor, 
            Runnable parallely) {
        this.parallelyExecutor = Optional.ofNullable(executor);
        if (parallely != null) {
            this.parallely = Optional.ofNullable(future->parallely.run());
        }
        return this;
    }
    
    /**
     * Specify what to return in case of exception.
     * 
     * @param defaultValue
     *            the value to return in case of exception.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> onException(
            T defaultValue) {
        if (onException != null) {
            this.onException = Optional.ofNullable(runtimeException->{
                return defaultValue;
            });
        }
        return this;
    }
    
    /**
     * Specify what to do in case of runtime exception.
     * 
     * @param onException
     *            the runnable of what to do. This will make the invocation returns the default value.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> onException(
            Runnable onException) {
        if (onException != null) {
            this.onException = Optional.ofNullable(runtimeException->{
                onException.run();
                return this.prepareDefaultValue();
            });
        }
        return this;
    }
    
    /**
     * Specify what to do in case of runtime exception.
     * 
     * @param onException
     *            the supplier of the result.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> onException(
            Supplier<T> onException) {
        if (onException != null) {
            this.onException = Optional.ofNullable(runtimeException->onException.get());
        }
        return this;
    }
    
    /**
     * Specify what to do in case of runtime exception.
     * 
     * @param onException
     *            the function of runtime exception to the result.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> onException(
            Function<RuntimeException, T> onException) {
        this.onException = Optional.ofNullable(onException);
        return this;
    }
    
    /**
     * Specify what to return in case of cancellation.
     * 
     * @param defaultValue
     *            the value to return in case of exception.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> onCancelled(
            T defaultValue) {
        this.onCancelled = Optional.ofNullable(()->{
            return defaultValue;
        });
        return this;
    }
    
    /**
     * Specify what to do in case of cancellation.
     * 
     * @param onCancelled
     *            the runnable of what to do. This will make the invocation returns the default value.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> onCancelled(
            Runnable onCancelled) {
        if (onCancelled != null){
            this.onCancelled = Optional.ofNullable(()->{
                onCancelled.run();
                return this.prepareDefaultValue();
            });
        }
        return this;
    }
    
    /**
     * Specify what to do in case of cancellation.
     * 
     * @param onCancelled
     *            the supplier of the result.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> onCancelled(
            Supplier<T> onCancelled) {
        this.onCancelled = Optional.ofNullable(onCancelled);
        return this;
    }
    
    /**
     * Specify what to return in case of interruption.
     * 
     * @param defaultValue
     *            the value to return.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> onInterrupted(
            T defaultValue) {
        this.onInterrupted = Optional.ofNullable(()->{
            return defaultValue;
        });
        return this;
    }
    
    /**
     * Specify what to do in case of interruption.
     * 
     * @param onInterrupted
     *            the runnable of what to do. This will make the invocation returns the default value.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> onInterrupted(
            Runnable onInterrupted) {
        if (onInterrupted != null){
            this.onInterrupted = Optional.ofNullable(()->{
                onInterrupted.run();
                return this.prepareDefaultValue();
            });
        }
        return this;
    }
    
    /**
     * Specify what to do in case of interruption.
     * 
     * @param onInterrupted
     *            the supplier of the result.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> onInterrupted(
            Supplier<T> onInterrupted) {
        this.onInterrupted = Optional.ofNullable(onInterrupted);
        return this;
    }
    
    /**
     * Set the timeout after the given wait time in millisecond. The invocation will returns the default value.
     * 
     * @param waitTime
     *            the wait time.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> onTimeout(
            long waitTime) {
        this.waitTime = waitTime;
        this.onTimeout = Optional.empty();
        return this;
    }
    
    /**
     * Set the timeout after the given wait time in millisecond and perform whatever needed.
     * 
     * @param waitTime
     *            the wait time.
     * @param onTimeout
     *            the timeout method.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> onTimeout(
            long waitTime,
            Runnable onTimeout) {
        this.waitTime = waitTime;
        if (onTimeout != null) {
            this.onTimeout = Optional.ofNullable(()->{
                onTimeout.run();
                return this.prepareDefaultValue();
            });
        }
        return this;
    }
    
    /**
     * Set the timeout after the given wait time in millisecond and perform whatever needed.
     * 
     * @param waitTime
     *            the wait time.
     * @param onTimeout
     *            the timeout method.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> onTimeout(
            long waitTime,
            Supplier<T> onTimeout) {
        this.waitTime = waitTime;
        this.onTimeout = Optional.ofNullable(onTimeout);
        return this;
    }
    
    /**
     * Specify the default value.
     * 
     * @param value
     *            the default value.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> orElse(
            T value) {
        Supplier<T> supplier = () -> value;
        this.orElse = Optional.ofNullable(supplier);
        return this;
    }
    
    /**
     * Specify the way to compute the default value.
     * 
     * @param supplier
     *            the supplier of the default value.
     * @return this AsyncToSync.
     */
    public AsyncToSync<T> orElse(
            Supplier<T> supplier) {
        this.orElse = Optional.ofNullable(supplier);
        return this;
    }
    
    /**
     * Make this sync a careless call in which there is no need to handle {@link InterruptedException} for the sync
     *   thread.
     * 
     * When interrupt occurs the default value will be returned.
     * 
     * @return the Carelessly object used for invoke without {@link InterruptedException}.
     */
    public Carelessly carelessly() {
        return new Carelessly(()->this.prepareDefaultValue());
    }
    
    /**
     * Make this sync a careless call in which there is no need to handle {@link InterruptedException} for the sync
     *   thread.
     * 
     * When interrupt occurs the given default value will be returned.
     * 
     * @return the Carelessly object used for invoke without {@link InterruptedException}.
     */
    public Carelessly carelessly(T defaultValue) {
        return new Carelessly(()->defaultValue);
    }
    
    /**
     * Make this sync a careless call in which there is no need to handle {@link InterruptedException} for the sync
     *   thread.
     * 
     * When interrupt occurs the given default value will be returned.
     * 
     * @return the Carelessly object used for invoke without {@link InterruptedException}.
     */
    public Carelessly carelessly(Supplier<T> onSynchronousInterrupted) {
        if (onSynchronousInterrupted == null) {
            return this.carelessly();
        } else {
            return new Carelessly(onSynchronousInterrupted);
        }
    }
    
    /**
     * Start the invocation to the completable future object.
     * 
     * @param aync
     *            the asynchronous future.
     * @return the result value.
     * @throws InterruptedException
     *            when the current thread is interrupted.
     */
    public T invoke(
            CompletableFuture<T> aync)
                    throws InterruptedException {
        T result = this.invoke((Future<T>)aync);
        return result;
    }
    
    /**
     * Start the invocation to the future object.
     * 
     * @param aync
     *            the asynchronous future.
     * @return the result value.
     * @throws InterruptedException
     *            when the current thread is interrupted.
     */
    public T invoke(
            Future<T> aync)
                    throws InterruptedException {
        CompletableFuture<T> future = prepareFuture(aync);
        if (future == null) {
            T defaultValue = prepareDefaultValue();
            return defaultValue;
        }
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> resultRef = new AtomicReference<>();
        AtomicReference<RuntimeException> exceptionRef = new AtomicReference<>();
        future.whenComplete((value, exception) -> {
            RuntimeException runtimeException = prepareRuntimeException(exception);
            exceptionRef.set(runtimeException);
            
            resultRef.set(value);
            latch.countDown();
        });
        
        Optional<CompletableFuture<Void>> atSameTime = Optional.empty();
        try {
            atSameTime = this.parallely
                    .map(consumer-> CompletableFuture.runAsync(()->{
                        consumer.accept(future);
                    }, parallelyExecutor.orElse(ForkJoinPool.commonPool())));
            
            boolean isTimeout = awaitInvocation(latch);
            if (isTimeout) {
                return prepareTimeout();
            }
        } finally {
            atSameTime.ifPresent(cf->{
                cf.cancel(true);
            });
        }
        
        RuntimeException rte = exceptionRef.get();
        if (rte != null) {
            return handleRuntimeException(rte);
        }
        
        return resultRef.get();
    }
    
    private boolean awaitInvocation(
            CountDownLatch latch)
            throws InterruptedException {
        boolean isTimeout;
        if (this.waitTime != null) {
            isTimeout = !latch.await(this.waitTime, TimeUnit.MILLISECONDS);
        } else {
            latch.await();
            isTimeout = false;
        }
        return isTimeout;
    }
    
    private CompletableFuture<T> prepareFuture(Future<T> future) {
        if (future == null) {
            return null;
        }
        
        if (future instanceof CompletableFuture) {
            return (CompletableFuture<T>) future;
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return future.get();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException)e;
                } else {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    
    private static RuntimeException prepareRuntimeException(
            Throwable exception) {
        if (exception == null) {
            return null;
        }
        
        RuntimeException runtimeException;
        while (exception instanceof CompletionException) {
            exception = ((CompletionException) exception).getCause();
        }
        if (exception instanceof RuntimeException) {
            runtimeException = (RuntimeException) exception;
            
        } else {
            assert false : "Don't expect this to happen.";
            runtimeException = new RuntimeException(exception);
        }
        return runtimeException;
    }
    
    private T prepareTimeout() {
        T result;
        if (this.onTimeout.isPresent()) {
            result = this.onTimeout.get().get();
            return result;
        } else {
            result = prepareDefaultValue();
        }
        return result;
    }
    
    private T prepareInterrupted(
            InterruptedException interruptedException)
                    throws InterruptedException {
        T result;
        if (this.onInterrupted.isPresent()) {
            Supplier<T> onInterrupted = this.onInterrupted.get();
            result = onInterrupted.get();
        } else if (this.orElse.isPresent()) {
            result = prepareDefaultValue();
        } else {
            throw interruptedException;
        }
        return result;
    }
    
    private T prepareCancelled(
            CancellationException cancellationException) {
        T result;
        if (this.onCancelled.isPresent()) {
            Supplier<T> onCancelled = this.onCancelled.get();
            result = onCancelled.get();
        } else if (this.orElse.isPresent()) {
            result = prepareDefaultValue();
        } else {
            throw cancellationException;
        }
        return result;
    }
    
    private T prepareDefaultValue() {
        T result = this.orElse
                .map(orElse -> orElse.get())
                .orElse(null);
        return result;
    }
    
    protected T handleRuntimeException(
            RuntimeException rte)
                    throws InterruptedException {
        if (this.onException.isPresent()) {
            T result = onException.get().apply(rte);
            return result;
        }
        
        if (rte instanceof CancellationException) {
            T result = prepareCancelled((CancellationException) rte);
            return result;
        }
        
        if (rte.getCause() instanceof InterruptedException) {
            T result = prepareInterrupted((InterruptedException)rte.getCause());
            return result;
        }
        
        if (this.orElse.isPresent()) {
            T result = prepareDefaultValue();
            return result;
        }
        
        throw rte;
    }
    
    // == AUX class ====================================================================================================
    
    /**
     * This helper class make it possible to run without having to worry about the interrupt exception of the sync
     *   thread.
     */
    public class Carelessly {
        
        private final Supplier<T> onSyncrhonousInterruppted;
        
        Carelessly(Supplier<T> onSyncrhonousInterruppted) {
            this.onSyncrhonousInterruppted = onSyncrhonousInterruppted;
        }
        
        /**
         * Start the invocation to the completable future object.
         * 
         * @param aync
         *            the asynchronous future.
         * @return the result value.
         * @throws InterruptedException
         *            when the current thread is interrupted.
         */
        public T invoke(
                CompletableFuture<T> aync) {
            T result = this.invoke((Future<T>)aync);
            return result;
        }
        
        /**
         * Start the invocation to the future object.
         * 
         * @param aync
         *            the asynchronous future.
         * @return the result value.
         * @throws InterruptedException
         *            when the current thread is interrupted.
         */
        public T invoke(
                Future<T> aync){
            try {
                T result = AsyncToSync.this.invoke(aync);
                return result;
            } catch(InterruptedException exception) {
                return onSyncrhonousInterruppted.get();
            }
        }
        
    }
    
}
