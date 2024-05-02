import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import sd23.*;

public class Task implements Runnable, Comparable<Task> {

  private String name;
  private String username;
  private int memory;

  private int priority; // Added priority field
  private byte[] input;
  private byte[] output;

  private int code;
  private String message;

  public String getUsername() {
    return username;
  }

  public String getMessage() {
    return message;
  }

  public int getCode() {
    return code;
  }

  public byte[] getOutput() {
    return output;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setMessage(String msg) {
    this.message = msg;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getMemory() {
    return memory;
  }

  public void setMemory(int memory) {
    this.memory = memory;
  }

  public byte[] getInput() {
    return input;
  }

  public void setInput(byte[] bytes) {
    this.input = bytes;
  }

  public Task(
    String name,
    String username,
    int memory,
    int priority,
    byte[] bytes
  ) {
    this.name = name;
    this.username = username;
    this.memory = memory;
    this.priority = priority;
    this.input = bytes;
    this.code = -1;
    this.message = "unknown";
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  @Override
  public void run() {
    // System.out.println(
    //         "Start Task '" + this.name + "' using " + this.memory + " bytes " + this.priority + " priority" + " -> " + Thread.currentThread().getName()
    // );
    try {
      // execute the task
      this.output = JobFunction.execute(this.input);

      // use the result or report the error
      System.err.println("success, returned " + output.length + " bytes");
    } catch (JobFunctionException e) {
      this.code = e.getCode();
      this.message = e.getMessage();

      System.err.println(
        "job failed: code = " + this.code + " message = " + this.message
      );
    } catch (Exception e) {
      System.err.println("error reading file: " + e);
    }
    // System.out.println(
    //         "Finished Task '" + this.name + "' using " + this.memory + " bytes"
    // );
  }

  @Override
  public String toString() {
    return (
      "Task{" +
      "name='" +
      name +
      '\'' +
      ", memory=" +
      memory +
      ", priority=" +
      priority +
      '}'
    );
  }

  @Override
  public int compareTo(Task other) {
    return Integer.compare(other.priority, this.priority);
  }

  public void increasePriority() {
    this.priority += 1;
  }
}
