/*
 * Copyright 2012-2013 Netherlands eScience Center.
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

package tests.other.webclient;

import nl.esciencecenter.ptk.crypt.Secret;
import nl.esciencecenter.ptk.data.StringHolder;
import nl.esciencecenter.ptk.ssl.CertificateStore;
import nl.esciencecenter.ptk.util.logging.ClassLogger;
import nl.esciencecenter.ptk.web.WebClient;

public class Test_WebClientSSL
{
    public static void main(String args[])
    {
        ClassLogger.getLogger(WebClient.class).setLevelToDebug();
        ClassLogger.getLogger(CertificateStore.class).setLevelToDebug();

        try
        {
            WebClient client = Test_WebClient.initClient("admin", new Secret("admin".toCharArray()), "localhost", 443, "/escXnat", false);

            // CertificateStore certStore=CertificateStore.getDefault(false);
            Secret secret = new Secret(CertificateStore.DEFAULT_PASSPHRASE.toCharArray());

            String cacertLocation = System.getProperty("user.home") + "/cacerts";

            CertificateStore certStore = CertificateStore.loadCertificateStore(cacertLocation, secret, true, true);
            client.setCertificateStore(certStore);

            // client.setCredentials(null, null);

            client.connect();

            StringHolder resultTextHolder = new StringHolder();
            StringHolder contentEncodingHolder = new StringHolder();

            client.doGet("/", resultTextHolder, contentEncodingHolder);

            outPrintf("> Get Encoding=%s\n", contentEncodingHolder.value);
            outPrintf("---- Get result ----\n%s\n----------------\n", resultTextHolder.value);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void outPrintf(String format, Object... args)
    {
        System.err.printf(format, args);
    }

}
