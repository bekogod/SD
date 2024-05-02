import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPool {

  private final TaskQueue taskQueue;
  private final WorkerThread[] workers;
  private Lock lCompTasks = new ReentrantLock();
  private final LinkedList<Task> completedTasks = new LinkedList<>();

  public ThreadPool(int numThreads) {
    this.taskQueue = new TaskQueue(2000);
    this.workers = new WorkerThread[numThreads];

    for (int i = 0; i < numThreads; i++) {
      workers[i] = new WorkerThread();
    }
  }

  public Task getCompTask(String username) {
    if (username == null) return null;

    this.lCompTasks.lock();
    try {
      for (Task task : completedTasks) {
        if (task.getUsername() == username) {
          completedTasks.remove(task);
          return task;
        }
      }
      return null;
    } finally {
      this.lCompTasks.unlock();
    }
  }

  public void start() {
    for (int i = 0; i < workers.length; i++) {
      workers[i].start();
    }
  }

  public void submitTask(Task task) {
    taskQueue.addTask(task);
  }

  public void shutdown() {
    taskQueue.shutdown();

    for (WorkerThread worker : workers) {
      worker.interrupt();
    }
  }

  public String getEstado() {
    StringBuilder estado = new StringBuilder();

    // Print the state of the task queue
    estado.append("Task Queue State:\n");
    estado.append(taskQueue.getState());
    estado.append("\n");

    // Print the state of each worker thread
    estado.append("Worker Threads State:\n");
    for (int i = 0; i < workers.length; i++) {
      estado.append("Thread ").append(i).append(": ");
      if (workers[i].currentTask != null) {
        estado
          .append(workers[i].currentTask.getName())
          .append(" is being processed\n");
      } else {
        estado.append("Idle\n");
      }
    }
    estado.append(
      "Memory in use: " + taskQueue.getCurrentMemory() + " Bytes.\n"
    );

    return estado.toString();
  }

  private class WorkerThread extends Thread {

    Task currentTask = null;

    @Override
    public void run() {
      try {
        while (true) {
          Task task = taskQueue.getTask();
          currentTask = task;
          if (task != null) {
            task.run();
            // task.output
            // task.id
            // insere na lista de resposta (Completed tasks)
            try {
              lCompTasks.lock();
              currentTask = null;
              completedTasks.addLast(task);
            } finally {
              lCompTasks.unlock();
            }
            taskQueue.reduceMemory(task.getMemory());
          }
        }
      } catch (InterruptedException e) {
        // Restore the interrupted status

        Thread.currentThread().interrupt();
      }
    }
  }
}
