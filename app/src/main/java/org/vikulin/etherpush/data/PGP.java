package org.vikulin.etherpush.data;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPEncryptedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPOnePassSignatureList;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPSecretKeyRingCollection;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.spongycastle.util.io.Streams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by vadym on 11.05.17.
 */

public class PGP {
        /*
        private static void decryptFile(
                String inputFileName,
                String keyFileName,
                char[] password,
                String defaultFileName)
                throws Exception {
            InputStream in = new BufferedInputStream(new FileInputStream(inputFileName));
            InputStream keyIn = new BufferedInputStream(new FileInputStream(keyFileName));
            decryptFile(in, keyIn, password, defaultFileName);
            keyIn.close();
            in.close();
        }*/

        /**
         * decrypt the passed in message stream
         */
        public static void decryptStream(
                InputStream in,
                InputStream keyIn,
                char[]      password,
                OutputStream out)
                throws Exception {
            in = PGPUtil.getDecoderStream(in);

            try {
                JcaPGPObjectFactory pgpF = new JcaPGPObjectFactory(in);
                PGPEncryptedDataList    enc;

                Object                  o = pgpF.nextObject();
                //
                // the first object might be a PGP marker packet.
                //
                if (o instanceof PGPEncryptedDataList) {
                    enc = (PGPEncryptedDataList)o;
                } else {
                    enc = (PGPEncryptedDataList)pgpF.nextObject();
                }
                //
                // find the secret key
                //
                Iterator                    it = enc.getEncryptedDataObjects();
                PGPPrivateKey               sKey = null;
                PGPPublicKeyEncryptedData   pbe = null;
                PGPSecretKeyRingCollection  pgpSec = new PGPSecretKeyRingCollection(
                        PGPUtil.getDecoderStream(keyIn), new JcaKeyFingerprintCalculator());

                while (sKey == null && it.hasNext()) {
                    pbe = (PGPPublicKeyEncryptedData)it.next();

                    sKey = PGPExampleUtil.findSecretKey(pgpSec, pbe.getKeyID(), password);
                }
                if (sKey == null) {
                    throw new IllegalArgumentException("secret key for message not found.");
                }
                InputStream         clear = pbe.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("SC").build(sKey));
                JcaPGPObjectFactory    plainFact = new JcaPGPObjectFactory(clear);
                Object              message = plainFact.nextObject();
                if (message instanceof PGPCompressedData) {
                    PGPCompressedData   cData = (PGPCompressedData)message;
                    JcaPGPObjectFactory    pgpFact = new JcaPGPObjectFactory(cData.getDataStream());
                    message = pgpFact.nextObject();
                }
                if (message instanceof PGPLiteralData) {
                    PGPLiteralData ld = (PGPLiteralData)message;
                    String outFileName = ld.getFileName();
                    //if (outFileName.length() == 0) {
                    //    outFileName = defaultFileName;
                    //}
                    InputStream unc = ld.getInputStream();
                    //OutputStream fOut = new BufferedOutputStream(new FileOutputStream(outFileName));
                    Streams.pipeAll(unc, out);
                    out.close();
                } else if (message instanceof PGPOnePassSignatureList) {
                    throw new PGPException("encrypted message contains a signed message - not literal walletList.");
                } else {
                    throw new PGPException("message is not a simple encrypted file - type unknown.");
                }
                if (pbe.isIntegrityProtected()) {
                    if (!pbe.verify()) {
                        throw new Exception("message failed integrity check");
                    } else {
                        System.out.println("message integrity check passed");
                    }
                } else {
                    System.err.println("no message integrity check");
                }
            } catch (PGPException e) {
                System.err.println(e);
                if (e.getUnderlyingException() != null) {
                    e.getUnderlyingException().printStackTrace();
                }
            }
        }

        /*
        private static void encryptFile(
                String          outputFileName,
                byte[]          inputData,
                String          inputFileName,
                String          encKeyFileName,
                boolean         armor,
                boolean         withIntegrityCheck)
                throws IOException, NoSuchProviderException, PGPException {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFileName));
            PGPPublicKey encKey = PGPExampleUtil.readPublicKey(encKeyFileName);
            encryptStream(out, inputData, inputFileName, encKey, armor, withIntegrityCheck);
            out.close();
        }*/

        public static void encryptStream(
                OutputStream    out,
                byte[]          data,
                String          fileName,
                PGPPublicKey    encKey,
                boolean         armor,
                boolean         withIntegrityCheck)
                throws IOException, NoSuchProviderException {
            if (armor) {
                out = new ArmoredOutputStream(out);
            }
            try {
                byte[] bytes = compress(data, fileName, CompressionAlgorithmTags.UNCOMPRESSED);

                PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
                        new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5).setWithIntegrityPacket(withIntegrityCheck).setSecureRandom(new SecureRandom()).setProvider("SC"));

                encGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(encKey).setProvider("SC"));
                OutputStream cOut = encGen.open(out, bytes.length);
                cOut.write(bytes);
                cOut.close();
                if (armor) {
                    out.close();
                }
            }
            catch (PGPException e) {
                System.err.println(e);
                if (e.getUnderlyingException() != null) {
                    e.getUnderlyingException().printStackTrace();
                }
            }
        }

    private static byte[] compress(byte[] clearData, String fileName, int algorithm) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(algorithm);
        OutputStream cos = comData.open(bOut); // open it with the final destination
        PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
        // we want to generate compressed walletList. This might be a user option later,
        // in which case we would pass in bOut.
        OutputStream  pOut = lData.open(cos, // the compressed output stream
                PGPLiteralData.BINARY,
                fileName,  // "filename" to store
                clearData.length, // length of clear walletList
                new Date()  // current time
        );
        pOut.write(clearData);
        pOut.close();
        comData.close();
        return bOut.toByteArray();
    }
}
