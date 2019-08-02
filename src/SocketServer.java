import java.io.*;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

/**
 * @Author: Changhai Man
 * @Date: 2019-07-18 11:03
 */
public class SocketServer {
    private static Logger log = Logger.getLogger("ServerLog");
    private static ServerSocket server;
    private static List<Service> clients = new ArrayList<>();


    public static void logException(Logger log, Exception e) {
        StringWriter trace = new StringWriter();
        e.printStackTrace(new PrintWriter(trace));
        log.warning(trace.toString());
    }

    private static void loggerInit() {
        //create logger
        try {
            new File("./logs").mkdirs();
            new File("./logs/Server_" + Calendar.getInstance().getTime().toString().replaceAll("[ \\:]", "_") + ".log").createNewFile();
            FileHandler logHandler = new FileHandler("./logs/Server_" + Calendar.getInstance().getTime().toString().replaceAll("[ \\:]", "_") + ".log");
            logHandler.setLevel(Level.ALL);
            logHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("[%s]:%s\r\n", record.getLevel(), record.getMessage());
                }
            });
            ConsoleHandler con = new ConsoleHandler();
            con.setLevel(Level.ALL);
            con.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("[%s]:%s\r\n", record.getLevel(), record.getMessage());
                }
            });
            log.addHandler(logHandler);
            log.addHandler(con);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("cannot create log\r\ncontinue?");
            Scanner inp = new Scanner(System.in);
            while (!inp.hasNextLine()) ;
            if (inp.nextLine().toLowerCase().charAt(0) != 'y') {
                System.err.println("program terminated");
                System.exit(-1);
            }
        }
    }

    private static void serverSocketInit(String[] args) {
        log.info("SocketServer start to initialize");
        int port = 55533;
        if (args.length > 0)
            try {
                port = Integer.parseInt(args[0]);
                if (port < 0 || port > 65535)
                    throw new ProtocolException("invalid port");
            } catch (NumberFormatException e) {
                logException(log, e);
                log.warning("Invalid argument: port, using default(55533)");
                port = 55533;
            } catch (ProtocolException e) {
                logException(log, e);
                log.warning("Invalid argument: port out of range(0-65535), using default(55533)");
                port = 55533;
            }
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            logException(log, e);
            log.severe("error creating server, program terminated");
            System.err.println("error creating server, program terminated");
            System.exit(-1);
        }
        log.info("Created SeverSocket on " + server.getLocalSocketAddress());
    }

    public static void main(String[] args) {
        loggerInit();
        serverSocketInit(args);

        System.out.println("waiting for connection");
        while (true) {
            try {
                Socket socket = server.accept();
                log.info(socket.getRemoteSocketAddress() + "comes in");
                Service client = new Service(socket);
                client.start();
                clients.add(client);
            } catch (IOException e) {
                logException(log, e);
            }
        }
    }

}
