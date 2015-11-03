package nawaman.papercuts.concurrency.asynctosync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import nawaman.papercuts.concurrent.asynctosync.AsyncToSync;

import org.junit.Test;

public class AsyncToSyncTest {
    
    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    public void asynRunAsSync()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        String result = new AsyncToSync<String>()
                .invoke(CompletableFuture.supplyAsync(() -> {
                    sleep(100);
                    logs.add("Inside");
                    return "-inside-";
                }));
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, Inside, After: -inside-]",
                logs.toString());
    }
    
    @Test
    public void carelesslyAsynRunAsSync() {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        String result = new AsyncToSync<String>()
                .carelessly()
                .invoke(CompletableFuture.supplyAsync(() -> {
                    sleep(100);
                    logs.add("Inside");
                    return "-inside-";
                }));
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, Inside, After: -inside-]",
                logs.toString());
    }
    
    @Test
    public void carelesslyAsynRunAsSync_interrupted() {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        Thread currentThread = Thread.currentThread();
        String result = new AsyncToSync<String>()
                .parallely(() -> {
                    sleep(500);
                    currentThread.interrupt();
                })
                .orElse("Damn!")
                .carelessly()
                .invoke(CompletableFuture.supplyAsync(() -> {
                    sleep(1000);
                    logs.add("Inside");
                    return "-inside-";
                }));
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, After: Damn!]",
                logs.toString());
    }
    
    @Test
    public void carelesslyAsynRunAsSync_withDefaultValue_interrupted() {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        Thread currentThread = Thread.currentThread();
        String result = new AsyncToSync<String>()
                .parallely(() -> {
                    sleep(500);
                    currentThread.interrupt();
                })
                .carelessly("Who care?!")
                .invoke(CompletableFuture.supplyAsync(() -> {
                    sleep(1000);
                    logs.add("Inside");
                    return "-inside-";
                }));
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, After: Who care?!]",
                logs.toString());
    }
    
    @Test
    public void carelesslyAsynRunAsSync_withSupplier_interrupted() {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        Thread currentThread = Thread.currentThread();
        String result = new AsyncToSync<String>()
                .parallely(() -> {
                    sleep(500);
                    currentThread.interrupt();
                })
                .carelessly(()->{
                    return "Who care?!";
                })
                .invoke(CompletableFuture.supplyAsync(() -> {
                    sleep(1000);
                    logs.add("Inside");
                    return "-inside-";
                }));
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, After: Who care?!]",
                logs.toString());
    }
    
    @Test
    public void asynRunAsSync_basicFuture() throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        String result = new AsyncToSync<String>()
                .invoke(new Future<String>() {
                    @Override
                    public boolean cancel(boolean mayInterruptIfRunning) {
                        return false;
                    }
                    @Override
                    public boolean isCancelled() {
                        return false;
                    }
                    @Override
                    public boolean isDone() {
                        return false;
                    }
                    @Override
                    public String get() throws InterruptedException, ExecutionException {
                        sleep(100);
                        logs.add("Inside");
                        return "-inside-";
                    }
                    @Override
                    public String get(long timeout, TimeUnit unit)
                            throws InterruptedException, ExecutionException,
                            TimeoutException {
                        return null;
                    }
                });
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, Inside, After: -inside-]",
                logs.toString());
    }
    
    @Test
    public void withNullFutureWillResultInTheDefaultValue()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        String result = new AsyncToSync<String>()
                .orElse("-orElse-")
                .invoke(null);
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, After: -orElse-]",
                logs.toString());
    }
    
    @Test
    public void withNullFutureWillResultInTheDefaultValue_lazyDefaultValue()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        String result = new AsyncToSync<String>()
                .orElse(() -> {
                    logs.add("Default");
                    return "-or" + "Else-";
                })
                .invoke(null);
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, Default, After: -orElse-]",
                logs.toString());
    }
    
    @Test
    public void parallelRun()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        String result = new AsyncToSync<String>()
                .parallely(() -> {
                    sleep(10);
                    logs.add("Parallelly");
                })
                .invoke(CompletableFuture.supplyAsync(() -> {
                    sleep(100);
                    logs.add("Inside");
                    return "-inside-";
                }));
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, Parallelly, Inside, After: -inside-]",
                logs.toString());
    }
    
    @Test
    public void exceptionIsRethrown()
            throws InterruptedException {
        RuntimeException theException = new RuntimeException();
        try {
            new AsyncToSync<String>()
                    .invoke(CompletableFuture
                    .supplyAsync(() -> {
                        sleep(100);
                        throw theException;
                    }));
            fail("Expect a RuntimeException!");
        } catch (RuntimeException exception) {
            assertEquals(theException, exception);
        }
    }
    
    @Test
    public void exceptionIsRethrown_butWithOrElse()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        String result = new AsyncToSync<String>()
                .orElse("Default")
                .invoke(CompletableFuture.supplyAsync(() -> {
                    sleep(100);
                    throw new RuntimeException();
                }));
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, After: Default]",
                logs.toString());
    }
    
    @Test
    public void exceptionIsRethrown_butWithOnException()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        String result = new AsyncToSync<String>()
                .onException(exception -> {
                    return exception.getClass().getSimpleName();
                })
                .invoke(CompletableFuture.supplyAsync(() -> {
                    sleep(100);
                    throw new RuntimeException();
                }));
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, After: RuntimeException]",
                logs.toString());
    }
    
    @Test
    public void exceptionIsRethrown_butWithOnExceptionAndOnElse_onExceptionIsUsed()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        String result = new AsyncToSync<String>()
                .orElse("Default")
                .onException(exception -> {
                    return exception.getClass().getSimpleName();
                })
                .invoke(CompletableFuture.supplyAsync(() -> {
                    sleep(100);
                    throw new RuntimeException();
                }));
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, After: RuntimeException]",
                logs.toString());
    }
    
    @Test
    public void runtimeExceptionIsThrownFromFuture()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        RuntimeException theException = new RuntimeException();
        try {
            new AsyncToSync<String>()
                    .invoke(new Future<String>() {
                        @Override
                        public boolean cancel(boolean mayInterruptIfRunning) {
                            return false;
                        }
                        @Override
                        public boolean isCancelled() {
                            return false;
                        }
                        @Override
                        public boolean isDone() {
                            return false;
                        }
                        @Override
                        public String get() throws InterruptedException, ExecutionException {
                            sleep(100);
                            throw theException;
                        }
                        @Override
                        public String get(long timeout, TimeUnit unit)
                                throws InterruptedException, ExecutionException,
                                TimeoutException {
                            return null;
                        }
                    });
            fail("Expect a RuntimeException!");
        } catch (RuntimeException exception) {
            assertEquals(theException, exception);
        }
    }
    
    @Test
    public void exceptionIsThrownFromFuture()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        ExecutionException theException = new ExecutionException("", null);
        try {
            new AsyncToSync<String>()
                    .invoke(new Future<String>() {
                        @Override
                        public boolean cancel(boolean mayInterruptIfRunning) {
                            return false;
                        }
                        @Override
                        public boolean isCancelled() {
                            return false;
                        }
                        @Override
                        public boolean isDone() {
                            return false;
                        }
                        @Override
                        public String get() throws InterruptedException, ExecutionException {
                            sleep(100);
                            throw theException;
                        }
                        @Override
                        public String get(long timeout, TimeUnit unit)
                                throws InterruptedException, ExecutionException,
                                TimeoutException {
                            return null;
                        }
                    });
            fail("Expect a RuntimeException!");
        } catch (RuntimeException exception) {
            assertEquals(theException, exception.getCause());
        }
    }
    
    @Test
    public void timeout_null()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        String result = new AsyncToSync<String>()
                .onTimeout(50)
                .invoke(CompletableFuture.supplyAsync(() -> {
                    sleep(100);
                    logs.add("Inside");
                    return "-inside-";
                }));
        logs.add("After: " + result);
        assertEquals(
                "[Before, After: null]",
                logs.toString());
    }
    
    @Test
    public void timeout_default()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        String result = new AsyncToSync<String>()
                .onTimeout(50)
                .orElse("-inside?-")
                .invoke(CompletableFuture.supplyAsync(() -> {
                    sleep(100);
                    logs.add("Inside");
                    return "-inside-";
                }));
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, After: -inside?-]",
                logs.toString());
    }
    
    @Test
    public void timeout_onTimeout()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        String result = new AsyncToSync<String>()
                .onTimeout(50, () -> {
                    logs.add("Hello-Inside");
                    return "Hello";
                })
                .orElse("-inside?-")
                .invoke(CompletableFuture.supplyAsync(() -> {
                    sleep(100);
                    logs.add("Inside");
                    return "-inside-";
                }));
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, Hello-Inside, After: Hello]",
                logs.toString());
    }
    
    @Test
    public void timeout_notTimeout()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        String result = new AsyncToSync<String>()
                .onTimeout(100)
                .invoke(CompletableFuture.supplyAsync(() -> {
                    sleep(50);
                    logs.add("Inside");
                    return "-inside-";
                }));
        logs.add("After: " + result);
        assertEquals(
                "[Before, Inside, After: -inside-]",
                logs.toString());
    }
    
    
    @Test(expected = InterruptedException.class)
    public void interrupt()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        AtomicReference<Thread> threadRef = new AtomicReference<>();
        new AsyncToSync<String>()
                .parallely(() -> {
                    sleep(10);
                    threadRef.get().interrupt();
                })
                .invoke(CompletableFuture.supplyAsync(() -> {
                    threadRef.set(Thread.currentThread());
                    sleep(10_000);
                    logs.add("Inside");
                    return "-inside-";
                }));
    }
    
    @Test
    public void interrupt_orElse()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        AtomicReference<Thread> threadRef = new AtomicReference<>();
        String result = new AsyncToSync<String>()
                .parallely(() -> {
                    sleep(10);
                    threadRef.get().interrupt();
                })
                .orElse("Interrupted")
                .invoke(CompletableFuture.supplyAsync(() -> {
                    threadRef.set(Thread.currentThread());
                    sleep(10_000);
                    logs.add("Inside");
                    return "-inside-";
                }));
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, After: Interrupted]",
                logs.toString());
    }
    
    @Test
    public void interrupt_onInterrupted()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        AtomicReference<Thread> threadRef = new AtomicReference<>();
        String result = new AsyncToSync<String>()
                .parallely(() -> {
                    sleep(10);
                    threadRef.get().interrupt();
                })
                .onInterrupted(() -> {
                    return "Interrupted";
                })
                .invoke(CompletableFuture.supplyAsync(() -> {
                    threadRef.set(Thread.currentThread());
                    sleep(10_000);
                    logs.add("Inside");
                    return "-inside-";
                }));
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, After: Interrupted]",
                logs.toString());
    }
    
    @Test
    public void cancelFutureWithoutOnCancelledNorOrElseValueWillThrowException()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        try {
            new AsyncToSync<String>()
                    .parallely(future -> {
                        sleep(10);
                        future.cancel(false);
                    })
                    .invoke(CompletableFuture.supplyAsync(() -> {
                        sleep(10_000);
                        logs.add("Inside");
                        return "-inside-";
                    }));
        } catch (CancellationException exception) {
            
        }
    }
    
    @Test
    public void cancelFutureWithoutOnCancelledButWithOrElseWillReturnTheOrElse()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        String result = new AsyncToSync<String>()
                .parallely(future -> {
                    sleep(10);
                    future.cancel(false);
                })
                .orElse("cancalled")
                .invoke(CompletableFuture.supplyAsync(() -> {
                    sleep(10_000);
                    logs.add("Inside");
                    return "-inside-";
                }));
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, After: cancalled]",
                logs.toString());
    }
    
    @Test
    public void cancelFutureWithOnCancelledWillAskTheHandler()
            throws InterruptedException {
        List<String> logs = new ArrayList<>();
        logs.add("Before");
        
        String result = new AsyncToSync<String>()
                .parallely(future -> {
                    sleep(10);
                    future.cancel(false);
                })
                .onCancelled(() -> {
                    return "cancalled - from onCancelled";
                })
                .invoke(CompletableFuture.supplyAsync(() -> {
                    sleep(10_000);
                    logs.add("Inside");
                    return "-inside-";
                }));
        
        logs.add("After: " + result);
        assertEquals(
                "[Before, After: cancalled - from onCancelled]",
                logs.toString());
    }
    
}
