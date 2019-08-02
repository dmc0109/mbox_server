import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * @Author: Changhai Man
 * @Date: 2019-07-18 11:16
 */
public class SocketClient {
    public static void main(String[] args) throws IOException {
        Scanner con = new Scanner(System.in);
        while (true) {
            System.out.println("host?");
            final String host = con.nextLine();
            int port;
            try {
                System.out.println("port?");
                port = Integer.parseInt(con.nextLine());
            } catch (NumberFormatException e) {
                e.printStackTrace();
                continue;
            }
            Socket server = new Socket(host, port);
            Scanner fromServer = new Scanner(server.getInputStream());
            OutputStreamWriter toServerWriter = new OutputStreamWriter(server.getOutputStream(), StandardCharsets.UTF_8);
            PrintWriter toServer = new PrintWriter(toServerWriter);
            while (true) {
                System.out.println("cmd?");
                switch (con.nextLine().toLowerCase().charAt(0)) {
                    case 's':
                        //byte[] bytes = (con.nextLine()+"\r\n").getBytes(StandardCharsets.UTF_8);
                        toServer.println(con.nextLine());
                        toServer.flush();
                        System.out.println(fromServer.nextLine());
                        break;
                    case 'r':
                        //while(fromServer.hasNextLine())
                            System.out.println(fromServer.nextLine());
                        break;
                    case 'c':
                        server = new Socket(host, port);
                        fromServer = new Scanner(server.getInputStream());
                        toServerWriter = new OutputStreamWriter(server.getOutputStream(), StandardCharsets.UTF_8);
                        toServer = new PrintWriter(toServerWriter);
                        break;
                    case 'e':
                        toServer.println("request=end");
                        toServer.flush();
                        fromServer.close();
                        toServer.close();
                        toServerWriter.close();
                        server.close();
                        break;
                    default:
                        break;
                }
            }
        }
    }
}

