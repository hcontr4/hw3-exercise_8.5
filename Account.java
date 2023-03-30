import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Account {

    private int balance;
    private ReentrantLock lock;
    private Condition ordinary;
    private Condition preferred;
    int preferredWaiting;

    public Account(int initialBalance) {
        balance = initialBalance;
        lock = new ReentrantLock();
        ordinary = lock.newCondition();
        preferred = lock.newCondition();
        preferredWaiting = 0;
    }

    int getBalance() {
        return balance;
    }

    void deposit(int k) {
        lock.lock();
        try {
            balance += k;
            // if preferred withdrawals are waiting only signal them
            if(preferredWaiting > 0) {
                preferred.signalAll();
                // only signalled preferred, exactly one preferred thread will take lock
                preferredWaiting--;
            }
            // only signal ordinary withdrawals if no preferred are waiting
            else {
                ordinary.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    void withdraw(int k, boolean preferredWithdraw){
        lock.lock();
        try {
            // while there is not enough money in account for withdrawal block/wait
            while (k > balance){
                if(preferredWithdraw) {
                    // thread with preferred withdrawal is waiting
                    preferredWaiting ++;
                    preferred.await();
                } else {
                    // ordinary thread waiting
                    ordinary.await();
                }
            }
            balance -= k;
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    void transfer(int k, Account reserve){
        lock.lock();
        try {
            reserve.withdraw(k, false);
            deposit(k);
        } finally {
            lock.unlock();
        }
    }
}