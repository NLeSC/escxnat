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

package nl.esciencecenter.xnattool.bootstrap;

/**
 * Wrapper class to bootstrap the XnatToolMain class.
 * 
 * @author Piter T. de Boer/Piter.NL
 */
public class startXnatToolMain
{
    public static void main(String[] args)
    {
        Bootstrapper.BootOptions opts = new Bootstrapper.BootOptions();
        opts.toolPrefix = "escxnat";

        Bootstrapper boot = new Bootstrapper(opts);
        String startClass = "nl.esciencecenter.xnattool.XnatToolMain";

        if ((args.length > 0) && (args[0].compareTo("-startClass") == 0))
        {
            if ((args.length <= 1) || (args[1] == null))
            {
                System.err.println("***Error: expected Java Class after -startClass\n");
            }
            startClass = args[1];
            // shift arguments;
            String newArgs[] = new String[args.length - 2];
            for (int i = 0; i < newArgs.length; i++)
            {
                newArgs[i] = args[i + 2];
            }
            args = newArgs;
        }

        try
        {
            boot.launch(startClass, args);
        }
        catch (Exception e)
        {
            System.out.println("***Error: Exception:" + e);
            e.printStackTrace();
        }

    }

}
