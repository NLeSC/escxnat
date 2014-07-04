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

package nl.esciencecenter.xnattool;

import java.util.ArrayList;
import java.util.List;

import nl.esciencecenter.medim.ImageDirScanner;
import nl.esciencecenter.medim.ScanSetInfo;
import nl.esciencecenter.medim.ScanSetInfo.FileDescriptor;
import nl.esciencecenter.medim.StudyInfo;
import nl.esciencecenter.medim.SubjectInfo;
import nl.esciencecenter.ptk.csv.CSVData;

public class MetaDataWriter
{
    private ImageDirScanner imageSource;

    private DBMapping dbMapping;

    private CSVData csvData;

    public MetaDataWriter(ImageDirScanner imageSource, DBMapping mappings)
    {
        this.imageSource = imageSource;
        this.dbMapping = mappings;
    }

    public CSVData toCSV() throws Exception
    {
        this.csvData = new CSVData(true);

        switch (imageSource.getDataSetType())
        {
            case DICOM_SCANSET:
                return createDicomCSV(csvData);
            case NIFTI_SCANSET:
                return createNiftiCSV(csvData);
            case NIFTI_ATLASSET:
                return createNiftiAtlasCSV(csvData);
        }

        throw new Exception("Couldn't determine DataSetType:" + imageSource.getDataSetType());
    }

    protected CSVData createDicomCSV(CSVData csvData) throws Exception
    {

        List<String> scanSetKeys = dbMapping.getAllScanSetKeys();

        // [CSV HEADER]
        String headers[] = new String[] {
                "subject.label",
                "session.label",
                "dicom.patient.id",
                "dicom.patient.name",
                "dicom.patient.gender",
                "dicom.patient.ageString",
                "dicom.patient.birthdate",
                "dicom.study.id",
                "dicom.study.uid",
                "dicom.study.date",
                "dicom.scan.description"
                // "dicom.scan.description"
        };

        csvData.setHeaders(headers);

        for (String scanSetKey : scanSetKeys)
        {
            ScanSetInfo scanSet = imageSource.getScanSet(scanSetKey);
            assertNotNull(scanSet, "ScanSet not found:" + scanSetKey);

            String sessionKey = dbMapping.getSessionKeyOfScanSetKey(scanSetKey);
            assertNotNull(sessionKey, "Could not find SesionKey of ScanSet:" + scanSetKey);
            String subjectKey = dbMapping.getSubjectKeyOfSessionKey(sessionKey);
            assertNotNull(subjectKey, "Could not find SubjectKey of Session:" + scanSetKey);

            // Subject:
            String subjectLabel = dbMapping.getXnatSubjectLabel(subjectKey);
            assertNotNull(subjectLabel, "Could not find SubjectLabel of Subject:" + subjectKey);

            // Session/Study
            StudyInfo studyInfo = scanSet.getStudyInfo();
            assertNotNull(scanSet, "Could not find SessionKey of ScanSet:" + scanSetKey);
            String sessionLabel = dbMapping.getXnatSessionLabel(subjectKey, sessionKey);
            assertNotNull(sessionLabel, "Could not find SessionLabel of Subject:" + subjectKey);

            SubjectInfo subjInfo = scanSet.getSubjectInfo();

            // [ CSV Entry ]
            String row[] = new String[headers.length];
            int index = 0;

            row[index++] = subjectLabel;
            row[index++] = sessionLabel;
            row[index++] = subjInfo.getPatientID();
            row[index++] = subjInfo.getPatientName();
            row[index++] = subjInfo.getPatientGender();
            row[index++] = subjInfo.getPatientAgeString();
            row[index++] = csvData.toCSVDateString(subjInfo.getPatientBirthDate());
            row[index++] = studyInfo.getStudyId();
            row[index++] = studyInfo.getStudyInstanceUID();
            row[index++] = csvData.toCSVDateString(studyInfo.getStudyDate());
            // Srow[10]=scanSet.getScanUID();
            row[index++] = scanSet.getSeriesDescription();

            csvData.addRow(row);
        }

        return csvData;
    }

