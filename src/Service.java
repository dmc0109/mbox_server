import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

/**
 * @Author: Changhai Man
 * @Date: 2019-07-21 22:13
 */
public class Service {
    private Socket client;
    private static Logger log = Logger.getLogger("ServerLog");
    private static Map<String, List<Map<String, String>>> serverRequest = new HashMap<>();
    private long lastCheck;

    public static void addServerRequest(String uid, Map<String, String> request) {
        if (!serverRequest.containsKey(uid))
            serverRequest.put(uid, new ArrayList<>());
        serverRequest.get(uid).add(request);
    }

    public Service(Socket client) {
        this.client = client;
    }

    public void start() {
        new Thread(() -> server()).start();

    }

    private void check() {
        this.lastCheck = System.currentTimeMillis() / 1000;
    }

    private void server() {
        Scanner fromClient;
        PrintWriter toClient;
        try {
            fromClient = new Scanner(client.getInputStream());
            toClient = new PrintWriter(client.getOutputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        check();
        while (!(client.isClosed() || client.isInputShutdown() || client.isOutputShutdown())) {
            if (lastCheck + 1 * 60 < System.currentTimeMillis() / 1000) {
                try {
                    client.close();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fromClient.hasNextLine()) {
                String request = fromClient.nextLine();
                Map<String, String> requestData = new HashMap<>();
                String[] requestPiece = request.split("\\&");
                for (String e : requestPiece) {
                    if (e == null)
                        continue;
                    else if (e.indexOf('=') == -1)
                        continue;
                    requestData.put(e.substring(0, e.indexOf('=')), e.substring(e.indexOf('=') + 1));
                }
                if (!requestData.containsKey("request"))
                    requestData.put("request", "but_no_request_at_all");
                log.info(String.format("[%s]:%s", client.getRemoteSocketAddress(), requestData.get("request")));
                log.info(String.format("[%s]:%s", client.getRemoteSocketAddress(), request));
                switch (requestData.get("request")) {
                    case "end":
                        toClient.println("end");
                        try {
                            client.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "preLogin":
                        toClient.println(Services.preLogin(requestData));
                        break;
                    case "login":
                        toClient.println(Services.login(requestData));
                        break;
                    case "preRegister":
                        toClient.println(Services.preRegister(requestData));
                        break;
                    case "register":
                        toClient.println(Services.register(requestData));
                        break;
                    case "verifyToken":
                        String returnStr = Services.verifyToken(requestData);
                        if (returnStr.contains("statue=success")) {
                            this.check();
                            returnStr = sendServerRequest(requestData, returnStr);
                        }
                        toClient.println(returnStr);
                        break;
                    case "applyAddFamily":
                        toClient.println(Services.applyAddFamily(requestData));
                        break;
                    case "acceptAddFamily":
                        toClient.println(Services.acceptAddFamily(requestData));
                        break;
                    case "getFamilyList":
                        toClient.println(Services.getFamilyList(requestData));
                        break;
                    case "addIntakeRecord":
                        toClient.println(Services.addIntakeRecord(requestData));
                        break;
                    case "getIntakeRecord":
                        toClient.println(Services.getIntakeRecord(requestData));
                        break;
                    case "addNewBox":
                        toClient.println(Services.addNewBox(requestData));
                        break;
                    case "getBoxes":
                        toClient.println(Services.getBoxes(requestData));
                        break;
                    case "removeUserBox":
                        toClient.println(Services.removeUserBox(requestData));
                        break;
                    default:
                        toClient.println(Services.invalidRequest(requestData));
                        break;
                }
                toClient.flush();
                if(!requestData.get("request").equals("verifyToken"))
                    check();
                try {
                    client.shutdownInput();
                    client.shutdownOutput();
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("disconect");
    }

    private String sendServerRequest(Map<String, String> clientRequest, String ori) {
        StringBuilder ansBuilder = new StringBuilder();
        ansBuilder.append(ori);
        ansBuilder.append('&');

        assert clientRequest.containsKey("uid");
        String uid = clientRequest.get("uid");
        if (serverRequest.containsKey(uid))
            if (serverRequest.get(uid).size() != 0) {
                Map<String, String> aServerRequest = serverRequest.get(uid).get(0);
                ansBuilder.append(Services.genRequest(aServerRequest));
                return ansBuilder.toString();
            }

        ansBuilder.append("serverRequest=no_request");
        return ansBuilder.toString();
    }
}
