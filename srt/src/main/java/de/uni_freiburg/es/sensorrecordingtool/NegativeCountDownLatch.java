package de.uni_freiburg.es.sensorrecordingtool;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NegativeCountDownLatch {
    private int count;
    private Lock reentrantLock;
    private Condition condition;

    public NegativeCountDownLatch(int count) {
        this.count = count;
        reentrantLock = new ReentrantLock();
        condition = reentrantLock.newCondition();
    }

    public void await() throws InterruptedException {
        reentrantLock.lock();
        try {
            while (count > 0) {
                condition.await();
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    public void countDown() {
        reentrantLock.lock();
        try {
            count--;
            condition.signalAll();
        } finally {
            reentrantLock.unlock();
        }
    }
}