    protected CSVData createNiftiCSV(CSVData csvData) throws Exception
    {

        List<String> scanSetKeys = dbMapping.getAllScanSetKeys();

        // [CSV HEADER]
        String headers[] = new String[] {
                "subject.label",
                "session.label",
                "nifti.subject.id",
                "nifti.session.id",
                // "nifti.scan.uid",
                "nifti.scan.id"
        };

        csvData.setHeaders(headers);

        for (String scanSetKey : scanSetKeys)
        {
            ScanSetInfo scanSet = imageSource.getScanSet(scanSetKey);
            assertNotNull(scanSet, "ScanSet not found:" + scanSetKey);

            String sessionKey = dbMapping.getSessionKeyOfScanSetKey(scanSetKey);
            assertNotNull(sessionKey, "Could not find SesionKey of ScanSet:" + scanSetKey);
            String subjectKey = dbMapping.getSubjectKeyOfSessionKey(sessionKey);
            assertNotNull(subjectKey, "Could not find SubjectKey of Session:" + scanSetKey);

            // Subject:
            String subjectLabel = dbMapping.getXnatSubjectLabel(subjectKey);
            assertNotNull(subjectLabel, "Could not find SubjectLabel of Subject:" + subjectKey);

            // Session/Study
            StudyInfo studyInfo = scanSet.getStudyInfo();
            assertNotNull(scanSet, "Could not find SessionKey of ScanSet:" + scanSetKey);
            String sessionLabel = dbMapping.getXnatSessionLabel(subjectKey, sessionKey);
            assertNotNull(sessionLabel, "Could not find SessionLabel of Subject:" + subjectKey);

            SubjectInfo subjInfo = scanSet.getSubjectInfo();

            // [ CSV Entry ]
            String row[] = new String[headers.length];
            int index = 0;
            row[index++] = subjectLabel;
            row[index++] = sessionLabel;
            row[index++] = subjInfo.getPatientID();
            row[index++] = studyInfo.getStudyId();
            // row[index++]=scanSet.getScanUID();
            row[index++] = scanSet.getScanLabel();

            csvData.addRow(row);
        }

        return csvData;
    }

    protected CSVData createNiftiAtlasCSV(CSVData csvData) throws Exception
    {

        List<String> scanSetKeys = dbMapping.getAllScanSetKeys();

        // [CSV HEADER]
        String headers[] = new String[] {
                "subject.label",
                "session.label",
                "nifti.subject.id",
                "nifti.session.id",
                "nifti.scan.id",
                "atlas.label"
        };

        csvData.setHeaders(headers);

        for (String scanSetKey : scanSetKeys)
        {
            ScanSetInfo scanSet = imageSource.getScanSet(scanSetKey);
            assertNotNull(scanSet, "ScanSet not found:" + scanSetKey);

            String sessionKey = dbMapping.getSessionKeyOfScanSetKey(scanSetKey);
            assertNotNull(sessionKey, "Could not find SesionKey of ScanSet:" + scanSetKey);
            String subjectKey = dbMapping.getSubjectKeyOfSessionKey(sessionKey);
            assertNotNull(subjectKey, "Could not find SubjectKey of Session:" + scanSetKey);

            // Subject:
            String subjectLabel = dbMapping.getXnatSubjectLabel(subjectKey);
            assertNotNull(subjectLabel, "Could not find SubjectLabel of Subject:" + subjectKey);

            // Session/Study
            StudyInfo studyInfo = scanSet.getStudyInfo();
            assertNotNull(scanSet, "Could not find SessionKey of ScanSet:" + scanSetKey);
            String sessionLabel = dbMapping.getXnatSessionLabel(subjectKey, sessionKey);
            assertNotNull(sessionLabel, "Could not find SessionLabel of Subject:" + subjectKey);

            SubjectInfo subjInfo = scanSet.getSubjectInfo();

            ArrayList<FileDescriptor> files = scanSet.getFileDescriptors();

            for (int i = 0; i < files.size(); i++)
            {
                // [ CSV Entry ]
                String row[] = new String[headers.length];
                int index = 0;
                row[index++] = subjectLabel;
                row[index++] = sessionLabel;
                row[index++] = subjInfo.getPatientID();
                row[index++] = studyInfo.getStudyId();
                // row[index++]=scanSet.getScanUID();
                row[index++] = scanSet.getScanLabel();
                row[index++] = files.get(i).fileLabel;

                csvData.addRow(row);
            }

        }

        return csvData;
    }

    public StringBuilder toCSV(StringBuilder sb) throws Exception
    {
        CSVData data = toCSV();
        data.toString(sb, ";", "\n");
        return sb;
    }

    private void assertNotNull(Object value, String message) throws Exception
    {
        if (value == null)
            throw new Exception(message);
    }

}
