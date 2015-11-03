package nawaman.papercuts.concurrency.asynctosync

import static java.util.concurrent.CompletableFuture.supplyAsync;

import nawaman.papercuts.concurrent.asynctosync.AsyncToSync;
import spock.lang.Specification;

class AsyncToSyncSpec extends Specification {
    
    def logs = []
    
    def "AsyncToSync make a Future invocation runs like synchronous."() {
        setup:
            logs << "Before";
        
        when:
            def result = new AsyncToSync().invoke(supplyAsync({->
                Thread.sleep(500);
                logs << "Inside";
                return "-inside-";
            }));
            logs << "After: ${result}";
            
        then:
            ["Before", "Inside", "After: -inside-"] == logs
    }
    
}
