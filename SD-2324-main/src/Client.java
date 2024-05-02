import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class Client {

  private Socket socket; // = new Socket("Legion", 9090);
  private SocketsManager sManager;
  private String serverStatus;
  Thread receiveThread;

  public String getServerStatus() {
    return serverStatus;
  }

  public void setServerStatus(String sStatus) {
    serverStatus = sStatus;
  }

  private String username;

  private Lock statusLock = new ReentrantLock();
  private Condition serverStatusUpdate = statusLock.newCondition();

  /*
   * {key: "TASKNAME" : Value: String -> Sem resultado
   *                                  -> Recebido com Sucesso, tmh: 1000 bytes
   *                                  -> Recebido sem Sucesso, code:138 , msg:
   * }
   */
  private final Map<String, String> tasksMap;
  private final ReentrantReadWriteLock mapLock;

  public Client() {
    try {
      socket = new Socket("localhost", 9090);
    } catch (ConnectException e) {
      e.printStackTrace();
      System.err.println("\nERROR IN CLIENT, check if server is open...\n");
    } catch (IOException e) {
      e.printStackTrace();
    }
    serverStatus = "No Status asked yet!!";
    sManager = new SocketsManager(socket);
    tasksMap = new HashMap<String, String>();
    mapLock = new ReentrantReadWriteLock();
  }

  /**
   * handles login logic, try's a username and a password
   * only finishes if login is successful.
   *
   * @throws IOException
   */
  public void start() throws IOException {
    receiveThread =
      new Thread(() -> {
        while (!sManager.isClosed()) {
          try {
            receive(sManager);
          } catch (SocketException e) {
            System.out.println("Socket Closed, client stopped listening");
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });
    receiveThread.start();
  }

  /**
   * Closes the client gracefully.
   */
  public void close() {
    try {
      // Send a quit message to the server before closing the socket
      quit();

      // Close the socket
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }

      if (receiveThread != null && receiveThread.isAlive()) {
        receiveThread.interrupt();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Boolean login(String username, String password) throws IOException {
    sManager.sendLogin(username, password);
    this.username = username;
    return sManager.recLogin();
  }

  public Boolean registo(String username, String password) throws IOException {
    sManager.sendRegist(username, password);
    this.username = username;
    return sManager.recRegist();
  }

  public void pedido(String task, int tam, byte[] code) throws IOException {
    mapLock.writeLock().lock();
    tasksMap.put(task, "A espera de resposta...");
    mapLock.writeLock().unlock();

    sManager.sendPedido(task, tam, code);
  }

  public void consulta() throws IOException {
    sManager.sendConsulta();
  }

  public void quit() throws IOException {
    sManager.sendQuit(username);
  }

  public String consultarMap() {
    Map<String, String> map = this.tasksMap;
    StringBuilder sb = new StringBuilder();
    // Entry<TaskName , TaskStatus>
    this.mapLock.readLock().lock();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      sb.append("  Task: " + entry.getKey()).append(", Status: ");
      sb.append(entry.getValue()).append("\n");
    }
    this.mapLock.readLock().unlock();
    return sb.toString();
  }

  public void executePedido(String input) throws IOException {
    String task[] = input.split(";");
    String taskName;
    byte code[];
    int size;

    if (task.length != 2) {
      System.out.println("O input não está no formato correto");
    } else {
      taskName = task[0];
      code = task[1].getBytes(StandardCharsets.UTF_8);
      size = code.length;

      this.pedido(taskName, size, code);
    }
  }

  public void receive(SocketsManager sManager) throws IOException {
    try {
      char type = sManager.readChar();
      //System.out.println("Received msg type " + type);
      if (type == 'x') { //resposta dum pedido
        char type2 = sManager.readChar();
        String taskName = sManager.readString();

        mapLock.writeLock().lock();
        try {
          if (type2 == 'S') {
            int length = sManager.readInt();
            byte output[] = sManager.readBytes(length);
            try (FileOutputStream fos = new FileOutputStream(taskName)) {
              // Write the byte array to the file
              fos.write(output);
            } catch (IOException e) {
              e.printStackTrace();
            }
            tasksMap.put(
              taskName,
              "Recebido com Sucesso, tmh: " + output.length + " bytes"
            );
          } else if (type2 == 'I') {
            int code = sManager.readInt();
            String msg = sManager.readString();
            tasksMap.put(
              taskName,
              "Recebido sem Sucesso, code: " + code + ", msg: " + msg
            );

            try (
              BufferedWriter writer = new BufferedWriter(
                new FileWriter(taskName)
              )
            ) {
              // Write the content to the file
              writer.write(
                "Recebido sem Sucesso, code: " + code + ", msg: " + msg
              );
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        } finally {
          mapLock.writeLock().unlock();
        }
      } else if (type == 'w') { //resposta duma consulta
        //System.out.println("Resposta duma consulta");
        statusLock.lock();
        this.serverStatus = sManager.readString();
        try {
          serverStatusUpdate.signal();
        } finally {
          statusLock.unlock();
        }
      }
    } catch (EOFException e) {
      System.err.println("EOF na Socket, Server fechado!!\n");
    }
  }

  public void consultaServidor() {
    this.statusLock.lock();
    try {
      sManager.sendConsulta();
      //System.out.println("waiting for response");
      serverStatusUpdate.await();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      this.statusLock.unlock();
    }
  }
}
