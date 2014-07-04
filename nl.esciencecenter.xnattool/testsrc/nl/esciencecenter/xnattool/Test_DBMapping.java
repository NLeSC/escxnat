package nl.esciencecenter.xnattool;

import java.io.UnsupportedEncodingException;

import nl.esciencecenter.ptk.crypt.CryptScheme;
import nl.esciencecenter.ptk.crypt.StringCrypter.EncryptionException;
import nl.esciencecenter.ptk.crypt.StringHasher;
import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.xnattool.CryptHashSettings;
import nl.esciencecenter.xnattool.DBMapping;

import org.junit.Assert;
import org.junit.Test;

public class Test_DBMapping
{
    // helpers

    public static CryptHashSettings createDBSettings(String sourceId, byte key[])
            throws UnsupportedEncodingException
    {
        CryptHashSettings settings = new CryptHashSettings(CryptScheme.DESEDE_ECB_PKCS5);

        settings.setHashing(StringHasher.SHA_256, 8);
        settings.setCredentials(sourceId, key);

        return settings;
    }

    @Test
    public void testDBMappingDemo() throws Exception
    {
        String keyPassphraseSourceText = "12345";
        String SubjectID = "Rembrandt";

        byte dataSetKey[];
        StringHasher hasher = new StringHasher(StringHasher.SHA_256);
        // hash digest of key:
        dataSetKey = hasher.hash(keyPassphraseSourceText, true, "", true);

        CryptHashSettings settings = createDBSettings("", dataSetKey);

        DBMapping dbMapping = new DBMapping(settings);

        // echo -n Rembrandt | openssl enc -des-ede3 -nosalt -pass pass:12345
        // -base64 -md sha256 -p
        byte crypt[] = testCrypt(dbMapping, SubjectID, "F5l/tnlsopme+xteX7Aw5A==");
        String cryptStr = StringUtil.toHexString(crypt);
        printf("Subject Crypted:%s\n", cryptStr);
        String expectedCryptStr = "17997FB6796CA2999EFB1B5E5FB030E4";

        // XNAT Subject:
        String subjectLabel = dbMapping.getCreateXnatSubjectLabel(SubjectID, true, true);
        printf("Subject Label:%s\n", subjectLabel);
        String expectedSubjLabel = "sub_FB06EFCC5C6F7841";
        Assert.assertEquals("encrypted ID does not match expected", expectedCryptStr, cryptStr);
        Assert.assertEquals("encrypted Subject Label does not match expected", expectedSubjLabel, subjectLabel);

    }

    @Test
    public void testDBMapping_set1() throws Exception
    {
        String sourceText = "12345";

        byte dataSetKey[];
        StringHasher hasher = new StringHasher(StringHasher.SHA_256);
        // hash digest of key:
        dataSetKey = hasher.hash(sourceText, true, "", true);
        CryptHashSettings settings = createDBSettings("", dataSetKey);

        DBMapping dbMapping = new DBMapping(settings);

        // echo -n 12345 | openssl enc -des-ede3 -nosalt -pass pass:12345
        // -base64 -md sha256 -p
        testCrypt(dbMapping, "12345", "xC5gLUJ1UxI=");

        // echo -n 0123456789012345678901234 | openssl enc -des-ede3 -nosalt
        // -pass pass:12345 -base64 -md sha256 -p
        testCrypt(dbMapping, "0123456789012345678901234", "3FpToewkL3fDIeGFWHCj9olKRKuErWn33oCk2oQdQdQ=");

        String subjKey = "subject1";
        String studyKey = "session1";
        String seriesKey = "scan1";

        // crypto-hash subject/session/scan
        testSubjectMapping(dbMapping, subjKey, "sub_8EE425AFFE9DF88C", true);
        testSessionMapping(dbMapping, subjKey, true, studyKey, true, "sub_8EE425AFFE9DF88C_ses_1FA9D8E4D6928905");
        testScanMapping(dbMapping, studyKey, seriesKey, "scn_", true, "scn_12D0B28957E6054F");

        dbMapping.deleteSubjectLabel(subjKey);
        dbMapping.deleteSessionLabel(subjKey, studyKey);
        dbMapping.deleteScanLabel(studyKey, seriesKey);

        // do no crypto-hash
        testSubjectMapping(dbMapping, subjKey, "sub_subject1", false);
        testSessionMapping(dbMapping, subjKey, false, studyKey, false, "sub_subject1_ses_session1");
        testScanMapping(dbMapping, studyKey, seriesKey, "scn_", false, "scn_scan1");

        dbMapping.deleteSubjectLabel(subjKey);
        dbMapping.deleteSessionLabel(subjKey, studyKey);
        dbMapping.deleteScanLabel(studyKey, seriesKey);

        testScanMapping(dbMapping, studyKey, "set1a", "scn_", false, "scn_set1a");
        testScanMapping(dbMapping, studyKey, "set1b", "recon_", false, "recon_set1b");
        testScanMapping(dbMapping, studyKey, "set1c", "atlas_", false, "atlas_set1c");

    }

    // ===============
    // Helpers
    // ===============

    private void testScanMapping(DBMapping dbMapping, String studyKey, String seriesKey, String labelPrefix, boolean cryptHashScan,
            String expectedScanLabel) throws EncryptionException
    {
        String scanLabel = dbMapping.getCreateXnatScanLabel(studyKey, seriesKey, cryptHashScan, labelPrefix, true);
        printf("scanLabel    =%s\n", scanLabel);
        Assert.assertEquals("Scan Label Mismatch!", expectedScanLabel, scanLabel);
    }

    private void testSessionMapping(DBMapping dbMapping, String subjKey, boolean cryptHashSubject, String studyKey,
            boolean cryptHashSession, String expectedStudyLbl) throws EncryptionException
    {
        String sessionLabel = dbMapping.getCreateXnatSessionLabel(subjKey, cryptHashSubject, studyKey, cryptHashSession, true);
        printf("studyLabel   =%s\n", sessionLabel);
        Assert.assertEquals("Session Label Mismatch!", expectedStudyLbl, sessionLabel);
    }

    private void testSubjectMapping(DBMapping dbMapping, String subjectKey, String expectedLabel, boolean cryptHashSubject)
            throws EncryptionException
    {
        String subjLabel = dbMapping.getCreateXnatSubjectLabel(subjectKey, cryptHashSubject, true);
        printf("subjectLabel =%s\n", subjLabel);
        Assert.assertEquals("Subject Label Mismatch!", expectedLabel, subjLabel);
    }

    byte[] testCrypt(DBMapping dbMapping, String value, String expectedCryptBase64) throws Exception
    {
        byte crypt[] = dbMapping.encryptID(value);
        String base64 = StringUtil.base64Encode(crypt);

        Assert.assertEquals("Encrypted String doesn match expected!", expectedCryptBase64, base64);

        String decryptedValue = dbMapping.decryptBase64EncodedID(base64);
        Assert.assertEquals("Decrypted String doesn't match expected1", value, decryptedValue);
        return crypt;

    }

    private void printf(String format, Object... args)
    {
        System.out.printf(format, args);
    }

}
