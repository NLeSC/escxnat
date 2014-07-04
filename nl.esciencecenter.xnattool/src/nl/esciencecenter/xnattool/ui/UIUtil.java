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

package nl.esciencecenter.xnattool.ui;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import nl.esciencecenter.ptk.util.logging.ClassLogger;

/**
 * UI Settings and util methods.
 */
public class UIUtil
{
    public static class UISettings
    {
        public static enum UIType
        {
            EDITABLE_TEXTFIELD,
            INFO_TEXTFIELD,
            SECTION_LABEL
        }

        public Color default_bgcolor = Color.WHITE;

        public Color default_fgcolor = Color.BLACK;

        public Color default_textfield_bgcolor = Color.WHITE;

        public Color default_textfield_fgcolor = Color.BLACK;

        public Color editable_textfield_bgcolor = default_textfield_bgcolor;

        public Color editable_textfield_fgcolor = default_textfield_fgcolor;

        public Color info_textfield_bgcolor = new Color(0.95f, 0.95f, 0.95f);

        public Color info_textfield_fgcolor = Color.BLACK;

        public Color section_label_bgcolor = new Color(0.90f, 0.90f, 0.90f);

        public Color section_label_fgcolor = Color.BLACK;

        public void applySetting(JComponent comp, UIType type)
        {
            switch (type)
            {
                case SECTION_LABEL:
                {
                    comp.setForeground(section_label_fgcolor);
                    comp.setBackground(section_label_bgcolor);
                    break;
                }
                case INFO_TEXTFIELD:
                {
                    comp.setForeground(info_textfield_fgcolor);
                    comp.setBackground(info_textfield_bgcolor);
                    break;
                }
                case EDITABLE_TEXTFIELD:
                {
                    comp.setForeground(editable_textfield_fgcolor);
                    comp.setBackground(editable_textfield_bgcolor);
                    break;
                }
                default:
                {
                    ClassLogger.getLogger(UISettings.class).infoPrintf("UIType not supported:%s\n" + type);
                }
            }
        }
    }

    public static enum LookAndFeelType
    {
        NATIVE,
        DEFAULT,
        WINDOWS,
        METAL,
        GTK,
        KDEQT,
        PLASTIC_3D,
        PLASTIC_XP,
        NIMBUS
    };

    private static String nimbusLookAndFeelClassName = null;

    // ========================================================================
    // PLAF Stuff
    // ========================================================================

    private static UISettings uiSettings = new UISettings();

    public static UISettings getUISettings()
    {
        return uiSettings;
    }

    public static boolean switchLookAndFeel(String lafstr)
    {
        return switchLookAndFeel(LookAndFeelType.valueOf(lafstr));
    }

    public static boolean switchLookAndFeel(LookAndFeelType newtype)
    {
        return switchLookAndFeelType(newtype); // switch
    }

    public static boolean switchLookAndFeelType(String lafstr)
    {
        return switchLookAndFeelType(LookAndFeelType.valueOf(lafstr));
    }

    /**
     * Switch Look and Feel. Call this method before creating any Swing GUI. Runtime switching may work, but Windows and
     * Frames must be recreated.
     */
    public static boolean switchLookAndFeelType(LookAndFeelType lafType)
    {
        // Set Look & Feel
        try
        {

            switch (lafType)
            {
                case NATIVE:
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
                case DEFAULT:
                case METAL:
                    javax.swing.UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                    break;
                case WINDOWS:
                    javax.swing.UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                    break;
                case GTK:
                    javax.swing.UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
                    break;
                case KDEQT:
                    // org.freeasinspeech.kdelaf.KdeLAF
                    break;
                case PLASTIC_3D:
                    javax.swing.UIManager.setLookAndFeel("com.jgoodies.looks.plastic.Plastic3DLookAndFeel");
                    break;
                case PLASTIC_XP:
                    javax.swing.UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
                    break;
                case NIMBUS:
                {
                    if (nimbusLookAndFeelClassName == null)
                    {
                        // name of nimbus package may vary
                        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                        {
                            if ("Nimbus".equals(info.getName()))
                            {
                                nimbusLookAndFeelClassName = info.getClassName();
                                break;
                            }
                        }
                    }
                    javax.swing.UIManager.setLookAndFeel(nimbusLookAndFeelClassName);
                    break;
                }
                default:
                    return false;
                    // break;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

}
