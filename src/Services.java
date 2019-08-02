
import java.security.KeyPair;
import java.util.*;
import java.util.logging.Logger;

/**
 * @Author: Changhai Man
 * @Date: 2019-07-20 13:09
 */
public class Services {
    private static Logger log = Logger.getLogger("ServerLog");

    public static String genRequest(Map<String, String> request) {
        StringBuilder requestBuilder = new StringBuilder();
        request.entrySet().forEach(e -> {
            requestBuilder.append(e.getKey());
            requestBuilder.append('=');
            requestBuilder.append(e.getValue());
            requestBuilder.append('&');
        });
        requestBuilder.deleteCharAt(requestBuilder.length() - 1);
        return requestBuilder.toString();
    }

    public static String preLogin(Map<String, String> request) {
        Map<String, String> result = new HashMap<>();
        result.put("answerRequest", "preLogin");
        result.put("statue", "success");

        Session session = Session.newSession();
        result.put("sessionID", session.sessionID);

        KeyPair keyPair = LoginUtils.RSAGenKeyPair();
        String pubKeyStr = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()).replaceAll("\\r\\n", "");
        result.put("publicKey", pubKeyStr);
        session.datas.put("keypair", keyPair);
        return genRequest(result);
        /*try {
            OutputStream toClient = client.getOutputStream();
            toClient.write(genRequest(result).getBytes(StandardCharsets.UTF_8));
            toClient.close();
            log.info(String.format("[%s]:%s", client.getRemoteSocketAddress(), result.get("statue")));
        } catch (IOException e) {
            e.printStackTrace();
            log.severe(String.format("[%s]:%s", client.getRemoteSocketAddress(), "service error"));
            SocketServer.logException(log, e);
            return -1;
        }
        return 0;*/
    }

    public static String login(Map<String, String> request) {
        Map<String, String> result = new HashMap<>();
        result.put("answerRequest", "login");
        result.put("statue", "success");
        if (!request.containsKey("sessionID"))
            result.put("statue", "requireSID");
        else if (Session.getSession(request.get("sessionID")) == null)
            result.put("statue", "invalidSID");
        else if (Session.getSession(request.get("sessionID")).datas.get("keypair") == null)
            result.put("statue", "invalidSID");
        else if (!request.containsKey("user_name"))
            result.put("statue", "require_user_name");
        else if (!request.containsKey("password"))
            result.put("statue", "require_password");
        else {
            String userName = request.get("user_name");
            String password = request.get("password");
            Session session = Session.getSession(request.get("sessionID"));
            KeyPair keyPair = (KeyPair) session.datas.get("keypair");
            Map<String, String> loginReturn = LoginUtils.login(userName, keyPair.getPrivate(), password);
            if (!loginReturn.containsKey("statue"))
                loginReturn.put("statue", "unknown_server_error");
            result.put("statue", loginReturn.get("statue"));
            if ("success".equals(loginReturn.get("statue"))) {
                result.put("token", loginReturn.get("token"));
                result.put("uid", loginReturn.get("uid"));
                Session.removeSession(session);
                LoginUtils.setToken(loginReturn.get("uid"), loginReturn.get("token"));
            }
        }
        return genRequest(result);
        /*
        try {
            OutputStream toClient = client.getOutputStream();
            toClient.write(genRequest(result).getBytes(StandardCharsets.UTF_8));
            toClient.close();
            log.info(String.format("[%s]:%s", client.getRemoteSocketAddress(), result.get("statue")));
        } catch (IOException e) {
            e.printStackTrace();
            log.severe(String.format("[%s]:%s", client.getRemoteSocketAddress(), "service error"));
            SocketServer.logException(log, e);
            return -1;
        }
        return 0;*/
    }

    public static String invalidRequest(Map<String, String> request) {
        Map<String, String> result = new HashMap<>();
        result.put("answerRequest", "invalidRequest");
        result.put("statue", "i_don_t_get_what_you_want");
        return genRequest(result);
        /*try {
            OutputStream toClient = client.getOutputStream();
            toClient.write(genRequest(result).getBytes(StandardCharsets.UTF_8));
            toClient.close();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;*/
    }

    public static String preRegister(Map<String, String> request) {
        Map<String, String> result = new HashMap<>();
        result.put("answerRequest", "preRegister");
        if (request.containsKey("sessionID"))
            if (Session.getSession(request.get("sessionID")) != null) {
                Session session = Session.getSession(request.get("sessionID"));
                result.put("sessionID", session.sessionID);
                KeyPair keyPair = (KeyPair) session.get("keypair");
                keyPair = keyPair == null ? LoginUtils.RSAGenKeyPair() : keyPair;
                session.put("keypair", keyPair);
                assert keyPair != null;
                String pubKeyStr = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()).replaceAll("\\r\\n", "");
                result.put("publicKey", pubKeyStr);
            }
        if (!request.containsKey("user_name"))
            result.put("statue", "require_user_name");
        else if (!LoginUtils.checkUsernameValid(request.get("user_name")))
            result.put("statue", "user_name_has_been_used");
        else {
            result.put("statue", "success");
            if (!result.containsKey("sessionID")) {
                Session session = Session.newSession();

                KeyPair keyPair = LoginUtils.RSAGenKeyPair();
                session.put("keypair", keyPair);

                result.put("sessionID", session.sessionID);

                String pubKeyStr = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()).replaceAll("\\r\\n", "");
                result.put("publicKey", pubKeyStr);
            }
        }
        return genRequest(result);
        /*try {
            OutputStream toClient = client.getOutputStream();
            toClient.write(genRequest(result).getBytes(StandardCharsets.UTF_8));
            toClient.close();
            log.info(String.format("[%s]:%s", client.getRemoteSocketAddress(), result.get("statue")));
        } catch (IOException e) {
            e.printStackTrace();
            log.severe(String.format("[%s]:%s", client.getRemoteSocketAddress(), "service error"));
            SocketServer.logException(log, e);
            return -1;
        }
        return 0;*/
    }

    public static String register(Map<String, String> request) {
        Map<String, String> result = new HashMap<>();
        result.put("answerRequest", "register");
        result.put("statue", "success");
        if (!request.containsKey("sessionID"))
            result.put("statue", "requireSID");
        else if (Session.getSession(request.get("sessionID")) == null)
            result.put("statue", "invalidSID");
        else if (Session.getSession(request.get("sessionID")).datas.get("keypair") == null)
            result.put("statue", "invalidSID");
        else if (!request.containsKey("user_name"))
            result.put("statue", "require_user_name");
        else if (!request.containsKey("password"))
            result.put("statue", "require_password");
        else if (!request.containsKey("nick_name"))
            result.put("statue", "require_nickname");
        else {
            Session session = Session.getSession(request.get("sessionID"));
            KeyPair keyPair = (KeyPair) session.get("keypair");
            String userName = request.get("user_name");
            String nickName = request.get("nick_name");
            String secretPassword = request.get("password");
            String uid = LoginUtils.register(userName, nickName, keyPair.getPrivate(), secretPassword);
            if ("-1".equals(uid))
                result.put("statue", "user_name_has_been_used");
            else if ("-2".equals(uid))
                result.put("statue", "server_error");
            else if (uid.length() != 32)
                result.put("statue", "unknown_error");
            else
                result.put("uid", uid);
        }
        return genRequest(result);
        /*try {
            OutputStream toClient = client.getOutputStream();
            toClient.write(genRequest(result).getBytes(StandardCharsets.UTF_8));
            toClient.close();
            log.info(String.format("[%s]:%s", client.getRemoteSocketAddress(), result.get("statue")));
        } catch (IOException e) {
            e.printStackTrace();
            log.severe(String.format("[%s]:%s", client.getRemoteSocketAddress(), "service error"));
            SocketServer.logException(log, e);
            return -1;
        }
        return 0;*/
    }

    public static String verifyToken(Map<String, String> request) {
        Map<String, String> result = new HashMap<>();
        result.put("answerRequest", "verifyToken");
        if (!request.containsKey("uid"))
            result.put("statue", "uid_needed");
        else if (!request.containsKey("token"))
            result.put("statue", "token_needed");
        else {
            int tokenStatue = LoginUtils.verifyToken(request.get("uid"), request.get("token"));
            if (tokenStatue == 0)
                result.put("statue", "success");
            else if (tokenStatue == 1) {
                result.put("statue", "token_updated");
                String token = LoginUtils.getUserToken(request.get("uid"));
                result.put("token", token);
            } else if (tokenStatue == -1)
                result.put("statue", "invalid_token_or_no_record");
            else
                result.put("statue", "server_error");
        }
        return genRequest(result);
        /*try {
            OutputStream toClient = client.getOutputStream();
            toClient.write(genRequest(result).getBytes(StandardCharsets.UTF_8));
            toClient.close();
            log.info(String.format("[%s]:%s", client.getRemoteSocketAddress(), result.get("statue")));
        } catch (IOException e) {
            e.printStackTrace();
            log.severe(String.format("[%s]:%s", client.getRemoteSocketAddress(), "service error"));
            SocketServer.logException(log, e);
            return -1;
        }
        return 0;*/
    }

    public static String applyAddFamily(Map<String, String> request) {
        Map<String, String> result = new HashMap<>();
        result.put("answerRequest", "applyAddFamily");
        if (!request.containsKey("uid"))
            result.put("statue", "uid_needed");
        else if (!request.containsKey("token"))
            result.put("statue", "token_needed");
        else if (!request.containsKey("family_user_name"))
            result.put("statue", "family_user_name_needed");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == -1)
            result.put("statue", "invalid_token");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) <= -2)
            result.put("statue", "server_error");
        else {
            if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == 1) {
                result.put("statue", "please_update_token");
                String token = LoginUtils.getUserToken(request.get("uid"));
                result.put("token", token);
            }
            String familyUID = LoginUtils.getUserUID(request.get("family_user_name"));
            if ("-1".equals(familyUID))
                result.put("statue", "no_such_a_user");
            else if ("-2".equals(familyUID))
                result.put("statue", "server_error");
            else {
                Session session = Session.newSession();
                session.put("applyUID", request.get("uid"));
                session.put("familyUID", familyUID);
                result.put("sessionID", session.sessionID);
                result.put("statue", "success");

                Map<String, String> serverRequest = new HashMap<>();
                serverRequest.put("serverRequest", "newFamily");
                serverRequest.put("s_applier", request.get("uid"));
                serverRequest.put("s_applierNickName", LoginUtils.getNickname(request.get("uid")));
                serverRequest.put("s_applierUserName", LoginUtils.getUserToken(request.get("uid")));
                serverRequest.put("s_sessionID", session.sessionID);
                Service.addServerRequest(familyUID, serverRequest);
            }
        }
        return genRequest(result);
    }

    public static String acceptAddFamily(Map<String, String> request) {
        Map<String, String> result = new HashMap<>();
        result.put("answerRequest", "applyAddFamily");
        if (!request.containsKey("uid"))
            result.put("statue", "uid_needed");
        else if (!request.containsKey("token"))
            result.put("statue", "token_needed");
        else if (!request.containsKey("sessionID"))
            result.put("statue", "sessionID_needed");
        else if (Session.getSession(request.get("sessionID")) == null)
            result.put("statue", "invalid_session_id");
        else if (Session.getSession(request.get("sessionID")).get("applyUID") == null)
            result.put("statue", "invalid_session_id");
        else if (!request.containsKey("reply"))
            result.put("statue", "reply_needed");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == -1)
            result.put("statue", "invalid_token");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) <= -2)
            result.put("statue", "server_error");
        else {
            result.put("statue", "success");
            if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == 1) {
                result.put("statue", "please_update_token");
                result.put("token", LoginUtils.getUserToken(request.get("uid")));
            }

            if ("accept".equals(request.get("reply"))) {
                Session session = Session.getSession(request.get("sessionID"));
                LoginUtils.addFamilies((String) session.get("applyUID"), (String) session.get("familyUID"));
                Session.removeSession(session);
                Map<String, String> serverRequest = new HashMap<>();
                serverRequest.put("serverRequest", "newFamilyReply");
                serverRequest.put("s_familyUserName", LoginUtils.getUsername((String) session.get("applyUID")));
                serverRequest.put("s_familyNickName", LoginUtils.getNickname((String) session.get("applyUID")));
                serverRequest.put("s_sessionID", session.sessionID);
                serverRequest.put("s_feedback", "accept");
                Service.addServerRequest((String) session.get("applyUID"), serverRequest);
            } else if ("deny".equals(request.get("reply"))) {
                Session session = Session.getSession(request.get("sessionID"));
                Session.removeSession(session);
                Map<String, String> serverRequest = new HashMap<>();
                serverRequest.put("serverRequest", "newFamilyReply");
                serverRequest.put("s_familyUserName", LoginUtils.getUsername((String) session.get("applyUID")));
                serverRequest.put("s_familyNickName", LoginUtils.getNickname((String) session.get("applyUID")));
                serverRequest.put("s_sessionID", session.sessionID);
                serverRequest.put("s_feedback", "deny");
                Service.addServerRequest((String) session.get("applyUID"), serverRequest);
            }
        }
        return genRequest(result);
    }

    public static String getFamilyList(Map<String, String> request) {
        Map<String, String> result = new HashMap<>();
        result.put("answerRequest", "getFamilyList");
        if (!request.containsKey("uid"))
            result.put("statue", "uid_needed");
        else if (!request.containsKey("token"))
            result.put("statue", "token_needed");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == -1)
            result.put("statue", "invalid_token");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) <= -2)
            result.put("statue", "server_error");
        else {
            String familiesList = LoginUtils.getFamilies(request.get("uid"));
            if ("server_error".equals(familiesList))
                result.put("statue", "server_error");
            else {
                result.put("families_list", familiesList);
                result.put("statue", "success");
                if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == 1) {
                    result.put("statue", "please_update_token");
                    result.put("token", LoginUtils.getUserToken(request.get("uid")));
                }
            }

        }
        return genRequest(result);
    }

    public static String addIntakeRecord(Map<String, String> request) {
        Map<String, String> result = new HashMap<>();
        result.put("answerRequest", "addIntakeRecord");
        if (!request.containsKey("uid"))
            result.put("statue", "uid_needed");
        else if (!request.containsKey("token"))
            result.put("statue", "token_needed");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == -1)
            result.put("statue", "invalid_token");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) <= -2)
            result.put("statue", "server_error");
        else if (!request.containsKey("boxUID"))
            result.put("statue", "box_uid_needed");
        else if (!request.containsKey("pillNumber"))
            result.put("statue", "pill_number_needed");
        else {

            if (!request.containsKey("time"))
                request.put("time", String.valueOf(System.currentTimeMillis() / 1000));
            if (LoginUtils.addIntakeRecord(request.get("uid"), request.get("boxUID"), Integer.parseInt(request.get("pillNumber")), request.get("time"))) {
                result.put("statue", "success");
                if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == 1) {
                    result.put("statue", "please_update_token");
                    result.put("token", LoginUtils.getUserToken(request.get("uid")));
                }
            }
            else
                result.put("statue", "server_error");
        }
        return genRequest(result);
    }

    public static String getIntakeRecord(Map<String, String> request) {
        Map<String, String> result = new HashMap<>();
        result.put("answerRequest", "getFamilyList");
        if (!request.containsKey("uid"))
            result.put("statue", "uid_needed");
        else if (!request.containsKey("token"))
            result.put("statue", "token_needed");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == -1)
            result.put("statue", "invalid_token");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) <= -2)
            result.put("statue", "server_error");
        else {
            long startTime = 0L, endTime = Long.MAX_VALUE;
            if (request.containsKey("startTime"))
                try {
                    startTime = Long.parseLong(request.get("startTime"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            if (request.containsKey("endTime"))
                try {
                    endTime = Long.parseLong(request.get("endTime"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            if (request.containsKey("medicineName"))
                result.put("record_list", LoginUtils.getIntakeRecord( request.get("uid"), request.get("boxUID"),startTime, endTime, request.get("medicineName")));
            else if(request.containsKey("boxUID"))
                result.put("record_list", LoginUtils.getIntakeRecord(request.get("uid"), request.get("boxUID"), startTime, endTime));
            else
                result.put("record_list", LoginUtils.getIntakeRecord(request.get("uid"), startTime, endTime));
            result.put("statue", "success");
            if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == 1) {
                result.put("statue", "please_update_token");
                result.put("token", LoginUtils.getUserToken(request.get("uid")));
            }
        }
        return genRequest(result);
    }

    public static String getBoxes(Map<String, String> request){
        Map<String, String> result = new HashMap<>();
        result.put("answerRequest", "getFamilyList");
        if (!request.containsKey("uid"))
            result.put("statue", "uid_needed");
        else if (!request.containsKey("token"))
            result.put("statue", "token_needed");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == -1)
            result.put("statue", "invalid_token");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) <= -2)
            result.put("statue", "server_error");
        else {
            result.put("statue", "success");
            if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == 1) {
                result.put("statue", "please_update_token");
                result.put("token", LoginUtils.getUserToken(request.get("uid")));
            }
            result.put("boxesList", LoginUtils.getBoxes(request.get("uid")));
        }
        return genRequest(result);
    }

    public static String removeUserBox(Map<String, String> request){
        Map<String, String> result = new HashMap<>();
        result.put("answerRequest", "getFamilyList");
        if (!request.containsKey("uid"))
            result.put("statue", "uid_needed");
        else if (!request.containsKey("token"))
            result.put("statue", "token_needed");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == -1)
            result.put("statue", "invalid_token");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) <= -2)
            result.put("statue", "server_error");
        else if (!request.containsKey("boxUID"))
            result.put("statue", "box_uid_needed");
        else {
            result.put("statue", "success");
            if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == 1) {
                result.put("statue", "please_update_token");
                result.put("token", LoginUtils.getUserToken(request.get("uid")));
            }
            List<String> boxes = new ArrayList<>();
            boxes.add(request.get("boxUID"));
            if(!LoginUtils.removeUserBoxes(request.get("uid"), boxes)){
                result.put("statue", "server_error");
            }
        }
        return genRequest(result);
    }

