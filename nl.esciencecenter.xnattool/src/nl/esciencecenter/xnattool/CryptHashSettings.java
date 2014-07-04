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

import nl.esciencecenter.ptk.crypt.CryptScheme;
import nl.esciencecenter.ptk.crypt.StringHasher;

/**
 * Crypt and Hash Settings object use by the DBMapping class.
 */
public class CryptHashSettings
{
    /** Hashing scheme */
    protected String hashAlgorithm = StringHasher.SHA_256;

    /** Hash length in bytes used in Subject,Session,Scan labels */
    protected int maxHashLength = 8; // length in bytes, not chars.

    /** Encryption Scheme */
    protected CryptScheme cryptScheme = CryptScheme.DESEDE_ECB_PKCS5;

    /** Authenticated ID */
    protected String sourceId = null;

    /** Actual encryption key */
    protected byte encryptionKey[] = null;

    protected boolean prefixSalt = false;

    /**
     * Create default settings
     */
    protected CryptHashSettings()
    {
    }

    public CryptHashSettings(CryptScheme cryptScheme)
    {
        this.cryptScheme = cryptScheme;
    }

    public void setHashing(String hashAlgorithm, int maxHashLength)
    {
        this.hashAlgorithm = hashAlgorithm;
        this.maxHashLength = maxHashLength;
    }

    public void setCredentials(String sourceId, byte key[])
    {
        this.sourceId = sourceId;
        this.encryptionKey = key;
    }

    public void setCryptScheme(CryptScheme scheme)
    {
        this.cryptScheme = scheme;
    }

    /** Source identity of data owner. Used for salting */
    public String getSourceID()
    {
        return sourceId;
    }

    /** Return hash length in bytes. Hexadecimal encoded hash String is 2x this length */
    public int getMaxHashLength()
    {
        return maxHashLength;
    }

    public byte[] getCryptKey()
    {
        return encryptionKey;
    }
}