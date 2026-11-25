package com.eventflow.eventflow.transaction;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class TransactionTestRunner implements CommandLineRunner {
    //temporar test class
    private final TransactionManager transactionManager;
    private final UndoLog undoLog;
    private final LockManager lockManager;
    private final WaitForGraph waitForGraph;

    public TransactionTestRunner(TransactionManager transactionManager, UndoLog undoLog, LockManager lockManager, WaitForGraph waitForGraph) {
        this.transactionManager = transactionManager;
        this.undoLog = undoLog;
        this.lockManager = lockManager;
        this.waitForGraph = waitForGraph;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n========== TRANSACTION TESTS ==========\n");

        testUndoLog();
        testCommit();
        testAbort();
        testDeadlock();

        System.out.println("\n========== TESTS FINISHED ==========\n");
    }


    private void testUndoLog() {

        System.out.println("\n--- TEST 1: UNDO LOG ---");

        Transaction transaction = transactionManager.beginTransaction();

        StringBuilder result = new StringBuilder();

        undoLog.addOperation(transaction.getId(), () -> {
            result.append("A");
        });

        undoLog.addOperation(transaction.getId(), () -> {
            result.append("B");
        });

        undoLog.addOperation(transaction.getId(), () -> {
            result.append("C");
        });

        undoLog.rollback(transaction.getId());

        System.out.println("Expected result: CBA");
        System.out.println("Actual result:   " + result);

        if (!result.toString().equals("CBA")) {
            throw new AssertionError("Undo operations were not executed in reverse order.");
        }

        transactionManager.abortTransaction(transaction.getId());

        System.out.println("[TEST PASSED] UndoLog uses reverse order.");
    }

    private void testCommit() throws InterruptedException {

        System.out.println("\n--- TEST 2: COMMIT ---");

        Transaction transaction = transactionManager.beginTransaction();

        LockResource resource = new LockResource("seats", "1");

        lockManager.acquireLock(
                transaction,
                resource,
                LockType.WRITE
        );

        System.out.println("Lock acquired: " +
                lockManager.hasLock(transaction, resource));

        transactionManager.commitTransaction(transaction.getId());

        System.out.println("Transaction status: " +
                transaction.getStatus());

        System.out.println("Lock after commit: " +
                lockManager.hasLock(transaction, resource));

        if (transaction.getStatus() != TransactionStatus.COMMITTED
                || lockManager.hasLock(transaction, resource)) {
            throw new AssertionError("Commit test failed.");
        }

        System.out.println("[TEST PASSED] Commit releases locks.");
    }

    private void testAbort() throws InterruptedException {

        System.out.println("\n--- TEST 3: ABORT ---");

        Transaction transaction = transactionManager.beginTransaction();

        LockResource resource = new LockResource("seats", "2");

        lockManager.acquireLock(
                transaction,
                resource,
                LockType.WRITE
        );

        undoLog.addOperation(transaction.getId(), () ->
                System.out.println("Undo operation executed.")
        );

        transactionManager.abortTransaction(transaction.getId());

        System.out.println("Transaction status: " +
                transaction.getStatus());

        System.out.println("Lock after abort: " +
                lockManager.hasLock(transaction, resource));

        if (transaction.getStatus() != TransactionStatus.ABORTED
                || lockManager.hasLock(transaction, resource)) {
            throw new AssertionError("Abort test failed.");
        }

        System.out.println("[TEST PASSED] Abort executes undo and releases locks.");
    }

    private void testDeadlock() throws Exception {

        System.out.println("\n--- TEST 4: DEADLOCK DETECTION ---");

        Transaction t1 = transactionManager.beginTransaction();
        Transaction t2 = transactionManager.beginTransaction();

        LockResource seat1 = new LockResource("seats", "101");
        LockResource seat2 = new LockResource("seats", "102");

        // T1 locks Seat 101.
        lockManager.acquireLock(t1, seat1, LockType.WRITE);

        // T2 locks Seat 102.
        lockManager.acquireLock(t2, seat2, LockType.WRITE);

        AtomicBoolean deadlockDetected = new AtomicBoolean(false);
        AtomicReference<Throwable> unexpectedError = new AtomicReference<>();

        // T1 requests Seat 102 and waits for T2.
        Thread thread1 = new Thread(() -> {
            try {
                lockManager.acquireLock(t1, seat2, LockType.WRITE);

                System.out.println("[TEST] T1 acquired Seat 102.");

                transactionManager.commitTransaction(t1.getId());

            } catch (Exception exception) {
                unexpectedError.compareAndSet(null, exception);

                if (t1.isActive()) {
                    transactionManager.abortTransaction(t1.getId());
                }
            }
        }, "Transaction-T1");

        thread1.start();

        // Wait until T1 is actually waiting for T2.
        long deadline = System.nanoTime()
                + 5_000_000_000L;

        while (waitForGraph.getDependencyCount() == 0
                && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }

        if (waitForGraph.getDependencyCount() == 0) {
            thread1.interrupt();
            thread1.join();

            throw new AssertionError(
                    "T1 did not enter the waiting state."
            );
        }

        System.out.println("[TEST] T1 is waiting for T2.");

        // T2 requests Seat 101, creating a cycle.
        Thread thread2 = new Thread(() -> {
            try {
                lockManager.acquireLock(t2, seat1, LockType.WRITE);

                // This should not happen in this test.
                transactionManager.commitTransaction(t2.getId());

            } catch (DeadlockDetectedException exception) {

                System.out.println(
                        "[TEST] Deadlock exception received: "
                                + exception.getMessage()
                );

                deadlockDetected.set(true);

                // Roll back T2 and release its locks.
                transactionManager.abortTransaction(t2.getId());

            } catch (Exception exception) {

                unexpectedError.compareAndSet(null, exception);

                if (t2.isActive()) {
                    transactionManager.abortTransaction(t2.getId());
                }
            }
        }, "Transaction-T2");

        thread2.start();

        thread1.join(5000);
        thread2.join(5000);

        if (thread1.isAlive() || thread2.isAlive()) {
            thread1.interrupt();
            thread2.interrupt();

            throw new AssertionError(
                    "Deadlock test timed out. A transaction is still waiting."
            );
        }

        if (unexpectedError.get() != null) {
            throw new AssertionError(
                    "Unexpected error during deadlock test.",
                    unexpectedError.get()
            );
        }

        System.out.println("Deadlock detected: " + deadlockDetected.get());
        System.out.println("T1 status: " + t1.getStatus());
        System.out.println("T2 status: " + t2.getStatus());

        if (!deadlockDetected.get()
                || t1.getStatus() != TransactionStatus.COMMITTED
                || t2.getStatus() != TransactionStatus.ABORTED) {
            throw new AssertionError("Deadlock test failed.");
        }

        if (lockManager.hasLock(t1, seat1)
                || lockManager.hasLock(t1, seat2)
                || lockManager.hasLock(t2, seat1)
                || lockManager.hasLock(t2, seat2)) {
            throw new AssertionError(
                    "Some locks were not released after deadlock resolution."
            );
        }

        System.out.println(
                "[TEST PASSED] Deadlock detected and resolved successfully."
        );
    }
}