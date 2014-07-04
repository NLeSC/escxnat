/*
 * Copyright 2012-2014 Netherlands eScience Center.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at the following location:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * For the full license, see: LICENSE.txt (located in the root folder of this distribution).
 * ---
 */
// source:

package nl.esciencecenter.medim.dicom;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import nl.esciencecenter.medim.dicom.types.DicomTags;
import nl.esciencecenter.medim.dicom.types.DicomTags.TagProcessingOption;
import nl.esciencecenter.medim.dicom.types.DicomTypes.VRType;
import nl.esciencecenter.ptk.crypt.StringCrypter;
import nl.esciencecenter.ptk.crypt.StringCrypter.EncryptionException;
import nl.esciencecenter.ptk.crypt.StringHasher;
import nl.esciencecenter.ptk.presentation.Presentation;
import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.ptk.util.logging.ClassLogger;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.VR;

/**
 * DicomProcessor class, processes Dicom Tags using a set of Tag Directives.
 * 
 * @author Piter T. de Boer
 */
public class DicomProcessor implements DicomObject.Visitor
{
    private static ClassLogger logger = null;

    static
    {
        logger = ClassLogger.getLogger(DicomProcessor.class);
    }

    // =======================================
    // Helper methods
    // =======================================

    /**
     * Create UID Like hash. Converts hash bytes to BigInteger and prefixes with the uidPrefix String. Size of the
     * created String not be bigger then 64.
     * 
     * @param uidPrefix
     *            - UID dotted decimal prefix. For example "1.2." or "99."
     * @param hashBytes
     *            - hash in bytes. If the hash is to long the hash will be truncated and the remainder will be exored
     *            with the truncated hash.
     * @see DicomProcessor#exorBytes(byte[], int)
     * @return dotted decimal compatible hash.
     */
    public static String createHashedUid(String uidPrefix, byte[] hashBytes, int maxHashLen)
    {
        // limited the hash to 24 bytes since each byte takes up to 2-3 digits
        // in the BigInteger!
        hashBytes = exorBytes(hashBytes, maxHashLen);
        String hashedUid = uidPrefix + StringUtil.toBigIntegerString(hashBytes, false, false);
        return hashedUid;
    }

    // truncate bytes sequence and exor the remainder with the beginning
    public static byte[] exorBytes(byte bytes[], int maxlen)
    {
        byte result[] = new byte[maxlen];
        // needed?
        for (int i = 0; i < maxlen; i++)
            result[i] = 0; // clear

        for (int i = 0; i < bytes.length; i++)
            result[i % maxlen] ^= bytes[i]; // exor and wrap around len
        return result;
    }

    // =======================================
    // Instance
    // =======================================

    private DicomTags tagOptions;

    private DicomWrapper dicom;

    private boolean started;

    private boolean finished;

    private StringCrypter crypter;

    private StringHasher hasher;

    private DicomProcessingProfile procOptions; // new ProcessingOptions();

    private Object processMutex = new Object();

    protected DicomProcessor()
    {
        this.dicom = null;
        // this.init();
    }

    public DicomProcessor(DicomTags tagOptions) throws Exception
    {
        this.dicom = null;
        this.procOptions = DicomProcessingProfile.createDefault();
        this.tagOptions = tagOptions;
        init();
    }

    public DicomProcessor(DicomTags tagOptions, DicomProcessingProfile procOpts) throws Exception
    {
        this.dicom = null;
        this.procOptions = procOpts;
        this.tagOptions = tagOptions;
        init();
    }

    public DicomObject getDicomObject()
    {
        return dicom.getDicomObject();
    }

    /**
     * Re-initializes DicomProcessor with new options.
     */
    public void updateProcessingOptions(DicomProcessingProfile options) throws Exception
    {
        if (this.hasStarted() == true)
            throw new Exception("Mutex Exception: Can't (re)initialize ProcessingOptions while processing!");

        this.procOptions = options;
        init();
    }

