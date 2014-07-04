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

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import nl.esciencecenter.medim.dicom.DicomTagFilters.DicomTagFilter;
import nl.esciencecenter.medim.dicom.types.DicomTypes;
//import nl.esciencecenter.medim.dicom.TagFilters.MutableVarHeap;
import nl.esciencecenter.medim.dicom.types.DicomTypes.VRType;
import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.ptk.util.logging.ClassLogger;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.DicomObject.Visitor;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;

/**
 * Dicom Wrapper utility. Wraps around org.dcm4che2.data.DicomObjects.
 * 
 * Provides helper methods to most used Dicom Fields. Before editing an Dicom object use setIsModifyable(true).
 * 
 */
public class DicomWrapper
{
    static private ClassLogger logger = ClassLogger.getLogger(DicomWrapper.class);

    // ===

    public static DicomWrapper readFrom(URI uri) throws IOException
    {
        return new DicomWrapper(DicomUtil.readDicom(uri.getPath()), uri);
    }

    // ====================================================================
    // Instance Fields
    // ====================================================================

    private DicomObject dicom;

    /**
     * When changing the dicom object, this field is set to true!
     */
    private boolean isModified = false;

    /**
     * Whether this dicom object is read-only.
     * 
     */
    private boolean canModify = false;

    /**
     * Default TimeZone for this file. If no time zone is specified us local one defined by TimeZone.getDefault();
     */
    protected TimeZone defaultTZ = TimeZone.getDefault();

    protected java.net.URI sourceUri = null;

    protected String defaultTansferSyntaxUID = DicomTypes.IMPLICIT_VR_LITTLE_ENDIAN;

    // TimeZone actualTZ = null; //

    // ====================================================================
    // Constructors/Initializers.
    // ====================================================================

    /**
     * Wrap util Object around Dicom Object. If possible provide an optional sourceUri.
     * 
     * @param dicom
     *            - the Dicom Object
     * @param sourceUri
     *            - optional source URI.
     */
    public DicomWrapper(DicomObject dicom, URI optSourceUri)
    {
        if (dicom == null)
        {
            throw new NullPointerException("Dicom Object can not be NULL!");
        }

        this.dicom = dicom;
        this.sourceUri = optSourceUri;
    }

    /**
     * This method must be called before any Dicom Field may be changed.
     * 
     * @param val
     */
    public void setIsModifyable(boolean value)
    {
        canModify = value;
    }

    public DicomObject getDicomObject()
    {
        return this.dicom;
    }

    public java.net.URI getSourceURI()
    {
        return sourceUri;
    }

    // ====================================================================
    //
    // ====================================================================

    public boolean acceptVisitor(Visitor visitor)
    {
        return dicom.accept(visitor);
    }

    // ====================================================================
    // UIDs and (Scan Set) Identity numbers.
    // ====================================================================

    public String getStudyInstanceUID()
    {
        return getStringValue(Tag.StudyInstanceUID);
    }

    public String getSeriesInstanceUID()
    {
        return getStringValue(Tag.SeriesInstanceUID);
    }

    /**
     * Type of Media.
     */
    public String getMediaStorageSOPClassUID()
    {
        return getStringValue(Tag.MediaStorageSOPClassUID);
    }

    /**
     * Machine generated UID for this (Slice0 File.
     */
    public String getMediaStorageSOPInstanceUID()
    {
        return getStringValue(Tag.MediaStorageSOPInstanceUID);
    }

    // ====================================================================
    // tags
    // ====================================================================

    public List<String> getTagNames(boolean sort)
    {
        StringList names = new StringList();

        Iterator<DicomElement> it = dicom.datasetIterator();
        for (DicomElement el = null; it.hasNext();)
        {
            el = it.next(); //
            String name = DicomUtil.getTagName(el.tag());
            // unknown tag:
            if (name != null)
                names.add(name);
            // else
            // names.add("#0x"+Integer.toHexString(el.tag()));
        }

        if (sort)
            names.sort(true);

        return names;
    }

    // ====================================================================
    // Subject/Patient Info:
    // ====================================================================

