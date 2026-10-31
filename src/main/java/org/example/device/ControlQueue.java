package org.example.device;

import java.util.ArrayList;
import java.util.List;

public class ControlQueue {
    private final Object lock = new Object();
    private final List<ControlRequest> requests = new ArrayList<>();
    private final int capacity;
    private boolean running = true;

    public ControlQueue(int capacity) {
        this.capacity = capacity;
    }

    public void put(ControlRequest request) {
        synchronized (lock) {
            while (requests.size() == capacity) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            requests.add(request);
            lock.notifyAll();
        }
    }

    // Returns null once the queue has been shut down and is empty.
    public ControlRequest take() {
        synchronized (lock) {
            while (requests.isEmpty() && running) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            if (requests.isEmpty()) {
                return null;
            }
            ControlRequest request = requests.remove(0);
            lock.notifyAll();
            return request;
        }
    }

    public void shutdown() {
        synchronized (lock) {
            running = false;
            lock.notifyAll();
        }
    }

    public List<ControlRequest> getPendingRequests() {
        synchronized (lock) {
            return List.copyOf(requests);
        }
    }
}