    protected void init() throws Exception
    {
        // Init can be called post Construction !
        synchronized (processMutex)
        {
            this.started = false;
            this.finished = false;
        }

        this.dicom = null;
        initHashAndCrypt();
    }

    private void initHashAndCrypt() throws EncryptionException, NoSuchAlgorithmException, UnsupportedEncodingException
    {
        if (procOptions == null)
        {
            throw new NullPointerException("No processing options. Please supply them.");
        }

        byte cryptKey[] = procOptions.getEncryptionKey();

        // if (cryptKey==null)
        // throw new
        // NullPointerException("EncryptKey from DicomProcessingProfile can not be null!");

        // Equivalent with:
        // openssl enc -des-ede3 -nosalt -pass pass:<pass> -base64 -p -md sha256
        if (cryptKey != null)
        {
            crypter = new StringCrypter(cryptKey, procOptions.cryptScheme, "SHA-256", StringCrypter.CHARSET_UTF8);
        }
        else
        {
            logger.warnPrintf("No encryption keys initialized!\n");
        }

        hasher = new StringHasher("SHA-256");
    }

    public byte[] getHashSalt()
    {
        return procOptions.hashSalt;
    }

    public DicomProcessingProfile getProcessingOptions()
    {
        return this.procOptions;
    }

    public StringCrypter getEncrypter()
    {
        return this.crypter;
    }

    public boolean hasStarted()
    {
        synchronized (processMutex)
        {
            return this.started;
        }
    }

    public boolean hasFinished()
    {
        synchronized (processMutex)
        {
            return this.finished;
        }
    }

    public void setTagOptions(DicomTags tagOptions)
    {
        synchronized (processMutex)
        {
            this.tagOptions = tagOptions;
        }
    }

    public DicomWrapper process(DicomWrapper dicomWrap) throws Exception
    {
        // PRE: critical section before processing:
        synchronized (processMutex)
        {
            if ((this.tagOptions == null) || (tagOptions.size() <= 0))
                throw new Exception("No Tag Options set. Please set first");

            if (this.crypter == null)
                throw new Exception("Encryption not yet initialized!");

            // if (dicom==null)
            // throw new Exception("NullPointer exception: No Dicom Object!");

            if (this.started == true)
                throw new IllegalStateException("Process currently running, can't reuse this Object");

            this.dicom = dicomWrap;

            this.started = true;
            this.finished = false;
        }

        boolean result = false;
        Throwable ex = null;

        try
        {
            result = dicom.acceptVisitor(this);
        }
        catch (Throwable t)
        {
            logger.logException(ClassLogger.ERROR, t, "Failed processing:%s\n", dicom);
            ex = t;
        }

        // POST: critical section after processing:
        synchronized (processMutex)
        {
            this.finished = true;
            this.started = false;

            if (ex != null)
                throw new Exception("Failed to process Dicom", ex);
        }

        return dicomWrap;
    }

    @Override
    public boolean visit(DicomElement dicomEl)
    {
        int tagNr = dicomEl.tag();

        TagProcessingOption opt = tagOptions.getOption(tagNr, TagProcessingOption.DELETE);
        logger.debugPrintf("> Option: %s on %s (%s/'%s')\n", opt, dicomEl, DicomUtil.getTagName(tagNr), tagOptions.getTagDescription(tagNr));

        try
        {
            switch (opt)
            {
                case KEEP:
                    // nothing.
                    break;
                case DELETE:
                {
                    doDeleteTag(dicomEl);
                    break;
                }
                case CLEAR:
                {
                    doClearTag(dicomEl);
                    break;
                }
                case HASH:
                {
                    hashTag(dicomEl);
                    break;
                }
                case HASH_UID:
                {
                    hashUIDTag(dicomEl);
                    break;
                }
                case ENCRYPT:
                {
                    doEncryptTag(dicomEl);
                    break;
                }
                case ENCRYPT_HASH:
                {
                    doHashEncryptTag(dicomEl);
                    break;
                }
                case ENCRYPT_HASH_UID:
                {
                    doEncryptAndHashUIDTag(dicomEl);
                    break;
                }

                case SET_DATE_TO_01JAN:
                {
                    setDateTo01Jan(dicomEl, true);
                    break;
                }
                case SET_DATE_TO_01JAN1900:
                {
                    setDateTo01jan1900(dicomEl, true);
                    break;
                }
                case SET_TIME_TO_0000HOURS:
                {
                    setTimeToZero(dicomEl, true);
                    break;
                }
                default:
                {
                    logger.errorPrintf("Unknown Tag Option:%s", opt);
                    break;
                }
            }
        }
        catch (Exception e)
        {
            logger.logException(ClassLogger.ERROR, this, e, "Exception:%s\n", e);
        }

        return true;
    }

