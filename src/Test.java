import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

/**
 * @Author: Changhai Man
 * @Date: 2019-07-20 12:58
 */
public class Test {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        long begin = System.currentTimeMillis();
        for(int i=0; i<1000000; i++){
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            KeyPair keypair = generator.generateKeyPair();
            if(i%100==0)
                System.out.println(i);
        }
        System.out.println(System.currentTimeMillis()-begin);
    }
}
