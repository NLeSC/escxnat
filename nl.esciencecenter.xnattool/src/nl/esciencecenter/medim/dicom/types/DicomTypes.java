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

package nl.esciencecenter.medim.dicom.types;

import org.dcm4che2.data.VR;

/** 
 * Dicom Tag Types. 
 */
public class DicomTypes
{
    // Some SOP Types: 
    public static final String IMPLICIT_VR_LITTLE_ENDIAN = "1.2.840.10008.1.2";
    public static final String EXPLICIT_VR_LITTLE_ENDIAN = "1.2.840.10008.1.2.1";
    public static final String EXPLICIT_VR_BIG_ENDIAN    = "1.2.840.10008.1.2.2";
    public static final String DEFLATE_TRANSFER_SYNTAX   = "1.2.840.10008.1.2.1.99";
    
    /** 
     * Simplified VR value types. 
     */
    public static enum ValueType
    {
        INTEGER, // Integer types: short,int and long. Since long is 4 bytes java int is good enough. 
        DOUBLE,  // Double/Float types  
        STRING,  // ASCII, String and Text. The only difference is length and used character set. 
        BYTES,   // 'other', return as bytes. 
        TIME,    // Time in 'hh:mm:ss.frac' or HHMMSS.FRAC style. 
        DATETIME,// GMT normalized time yyyyy-mm-dd HH:MM:SS.Frac 
        UID,     // UID, but can be represented as String.
        UNKNOWN  // Can only return value as bytes; 
    };

    //    AE          16          //Application Name
    //    AS          4           //Age String: nnnW or nnnM or nnnY
    //    AT          4           //Attribute Tag gggg,eeee
    //    CS          16          //Code String
    //    DA          8           //Date yyyymmdd (check for yyyy.mm.dd also and convert)
    //    DS          16          //Decimal String may start with + or - and may be padded with l or t space
    //    DT          26          //Date Time YYYYMMDDHHMMSS.FFFFFF&ZZZZ (&ZZZ is optional & = + or -)
    //    FL          4           //Single precision floating pt number (float)
    //    FD          16          //Double precision floating pt number (double)
    //    IS          12          //Integer encoded as string. may be padded
    //    LO          64          //Character string. can be padded. cannot contain \ or any control chars except ESC
    //    LT          10240       //Long Text. Leading spaces are significant. trailing spaces aren't
    //    OB          -           //Other bytes  (can also be: single trailing 0x00 to make even number of bytes. Transfer Syntax determines len)
    //    OF          -           //Other Floats. Array of 32 bits floats 
    //    OW          -           //Other Words.  Array of 16 bits words (short)
    //    PN          -           //Person's Name 64byte max per component. 5 components. delimiter = ^
    //    SH          16          //Short String. may be padded
    //    SL          4           //signed long integer
    //    SQ          -           //Sequence of zero or more items
    //    SS          2           //signed short integer (word)
    //    ST          1024        //Short Text of chars
    //    TM          16          //Time hhmmss.frac (or older format: hh:mm:ss.frac)
    //    UI          64          //Unique Identifier (delimiter = .) 0-9 only, trailing space to make even #
    //    UL          4           //Unsigned long integer
    //    UN          -           //unknown
    //    US          2           //Unsigned short integer (word)
    //    UT          -           //Unlimited Text. trailing spaces ignored
    
    // #Special (Meta) Types: 
    //    OX          -           // This meta type can either be OB or OW (bytes or 16 bit word) 
    //                               depending on the transfer alignment (bytes or words).
    //                               Dicom files themself specify either OB or OW.  
    //    DL          -           // Delimiter Types. 
    /**
     *  Enum type for VR types. 
     *  Also maps VR type to basic Java Type. 
     */
    public static enum VRType
    {
        AE(VR.AE,ValueType.STRING), // AE, 16 // Application Name
        AS(VR.AS,ValueType.STRING),
        AT(VR.AT,ValueType.STRING),
        CS(VR.CS,ValueType.STRING), // CS, 16 // Code String
        DA(VR.DA,ValueType.STRING), // treat simplified date string as string for now 
        DS(VR.DS,ValueType.DOUBLE),
        DT(VR.DT,ValueType.DATETIME),
        FL(VR.FL,ValueType.DOUBLE),
        FD(VR.FD,ValueType.DOUBLE),
        IS(VR.IS,ValueType.STRING), // 12 characters coded integer ? 
        LO(VR.LO,ValueType.STRING),
        LT(VR.LT,ValueType.STRING),
        OB(VR.OB,ValueType.BYTES),  // other byte/bytes 
        OF(VR.OF,ValueType.BYTES),  // other float/floats
        OW(VR.OW,ValueType.BYTES),  // other word/words
        PN(VR.PN,ValueType.STRING),
        SH(VR.SH,ValueType.STRING),
        SL(VR.SL,ValueType.INTEGER), // 4 bytes long = (java) int 
        SQ(VR.SQ,null), // sequence!
        SS(VR.SS,ValueType.INTEGER),
        ST(VR.ST,ValueType.STRING),
        TM(VR.TM,ValueType.TIME),
        UI(VR.UI,ValueType.UID),
        UL(VR.UL,ValueType.INTEGER),
        UN(VR.UN,ValueType.UNKNOWN),
        US(VR.US,ValueType.INTEGER),
        UT(VR.UT,ValueType.STRING), 
        // Special meta types: 
        OX(VR.OB,ValueType.BYTES), // can be OB or OW. Default to OB 
        DL(VR.OB,null)  // Meta type like SQ.  
        ; 
        
        // === Private === //
        /** org.dcm4che2.data.VR Type */
        private VR vr;

        /** Matching Java Value Type */
        private ValueType valueType;

        private VRType(VR vr, ValueType valueType)
        {
            this.vr = vr;
            this.valueType = valueType;
        }

        public VR vr()
        {
            return vr;
        };

        public static VRType valueOf(VR vr)
        {
            // hash ?
            VRType[] values = VRType.values();
            for (VRType vrType : values)
                if (vrType.vr() == vr)
                    return vrType;

            return null;
        }

        public boolean isString()
        {
            switch (valueType)
            {
                case STRING:
                case UID:
                    return true;
                default:
                    return false;
            }
        }

        /** Numerical value */
        public boolean isValue()
        {
            switch (valueType)
            {
                case INTEGER:
                case DOUBLE:
                    return true;
                default:
                    return false;
            }
        }

        public ValueType getValueType()
        {
            return valueType;
        }

        public boolean isSequence()
        {
            return (vr == VR.SQ);
        }

        /** Whether value is stored as plain byte or word array. (OB/OW/OX) */
        public boolean isBinary()
        {
            switch (this)
            {
                case OB:
                case OW:
                case OX:
                    return true;
                default:
                    return false;
            }

        }
    };
    
}
