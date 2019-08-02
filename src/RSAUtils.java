import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * @Author: Changhai Man
 * @Date: 2019-07-20 18:34
 */
public class RSAUtils {
    private static String RSAEncrypt(Key publicKey, String publicStr) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] b = cipher.doFinal(publicStr.getBytes());
            return new BASE64Encoder().encode(b);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static String RSAEncrypt(String publicKeyStr, String publicStr){
        X509EncodedKeySpec pubKeySpec;
        try {
            pubKeySpec = new X509EncodedKeySpec(new BASE64Decoder().decodeBuffer(publicKeyStr));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);
            return RSAEncrypt(publicKey, publicStr);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static void main(String[] args){
        System.out.print(RSAEncrypt("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCH9bpKPqt3TSk5W0V3g+1tzlK4KarY3ehEN+WyUsAAhnIivumNfoMApFnQagk3jwOOaUmICYrT2ao7p2OBVtRardrZ3pVUw6xIMsVhpU9Zjnm3pachJTsLCwh/ue5bgaDwA/Hmn/xFxlligTXPvV20Ix0oPs8JpkCXpH7g7WuPVQIDAQAB",
                "password").replaceAll("[\\r\\n]", ""));
    }
}
