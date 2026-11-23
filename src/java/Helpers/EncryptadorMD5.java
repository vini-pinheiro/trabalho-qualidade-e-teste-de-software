/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Helpers;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kener_000
 */
public class EncryptadorMD5 {

    // Entrega 2 (Sonar): logger no lugar de System.out
    private static final Logger LOGGER = Logger.getLogger(EncryptadorMD5.class.getName());

    public String encryptar(String senha){
        String plaintext = senha;
        try {
            
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(plaintext.getBytes());
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1,digest);
            // Entrega 2 (Sonar): StringBuilder no lugar de concatenar String dentro do laço
            StringBuilder hashtext = new StringBuilder(bigInt.toString(16));
            // Now we need to zero pad it if you actually want the full 32 chars.
            while(hashtext.length() < 32 ){
              hashtext.insert(0, '0');
            }
            return hashtext.toString();

        } catch(Exception e) {
            LOGGER.log(Level.WARNING, "Falha ao gerar o hash MD5", e);
        }
        return null;    
    } 


    
}
