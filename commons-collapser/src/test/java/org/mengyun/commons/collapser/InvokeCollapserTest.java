package org.mengyun.commons.collapser;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvokeCollapserTest {

    private InvokeCollapser invokeCollapser;

    private Callable<Long, Object> callable;

    @Before
    public void before() {

        invokeCollapser = new InvokeCollapser();
        invokeCollapser.init();

        callable = new Callable<Long, Object>() {
            @Override
            public Map<Long, Object> call(List<Long> requests) {
                Map<Long, Object> resultMap = new HashMap<>();

                for (Long id : requests) {
                    resultMap.put(id, new Object());
                }

                return resultMap;
            }
        };
    }

    @Test
    public void test_invoke() throws InterruptedException {

        for (long i = 1l; i < 100l; i++) {
            invokeCollapser.invoke("1000", i, callable);
        }
    }


    
}
