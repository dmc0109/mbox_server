import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: Changhai Man
 * @Date: 2019-07-20 14:36
 */
public class Session {
    private static Map<String, Session> sessionsPool = new HashMap<>();
    static{
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Map<String, Session> newSessionsPool = new HashMap<>();
                List<Map.Entry<String, Session>> newSessionsList = sessionsPool.entrySet().stream().filter(e ->
                        System.currentTimeMillis()-e.getValue().startTime<=1000*60*10
                ).collect(Collectors.toList());
                for(Map.Entry<String, Session> e:newSessionsList)
                    newSessionsPool.put(e.getKey(), e.getValue());
                sessionsPool = newSessionsPool;
            }
        }, 0, 1*60*1000);
    }

    public Map<String, Object> datas = new HashMap<>();
    public String sessionID = UUID.randomUUID().toString().replaceAll("[\\{\\}\\-]", "");
    public long startTime = System.currentTimeMillis();

    public static Session newSession(){
        Session instance = new Session();
        sessionsPool.put(instance.sessionID, instance);
        return instance;
    }

    public static Session getSession(String sessionID){
        return sessionsPool.get(sessionID);
    }

    public static void removeSession(String sessionID){
        sessionsPool.remove(sessionID);
    }

    public static void removeSession(Session session){
        removeSession(session.sessionID);
    }

    public Object get(String key){
        return this.datas.get(key);
    }

    public void put(String key, Object value){
        this.datas.put(key, value);
    }

    public void remove(String key){
        this.datas.remove(key);
    }

    @Override
    public int hashCode(){
        return sessionID.hashCode();
    }

}