//    public static String addNewBox(Map<String, String> request) {
//        Map<String, String> result = new HashMap<>();
//        result.put("answerRequest", "addNewBox");
//        if (!request.containsKey("uid"))
//            result.put("statue", "uid_needed");
//        else if (!request.containsKey("token"))
//            result.put("statue", "token_needed");
//        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == -1)
//            result.put("statue", "invalid_token");
//        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) <= -2)
//            result.put("statue", "server_error");
//        else if (!request.containsKey("boxList"))
//            result.put("statue", "box_list_needed");
////        else if (!request.containsKey("boxUID"))
////            result.put("statue", "box_uid_needed");
//        else{
//            List<String> boxes = new ArrayList<>();
//            String a = request.get("boxList").replaceAll("[\\[\\]]", "");
//            for(String e:a.split(","))
//                boxes.add(e);
//            if(boxes.size()==0&&a.indexOf("medicineName")!=-1)
//                boxes.add(a);
//            for(String e:boxes){
//                e = e.replaceAll("[\\{\\}]", "");
//                List<String> features = new ArrayList<>();
//                for(String f:e.split("\\$\\$"))
//                    features.add(f);
//                if(features.size()==0&&e.indexOf("medicineName")!=-1)
//                    features.add(e);
//                Map<String, String> featuresList = new HashMap<>();
//                for(String f:features){
//                    f = f.replaceFirst("^\\*\\*", "");
//                    String key = f.substring(0, f.indexOf("**"));
//                    String value = f.substring(f.indexOf("**")+3);
//                    featuresList.put(key, value);
//                }
//                if(!featuresList.containsKey("medicineName"))
//                    continue;
//                if(featuresList.containsKey("pillNumber"))
//                    LoginUtils.createBoxes(request.get("boxUID"), featuresList.get("medicineName"), request.get("uid"), Integer.parseInt(featuresList.get("pillNumber")));
//                else
//                    LoginUtils.createBoxes(request.get("boxUID"), featuresList.get("medicineName"), request.get("uid"));
//            }
//        }
//        return genRequest(result);
//    }

    public static String addNewBox(Map<String, String> request){
        Map<String, String> result = new HashMap<>();
        result.put("answerRequest", "addNewBox");
        if (!request.containsKey("uid"))
            result.put("statue", "uid_needed");
        else if (!request.containsKey("token"))
            result.put("statue", "token_needed");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) == -1)
            result.put("statue", "invalid_token");
        else if (LoginUtils.verifyToken(request.get("uid"), request.get("token")) <= -2)
            result.put("statue", "server_error");
        else if (!request.containsKey("medicineName"))
            result.put("statue", "medicine_name_needed");
        else if (!request.containsKey("boxUID"))
            result.put("statue", "box_uid_needed");
        else{
            int pillNumber = -1;
            if(request.containsKey("pillNumber"))
                try{
                    pillNumber = Integer.parseInt(request.get("pillNumber"));
                }catch (NumberFormatException e){
                    e.printStackTrace();
                }
            String response = LoginUtils.createBoxes(request.get("boxUID"), request.get("medicineName"), request.get("uid"), pillNumber);
            if(response.length()!=32)
                result.put("statue", response);
            else
                result.put("statue", "success");
        }
        return genRequest(result);
    }
}
