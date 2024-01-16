package org.mengyun.commons.collapser;

import org.junit.Test;

import java.util.concurrent.locks.LockSupport;

/**
 * @Author huabao.fang
 * @Date 2024/1/8 14:45
 **/
public class LockSupportTest {

    /**
     * 1秒(s) ＝1000毫秒(ms)
     * 1毫秒(ms)＝1000微秒 (us)
     * 1微秒(us)＝1000纳秒 (ns)
     * 1纳秒(ns)＝1000皮秒 (ps)
     */
    @Test
    public void test() {
        long start = System.currentTimeMillis();
        long waitMills = 10;
        // wait 10ms
        LockSupport.parkNanos(waitMills * 1000 * 1000l);
        long end = System.currentTimeMillis();
        long cost = end - start;

        assert cost > waitMills && cost < (waitMills + 5);

    }

}
