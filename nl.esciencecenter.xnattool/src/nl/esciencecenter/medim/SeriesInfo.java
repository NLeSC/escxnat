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

package nl.esciencecenter.medim;

import java.util.Date;

/**
 * A Dicom Series is an actual ScanSet.
 */
public class SeriesInfo extends DataInfo 
{
    /**
     * Series InstanceUID is the actual ScanSet UID
     */
    private String seriesInstanceUID;

    /**
     * Descriptive Text
     */
    private String seriesDescription;

    /**
     * Incremental number identifying this Serie(s) in a Study (Or Session). 
     * Can be null ! 
     */
    private Integer seriesNr;

    /**
     * Combined Date+Time object.
     */
    private Date seriesDate;

    protected SeriesInfo()
    {
    }
    
    public SeriesInfo(String seriesUid)
    {
        seriesInstanceUID=seriesUid; 
    }


    public String getSeriesDescription()
    {
        return seriesDescription;
    }

    public void setSeriesInstanceUID(String seriesUid) 
    {
       this.seriesInstanceUID=seriesUid;
    }
    
    public String getSeriesInstanceUID()
    {
        return this.seriesInstanceUID; 
    }
    
    public Integer getSeriesNr()
    {
        return this.seriesNr;
    }
    
    public void setSeriesNumber(Integer seriesNr)
    {
        this.seriesNr=seriesNr; 
    }
    
    public Date getSeriesDate()
    {
        return this.seriesDate;
    }

    public void setSeriesDate(Date date)
    {
        this.seriesDate=date; 
    }

    public void setSeriesDescription(String description)
    {
       this.seriesDescription=description; 
    }

    @Override
    public String getDataType()
    {
        return "SeriesInfo"; 
    }

    @Override
    public String getDataUID()
    {
        return this.seriesInstanceUID; 
    }

}