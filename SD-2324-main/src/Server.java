import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Server {

  private ServerSocket socket;
  private final Map<String, Utilizador> userMap;
  private final ReentrantReadWriteLock mapLock;
  private final ThreadPool threadPool;

  public Server(int numberOfThreads) throws IOException {
    socket = new ServerSocket(9090);
    userMap = new HashMap<>();
    mapLock = new ReentrantReadWriteLock();

    // Create a thread pool with worker threads
    threadPool = new ThreadPool(numberOfThreads);
  }

  public void start() throws IOException {
    // Start the threadPool to handle the execution of the tasks
    threadPool.start();

    while (true) {
      Socket clientSoc = socket.accept();
      System.out.println(
        "Accepted connection on address " +
        clientSoc.getLocalAddress() +
        " in port " +
        clientSoc.getLocalPort()
      );

      ClientHandler client = new ClientHandler(clientSoc);

      new Thread(client).start();
    }
  }

  public void close() {
    try {
      // Shutdown the thread pool
      threadPool.shutdown();
      // Close the server socket
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Boolean regist(String username, String password) throws IOException {
    Boolean result = true;

    mapLock.readLock().lock();
    //verificar se o username ja existe
    if (userMap.containsKey(username)) {
      mapLock.readLock().unlock();
      System.out.println("O username já existe!\n");
      result = false;
    }
    //senao existir criar
    else {
      mapLock.readLock().unlock();
      Utilizador newUser = new Utilizador(username, password);
      mapLock.writeLock().lock();
      userMap.put(username, newUser);
      mapLock.writeLock().unlock();
    }
    return result;
  }

  public Boolean login(String username, String password) throws IOException {
    Boolean result = false;

    mapLock.readLock().lock();
    //verifica se o user name existe
    if (userMap.containsKey(username)) {
      //verifica se a password esta correta
      if (userMap.get(username).getPassword().equals(password)) {
        //verifica se o user ja nao esta online
        if (userMap.get(username).getStatus().equals(false)) {
          mapLock.readLock().unlock();
          mapLock.writeLock().lock();
          userMap.get(username).setStatus(true);
          mapLock.writeLock().unlock();
          result = true;
        } else {
          mapLock.readLock().unlock();
          System.out.println("O user já está autenticado!");
        }
      } else {
        mapLock.readLock().unlock();
        System.out.println("Password errada!\n");
      }
    } else {
      mapLock.readLock().unlock();
      System.out.println("User não existe!\n");
    }

    return result;
  }

  public void quitServer(String username) throws IOException {
    mapLock.writeLock().lock();
    userMap.get(username).setStatus(false);
    mapLock.writeLock().unlock();
  }

  private class ClientHandler implements Runnable {

    // private final Socket client;
    private SocketsManager sManager;

    //constructor
    public ClientHandler(Socket socket) {
      this.sManager = new SocketsManager(socket);
    }

    public void run() {
      try {
        String username = null;

        while (!sManager.isClosed()) {
          Task taskComp = threadPool.getCompTask(username);
          if (taskComp != null) {
            sManager.sendPedidoResponse(taskComp);
          } else {
            if (sManager.available() > 0) { // le socket
              try {
                char type = sManager.readChar();

                // Logica de diferenciar a mensagem
                if (type == 'l') { // tentativa de logging
                  username = sManager.readString();
                  String password = sManager.readString();

                  Boolean r = login(username, password);

                  sManager.sendLoginResponse(r);
                } else if (type == 'p') { // pedido de processamento
                  String nome = sManager.readString();
                  int tmh = sManager.readInt();
                  byte code[] = sManager.readBytes(tmh);

                  Task task = new Task(nome, username, tmh, 0, code);

                  // Submit uma task na thread pool
                  threadPool.submitTask(task);
                } else if (type == 'r') {
                  username = sManager.readString();

                  String password = sManager.readString();
                  Boolean r = regist(username, password);

                  sManager.sendRegistResponse(r);
                } else if (type == 'c') {
                  String estado = threadPool.getEstado();
                  sManager.sendConsultaResponse(estado);
                } else if (type == 'q') {
                  quitServer(username);
                } else { // pedido de processamento
                  System.err.println("Mensagem não reconhecida");
                }
              } catch (EOFException e) {
                System.err.println("EOF na Socket, cliente fechado!!\n");
              }
            }
          }
        }
      } catch (IOException ioException) {
        System.err.println("Error in server loop: " + ioException.getMessage());
        ioException.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        System.out.println("Client Closed!!");
      }
    }
  }

  public static void main(String[] args) throws IOException {
    Server s = new Server(3);
    s.start();
  }
}
