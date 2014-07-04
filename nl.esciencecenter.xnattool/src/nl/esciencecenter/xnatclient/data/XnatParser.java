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

package nl.esciencecenter.xnatclient.data;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.ptk.util.logging.ClassLogger;
import nl.esciencecenter.xnatclient.data.XnatObject.XnatObjectType;
import nl.esciencecenter.xnatclient.exceptions.XnatParseException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parse utils.
 * 
 * @author Piter T. de Boer
 */
public class XnatParser
{
    public static ClassLogger logger = ClassLogger.getLogger(ClassLogger.class);

    static
    {
        // logger.setLevelToDebug();
    }

    public static class XnatListParser<T extends XnatObject>
    {
        public XnatListParser()
        {
        }

        public List<T> parseJsonList(String jsonStr)
        {
            return null;
        }

    }

    public static List<? extends XnatObject> parseJsonResult(XnatObjectType type, String jsonStr) throws XnatParseException
    {
        switch (type)
        {
            case XNAT_PROJECT:
                return new XnatListParser<XnatProject>().parseJsonList(jsonStr);
            case XNAT_SESSION:
                return new XnatListParser<XnatSession>().parseJsonList(jsonStr);
            case XNAT_SUBJECT:
                return new XnatListParser<XnatSubject>().parseJsonList(jsonStr);
            case XNAT_RECONSTRUCTION:
                return new XnatListParser<XnatReconstruction>().parseJsonList(jsonStr);
            case XNAT_SCAN:
                return new XnatListParser<XnatScan>().parseJsonList(jsonStr);
            case XNAT_FILE:
                return new XnatListParser<XnatFile>().parseJsonList(jsonStr);
            default:
                throw new Error("Unknown XnatObject type" + type);
        }
    }

    public static int parseJsonResult(XnatObjectType type, String jsonStr, List list) throws XnatParseException
    {
        if (StringUtil.isEmpty(jsonStr))
            return 0;
        try
        {
            JsonFactory jsonFac = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper();

            // use dom like parsing:
            JsonNode tree = mapper.readTree(jsonStr);
            JsonNode rootNode = null;

            JsonNode resultSet = tree.get("ResultSet");
            if (resultSet == null)
            {
                logger.warnPrintf("Couldn't find 'ResultSet' in jsonTree\n");
                // return 0;
            }

            JsonNode result = resultSet.get("Result");
            if (result == null)
            {
                logger.warnPrintf("Couldn't find 'Result' in jsonTree\n");
                return 0;
            }

            if (result.isArray() == false)
            {
                // logger.warnPrintf("Couldn't find 'Result' in jsonTree\n");
                return 0;
            }
            rootNode = result;

            // parse objects:
            Iterator<JsonNode> els = rootNode.elements();
            while (els.hasNext())
            {
                JsonNode el = els.next();
                list.add(parseXnatObject(type, el));
            }
        }
        // wrap exception:
        catch (JsonParseException e)
        {
            throw new XnatParseException("Couldn't parse result:\n" + jsonStr + "\n---\n" + e.getMessage(), e);
        }
        catch (IOException e)
        {
            throw new XnatParseException("IOException:" + e.getMessage(), e);
        }

        return list.size();
    }

    /**
     * Expect a single XnatObject at JsonNode
     * 
     * @return
     */
    public static XnatObject parseXnatObject(XnatObjectType type, JsonNode node)
    {
        Iterator<String> names = node.fieldNames();
        // create new object
        XnatObject obj = XnatObject.create(type);
        logger.debugPrintf(" - New XnatObject:<%s>\n", obj.getObjectType());

        // use headers from XnatObject definition (not json):
        StringList headerList = new StringList(obj.getFieldNames());
        for (int i = 0; i < headerList.size(); i++)
        {
            String name = headerList.get(i);
            JsonNode fieldObj = node.get(name);
            if (fieldObj != null)
            {
                String text = fieldObj.asText();
                logger.debugPrintf(" - - set %s='%s'\n", name, text);
                obj.set(name, text);
            }
        }

        return obj;
    }

    /**
     * Xnat Rest Query has a diferrent Json Tree. Parse structure
     * 
     * @throws XnatParseException
     */
    public static int parseJsonQueryResult(XnatObjectType type, String jsonStr, List list) throws XnatParseException
    {
        if (StringUtil.isEmpty(jsonStr))
            return 0;

        try
        {
            JsonFactory jsonFac = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper();

            // use dom like parsing:
            JsonNode tree = mapper.readTree(jsonStr);

            JsonNode items = tree.get("items");

            if (items == null)
            {
                logger.warnPrintf("Couldn't find 'items' in jsonTree\n");
                return 0;
            }

            // parse objects:
            JsonNode node = null;
            Iterator<JsonNode> els = items.elements();
            int index = 0;
            while (els.hasNext())
            {
                logger.debugPrintf(" - item[%d]\n", index++);
                JsonNode el = els.next();
                node = el.get("data_fields");
                if (node != null)
                    list.add(parseXnatObject(type, node));
                else
                    logger.warnPrintf("jsonNode doesn't have 'data_fields' element:%s", el);
            }
        }
        // wrap exception:
        catch (JsonParseException e)
        {
            throw new XnatParseException("JsonParseException:" + e.getMessage(), e);
        }
        catch (IOException e)
        {
            throw new XnatParseException("IOException:" + e.getMessage(), e);
        }

        return list.size();
    }

    // public static String[] parseCSVHeaders(String headerLine)
    // {
    // String headers[]=headerLine.split(",");
    // for (int i=0;i<headers.length;i++)
    // headers[i]=XnatParser.stripQuotes(headers[i]);
    // return headers;
    // }
    //
    // public static String stripQuotes(String string)
    // {
    // if (string==null)
    // return null;
    //
    // int start=0;
    // int end=string.length();
    // if (string.startsWith("\""))
    // start++;
    // if (string.endsWith("\""))
    // end--;
    // return string.substring(start,end);
    //
    // }

    // public static int parseCSVResult(XnatObjectType type,String
    // csvstring,List list)
    // {
    // if (csvstring==null)
    // return 0;
    //
    // String lines[]=csvstring.split("\n");
    // if (lines.length<=0)
    // return 0;
    // String headerLine=lines[0];
    // if (lines.length<=1)
    // return 0;
    //
    // String headers[]=XnatClient.parseCSVHeaders(headerLine);
    // if (headers.length<=0)
    // return 0;
    //
    // for (int i=1;i<lines.length;i++)
    // {
    // XnatObject obj = XnatObject.create(type);
    //
    // XnatClient.parseCSVObject(obj,headers,lines[i]);
    // list.add(obj);
    // }
    //
    // return list.size();
    // }

    // public static void parseCSVObject(XnatObject obj, String[] headers,
    // String csvLine)
    // {
    // String fields[]=csvLine.split(",");
    // for (int i=0;i<headers.length;i++)
    // {
    // if (i<fields.length)
    // {
    // String value=XnatParser.stripQuotes(fields[i]);
    // obj.set(headers[i],value);
    // }
    // else
    // {
    // logger.warnPrintf("Object '%s' is missing field '%s'\n",obj.getObjectType(),headers[i]);
    // }
    // }
    // }

}
