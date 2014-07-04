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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;

import nl.esciencecenter.xnattool.ui.UIUtil.UISettings;

public class MessageDialog extends javax.swing.JDialog implements ActionListener
{
	private static final long serialVersionUID = -3135290068915968993L;
	private JPanel topPanel;
	private JPanel mainPanel;
	private JLabel messageLbl;
    private JScrollPane jScrollPane1;
	private JPanel buttonPanel;
	private JTextArea messageTA;
	private JButton[] buttons;
	private String value;

	/**
	 * Auto-generated main method to display this JDialog
	 */
	public static void main(String[] args)
	{
		String buts[]={"yes","no","cancel"};
		String value=MessageDialog.showMessage((JFrame)null,"Warning","This is a Warning",buts);
		System.err.println("Value="+value); 
	}
	
	public MessageDialog(JFrame frame) 
	{
		super(frame);
		String buts[]={"Yes","No","Cancel"};
		init("Warning","This is an important Warning!",buts);
	}
	
	public MessageDialog(String title, String messageText, String[] buttons)
	{
		super((JFrame)null);
		init(title,messageText,buttons);
	}

	public MessageDialog(JFrame parentFrame,String title, String messageText, String[] buttons)
    {
        super(parentFrame);
        init(title,messageText,buttons);
    }
	
	private void init(String title, String messageText, String[] buts)
	{
		initGUI();
		
		this.setTitle(title);
		this.messageLbl.setText(title); 
		this.messageTA.setText(messageText);
		
		this.setLocationRelativeTo(null);
		initButtons(buts);
	}

	private void initButtons(String names[])
	{
		buttons=new JButton[names.length];
		
		int index=0; 
		
		for (String name:names)
		{
			JButton but=new JButton();
			but.setText(name);
			but.setActionCommand(name);
			but.addActionListener(this); 
			buttonPanel.add(but);
			buttons[index] = but; 
			index++;
		}
	}

	private void initGUI()
	{
	    UISettings uiSettings=UIUtil.getUISettings(); 
	    
		try 
		{
			BorderLayout thisLayout = new BorderLayout();
			getContentPane().setLayout(thisLayout);
			{
				mainPanel = new JPanel();
				BorderLayout mainPanelLayout = new BorderLayout();
				mainPanel.setLayout(mainPanelLayout);
				getContentPane().add(mainPanel, BorderLayout.CENTER);
				mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				{
					topPanel = new JPanel();
					FlowLayout topPanelLayout = new FlowLayout();
					topPanel.setLayout(topPanelLayout);
					mainPanel.add(topPanel, BorderLayout.NORTH);
					topPanel.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
					{
						messageLbl = new JLabel();
						FlowLayout warningLabelLayout = new FlowLayout();
						messageLbl.setLayout(warningLabelLayout);
						topPanel.add(messageLbl);
						messageLbl.setText("Please read the following message.");
					}
				}
				{
					buttonPanel = new JPanel();
					mainPanel.add(buttonPanel, BorderLayout.SOUTH);
				
				}
				{
				    messageTA = new JTextArea();
                    mainPanel.add(messageTA);
				    messageTA.setText("Message Text");
				    messageTA.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
				    messageTA.setEditable(false);
				    uiSettings.applySetting(messageTA, UISettings.UIType.INFO_TEXTFIELD); 
				    messageTA.setLineWrap(true); 
				}
			
			}
			this.setSize(444, 204);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		String cmd=e.getActionCommand(); 
		this.value=cmd; 
		this.setVisible(false); 
	}
	
	public String getValue()
	{
		return this.value; 
	}
	
	public static String showMessage(JFrame frame,String title,String text,String buttons[])
	{
		MessageDialog dialog=new MessageDialog(frame,title,text,buttons);
		dialog.setModal(true); 
		dialog.setVisible(true); 
		return dialog.getValue(); 
	}
	
	public static String showWarning(JFrame frame,String title,String text,String buttons[])
	{
		MessageDialog dialog=new MessageDialog(frame,title,text,buttons);
		dialog.setModal(true); 
		dialog.setVisible(true); 
		return dialog.getValue(); 
	}
	
}

