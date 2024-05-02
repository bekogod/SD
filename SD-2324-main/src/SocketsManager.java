import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import javax.print.FlavorException;

/*
 flags:
    login = l;
    registo = r;
    pedido de execução = e;
    consulta = c;
    quit = q;
    login response = z;
    register response = y;
    pedido response = x;
    consulta response = w;

 */

class SocketsManager {

  private Socket socket;

  private DataInputStream in;
  private DataOutputStream out;

  SocketsManager(Socket socket) {
    this.socket = socket;

    try {
      this.in = new DataInputStream(socket.getInputStream());
      this.out = new DataOutputStream(socket.getOutputStream());
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  public char readChar() throws IOException {
    return in.readChar();
  }

  public String readString() throws IOException {
    return in.readUTF();
  }

  public int readInt() throws IOException {
    return in.readInt();
  }

  public byte[] readBytes(int length) throws IOException {
    byte array[] = new byte[length];

    for (int i = 0; i < length; i++) array[i] = in.readByte();

    return array;
  }

  public void sendLogin(String user, String password) throws IOException {
    //System.out.println("Sending data");
    out.writeChar('l');
    out.writeUTF(user);
    out.writeUTF(password);
    out.flush();
  }

  public void sendLoginResponse(Boolean b) throws IOException {
    //System.out.println("Sending data");
    out.writeChar('z');
    out.writeBoolean(b);
    out.flush();
  }

  public Boolean recLogin() throws IOException {
    Boolean result = false;
    char flag = in.readChar();
    if (flag == 'z') {
      result = in.readBoolean();
    }
    return result;
  }

  public void sendRegist(String user, String password) throws IOException {
    //System.out.println("Sending data");
    out.writeChar('r');
    out.writeUTF(user);
    out.writeUTF(password);
    out.flush();
  }

  public void sendRegistResponse(Boolean b) throws IOException {
    //System.out.println("Sending data");
    out.writeChar('y');
    out.writeBoolean(b);
    out.flush();
  }

  public Boolean recRegist() throws IOException {
    Boolean result = false;
    char flag = in.readChar();
    if (flag == 'y') {
      result = in.readBoolean();
    }
    return result;
  }

  public void sendPedido(String task, int tam, byte[] code) throws IOException {
    //System.out.println("Sending data");
    out.writeChar('p');
    out.writeUTF(task);
    out.writeInt(tam);
    out.write(code);
    out.flush();
  }

  public void sendPedidoResponse(Task task) throws IOException {
    //System.out.println("Sending data");
    String taskName = task.getName();
    String message = task.getMessage();
    out.writeChar('x');
    if (message == "unknown") {
      out.writeChar('S');
      out.writeUTF(taskName);
      byte[] output = task.getOutput();
      out.writeInt(output.length);
      out.write(output);
    } else {
      out.writeChar('I');
      out.writeUTF(taskName);
      int code = task.getCode();
      out.writeInt(code);
      out.writeUTF(message);
    }

    out.flush();
  }

  public void sendConsulta() throws IOException {
    //System.out.println("Sending data");
    out.writeChar('c');
    out.flush();
  }

  public void sendConsultaResponse(String estado) throws IOException {
    //System.out.println("Sending data");
    out.writeChar('w');
    out.writeUTF(estado);
    out.flush();
  }

  public void sendQuit(String username) throws IOException {
    //System.out.println("Sending quit message");
    out.writeChar('q');
    out.writeUTF(username);
    out.flush();
  }

  public void close() throws IOException {
    this.socket.close();
  }

  public boolean isClosed() {
    return this.socket.isClosed();
  }

  public int available() throws IOException {
    int bytesToRead = in.available();

    return bytesToRead;
  }
}