    /**
     * Tag (0010,0010) "Patient Name".
     */
    public String getPatientName()
    {
        return getStringValue(Tag.PatientName);
    }

    /**
     * Tag (0010,0020) "Patient ID". Can be a name.
     */
    public String getPatientID()
    {
        return getStringValue(Tag.PatientID);
    }

    // ====================================================================
    // Study(Session) / Series(Scan Set) Values
    // ====================================================================

    /**
     * Tag (0020,10) "StudyID". Number or other String identifier
     */
    public String getStudyID()
    {
        return getStringValue(Tag.StudyID);
    }

    public Integer getStudyIDAsNumber(int defaultValue)
    {
        String strVal = this.getStudyID();
        if (StringUtil.isEmpty(strVal))
            return defaultValue;

        try
        {
            Integer val = Integer.parseInt(strVal);
            if (val == null)
                return defaultValue;
            return val;
        }
        catch (Throwable t)
        {
            return defaultValue;
        }
    }

    /**
     * ScanSet Series number. Relative number of this Series within a Study.
     */
    public Integer getSeriesNumber()
    {
        return getIntegerValue(Tag.SeriesNumber);
    }

    /**
     * Acquisition Number. Relative Slice Number within a Study(!) When a Study has multiple Series, this (slice) number
     * is increased per slice but continues in the next Series. The InstanceNumber typically restarts at 0. Not much
     * used. Better use InstanceNumber
     * 
     * @see DicomWrapper#getImagePositionPatient() for alternative way to determine the "Slice" index.
     * @returns AcquisitionNumber as Integer Object. Might return null if missing.
     */
    public Integer getAcquisitionNumber()
    {
        return getIntegerValue(Tag.AcquisitionNumber);
    }

    /**
     * Returns slice instanceNumber. Returns NULL if not set !
     * <p>
     * 
     * @see DicomWrapper#getImagePositionPatient() for alternative way to determine the "Slice" index.
     */
    public Integer getInstanceNumber()
    {
        return getIntegerValue(Tag.InstanceNumber);
    }

    /**
     * Significant Study ID numbers. Values are Non UID Values! As Study ID can also be a String. Don't trust number "0"
     * as Study Id! The others are numbers. This set of numbers "should" identify this slice within the context of a
     * Study and Series.
     * <ul>
     * <li>StudyID
     * <li>SeriesNumber - relative number of this Series within a Study
     * <li>AcquisistionNumber - slice number relative within this <strong>Study</strong>, not used much (!).
     * <li>InstanceNumber - slice number relative within this <strong>Series</strong>, usual the slice number but might
     * be missing.
     * </ul>
     * 
     * @returns [StudyID,SeriesNumber,AcquisitationNumber,InstanceNumber].
     */
    public boolean getSetIdentificationNrs(int[] nrs)
    {
        int index = 0;
        nrs[index++] = this.getStudyIDAsNumber(0); // might not be so.
        nrs[index++] = getIntegerValue(Tag.SeriesNumber, 0);
        nrs[index++] = getIntegerValue(Tag.AcquisitionNumber, 0); // might be
                                                                  // missing !
        nrs[index++] = getIntegerValue(Tag.InstanceNumber, 0); // might be
                                                               // missing !
        // nrs[index++]=0; // to be specified

        return true;
    }