    protected void doDeleteTag(DicomElement el) throws Exception
    {
        // ToBeChecked:does a delete effect the visit order or not?
        dicom.deleteTag(el.tag());
    }

    protected void doClearTag(DicomElement el) throws Exception
    {
        logger.debugPrintf("Clearing element:%s\n", el);

        VR vr = el.vr();
        VRType vrType = VRType.valueOf(vr);
        switch (vrType.getValueType())
        {
            case INTEGER:
            {
                dicom.setTag(el.tag(), 0);
                break;
            }
            case DOUBLE:
            {
                dicom.setTag(el.tag(), (double) 0);
                break;
            }
            case STRING:
            {
                dicom.setTag(el.tag(), "");
                break;
            }
            case BYTES:
            {
                dicom.setTag(el.tag(), new byte[]
                        {});
                break;
            }
            case TIME:
            {
                dicom.setTag(el.tag(), new byte[]
                        {});
                break;
            }
            case DATETIME:
            {
                dicom.setTag(el.tag(), new byte[]
                        {});
                break;
            }
            case UID:
            {
                // This might break the standards.
                logger.warnPrintf("Clearing UID value of element:%s\n", el);
                dicom.setTag(el.tag(), "");
                break;
            }
            case UNKNOWN:
            {
                dicom.setTag(el.tag(), new byte[]
                        {});
                break;
            }
        }
    }

    protected void setDateTo01jan1900(DicomElement el, boolean skipIfNotSet) throws Exception
    {
        int tagNr = el.tag();
        Date date = DicomWrapper.element2Date(el);

        if (date == null)
        {
            if (skipIfNotSet)
                return;

            date = Presentation.now();

            // already anonimized: field is null!
            logger.warnPrintf("setDateTo01jan1900(): Date is already NULL (element=%s)\n", el.toString());
        }

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        int year = 1900;
        int month = Calendar.JANUARY; // cal.get(Calendar.Calendar.MONTH);
        int day = 1; // cal.get(Calendar.DAY_OF_MONTH);
        cal.set(year, month, day);
        date = cal.getTime();
        dicom.setDateValue(tagNr, date);
    }

    protected void setDateTo01jan1950(DicomElement el, boolean skipIfNotSet) throws Exception
    {
        int tagNr = el.tag();
        Date date = DicomWrapper.element2Date(el);

        if (date == null)
        {
            if (skipIfNotSet)
                return;

            date = Presentation.now();

            // already anonimized: field is null!
            logger.warnPrintf("setDateTo01jan1950():Date is already NULL (element=%s)\n", el.toString());
            return;
        }
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        int year = 1950;
        int month = Calendar.JANUARY; // cal.get(Calendar.Calendar.MONTH);
        int day = 1; // cal.get(Calendar.DAY_OF_MONTH);
        cal.set(year, month, day);
        date = cal.getTime();
        dicom.setDateValue(tagNr, date);
    }

