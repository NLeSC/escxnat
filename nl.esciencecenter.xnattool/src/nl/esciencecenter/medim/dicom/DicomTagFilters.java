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

import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.ptk.util.logging.ClassLogger;

import org.dcm4che2.data.DicomElement;

public class DicomTagFilters
{
    public static interface DicomTagFilter
    {
        /**
         * Tag nr this filter applies to.
         */
        public int getTagNr();

        /**
         * Return true if this filter matches the DicomElement. If the tag nr does NOT match the DicomElemen tag nr,
         * this filter return false
         * 
         * @param el
         *            - the DicomElement;
         * @return true - if the element value matches this specific filter.
         */
        public boolean matches(DicomElement el);

        public boolean allowNullValues();
    }

    public static abstract class TagValueFilter implements DicomTagFilter
    {
        protected static ClassLogger logger = ClassLogger.getLogger(TagValueFilter.class);

        // --- instance --- //

        int tagNr;

        public TagValueFilter(int tagNr)
        {
            this.tagNr = tagNr;
        }

        public int getTagNr()
        {
            return tagNr;
        }
    }

    public static class MinMaxFilter extends TagValueFilter
    {
        private double minVal;

        private double maxVal;

        public MinMaxFilter(int tagNr, double min, double max)
        {
            super(tagNr);
            this.minVal = min;
            this.maxVal = max;
        }

        @Override
        public boolean matches(DicomElement el)
        {
            if (el == null)
            {
                // [Min,Max] filter must have non zero/null value -> null
                // evaluates to false.
                return false;
            }

            if (el.tag() != tagNr)
            {
                return false;
            }

            double val = DicomWrapper.element2Double(el, 0);

            logger.debugPrintf("MinMaxFilter:Checking: <%s> %f <= %f <=%f <<< \n",
                    DicomUtil.getTagName(tagNr), minVal, val, maxVal);

            if ((minVal <= val) && (val <= maxVal))
            {
                return true;
            }

            return false;
        }

        public boolean allowNullValues()
        {
            return true;
        }
    }

    public static class StringMatchFilter implements DicomTagFilter
    {
        protected static ClassLogger logger = ClassLogger.getLogger(StringMatchFilter.class);

        // ---

        int tagNr;

        String theValue;

        boolean ignoreCase = false;

        public StringMatchFilter(int tagNr, String value, boolean ignoreCase)
        {
            this.tagNr = tagNr;
            this.theValue = value;
            this.ignoreCase = ignoreCase;
        }

        public int getTagNr()
        {
            return tagNr;
        }

        @Override
        public boolean matches(DicomElement el)
        {
            if (el == null)
            {
                return false; // unless the value should/must be null/empty
                              // string,etc.
            }

            if (el.tag() != tagNr)
                return false;

            String valStr = DicomWrapper.element2String(el, null);
            int result = StringUtil.compare(valStr, theValue, ignoreCase);
            logger.debugPrintf("MinMaxFilter:Checking: <%s>('%s'=='%s') == %d \n",
                    DicomUtil.getTagName(tagNr), valStr, theValue, result);

            return (result == 0);
        }

        public boolean allowNullValues()
        {
            return true;
        }
    }

}
