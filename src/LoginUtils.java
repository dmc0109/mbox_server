
import javax.crypto.Cipher;
import java.math.BigInteger;
import java.security.*;
import java.sql.*;
import java.util.*;

/**
 * @author: Changhai Man
 */
public class LoginUtils {

    private static final int BOXES_START = 2, BOXES_END = 17;

    /**
     * update token and distinct box periodic
     */
    static {
        init();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                bufAndUpdateToken();
            }
        }, 6000, 1000 * 30);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                distinctBoxes();
            }
        }, 6000, 1000 * 60 * 15);
    }

    private static void bufAndUpdateToken() {
        //System.out.println("working now");
        Statement stm;
        final String sqlAsk = "SELECT uid,token,validity_time FROM tokens WHERE validity_time<" + System.currentTimeMillis() / 1000;
        final String sqlUpdate = "UPDATE tokens SET token=\'%s\',old_token=\'%s\',validity_time=\'%d\' WHERE uid=\'%s\'";
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            ResultSet results = stm.executeQuery(sqlAsk);
            Map<String, String> resultsMap = new HashMap<>();
            while (results.next())
                resultsMap.put(results.getString("uid"), results.getString("token"));
            for (Map.Entry<String, String> e : resultsMap.entrySet()) {
                String newToken = genToken();
                stm.executeUpdate(String.format(sqlUpdate, newToken, e.getValue(), System.currentTimeMillis() / 1000 + 3600, e.getKey()));
                resultsMap.put(e.getKey(), newToken);
            }
            tokensBuf = resultsMap;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void distinctBoxes() {
        Statement stm;
        final String sqlAll = "SELECT * FROM boxes";
        Map<String, List<String>> boxes = new HashMap<>();
        List<String> toBeRemove = new ArrayList<>();
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            ResultSet res = stm.executeQuery(sqlAll);
            while (res.next()) {
                if (!boxes.containsKey(res.getString("owner")))
                    boxes.put(res.getString("owner"), new ArrayList<>());
                boxes.get(res.getString("owner")).add(res.getString("box_uid"));
            }
            for (Map.Entry<String, List<String>> e : boxes.entrySet()) {
                List<String> checked = new ArrayList<>();
                final String sqlUser = "SELECT * FROM user_boxes WHERE uid=\'" + e.getKey() + "\'";
                res = stm.executeQuery(sqlUser);
                if (!res.next())
                    toBeRemove.addAll(e.getValue());
                else
                    for (int i = BOXES_START; i <= BOXES_END; i++) {
                        for (String f : e.getValue())
                            if (f.equals(res.getString(i)))
                                checked.add(f);
                    }
                for (String f : e.getValue()) {
                    boolean check = false;
                    for (String g : checked)
                        if (f.equals(g)) {
                            check = true;
                            break;
                        }
                    if (!check)
                        toBeRemove.add(f);
                }
            }
            for (String e : toBeRemove) {
                String sqlDel = "DELETE FROM boxes WHERE box_uid=\'" + e + "\'";
                stm.executeUpdate(sqlDel);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Connection conn;
    private static Map<String, String> tokensBuf = new HashMap<>();

    /**
     *  prepare the connection to sql
     */
    public static void init() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://39.108.228.130:3306/mbox_database?" + "user=mbox_server&password=password");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param username
     * @return if this username is usable
     */
    public static boolean checkUsernameValid(String username) {
        Statement stm;
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        String sql = "SELECT uid, password, user_name From user_info Where user_name=\'" + username + "\'";
        try {
            ResultSet res = stm.executeQuery(sql);
            while (res.next())
                return false;
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @return generate a usable uid
     */
    public static String genUID() {
        try {
            while (conn.isClosed())
                init();
            Statement stm = conn.createStatement();
            while (true) {
                String ans = UUID.randomUUID().toString().replaceAll("[\\{\\}\\-]", "").toLowerCase();
                String sql = "SELECT uid FROM user_info WHERE uid=\'" + ans + "\'";
                ResultSet res = stm.executeQuery(sql);
                if (!res.next())
                    return ans;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     *
     * @param uid
     * @param token
     * @return 0=success -1=invalid_token -2=server_error 1=token_updated
     */
    public static int verifyToken(String uid, String token) {
        if (tokensBuf.containsKey(uid))
            if (tokensBuf.get(uid).equals(token))
                return 0;
        Statement stm;
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            final String sql = "SELECT token,old_token FROM tokens WHERE uid=\'" + uid + "\'";
            ResultSet results = stm.executeQuery(sql);
            while (results.next())
                if (results.getString("token").equals(token)) {
                    final String updateTime = "UPDATE tokens SET last_check=" + System.currentTimeMillis() / 1000 + " WHERE uid=\'" + uid + "\'";
                    stm.executeUpdate(updateTime);
                    return 0;
                } else if (results.getString("old_token").equals(token))
                    return 1;
                else
                    return -1;
            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -2;
        }
    }

    /**generate a new token
     * @return new token
     */
    private static String genToken() {
        return UUID.randomUUID().toString().replaceAll("[\\{\\}\\-]", "").toLowerCase();
    }

    /**
     * generate a RSA key-pair
     * @return a KeyPair
     */
    static KeyPair RSAGenKeyPair() {
        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * @param privateKey
     * @param secretStr
     * @return
     */
    private static String RSADecrypt(Key privateKey, String secretStr) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] b = cipher.doFinal(Base64.getDecoder().decode(secretStr));
            return new String(b);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String RSAEncrypt(Key publicKey, String publicStr) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            System.err.println("ori data: ");
            for (Byte e : publicStr.getBytes()) {
                System.err.printf("%x ", e.byteValue());
            }
            System.err.println();
            System.err.println(new BigInteger(publicStr.getBytes()));
            byte[] b = cipher.doFinal(publicStr.getBytes());
            System.err.println("encrypt data: ");
            for (Byte e : b) {
                System.err.printf("%x ", e.byteValue());
            }
            System.err.println();
            System.err.println(new BigInteger(b));
            return Base64.getEncoder().encodeToString(b);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static int setToken(String uid, String token) {
        PreparedStatement stm;
        final String sqlDel = "DELETE FROM tokens WHERE uid=\'" + uid + "\'";
        final String sqlCreate = "INSERT INTO tokens (token,uid,validity_time,old_token,last_check) VALUES (?,?,?,?,?)";
        try {
            while (conn.isClosed())
                init();
            stm = conn.prepareStatement(sqlDel);
            stm.executeUpdate();
            stm = conn.prepareStatement(sqlCreate);
            stm.setString(1, token);
            stm.setString(2, uid);
            stm.setLong(3, System.currentTimeMillis() / 1000 + 60 * 15);
            stm.setString(4, token);
            stm.setLong(5, System.currentTimeMillis() / 1000);
            stm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String getUserToken(String uid) {
        Statement stm;
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            final String sql = "SELECT token FROM tokens WHERE uid=\'" + uid + "\'";
            ResultSet res = stm.executeQuery(sql);
            while (res.next())
                return res.getString("token");
            return "no_record_please_login";
        } catch (SQLException e) {
            e.printStackTrace();
            return "server_error";
        }
    }

    public static String getUserUID(String username) {
        Statement stm;
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            final String sql = "SELECT uid FROM user_info WHERE user_name=\'" + username + "\'";
            ResultSet res = stm.executeQuery(sql);
            while (res.next())
                return res.getString("uid");
            return "-1";
        } catch (SQLException e) {
            e.printStackTrace();
            return "-2";
        }
    }

    public static String getUsername(String uid) {
        Statement stm;
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            final String sql = "SELECT user_name FROM user_info WHERE uid=\'" + uid + "\'";
            ResultSet res = stm.executeQuery(sql);
            while (res.next())
                return res.getString("user_name");
            return "-1";
        } catch (SQLException e) {
            e.printStackTrace();
            return "-2";
        }
    }

    public static String getNickname(String uid) {
        Statement stm;
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            final String sql = "SELECT nick_name FROM user_info WHERE uid=\'" + uid + "\'";
            ResultSet res = stm.executeQuery(sql);
            while (res.next())
                return res.getString("nick_name");
            return "-1";
        } catch (SQLException e) {
            e.printStackTrace();
            return "-2";
        }
    }

    public static String getMedicineName(String boxUID) {
        Statement stm;
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            final String sql = "SELECT medicine_name FROM boxes WHERE box_uid=\'" + boxUID + "\'";
            ResultSet res = stm.executeQuery(sql);
            if (res.next())
                return res.getString("medicine_name");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean addIntakeRecord(String uid, String boxUID, int num, String time) {
        Statement stm;
        String medicineName = getMedicineName(boxUID);
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            String sql = String.format("SELECT pill_number FROM boxes WHERE box_uid=\'%s\'", boxUID);
            ResultSet res = stm.executeQuery(sql);
            if (!res.next())
                return false;
            int pillNumber = res.getInt("pill_number") - num;
            final long timeInt = Long.parseLong(time);
            sql = String.format("INSERT INTO intake_record (uid, time, medicine_name, box_uid, pill_num) VALUES (\'%s\', %d, \'%s\', \'%s\', %d)", uid, timeInt, medicineName, boxUID, num);
            stm.executeUpdate(sql);
            sql = String.format("UPDATE boxes SET pill_number=%d WHERE box_uid=\'%s\'", pillNumber, boxUID);
            stm.executeUpdate(sql);
            return true;
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getIntakeRecord(String uid, String boxUID, long startTime, long endTime, String medicineName) {
        Statement stm;
        String sql = "SELECT uid,time,medicine_name,pill_num FROM intake_record WHERE uid=\'" + uid + "\' and time>=" + startTime + " and time<=" + endTime;
        if (boxUID != null)
            sql = sql + " and box_uid=\'" + boxUID + "\'";
        StringBuilder ansBuilder = new StringBuilder();
        if (medicineName != null)
            sql = sql + " and medicine_name=\'" + medicineName + "\'";
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            ResultSet res = stm.executeQuery(sql);
            ansBuilder.append('[');
            boolean writen = false;
            while (res.next()) {
                writen = true;
                ansBuilder.append('{');
                ansBuilder.append("**time**=");
                ansBuilder.append(res.getLong("time"));
                ansBuilder.append("$$");
                ansBuilder.append("**medicine_name**=");
                ansBuilder.append(res.getString("medicine_name"));
                ansBuilder.append("$$");
                ansBuilder.append("**uid**=");
                ansBuilder.append(res.getString("uid"));
                ansBuilder.append("$$");
                ansBuilder.append("**pill_num**=");
                ansBuilder.append(res.getInt("pill_num"));
                ansBuilder.append("},");
            }
            if (writen)
                ansBuilder.deleteCharAt(ansBuilder.length() - 1);
            ansBuilder.append(']');
            return ansBuilder.toString();
        } catch (SQLException e) {
            e.printStackTrace();
            return "server_error";
        }
    }

    public static String getIntakeRecord(String uid, String boxUID, long startTime, long endTime) {
        return getIntakeRecord(uid, boxUID, startTime, endTime, null);
    }

    public static String getIntakeRecord(String uid, long startTime, long endTime) {
        return getIntakeRecord(uid, null, startTime, endTime);
    }

    public static String register(String username, String nickname, PrivateKey privateKey, String secretPassword) {
        if (!checkUsernameValid(username))
            return String.valueOf(-1);
        String uid = genUID();
        PreparedStatement stm;
        final String sql = "INSERT INTO user_info (uid, user_name, password, nick_name) VALUES(?,?,?,?)";
        try {
            while (conn.isClosed())
                init();
            stm = conn.prepareStatement(sql);
            stm.setString(1, uid);
            stm.setString(2, username);
            stm.setString(3, RSADecrypt(privateKey, secretPassword));
            stm.setString(4, nickname);
            stm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return String.valueOf(-2);
        }
        return uid;
    }

    public static Map<String, String> login(String username, PrivateKey privateKey, String secretPassword) {
        Map<String, String> result = new HashMap<>();
        Statement stm;
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
            result.put("statue", "sql_error");
            return result;
        }
        String sql = "SELECT uid, password, user_name From user_info Where user_name=\'" + username + "\'";
        try {
            ResultSet res = stm.executeQuery(sql);
            while (res.next())
                if (res.getString("user_name").equals(username)
                        && RSADecrypt(privateKey, secretPassword).equals(res.getString("password"))) {
                    result.put("statue", "success");
                    result.put("uid", res.getString("uid"));
                    result.put("token", genToken());
                }
        } catch (SQLException e) {
            e.printStackTrace();
            result.put("statue", "sql_error");
        }
        if (!result.containsKey("statue"))
            result.put("statue", "wrong_password_or_account");
        return result;
    }

    public static boolean addFamilies(String uid, String familyUID) {
        final String sqla = String.format("INSERT INTO families (uid, families_uid) VALUES (\'%s\',\'%s\')", uid, familyUID);
        final String sqlb = String.format("INSERT INTO families (uid, families_uid) VALUES (\'%s\',\'%s\')", familyUID, uid);
        Statement stm;
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            stm.execute(sqla);
            stm.execute(sqlb);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getFamilies(String uid) {
        StringBuilder ansBuilder = new StringBuilder();
        ansBuilder.append("[");
        Statement stm;
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            final String sql = "SELECT families_uid FROM families WHERE uid=\'" + uid + "\'";
            ResultSet res = stm.executeQuery(sql);
            while (res.next()) {
                ansBuilder.append('{');
                ansBuilder.append("**uid**=");
                String uidd = res.getString("families_uid");
                ansBuilder.append(uidd);
                ansBuilder.append("$$");
                ansBuilder.append("**user_name**=");
                ansBuilder.append(LoginUtils.getUsername(uidd));
                ansBuilder.append("$$");
                ansBuilder.append("**nick_name**=");
                ansBuilder.append(LoginUtils.getNickname(uidd));
                ansBuilder.append("}");
                ansBuilder.append(",");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "server_error";
        }
        ansBuilder.append("]");
        return ansBuilder.toString();
    }

    public static List<String> getUserBoxesUID(String uid) {
        List<String> ans = new ArrayList<>();
        Statement stm;
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            final String sql = "SELECT * FROM user_boxes WHERE uid=\'" + uid + "\'";
            ResultSet res = stm.executeQuery(sql);
            while (res.next()) {
                for (int i = BOXES_START; i <= BOXES_END; i++) {
                    if (res.getString(i).length() == 32)
                        ans.add(res.getString(i));
                }
                return ans;
            }
            return ans;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static boolean setUserBoxes(String uid, List<String> boxes) {
        Statement stm;
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            String sql = "SELECT * FROM user_boxes WHERE uid=\'" + uid + "\'";
            ResultSet res = stm.executeQuery(sql);
            boolean created = res.next();
            if (!created) {
                sql = "INSERT INTO user_boxes (uid) VALUES (\'" + uid + "\')";
                stm.executeUpdate(sql);
                sql = "SELECT * FROM user_boxes WHERE uid=\'" + uid + "\'";
                res = stm.executeQuery(sql);
            }

            res.first();
            out:
            for (String e : boxes) {
                boolean alreadyHave = false;
                for (int i = BOXES_START; i < BOXES_END; i++)
                    if (e.equals(res.getString(i))) {
                        alreadyHave = true;
                        break;
                    }
                if (!alreadyHave) {
                    int position = -1;
                    for (int i = BOXES_START; i < BOXES_END; i++)
                        if (res.getString(i).length() != 32) {
                            position = i;
                            break;
                        }
                    if (position > BOXES_END || position < BOXES_START)
                        //shit happened
                        return false;
                    sql = String.format("UPDATE user_boxes SET box%d=\'%s\' WHERE uid=\'%s\'", position - 1, e, uid);
                    stm.executeUpdate(sql);
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean removeUserBoxes(String uid, List<String> boxes) {
        Statement stm;
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            for (String e : boxes) {
                String sql = "SELECT * FROM user_boxes WHERE uid=\'" + uid + "\'";
                ResultSet res = stm.executeQuery(sql);
                if (res.next()) {
                    for (int i = BOXES_START; i <= BOXES_END; i++) {
                        String boxUID = res.getString(i);
                        if (boxUID.equals(e)) {
                            sql = String.format("UPDATE user_boxes SET box%d=\'%s\' WHERE uid=\'%s\'", i - 1, "", uid);
                            stm.executeUpdate(sql);
                            sql = String.format("DELETE FROM boxes WHERE box_uid=\'%s\'", boxUID);
                            stm.executeUpdate(sql);
                            break;
                        }
                    }
                } else
                    return false;
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String createBoxes(String boxUID, String medicineName, String owner, int pillNumber) {
        Statement stm;
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            final String uid = boxUID;
            if (owner == null)
                owner = "-1";
            if (owner.length() != 32)
                owner = "-1";
            final String sql = String.format("INSERT boxes (box_uid,medicine_name,pill_number,owner) VALUES (\'%s\',\'%s\',%d,\'%s\')", uid, medicineName, pillNumber, owner);
            stm.executeUpdate(sql);
            if (owner.length() == 32) {
                List<String> thisBox = new ArrayList<>();
                thisBox.add(uid);
                setUserBoxes(owner, thisBox);
            }
            return uid;
        } catch (SQLIntegrityConstraintViolationException e) {
            return "boxUID_has_been_used";
        } catch (SQLException e) {
            e.printStackTrace();
            return "server_error";
        }
    }

    public static String createBoxes(String boxUID, String medicineName, String owner) {
        return createBoxes(boxUID, medicineName, owner, -1);
    }

    public static String createBoxes(String boxUID, String medicineName) {
        return createBoxes(boxUID, medicineName, "-1");
    }

    public static String getBoxes(String uid) {
        Statement stm;
        try {
            while (conn.isClosed())
                init();
            stm = conn.createStatement();
            StringBuilder ansBuilder = new StringBuilder();
            List<String> boxesUID = getUserBoxesUID(uid);
            List<Map<String, String>> boxes = new ArrayList<>();
            ansBuilder.append("[");
            boolean writed = false;
            for (String buid : boxesUID) {
                writed = true;
                StringBuilder pieceBuilder = new StringBuilder();
                String sql = "SELECT * FROM boxes WHERE box_uid=\'" + buid + "\'";
                ResultSet res = stm.executeQuery(sql);
                if (!res.next())
                    continue;
                pieceBuilder.append("{");
                pieceBuilder.append("**medicineName**=");
                pieceBuilder.append(res.getString("medicine_name"));
                pieceBuilder.append("$$");
                pieceBuilder.append("**pillNumber**=");
                pieceBuilder.append(res.getString("pill_number"));
                pieceBuilder.append("$$");
                pieceBuilder.append("**boxUID**=");
                pieceBuilder.append(res.getString("box_uid"));
                pieceBuilder.append("}");
                ansBuilder.append(pieceBuilder.toString());
                ansBuilder.append(",");
            }
            if (writed)
                ansBuilder.deleteCharAt(ansBuilder.length() - 1);
            ansBuilder.append("]");
            return ansBuilder.toString();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "[]";
    }

    public static void main(String[] args) {
        KeyPair keys = RSAGenKeyPair();
        String secret = RSAEncrypt(keys.getPublic(), "mch20010109..").replaceAll("[\\r\\n]", "");
        String pub = RSADecrypt(keys.getPrivate(), secret);
        //secret.replaceAll()
        System.out.println(secret);
        System.out.println(pub);

    }
}