    protected void setTimeToZero(DicomElement el, boolean skipIfNotSet) throws Exception
    {
        int tagNr = el.tag();
        Date date = DicomWrapper.element2Date(el);

        if (date == null)
        {
            if (skipIfNotSet)
                return;

            date = Presentation.now();

            // already anonimized: field is null!
            logger.warnPrintf("setTimeToZero():Date is already NULL (zero bytes:element='%s')\n", el.toString());
            return;
        }

        GregorianCalendar cal = new GregorianCalendar();
        // cal.setTime(date);// clean date; s
        int hours = 0;
        int min = 0;
        int secs = 0;
        // int frac=0;

        int year = 1900;
        int month = Calendar.JANUARY; // cal.get(Calendar.Calendar.MONTH);
        int day = 1; // cal.get(Calendar.DAY_OF_MONTH);
        cal.set(year, month, day, hours, min, secs);
        date = cal.getTime();
        dicom.setDateValue(tagNr, date);
    }

    protected void setDateTo01Jan(DicomElement el, boolean skipIfNotSet) throws Exception
    {
        int tagNr = el.tag();
        Date date = DicomWrapper.element2Date(el);

        if (date == null)
        {
            if (skipIfNotSet)
                return;

            date = Presentation.now();

            // already anonimized: field is null!
            logger.warnPrintf("setDateTo01Jan():Date is already NULL (element=%s)\n", el.toString());
        }

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = Calendar.JANUARY;// Note January is Month 0!
        // month=cal.get(Calendar.Calendar.MONTH);
        int day = 1; // cal.get(Calendar.DAY_OF_MONTH);
        cal.set(year, month, day);
        date = cal.getTime();
        dicom.setDateValue(tagNr, date);
    }

    protected void doEncryptTag(DicomElement el) throws Exception
    {
        int tagNr = el.tag();
        VRType vrType = VRType.valueOf(el.vr());

        if (vrType.isString())
        {
            String newValue = null;
            String strValue = DicomWrapper.element2String(el, null);
            newValue = crypter.encryptToBase64(strValue);
            dicom.setTag(tagNr, newValue);
            logger.debugPrintf("> CRYPT RESULT:%s\n", dicom.getElement(tagNr));
        }
        // else if (vrType.isBinary()) {} // could encrypt bytes here.
        else
        {
            // number values could be encrypted and stored as encrypted
            // numerical value;
            throw new Exception("Cannot encrypt non String tags!:" + el);
        }
    }

    /**
     * Hashes String value to base64 encoded hash
     */
    protected void hashTag(DicomElement el) throws Exception
    {
        int tagNr = el.tag();
        VRType vrType = VRType.valueOf(el.vr());

        if (vrType.isString())
        {
            String strVal = DicomWrapper.element2String(el, null);

            String hashStr = null;

            if (strVal == null)
            {
                logger.warnPrintf("hashTag(): Tag Value is NULL:%s\n", DicomUtil.getTagName(el.tag()));
                hashStr = null;// Element has been cleared already, keep null
                               // value.
            }
            else
            {
                // Note: Using base64 encoding increases String size by 33% (6
                // i.s.o 8 bits/byte)
                // 256 bits hash is 32 bytes -> +/48 bytes.
                hashStr = hashToBase64(strVal);
            }

            dicom.setTag(tagNr, hashStr);
            logger.debugPrintf("> HASH RESULT:%s\n", dicom.getElement(tagNr));
        }
        else
            throw new Exception("Cannot hash non String tags (yet) !:" + el);

    }

    /**
     * Hash the UID and convert the hash bytes back to an UID like (dotted decimal) String.
     */
    protected void hashUIDTag(DicomElement el) throws Exception
    {
        int tagNr = el.tag();
        VRType vrType = VRType.valueOf(el.vr());

        if (vrType.isString())
        {
            String uid = DicomWrapper.element2String(el, null);
            // 32 bytes hash (SHA-256 = 256 bits)
            byte hashBytes[] = hash(uid);
            String hashedUid = hashToUidString(hashBytes);

            dicom.setTag(tagNr, hashedUid);
            logger.debugPrintf("> HASH UID RESULT:%s\n", dicom.getElement(tagNr));
        }
        else
            throw new Exception("Cannot hash non String tags (yet) !:" + el);
    }