    /**
     * Returns true if the studyID is an actual number and not another identification String. As the VR type is "SH"
     * this can be any 16 bytes String.
     */
    public boolean checkStudyIDisNumber()
    {
        String strVal = this.getStudyID();
        if (StringUtil.isEmpty(strVal))
            return false;

        try
        {
            int intVal = Integer.parseInt(strVal, 10);
            return true;
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    public String getFileSetID()
    {
        return this.getStringValue(Tag.FileSetID);
    }

    /**
     * Returns Image Position [X,Y,Z] of this slice.
     */
    public double[] getImagePositionPatient()
    {
        return element2Doubles(dicom.get(Tag.ImagePositionPatient));
    }

    /**
     * Returns patient orientation [RowX,RowY,RowZ,ColX,ColY,ColZ].
     */
    public double[] getImageOrientationPatient()
    {
        return element2Doubles(dicom.get(Tag.ImageOrientationPatient));
    }

    // ========================================================================
    // Scan Parameters
    // ========================================================================

    /**
     * @Return Repetition Time as double or 0 if missing.
     */
    public double getRepetitionTimeDouble()
    {
        return element2Double(this.dicom.get(Tag.RepetitionTime), 0d);
    }

    /**
     * @Returns Inversion Time as double or 0 if missing.
     */
    public double getInversionTimeDouble()
    {
        return element2Double(this.dicom.get(Tag.InversionTime), 0d);
    }

    /**
     * @Returns Echo Time as double or 0 if missing.
     */
    public double getEchoTimeDouble()
    {
        return element2Double(this.dicom.get(Tag.EchoTime), 0d);
    }

    /**
     * @Returns Flip Angle as double.
     */
    public double getFlipAngleDouble()
    {
        return element2Double(this.dicom.get(Tag.FlipAngle), 0d);
    }

    /**
     * @Returns ModalityType { "MR", "CT" , "EC", ... }
     */
    public String getModalityType()
    {
        return element2String(this.dicom.get(Tag.Modality), null);
    }

    /**
     * @Returns ScanningsSequence Type as String.
     */
    public String getScanningSequenceTypeString()
    {
        return element2String(this.dicom.get(Tag.ScanningSequence), null);
    }

    /**
     * @Returns ScanningsSequence Type as String.
     */
    public String[] getScanningSequenceTypes()
    {
        return element2Strings(this.dicom.get(Tag.ScanningSequence), null);
    }

    // ========================================================================
    // Date/Time
    // ========================================================================

    /**
     * @Returns Combined StudyTime and StudyDate field to one java.util.Date object.
     */
    public Date getStudyDateTime()
    {
        return getDateTime(Tag.StudyDate, Tag.StudyTime);
    }

    /**
     * @Returns Combined BirthTime and BirthDate field to one java.util.Date object.
     */
    public Date getPatientBirthDateTime()
    {
        return getDateTime(Tag.PatientBirthDate, Tag.PatientBirthDate);
    }

    /**
     * @returns Combined InstanceCreateDate and InstanceCreationTime
     */
    public Date getInstanceCreationDateTime()
    {
        return getDateTime(Tag.InstanceCreationDate, Tag.InstanceCreationTime);
    }

    // ========================================================================
    // Generic getters and field convertors.
    // ========================================================================

    public double getDoubleValue(int tag, double defaultVal)
    {
        DicomElement val = dicom.get(tag);
        if (val == null)
            return defaultVal;

        return element2Double(val, defaultVal);
    }

    public String getStringValue(int tag)
    {
        DicomElement val = dicom.get(tag);
        if (val == null)
            return null;

        return element2String(val, null);
    }

    /**
     * Return Integer object or NULL of tag can't be found or cannot be parsed into an Integer
     */
    public Integer getIntegerValue(int tag)
    {
        DicomElement val = dicom.get(tag);

        if (val == null)
        {
            return null;
        }

        return element2Int(val);
    }

    /**
     * Dicom "Integers" are 16 bits thus fit in java's 32 bits integer.
     */
    public int getIntegerValue(int tag, int defaultValue)
    {
        DicomElement val = dicom.get(tag);
        if (val == null)
            return defaultValue;

        return element2Int(val, defaultValue);
    }

    /**
     * Important: When converting an unsigned 32 bits 'long' it must keep its positive value. Store in java's
     * <strong>signed</strong> long.
     */
    public long getLongValue(int tag, long defaultValue)
    {
        DicomElement val = dicom.get(tag);
        if (val == null)
            return defaultValue;

        return element2Long(val, defaultValue);
    }

    public Object getValue(int tag)
    {
        DicomElement val = dicom.get(tag);

        if (val == null)
            return null;

        return element2Object(val);
    }

    /**
     * @Returns Dicom Tag value as String, for most recognized Objects this will return the Object.toString() value.
     */
    public String getValueAsString(int tagNr)
    {
        DicomElement val = dicom.get(tagNr);

        if (val == null)
            return null;

        return element2String(val, null);
    }

    public Date getDateValue(int tag)
    {
        DicomElement val = dicom.get(tag);
        if (val == null)
            return null;

        return element2Date(val);
    }

    /**
     * Returns combination of a Date and a Time tag in one Java Data object.
     */
    public Date getDateTime(int dateTag, int timeTag)
    {
        // need at least a date tag:
        DicomElement dateVal = dicom.get(dateTag);
        if (dateVal == null)
            return null;

        // optional:
        DicomElement timeVal = dicom.get(timeTag);
        if (timeVal == null)
        {
            return element2Date(dateVal);
        }
        else
        {
            return element2DateTime(dateVal, timeVal, getTimeZone());
        }
    }

    /**
     * Returns TimeZone or default TimeZone if not set in the Dicom File
     */
    public TimeZone getTimeZone()
    {
        DicomElement tzOffset = dicom.get(Tag.TimezoneOffsetFromUTC);
        if (tzOffset == null)
        {
            return defaultTZ; // TimeZone.getDefault(); //
                              // TimeZone.getTimeZone("UTC");
        }

        String tzStr = element2String(tzOffset, null);
        // [+|-]HHMM
        // int tzInt=element2Int(tzOffset, 0);
        char c = tzStr.charAt(0);
        if ((c != '-') && (c != '+'))
            tzStr = "+" + tzStr;

        TimeZone tz = TimeZone.getTimeZone("GMT" + tzStr);

        return tz;
    }

    /**
     * Set default TimeZone to be used if TimeZone field is not specified in DicomFile
     */
    public void setDefaultTimeZone(TimeZone tz)
    {
        this.defaultTZ = tz;
    }

    /**
     * Serialize object to actual bytes. Uses TranferSyntaxUID as actual serialization format.
     */
    public byte[] getBytes() throws IOException
    {
        return DicomUtil.getBytes(dicom);
    }

    // ========================================================================
    //
    // ========================================================================

    public static Object element2Object(DicomElement el)
    {
        if (el == null)
            return null;

        VRType vrType = DicomUtil.getVRType(el.vr());

        System.err.println("#items:" + el.countItems());

        switch (vrType.getValueType())
        {
            case INTEGER:
                return element2Int(el, 0);
            case DOUBLE:
                return element2Double(el, 0);
            case BYTES:
                return el.getBytes();
            case DATETIME:
                return element2Date(el);
            case UNKNOWN:
                return null;
            case UID:
            case STRING:
            default:
                return element2String(el, null);
        }
    }

    /**
     * Check whether all filters match
     */
    public boolean matches(List<DicomTagFilter> filters)
    {
        if (filters == null)
            return true; // no filters => all match.

        // Logical AND of all filters: All must match;

        for (DicomTagFilter filt : filters)
        {
            int tagNr = filt.getTagNr();
            DicomElement el = this.dicom.get(tagNr);

            if (el == null)
            {
                logger.warnPrintf("Filter defined for Tag that does not exists:%s\n", DicomUtil.getTagName(tagNr));
                // filter must evaluate 'null' value.
            }

            if (filt.allowNullValues() == false)
            {
                return false;
            }

            if (filt.matches(el) == false)
            {
                return false;
            }

        }
        return true;
    }

    // ========================================================================
    // Generic setters, implements MutableTagHeap
    // ========================================================================

    /**
     * Write integer or other short value. Auto converts to actual VR type
     * 
     * @throws Exception
     */
    public void setTag(int tagNr, int intValue) throws Exception
    {
        if (this.canModify == false)
            throw new Exception("Unmodifyable Dicom");

        DicomElement el = this.dicom.get(tagNr);
        VR vr;
        if (el != null)
            vr = el.vr();
        else
            vr = DicomUtil.getVRofTag(tagNr);

        dicom.putInt(tagNr, vr, intValue);

        this.isModified = true;
    }

    /**
     * Write float,double or "real" value. Auto converts to actual VR type
     * 
     * @throws Exception
     */
    public void setTag(int tagNr, double doubleVal) throws Exception
    {
        if (this.canModify == false)
            throw new Exception("Unmodifyable Dicom");

        // check float ?
        DicomElement el = this.dicom.get(tagNr);

        VR vr;
        if (el != null)
            vr = el.vr();
        else
            vr = DicomUtil.getVRofTag(tagNr);

        dicom.putDouble(tagNr, vr, doubleVal);
        this.isModified = true;
    }

    public void setDateValue(int tagNr, Date date) throws Exception
    {
        if (this.canModify == false)
            throw new Exception("Unmodifyable Dicom");

        // check float ?
        DicomElement el = this.dicom.get(tagNr);
        VR vr;
        if (el != null)
            vr = el.vr();
        else
            vr = DicomUtil.getVRofTag(tagNr);
        // Assert Date/Time ?
        dicom.putDate(tagNr, vr, date);

        this.isModified = true;
    }

    /**
     * Write ASCI/String,etc. Auto converts to actual VR type
     * 
     * @throws Exception
     */
    public void setTag(int tagnr, String value) throws Exception
    {
        if (this.canModify == false)
            throw new Exception("Unmodifyable Dicom");

        DicomElement el = this.dicom.get(tagnr);
        VR vr;
        if (el != null)
            vr = el.vr();
        else
            vr = DicomUtil.getVRofTag(tagnr);

        if (vr == null)
            throw new NullPointerException("setTag():" + tagnr + ":Resolved VR type is NULL");
        dicom.putString(tagnr, vr, value);

        this.isModified = true;
    }

    /**
     * Write ASCI/String,etc. Auto converts to actual VR type
     * 
     * @throws Exception
     */
    public void setTag(int tagnr, byte bytes[]) throws Exception
    {
        if (this.canModify == false)
            throw new Exception("Unmodifyable Dicom");

        DicomElement el = this.dicom.get(tagnr);
        VR vr;
        if (el != null)
            vr = el.vr();
        else
            vr = DicomUtil.getVRofTag(tagnr);

        dicom.putBytes(tagnr, vr, bytes);

        this.isModified = true;
    }

    public boolean deleteTag(int tagnr) throws Exception
    {
        if (this.canModify == false)
            throw new Exception("Unmodifyable Dicom (Set modification flag first, before editing)");

        if (this.dicom.get(tagnr) == null)
            return true; // false ?

        this.isModified = true;
        this.dicom.remove(tagnr);

        return true;
    }

    // ========================================================================
    // Static Formatters
    // ========================================================================

    /**
     * Return value as String.
     * 
     * @param el
     *            - DicomElement
     * @param charSet
     *            - optional Character Set. May be null.
     * @return Value as String
     */
    public static String element2String(DicomElement el, SpecificCharacterSet charSet)
    {
        if (el == null)
        {
            return null;
        }

        // check exceptions ?
        return el.vr().toString(el.getBytes(), el.bigEndian(), charSet);
    }

    public static String[] element2Strings(DicomElement el, SpecificCharacterSet charSet)
    {
        if (el == null)
        {
            return null;
        }

        // check exceptions ?
        return el.vr().toStrings(el.getBytes(), el.bigEndian(), charSet);
    }

    public static double element2Double(DicomElement el, double defaultValue)
    {
        if (el == null)
            return defaultValue; // Be Robust

        byte bytes[] = el.getBytes();
        if ((bytes == null) || (bytes.length <= 0))
            return defaultValue;

        return el.vr().toDouble(bytes, el.bigEndian());
    }

    public static double[] element2Doubles(DicomElement el)
    {
        if (el == null)
        {
            return null;
        }

        byte bytes[] = el.getBytes();
        if ((bytes == null) || (bytes.length <= 0))
        {
            return null;
        }
        // should convert floats to doubles as well:
        return el.vr().toDoubles(bytes, el.bigEndian());
    }

    public static float element2Float(DicomElement el, float defaultValue)
    {
        if (el == null)
            return defaultValue; // Be Robust

        byte bytes[] = el.getBytes();
        if ((bytes == null) || (bytes.length <= 0))
            return defaultValue;

        return el.vr().toFloat(bytes, el.bigEndian());
    }

    public static Date element2Date(DicomElement el)
    {
        if (el == null)
            return null;

        return el.vr().toDate(el.getBytes());
    }

    /**
     * Converts Date and Time element to combined DateTime. Java Date object contains time code attributes as well.
     * 
     * @param timeZone
     * @return
     */
    public static Date element2DateTime(DicomElement dateEl, DicomElement timeEl, TimeZone timeZone)
    {
        if (dateEl == null)
            return null;
        Date date = dateEl.vr().toDate(dateEl.getBytes());

        if (date == null)
        {
            logger.errorPrintf("Couldn't get Date object from elelement:%s\n", dateEl.toString());
            return null;
        }

        Date time = timeEl.vr().toDate(timeEl.getBytes());

        GregorianCalendar dateCal = new GregorianCalendar();
        dateCal.setTime(date);
        dateCal.setTimeZone(timeZone); // TimeZone.getTimeZone("UTC"));

        GregorianCalendar timeCal = new GregorianCalendar();
        timeCal.setTime(time);

        int year = dateCal.get(GregorianCalendar.YEAR);
        int month = dateCal.get(GregorianCalendar.MONTH);
        int dayOfMonth = dateCal.get(GregorianCalendar.DAY_OF_MONTH);
        int hourOfDay = timeCal.get(GregorianCalendar.HOUR_OF_DAY);
        int minutes = timeCal.get(GregorianCalendar.MINUTE);
        int seconds = timeCal.get(GregorianCalendar.SECOND);
        // int millies=timeCal.get(GregorianCalendar.MILLISECOND);

        logger.debugPrintf("Date=%s,Time=%s ", dateCal.getTime(), timeCal.getTime());

        // update with time
        dateCal.set(year, month, dayOfMonth, hourOfDay, minutes, seconds);

        logger.debugPrintf(" => %s \n", dateCal.getTime());

        return dateCal.getTime();
    }

    /**
     * Returns Integer value as int (Java native type) or defaultValue of value is null or not an integer.
     */
    public static int element2Int(DicomElement el, int defaultValue)
    {
        Integer value = element2Int(el);

        if (value != null)
        {
            return value;
        }
        else
        {
            return defaultValue;
        }
    }

    /**
     * Returns Integer value, as Java Integer Object, of element or NULL(!) if not defined.
     */
    public static Integer element2Int(DicomElement el)
    {
        if (el == null)
        {
            return null;
        }

        byte bytes[] = el.getBytes();
        // happens in SPI files! Element is set, but NO bytes.
        if ((bytes == null) || (bytes.length <= 0))
        {
            return null;
        }

        return el.vr().toInt(bytes, el.bigEndian());
    }

    // note: dicom only support 32 bits integer.
    public static long element2Long(DicomElement el, long defaultValue)
    {
        if (el == null)
            return defaultValue;

        byte bytes[] = el.getBytes();
        if ((bytes == null) || (bytes.length <= 0))
            return defaultValue;

        return el.vr().toInt(bytes, el.bigEndian());
    }

    public DicomElement getElement(int tagNr)
    {
        return this.dicom.get(tagNr);
    }

    public String getFilename()
    {
        if (sourceUri != null)
            return sourceUri.getPath();

        return null;
    }

    /**
     * Check consistancy, mandatory field types, etc.
     * 
     * @param autoFix
     * @throws Exception
     */
    public void performChecks(boolean autoFix) throws Exception
    {
        String tsUid = this.getStringValue(Tag.TransferSyntaxUID);

        if (StringUtil.isEmpty(tsUid))
        {
            String autoFixStr = "";

            if (autoFix)
            {
                autoFixStr = "Autofixing. Setting to Implicit Little Endian!";
            }
            else
            {
                autoFixStr = "NOT Autofixing!";
            }

            logger.warnPrintf("Warning:TransferSyntaxUID is missing. %s\n", autoFixStr);

            if (autoFix)
            {
                this.setTag(Tag.TransferSyntaxUID, defaultTansferSyntaxUID);
            }
        }
        else
        {
            logger.debugPrintf(" - check: transferSyntaxUID=%s\n", tsUid);
        }
    }

    public void dispose()
    {
        dicom = null;
    }

}
