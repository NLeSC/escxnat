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
import nl.esciencecenter.ptk.web.WebClient;
import nl.esciencecenter.ptk.web.WebConfig;

public class Test_WebClient
{

    public static void main(String args[])
    {
        try
        {
            WebClient client = initClient("admin", new Secret("admin".toCharArray()), "localhost", 80, "/escXnat", true);

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

    static WebClient initClient(String user, Secret passwd, String hostname, int port, String servicePath, boolean connect)
            throws Exception
    {
        WebConfig config;

        if (port == 443)
            config = new WebConfig(new java.net.URI("https://" + hostname + ":" + port + servicePath));
        else
            config = new WebConfig(new java.net.URI("http://" + hostname + ":" + port + servicePath));

        config.setCredentials(user, passwd);

        config.setJSessionInitPart("REST/JSESSION");

        WebClient client = new WebClient(config);

        if (connect)
            client.connect();

        String newsession = client.getJSessionID();
        outPrintf("New JSESSIONID=%s\n", newsession);

        return client;
    }
}