    protected void doHashEncryptTag(DicomElement el) throws Exception
    {
        int tagNr = el.tag();
        VRType vrType = VRType.valueOf(el.vr());

        if (vrType.isString())
        {
            byte cryptBytes[] = null;
            String strValue = DicomWrapper.element2String(el, null);
            if (strValue == null)
            {
                logger.warnPrintf("> CRYPT&HASH Stage (I) : Ignoring NULL Value for:%s\n", el);
            }
            else
            {
                // first encrypt: increases byte size!
                cryptBytes = crypter.encrypt(strValue);
                logger.debugPrintf("> CRYPT&HASH Stage (I) :%s\n", StringUtil.toHexString(cryptBytes));
                // hash encrypted result and store as base64 string.

                byte cryptHashBytes[] = hasher.hash(cryptBytes, true, this.getHashSalt(), procOptions.prefixHashSalt);
                String cryptHashStr = StringUtil.base64Encode(cryptHashBytes);

                dicom.setTag(tagNr, cryptHashStr);
                logger.debugPrintf("> CRYPT&HASH Stage (IIa):%s\n", StringUtil.toHexString(cryptBytes));
                logger.debugPrintf("> CRYPT&HASH Stage (IIb):%s\n", dicom.getElement(tagNr));
            }
        }
        // else if (vrType.isBinary()) {} // could encrypt bytes here.
        else
        {
            // number values could be encrypted and stored as encrypted
            // numerical value;
            throw new Exception("Cannot encrypt non String tags!:" + el);
        }
    }

    protected void doEncryptAndHashUIDTag(DicomElement el) throws Exception
    {
        int tagNr = el.tag();
        VRType vrType = VRType.valueOf(el.vr());

        if (vrType.isString())
        {
            byte cryptBytes[] = null;
            String strValue = DicomWrapper.element2String(el, null);

            // first encrypt, then hash, then UID encode.
            cryptBytes = crypter.encrypt(strValue);
            byte hashBytes[] = hash(cryptBytes);
            String hashedUid = hashToUidString(hashBytes);

            dicom.setTag(tagNr, hashedUid);
            logger.debugPrintf("> CRYPT&HASH UID RESULT:%s\n", dicom.getElement(tagNr));
        }
        // else if (vrType.isBinary()) {} // could encrypt bytes here.
        else
        {
            // number values could be encrypted and stored as encrypted
            // numerical value;
            throw new Exception("Cannot encrypt non String tags!:" + el);
        }
    }

    // =======================================
    // Hashing/Crypters helper methods.
    // =======================================

    private byte[] hash(byte bytes[])
    {
        return hasher.hash(bytes, true, this.getHashSalt(), procOptions.prefixHashSalt);
    }

    private byte[] hash(String text)
    {
        return hasher.hash(text.getBytes(hasher.getEncoding()), true, this.getHashSalt(), procOptions.prefixHashSalt);
    }

    private String hashToBase64(String text)
    {
        byte bytes[] = hasher.hash(text.getBytes(hasher.getEncoding()), true, this.getHashSalt(), procOptions.prefixHashSalt);

        return StringUtil.base64Encode(bytes);
    }

    private String hashToUidString(byte hashBytes[]) throws Exception
    {
        String hashedUid = createHashedUid(procOptions.getUIDPrefix(), hashBytes, procOptions.getMaxHashedUIDByteLength());

        // be strict!
        if (hashedUid.length() > 64)
            throw new Exception("Fatal: Hashed UID is to long! Length =" + hashedUid.length() + " of:" + hashedUid);

        return hashedUid;
    }

    // =====================
    // Life cycle management
    // =====================

    /**
     * Dispose object. After a dispose() the object must not be used anymore.
     */
    public void dispose()
    {

    }

}
